package com.webunime.mobile.data.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.webunime.mobile.data.toEpisodeKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

enum class EpisodeVote { LIKE, DISLIKE }

data class EpisodeEngagement(
    val likes: Int = 0,
    val dislikes: Int = 0,
    val views: Int = 0,
    val myVote: EpisodeVote? = null,
    val viewed: Boolean = false,
)

/**
 * Like / dislike / view per episode, terikat UID Google.
 * Total di `episodeStats/{id}`; suara & view unik di `users/{uid}/episodeEngagement/{id}`.
 */
class EpisodeEngagementRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val writeMutex = Mutex()
    private val inFlight = mutableSetOf<String>()

    fun statsFlow(slug: String, episode: Double): Flow<EpisodeEngagement> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(EpisodeEngagement())
            awaitClose { }
            return@callbackFlow
        }
        val id = docId(slug, episode)
        val uid = auth?.currentUser?.uid
        var totals = EpisodeEngagement()
        var vote: EpisodeVote? = null
        var viewed = false

        fun emit() {
            trySend(
                totals.copy(myVote = vote, viewed = viewed),
            )
        }

        val statsReg = firestore.collection(COL_STATS).document(id)
            .addSnapshotListener { snap, _ ->
                totals = EpisodeEngagement(
                    likes = (snap?.getLong("likes") ?: 0L).toInt().coerceAtLeast(0),
                    dislikes = (snap?.getLong("dislikes") ?: 0L).toInt().coerceAtLeast(0),
                    views = (snap?.getLong("views") ?: 0L).toInt().coerceAtLeast(0),
                )
                emit()
            }

        val voteReg = if (uid != null) {
            firestore.collection("users").document(uid)
                .collection(COL_MINE).document(id)
                .addSnapshotListener { snap, _ ->
                    vote = when (snap?.getString("vote")) {
                        "like" -> EpisodeVote.LIKE
                        "dislike" -> EpisodeVote.DISLIKE
                        else -> null
                    }
                    viewed = snap?.getBoolean("viewed") == true
                    emit()
                }
        } else {
            null
        }

        emit()
        awaitClose {
            statsReg.remove()
            voteReg?.remove()
        }
    }

    suspend fun recordView(slug: String, episode: Double) {
        val firestore = db ?: return
        val uid = auth?.currentUser?.uid ?: return
        val id = docId(slug, episode)
        if (!acquire("view:$id")) return
        try {
            val mine = firestore.collection("users").document(uid).collection(COL_MINE).document(id)
            val stats = firestore.collection(COL_STATS).document(id)
            runCatching {
                firestore.runTransaction { tx ->
                    val mineSnap = tx.get(mine)
                    if (mineSnap.getBoolean("viewed") == true) return@runTransaction
                    tx.set(
                        mine,
                        mapOf(
                            "slug" to slug,
                            "episode" to episode,
                            "viewed" to true,
                            "viewedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    tx.set(
                        stats,
                        mapOf(
                            "slug" to slug,
                            "episode" to episode,
                            "views" to FieldValue.increment(1),
                        ),
                        SetOptions.merge(),
                    )
                }.await()
            }
        } finally {
            release("view:$id")
        }
    }

    suspend fun toggleLike(slug: String, episode: Double) =
        setVote(slug, episode, EpisodeVote.LIKE)

    suspend fun toggleDislike(slug: String, episode: Double) =
        setVote(slug, episode, EpisodeVote.DISLIKE)

    private suspend fun setVote(slug: String, episode: Double, target: EpisodeVote) {
        val firestore = db ?: return
        val uid = auth?.currentUser?.uid ?: return
        val id = docId(slug, episode)
        if (!acquire("vote:$id")) return
        try {
            val mine = firestore.collection("users").document(uid).collection(COL_MINE).document(id)
            val stats = firestore.collection(COL_STATS).document(id)
            val targetKey = if (target == EpisodeVote.LIKE) "like" else "dislike"
            runCatching {
                firestore.runTransaction { tx ->
                    val mineSnap = tx.get(mine)
                    val prev = mineSnap.getString("vote")?.takeIf { it == "like" || it == "dislike" }
                    var likeDelta = 0L
                    var dislikeDelta = 0L
                    val next: String
                    when {
                        prev == targetKey -> {
                            next = ""
                            if (target == EpisodeVote.LIKE) likeDelta = -1 else dislikeDelta = -1
                        }
                        prev == "like" && target == EpisodeVote.DISLIKE -> {
                            next = "dislike"
                            likeDelta = -1
                            dislikeDelta = 1
                        }
                        prev == "dislike" && target == EpisodeVote.LIKE -> {
                            next = "like"
                            likeDelta = 1
                            dislikeDelta = -1
                        }
                        else -> {
                            next = targetKey
                            if (target == EpisodeVote.LIKE) likeDelta = 1 else dislikeDelta = 1
                        }
                    }
                    tx.set(
                        mine,
                        mapOf(
                            "slug" to slug,
                            "episode" to episode,
                            "vote" to next,
                            "votedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    val statsUpdate = hashMapOf<String, Any>(
                        "slug" to slug,
                        "episode" to episode,
                    )
                    if (likeDelta != 0L) statsUpdate["likes"] = FieldValue.increment(likeDelta)
                    if (dislikeDelta != 0L) statsUpdate["dislikes"] = FieldValue.increment(dislikeDelta)
                    tx.set(stats, statsUpdate, SetOptions.merge())
                }.await()
            }
        } finally {
            release("vote:$id")
        }
    }

    private suspend fun acquire(id: String): Boolean =
        writeMutex.withLock { inFlight.add(id) }

    private suspend fun release(id: String) {
        writeMutex.withLock { inFlight.remove(id) }
    }

    companion object {
        private const val COL_STATS = "episodeStats"
        private const val COL_MINE = "episodeEngagement"

        fun docId(slug: String, episode: Double): String {
            val s = slug.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "_")
            val ep = episode.toEpisodeKey().replace('.', '_')
            return "${s}__$ep"
        }

        fun formatCount(n: Int): String {
            val v = n.coerceAtLeast(0)
            return when {
                v >= 1_000_000 -> String.format("%.1fJt", v / 1_000_000.0).replace(".0Jt", "Jt")
                v >= 1_000 -> String.format("%.1fK", v / 1_000.0).replace(".0K", "K")
                else -> v.toString()
            }
        }
    }
}
