/**
 * Cloud Functions — Weeboonime Mobile
 *
 * Deploy (butuh Firebase Blaze untuk scheduled):
 *   cd functions && npm install
 *   firebase deploy --only functions,firestore:rules
 *
 * Setup:
 *   firebase login
 *   firebase use <projectId>   # myproject-fbbb9
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

const CATALOG_LATEST =
  process.env.CATALOG_LATEST_URL ||
  "https://webunime-catalog-api.vercel.app/v1/anime/latest";

function topicForSlug(slug) {
  const safe = String(slug || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9\-_.~%]/g, "_")
    .slice(0, 80) || "unknown";
  return `anime_${safe}`;
}

function catalogSlug(row) {
  return String(row.anime_slug || row.slug || "").trim();
}

function episodeNum(row) {
  const n = Number(row.episode);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

function titleOf(row) {
  return row.judul || row.nama || catalogSlug(row) || "Anime";
}

/** Grant 1 key after rewarded ad (SSV / trusted client call + rate limit nanti). */
exports.grantKey = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login dulu");
  }
  const uid = context.auth.uid;
  const ref = db.collection("users").doc(uid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const keys = (snap.exists ? snap.get("keys") : 0) || 0;
    tx.set(
      ref,
      { keys: keys + 1, updatedAt: admin.firestore.FieldValue.serverTimestamp() },
      { merge: true },
    );
  });
  return { ok: true };
});

/** Consume 1 key for an episode unless premium. */
exports.consumeKey = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login dulu");
  }
  const uid = context.auth.uid;
  const slug = String(data.slug || "");
  const episode = Number(data.episode || 0);
  const ref = db.collection("users").doc(uid);
  const result = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const isPremium = snap.get("isPremium") === true;
    const premiumUntil = snap.get("premiumUntilMs") || 0;
    const now = Date.now();
    if (isPremium && (!premiumUntil || premiumUntil > now)) {
      return { ok: true, premium: true };
    }
    const keys = snap.get("keys") || 0;
    if (keys < 1) {
      throw new functions.https.HttpsError("failed-precondition", "Kunci habis");
    }
    tx.set(
      ref,
      { keys: keys - 1, updatedAt: admin.firestore.FieldValue.serverTimestamp() },
      { merge: true },
    );
    return { ok: true, premium: false, slug, episode };
  });
  return result;
});

/**
 * Cek feed latest tiap 20 menit → FCM topic anime_<slug> untuk episode baru.
 * State: meta/episodeNotify.episodes = { [slug]: lastEpisode }
 */
exports.notifyNewEpisodes = functions.pubsub
  .schedule("every 20 minutes")
  .timeZone("Asia/Jakarta")
  .onRun(async () => {
    const res = await fetch(CATALOG_LATEST, {
      headers: { "User-Agent": "Weeboonime-Functions/1.0" },
    });
    if (!res.ok) {
      console.error("catalog latest HTTP", res.status);
      return null;
    }
    const body = await res.json();
    const items = Array.isArray(body.items) ? body.items : [];
    if (!items.length) return null;

    const metaRef = db.collection("meta").doc("episodeNotify");
    const metaSnap = await metaRef.get();
    const prev = (metaSnap.exists && metaSnap.get("episodes")) || {};
    const next = { ...prev };
    let sent = 0;

    // Ambil episode tertinggi per slug dari batch feed
    const bySlug = new Map();
    for (const row of items) {
      const slug = catalogSlug(row);
      if (!slug) continue;
      const ep = episodeNum(row);
      const cur = bySlug.get(slug);
      if (!cur || ep > cur.ep) {
        bySlug.set(slug, { ep, row });
      }
    }

    for (const [slug, { ep, row }] of bySlug.entries()) {
      if (ep <= 0) continue;
      const last = Number(prev[slug] || 0);
      if (ep <= last) continue;

      const title = titleOf(row);
      const topic = topicForSlug(slug);
      try {
        await admin.messaging().send({
          topic,
          notification: {
            title: "Episode baru",
            body: `${title} — Episode ${ep}`,
          },
          data: {
            slug,
            episode: String(ep),
            animeTitle: String(title),
            title: "Episode baru",
            body: `${title} — Episode ${ep}`,
          },
          android: {
            priority: "high",
            notification: {
              channelId: "episode_updates",
            },
          },
        });
        next[slug] = ep;
        sent += 1;
      } catch (e) {
        console.error("FCM send failed", topic, e.message || e);
      }
    }

    await metaRef.set(
      {
        episodes: next,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        lastSent: sent,
        source: CATALOG_LATEST,
      },
      { merge: true },
    );
    console.log(`notifyNewEpisodes: sent=${sent} tracked=${Object.keys(next).length}`);
    return null;
  });
