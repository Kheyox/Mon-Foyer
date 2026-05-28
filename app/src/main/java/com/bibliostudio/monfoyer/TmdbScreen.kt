package com.bibliostudio.monfoyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Cinema dark palette constants
private val CinemaBackground = Color(0xFF0F0F0F)
private val CinemaCard = Color(0xFF1C1C1E)
private val CinemaCardDark = Color(0xFF2C2C2E)
private val CinemaAccent = Color(0xFFE5B84D)
private val CinemaTextMuted = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmdbScreen(vm: MonFoyerViewModel) {
    LaunchedEffect(Unit) { vm.loadTmdbHome() }

    val state = vm.state
    val isSearching = state.tmdbSearchQuery.isNotBlank()
    var selectedMedia by remember { mutableStateOf<TmdbMedia?>(null) }
    var showAddBook by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<MediaRequest?>(null) }
    val isAdmin = state.isCurrentUserAdmin()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Search bar
            item {
                Spacer(Modifier.height(12.dp))
                TmdbSearchBar(
                    query = state.tmdbSearchQuery,
                    onQueryChange = { vm.searchTmdb(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            if (isSearching) {
                // Search results
                if (state.tmdbSearching) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CinemaAccent)
                        }
                    }
                } else if (state.tmdbSearchResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎬", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Aucun résultat",
                                    color = CinemaTextMuted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Résultats de recherche",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    item {
                        TmdbSearchGrid(
                            results = state.tmdbSearchResults,
                            onMediaClick = { selectedMedia = it }
                        )
                    }
                }
            } else {
                // Trending section
                if (state.tmdbTrending.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        TmdbSectionHeader(emoji = "🔥", title = "Tendances")
                        Spacer(Modifier.height(10.dp))
                        TmdbHorizontalRow(
                            items = state.tmdbTrending,
                            onItemClick = { selectedMedia = it }
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Popular movies section
                if (state.tmdbPopularMovies.isNotEmpty()) {
                    item {
                        TmdbSectionHeader(emoji = "🎬", title = "Films populaires")
                        Spacer(Modifier.height(10.dp))
                        TmdbHorizontalRow(
                            items = state.tmdbPopularMovies,
                            onItemClick = { selectedMedia = it }
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Popular TV section
                if (state.tmdbPopularTv.isNotEmpty()) {
                    item {
                        TmdbSectionHeader(emoji = "📺", title = "Séries populaires")
                        Spacer(Modifier.height(10.dp))
                        TmdbHorizontalRow(
                            items = state.tmdbPopularTv,
                            onItemClick = { selectedMedia = it }
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // Loading state when nothing loaded yet
                if (state.tmdbTrending.isEmpty() && state.tmdbPopularMovies.isEmpty() && state.tmdbPopularTv.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CinemaAccent)
                        }
                    }
                }
            }

            // Divider and household requests section
            item {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF2C2C2E))
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Demandes du foyer",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Add book button
                    Surface(
                        color = CinemaCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { showAddBook = true }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Ajouter un livre",
                                tint = CinemaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("📚 Livre", color = CinemaAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.mediaRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 32.dp)
                        ) {
                            Text("🎬", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aucune demande",
                                color = CinemaTextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Cherche un film, une série ou un livre à demander",
                                color = Color(0xFF6E6E73),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(state.mediaRequests) { request ->
                    TmdbRequestCard(
                        request = request,
                        canModerate = isAdmin && request.status == "pending",
                        canDelete = isAdmin || request.requesterId == state.currentUserId,
                        onApprove = { vm.updateMediaRequestStatus(request, "approved") },
                        onReject = { vm.updateMediaRequestStatus(request, "rejected") },
                        onDelete = { confirmDelete = request }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // Detail bottom sheet
    selectedMedia?.let { media ->
        TmdbDetailSheet(
            media = media,
            providers = state.tmdbDetailProviders,
            alreadyRequested = state.mediaRequests.any { it.tmdbId == media.id && it.status != "rejected" },
            onDismiss = { selectedMedia = null },
            onRequest = {
                vm.addTmdbRequest(media)
                selectedMedia = null
            },
            onLoadProviders = { vm.loadProviders(media.id, media.mediaType) }
        )
    }

    // Add book sheet
    if (showAddBook) {
        AddMediaRequestSheet(
            kind = "Livre",
            onDismiss = { showAddBook = false },
            onAdd = { title ->
                vm.addMediaRequest(title, "Livre")
                showAddBook = false
            }
        )
    }

    // Confirm delete dialog
    confirmDelete?.let { request ->
        ConfirmDeleteDialog(
            title = "Supprimer la demande ?",
            message = "${request.title} sera retiré de la liste.",
            onConfirm = {
                vm.delete("requests", request.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun TmdbSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Chercher un film, une série…",
                color = CinemaTextMuted,
                fontSize = 16.sp
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = CinemaTextMuted,
                modifier = Modifier.size(22.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CinemaAccent,
            unfocusedBorderColor = Color(0xFF3A3A3C),
            focusedContainerColor = CinemaCard,
            unfocusedContainerColor = CinemaCard,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = CinemaAccent
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun TmdbSectionHeader(emoji: String, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(4.dp))
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Tout voir →",
            color = CinemaAccent,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TmdbHorizontalRow(
    items: List<TmdbMedia>,
    onItemClick: (TmdbMedia) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { media ->
            TmdbPosterCard(
                media = media,
                width = 120.dp,
                height = 180.dp,
                onClick = { onItemClick(media) }
            )
        }
    }
}

@Composable
private fun TmdbSearchGrid(
    results: List<TmdbMedia>,
    onMediaClick: (TmdbMedia) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val cardWidth = (screenWidthDp - 48.dp) / 2
    val cardHeight = cardWidth * 1.5f

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
        modifier = Modifier.height(
            ((results.size + 1) / 2 * (cardHeight.value.toInt() + 10) + 16).dp
        )
    ) {
        gridItems(results) { media ->
            TmdbPosterCard(
                media = media,
                width = cardWidth,
                height = cardHeight,
                onClick = { onMediaClick(media) }
            )
        }
    }
}

@Composable
private fun TmdbPosterCard(
    media: TmdbMedia,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(CinemaCardDark)
            .clickable(onClick = onClick)
    ) {
        if (media.posterUrl.isNotEmpty()) {
            AsyncImage(
                model = media.posterUrl,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (media.mediaType == "tv") "📺" else "🎬",
                    fontSize = 36.sp
                )
            }
        }

        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // Rating badge
        if (media.voteAverage > 0) {
            Surface(
                color = CinemaAccent,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Text(
                    media.ratingDisplay,
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TmdbDetailSheet(
    media: TmdbMedia,
    providers: List<TmdbProvider>,
    alreadyRequested: Boolean,
    onDismiss: () -> Unit,
    onRequest: () -> Unit,
    onLoadProviders: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(media.id) { onLoadProviders() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Backdrop image with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (media.backdropUrl.isNotEmpty()) {
                    AsyncImage(
                        model = media.backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (media.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CinemaCardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (media.mediaType == "tv") "📺" else "🎬",
                            fontSize = 64.sp
                        )
                    }
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                ),
                                startY = 80f
                            )
                        )
                )

                // Title and metadata at bottom of image
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        media.title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (media.year.isNotEmpty()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    media.year,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Surface(
                            color = CinemaAccent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                if (media.mediaType == "tv") "Série" else "Film",
                                color = CinemaAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Content below image
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Poster + overview row
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Small poster
                    if (media.posterUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CinemaCardDark)
                        ) {
                            AsyncImage(
                                model = media.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Overview + rating
                    Column(modifier = Modifier.weight(1f)) {
                        if (media.voteAverage > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    media.ratingDisplay,
                                    color = CinemaAccent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "/ 10",
                                    color = CinemaTextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (media.overview.isNotEmpty()) {
                            Text(
                                media.overview,
                                color = CinemaTextMuted,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Watch providers
                if (providers.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "📺 Disponible sur",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(providers) { provider ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (provider.logoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = provider.logoUrl,
                                        contentDescription = provider.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CinemaCardDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📺", fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    provider.name,
                                    color = CinemaTextMuted,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Request button
                if (alreadyRequested) {
                    Surface(
                        color = Color(0xFF3A3A3C),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Déjà demandé ✓",
                                color = CinemaTextMuted,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                } else {
                    Surface(
                        color = DeepGreen,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clickable(onClick = onRequest)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Demander au foyer",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TmdbRequestCard(
    request: MediaRequest,
    canModerate: Boolean,
    canDelete: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (request.status) {
        "approved" -> Color(0xFF34C759)
        "rejected" -> Color(0xFFFF453A)
        else -> CinemaAccent
    }
    val statusText = when (request.status) {
        "approved" -> "Validé"
        "rejected" -> "Refusé"
        else -> "En attente"
    }

    Surface(
        color = CinemaCard,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Poster thumbnail or emoji
                if (request.posterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = request.posterUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .width(50.dp)
                            .height(75.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(75.dp)
                            .background(CinemaCardDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (request.kind == "Livre") "📚" else "🎬",
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        request.title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        request.requesterName.ifBlank { "Membre" },
                        color = CinemaTextMuted,
                        fontSize = 13.sp
                    )
                    if (request.year.isNotEmpty()) {
                        Text(
                            request.year,
                            color = CinemaTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Status badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Moderate / delete buttons
            if (canModerate || canDelete) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (canModerate) {
                        Surface(
                            color = Color(0xFF34C759).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable(onClick = onApprove)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Valider",
                                    color = Color(0xFF34C759),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFFFF453A).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable(onClick = onReject)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Refuser",
                                    color = Color(0xFFFF453A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    if (canDelete) {
                        Surface(
                            color = Color(0xFF3A3A3C),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable(onClick = onDelete)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", color = CinemaTextMuted, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
