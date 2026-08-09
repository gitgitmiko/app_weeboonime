package com.webunime.mobile.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.AnimeDetail
import com.webunime.mobile.ui.player.PlayerActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    slug: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.catalogApi.anime(slug) }
                .onSuccess { detail = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(slug) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.displayTitle() ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            loading && detail == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            error != null && detail == null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "")
                    TextButton(onClick = { reload() }) { Text("Coba lagi") }
                }
            }

            else -> {
                val data = detail ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            AsyncImage(
                                model = data.thumbnail,
                                contentDescription = data.displayTitle(),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(0.38f)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Column(modifier = Modifier.weight(0.62f)) {
                                Text(data.displayTitle(), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                data.rating?.let {
                                    Text("â˜… $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                data.genre?.takeIf { it.isNotEmpty() }?.let {
                                    Text(
                                        it.joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                data.episodes_count?.let {
                                    Text("$it episode", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    data.sinopsis?.takeIf { it.isNotBlank() }?.let { syn ->
                        item {
                            Text("Sinopsis", style = MaterialTheme.typography.titleMedium)
                            Text(syn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    item { Text("Episode", style = MaterialTheme.typography.titleMedium) }
                    items(data.episodes.asReversed()) { ep ->
                        val n = ep.episode ?: return@items
                        Button(
                            onClick = {
                                val i = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra(PlayerActivity.EXTRA_SLUG, slug)
                                    putExtra(PlayerActivity.EXTRA_EPISODE, n)
                                    putExtra(PlayerActivity.EXTRA_TITLE, data.displayTitle())
                                }
                                context.startActivity(i)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(ep.title?.takeIf { it.isNotBlank() } ?: "Episode $n")
                        }
                    }
                }
            }
        }
    }
}
