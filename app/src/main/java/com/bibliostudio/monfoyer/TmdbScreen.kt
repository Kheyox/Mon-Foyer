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
    LaunchedEffect("tmdb") { vm.loadTmdbHome() }
    LaunchedEffect("books") { vm.loadBooksHome() }

    val state = vm.state
    val isSearching = state.tmdbSearchQuery.isNotBlank()
    var selectedMedia by remember { mutableStateOf<TmdbMedia?>(null) }
    var selectedBook by remember { mutableStateOf<GoogleBook?>(null) }
    var confirmDelete by remember { mutableStateOf<MediaRequest?>(null) }
    val isAdmin = state.isCurrentUserAdmin()
    var selectedTab by remember { mutableStateOf(0) } // 0 = films, 1 = livres
    var expandedSection by remember { mutableStateOf<String?>(null) }

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
            // Tab chips
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabChip(
                        text = "🎬 Films & Séries",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    TabChip(
                        text = "📚 Livres",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (selectedTab == 0) {
                // Search bar
                item {
                    Spacer(Modifier.height(4.dp))
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
                            TmdbSectionHeader(
                                emoji = "🔥", title = "Tendances",
                                expanded = expandedSection == "trending",
                                onToggle = { expandedSection = if (expandedSection == "trending") null else "trending" }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (expandedSection == "trending") {
                            items(state.tmdbTrending.chunked(2)) { row ->
                                TmdbGridRow(row) { selectedMedia = it }
                            }
                        } else {
                            item {
                                TmdbHorizontalRow(items = state.tmdbTrending, onItemClick = { selectedMedia = it })
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    // Popular movies section
                    if (state.tmdbPopularMovies.isNotEmpty()) {
                        item {
                            TmdbSectionHeader(
                                emoji = "🎬", title = "Films populaires",
                                expanded = expandedSection == "movies",
                                onToggle = { expandedSection = if (expandedSection == "movies") null else "movies" }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (expandedSection == "movies") {
                            items(state.tmdbPopularMovies.chunked(2)) { row ->
                                TmdbGridRow(row) { selectedMedia = it }
                            }
                        } else {
                            item {
                                TmdbHorizontalRow(items = state.tmdbPopularMovies, onItemClick = { selectedMedia = it })
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }

                    // Popular TV section
                    if (state.tmdbPopularTv.isNotEmpty()) {
                        item {
                            TmdbSectionHeader(
                                emoji = "📺", title = "Séries populaires",
                                expanded = expandedSection == "tv",
                                onToggle = { expandedSection = if (expandedSection == "tv") null else "tv" }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (expandedSection == "tv") {
                            items(state.tmdbPopularTv.chunked(2)) { row ->
                                TmdbGridRow(row) { selectedMedia = it }
                            }
                        } else {
                            item {
                                TmdbHorizontalRow(items = state.tmdbPopularTv, onItemClick = { selectedMedia = it })
                            }
                        }
                        item { Spacer(Modifier.height(20.dp)) }
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

                // Divider and household requests section (films tab — all non-book requests)
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF2C2C2E))
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Demandes du foyer",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                val filmRequests = state.mediaRequests.filter { it.kind != "Livre" }
                if (filmRequests.isEmpty()) {
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
                                    "Cherche un film ou une série à demander",
                                    color = Color(0xFF6E6E73),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(filmRequests) { request ->
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
            } else {
                // ── Books tab ──────────────────────────────────────────────
                val isBooksSearching = state.booksSearchQuery.isNotBlank()

                // Books search bar
                item {
                    Spacer(Modifier.height(4.dp))
                    BooksSearchBar(
                        query = state.booksSearchQuery,
                        onQueryChange = { vm.searchBooks(it) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (isBooksSearching) {
                    if (state.booksSearching) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = CinemaAccent)
                            }
                        }
                    } else if (state.booksSearchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📚", fontSize = 48.sp)
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
                            BooksSearchGrid(
                                results = state.booksSearchResults,
                                onBookClick = { selectedBook = it }
                            )
                        }
                    }
                } else {
                    // Browse categories
                    if (state.booksPopularRomans.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            TmdbSectionHeader(emoji = "📚", title = "Romans")
                            Spacer(Modifier.height(10.dp))
                            BooksHorizontalRow(
                                books = state.booksPopularRomans,
                                onBookClick = { selectedBook = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    if (state.booksPopularScifi.isNotEmpty()) {
                        item {
                            TmdbSectionHeader(emoji = "🚀", title = "Science-Fiction")
                            Spacer(Modifier.height(10.dp))
                            BooksHorizontalRow(
                                books = state.booksPopularScifi,
                                onBookClick = { selectedBook = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    if (state.booksPopularThriller.isNotEmpty()) {
                        item {
                            TmdbSectionHeader(emoji = "🔪", title = "Thrillers")
                            Spacer(Modifier.height(10.dp))
                            BooksHorizontalRow(
                                books = state.booksPopularThriller,
                                onBookClick = { selectedBook = it }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    if (state.booksPopularRomans.isEmpty() && state.booksPopularScifi.isEmpty() && state.booksPopularThriller.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.booksLoading) {
                                    CircularProgressIndicator(color = CinemaAccent)
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("📡", fontSize = 48.sp)
                                        Text(
                                            "Impossible de charger les livres",
                                            color = CinemaTextMuted,
                                            fontSize = 15.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )
                                        Surface(
                                            color = CinemaAccent,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.clickable { vm.loadBooksHome() }
                                        ) {
                                            Text(
                                                "Réessayer",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Book requests section
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF2C2C2E))
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Demandes de livres",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                val bookRequests = state.mediaRequests.filter { it.kind == "Livre" }
                if (bookRequests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 32.dp)
                            ) {
                                Text("📚", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Aucune demande",
                                    color = CinemaTextMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Cherche un livre à demander au foyer",
                                    color = Color(0xFF6E6E73),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(bookRequests) { request ->
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
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // Detail bottom sheet (films)
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

    // Detail bottom sheet (books)
    selectedBook?.let { book ->
        BookDetailSheet(
            book = book,
            alreadyRequested = state.mediaRequests.any { it.title == book.title && it.kind == "Livre" && it.status != "rejected" },
            onDismiss = { selectedBook = null },
            onRequest = {
                vm.addBookRequest(book)
                selectedBook = null
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
private fun TmdbSectionHeader(
    emoji: String,
    title: String,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier),
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
        if (onToggle != null) {
            Text(
                if (expanded) "Réduire ↑" else "Tout voir →",
                color = CinemaAccent,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TmdbGridRow(row: List<TmdbMedia>, onItemClick: (TmdbMedia) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        row.forEach { media ->
            Box(modifier = Modifier.weight(1f)) {
                TmdbPosterCard(
                    media = media,
                    width = 0.dp,
                    height = 190.dp,
                    onClick = { onItemClick(media) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (width > 0.dp) Modifier.width(width) else Modifier)
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
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) CinemaAccent else CinemaCard,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text,
            color = if (selected) Color(0xFF1C1C1E) else CinemaTextMuted,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun BooksSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Chercher un livre, un auteur…",
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
private fun BooksHorizontalRow(
    books: List<GoogleBook>,
    onBookClick: (GoogleBook) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(books) { book ->
            BookCoverCard(book = book, onClick = { onBookClick(book) })
        }
    }
}

@Composable
private fun BooksSearchGrid(
    results: List<GoogleBook>,
    onBookClick: (GoogleBook) -> Unit
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
        gridItems(results) { book ->
            BookCoverCard(
                book = book,
                onClick = { onBookClick(book) },
                overrideWidth = cardWidth,
                overrideHeight = cardHeight
            )
        }
    }
}

@Composable
private fun BookCoverCard(
    book: GoogleBook,
    onClick: () -> Unit,
    overrideWidth: androidx.compose.ui.unit.Dp = 110.dp,
    overrideHeight: androidx.compose.ui.unit.Dp = 165.dp
) {
    Surface(
        color = CinemaCard,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(overrideWidth)
            .height(overrideHeight)
            .clickable(onClick = onClick)
    ) {
        Box {
            if (book.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(CinemaCardDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📚", fontSize = 32.sp)
                }
            }
            // Bottom gradient + title
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xDD000000))
                        )
                    )
                    .padding(6.dp)
            ) {
                Text(
                    book.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Rating badge if available
            if (book.averageRating > 0) {
                Surface(
                    color = CinemaAccent,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        book.ratingDisplay,
                        color = Color(0xFF1C1C1E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailSheet(
    book: GoogleBook,
    alreadyRequested: Boolean,
    onDismiss: () -> Unit,
    onRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaCard
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            item {
                // Cover + info row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Book cover
                    Surface(
                        color = CinemaCardDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(100.dp)
                            .height(150.dp)
                    ) {
                        if (book.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📚", fontSize = 40.sp)
                            }
                        }
                    }
                    // Info
                    Column(Modifier.weight(1f)) {
                        Text(
                            book.title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            book.authorsDisplay,
                            color = CinemaAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (book.year.isNotEmpty()) {
                            Text(book.year, color = CinemaTextMuted, fontSize = 13.sp)
                        }
                        if (book.pageCount > 0) {
                            Text("${book.pageCount} pages", color = CinemaTextMuted, fontSize = 13.sp)
                        }
                        if (book.publisher.isNotEmpty()) {
                            Text(
                                book.publisher,
                                color = CinemaTextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (book.ratingDisplay.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = CinemaAccent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    book.ratingDisplay,
                                    color = Color(0xFF1C1C1E),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Description
                if (book.description.isNotEmpty()) {
                    Text(
                        "Synopsis",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        book.description,
                        color = CinemaTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(20.dp))
                }
                // Request button
                Surface(
                    color = if (alreadyRequested) CinemaCard else Color(0xFF174C43),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(enabled = !alreadyRequested, onClick = onRequest)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (alreadyRequested) "✓ Déjà demandé" else "📚 Demander au foyer",
                            color = if (alreadyRequested) CinemaTextMuted else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
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
