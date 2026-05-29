package com.bibliostudio.monfoyer

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.parseColor("#103F37")
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent { MonFoyerApp() }
    }
}

@Composable
fun MonFoyerApp(vm: MonFoyerViewModel = viewModel()) {
    val colors = lightColorScheme(
        primary = DeepGreen,
        secondary = Leaf,
        tertiary = Coral,
        background = Cream,
        surface = Color.White,
        surfaceVariant = SoftGrey,
        onPrimary = Color.White,
        onSurface = Ink
    )
    val view = LocalView.current
    SideEffect {
        (view.context as? ComponentActivity)?.window?.apply {
            statusBarColor = android.graphics.Color.parseColor("#103F37")
            WindowCompat.getInsetsController(this, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
            when {
                vm.state.loading -> SplashScreen()
                !vm.state.signedIn -> SignInScreen(vm)
                vm.state.household == null -> HouseholdGate(vm)
                else -> HomeShell(vm)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Mon Foyer", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text("Chargement…", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
fun SignInScreen(vm: MonFoyerViewModel) {
    val context = LocalContext.current as ComponentActivity
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo()
        Spacer(Modifier.height(36.dp))
        Text("Bienvenue dans ton foyer", fontSize = 38.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(12.dp))
        Text("Courses, agenda, taches et petites notes au meme endroit.", fontSize = 18.sp, color = Muted)
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = "Continuer avec Google", icon = Icons.Filled.Group) {
            scope.launch { vm.signInWithGoogle(context, context.getString(R.string.web_client_id)) }
        }
        vm.state.error?.let { ErrorText(it) }
    }
}

@Composable
fun HouseholdGate(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("Mon foyer") }
    var code by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        item {
            BrandLogo()
            Spacer(Modifier.height(28.dp))
            Text("Ton espace commun", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("En trois minutes, ton foyer est pret a partager courses, taches, agendas et demandes.", fontSize = 17.sp, lineHeight = 22.sp, color = Muted)
            Spacer(Modifier.height(18.dp))
            OnboardingStep("1", "Cree ou rejoins un foyer prive", "Chaque foyer a son code d'invitation.")
            OnboardingStep("2", "Invite les membres", "Chaque membre garde sa couleur dans toute l'app.")
            OnboardingStep("3", "Organisez sans friction", "Les actions importantes alimentent l'historique.")
            Spacer(Modifier.height(22.dp))
            SoftInput(value = name, onValueChange = { name = it }, label = "Nom du foyer", leadingIcon = Icons.Filled.Home)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Creer mon foyer", icon = Icons.Filled.Home) { vm.createHousehold(name) }
            Spacer(Modifier.height(24.dp))
            SoftInput(value = code, onValueChange = { code = it }, label = "Code d'invitation", leadingIcon = Icons.Filled.Group)
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = "Rejoindre", icon = Icons.Filled.Group) { vm.joinHousehold(code) }
            vm.state.error?.let { ErrorText(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun OnboardingStep(number: String, title: String, body: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Mint, shape = CircleShape, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number, color = DeepGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(body, fontSize = 13.sp, color = Muted, lineHeight = 16.sp)
            }
        }
    }
}

private val PRIMARY_NAV_TABS = listOf(Tab.Home, Tab.Tasks, Tab.Shopping, Tab.Calendar)
private val EXTRA_TABS = listOf(
    Tab.Requests, Tab.Activity, Tab.Birthdays, Tab.Notes,
    Tab.Members, Tab.Budget, Tab.Expenses, Tab.Recipes, Tab.Contacts
)

@Composable
fun HomeShell(vm: MonFoyerViewModel) {
    val context = LocalContext.current as ComponentActivity
    NotificationPermissionEffect()
    LaunchedEffect(Unit) {
        vm.setAppContext(context)
        vm.checkForUpdate(context, notify = true)
    }
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { vm.setOffline(false) }
            override fun onLost(network: Network) { vm.setOffline(true) }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        vm.setOffline(!isConnected)
        onDispose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }
    LaunchedEffect(vm.state.events, vm.state.tasks, vm.state.birthdays) {
        ReminderScheduler.refresh(context, vm.state.events, vm.state.tasks, vm.state.birthdays)
    }
    MediaRequestNotificationEffect(vm.state)
    BackHandler(enabled = vm.state.selectedTab != Tab.Home) {
        vm.select(Tab.Home)
    }
    val isDark = vm.state.darkMode || vm.state.selectedTab == Tab.Requests
    val bgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF0F0F0F) else Cream,
        animationSpec = tween(300), label = "bg"
    )
    var moreNavOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalAppDarkMode provides isDark) {
        Box(Modifier.fillMaxSize().background(bgColor).systemBarsPadding()) {
            Column(Modifier.fillMaxSize()) {
                AppHeader(
                    activeTab = vm.state.selectedTab,
                    householdName = vm.state.household?.name ?: "Mon Foyer",
                    isOffline = vm.state.isOffline,
                    isDark = isDark,
                    darkMode = vm.state.darkMode,
                    onToggleDark = { vm.toggleDarkMode() },
                    onCheckUpdate = { vm.checkForUpdate(context) },
                    onSignOut = { vm.signOut(context) }
                )
                vm.state.error?.let { ErrorText(it, Modifier.padding(horizontal = 24.dp)) }
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = vm.state.selectedTab,
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { fullWidth -> fullWidth * direction }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { fullWidth -> -fullWidth * direction }
                            )
                        },
                        label = "tab-slide"
                    ) { tab ->
                        when (tab) {
                            Tab.Home -> Dashboard(vm)
                            Tab.Shopping -> ShoppingScreen(vm)
                            Tab.Tasks -> TasksScreen(vm)
                            Tab.Calendar -> AgendaScreen(vm)
                            Tab.Requests -> TmdbScreen(vm)
                            Tab.Activity -> ActivityScreen(vm)
                            Tab.Birthdays -> BirthdaysScreen(vm)
                            Tab.Notes -> NotesScreen(vm)
                            Tab.Members -> MembersScreen(vm)
                            Tab.Budget -> BudgetScreen(vm)
                            Tab.Expenses -> ExpensesScreen(vm)
                            Tab.Recipes -> RecipesScreen(vm)
                            Tab.Contacts -> ContactsScreen(vm)
                        }
                    }
                    FoyerSnackbar(
                        event = vm.snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                AppBottomNav(
                    selected = vm.state.selectedTab,
                    isDark = isDark,
                    darkMode = vm.state.darkMode,
                    moreOpen = moreNavOpen,
                    onMoreOpenChange = { moreNavOpen = it },
                    onSelect = { vm.select(it) },
                    onCheckUpdate = { vm.checkForUpdate(context) },
                    onSignOut = { vm.signOut(context) },
                    onToggleDark = { vm.toggleDarkMode() }
                )
            }
            vm.state.updateInfo?.let { update ->
                UpdateAvailableDialog(update = update, onDismiss = { vm.clearUpdateInfo() })
            }
        }
    }
}

@Composable
fun AppBottomNav(
    selected: Tab,
    isDark: Boolean,
    darkMode: Boolean,
    moreOpen: Boolean,
    onMoreOpenChange: (Boolean) -> Unit,
    onSelect: (Tab) -> Unit,
    onCheckUpdate: () -> Unit,
    onSignOut: () -> Unit,
    onToggleDark: () -> Unit
) {
    val navBgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1C1C1E) else Color.White,
        animationSpec = tween(300), label = "nav-bg"
    )
    Surface(color = navBgColor, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PRIMARY_NAV_TABS.forEach { tab ->
                key(tab) {
                    val isSelected = selected == tab
                    val itemTint by animateColorAsState(
                        targetValue = when {
                            isSelected -> DeepGreen
                            isDark -> Color(0xFF9E9E9E)
                            else -> Muted
                        },
                        animationSpec = tween(200), label = "nav-${tab.name}"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(tab) }
                            .padding(vertical = 10.dp)
                    ) {
                        Icon(tab.icon, contentDescription = tab.label, tint = itemTint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tab.label,
                            fontSize = 10.sp,
                            color = itemTint,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // "Plus" — dropdown pour tous les autres onglets
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().wrapContentSize(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                val isExtraSelected = EXTRA_TABS.contains(selected)
                val plusTint by animateColorAsState(
                    targetValue = if (isExtraSelected) DeepGreen else if (isDark) Color(0xFF9E9E9E) else Muted,
                    animationSpec = tween(200), label = "nav-plus"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onMoreOpenChange(true) }
                        .padding(vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Plus", tint = plusTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Plus",
                        fontSize = 10.sp,
                        color = plusTint,
                        fontWeight = if (isExtraSelected) FontWeight.Black else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = moreOpen,
                    onDismissRequest = { onMoreOpenChange(false) },
                    containerColor = Color.White
                ) {
                    EXTRA_TABS.forEach { tab ->
                        DropdownMenuItem(
                            text = { Text(tab.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                            leadingIcon = { Icon(tab.icon, contentDescription = null, tint = DeepGreen) },
                            onClick = { onMoreOpenChange(false); onSelect(tab) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (darkMode) "Mode clair" else "Mode sombre", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                        leadingIcon = { Text(if (darkMode) "☀️" else "🌙", fontSize = 18.sp) },
                        onClick = { onMoreOpenChange(false); onToggleDark() }
                    )
                    DropdownMenuItem(
                        text = { Text("Mise a jour", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreen) },
                        onClick = { onMoreOpenChange(false); onCheckUpdate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Deconnexion", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                        leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = Muted) },
                        onClick = { onMoreOpenChange(false); onSignOut() }
                    )
                }
            }
        }
    }
}

@Composable
fun AppHeader(
    activeTab: Tab,
    householdName: String,
    isOffline: Boolean = false,
    isDark: Boolean = false,
    darkMode: Boolean = false,
    onToggleDark: () -> Unit = {},
    onCheckUpdate: () -> Unit,
    onSignOut: () -> Unit
) {
    val textColor by animateColorAsState(if (isDark) Color.White else Ink, tween(300), label = "text")
    val mutedColor by animateColorAsState(if (isDark) Color(0xFF9E9E9E) else Muted, tween(300), label = "muted")
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandIcon(size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(
                if (activeTab == Tab.Home) "Mon Foyer" else activeTab.label,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            if (activeTab == Tab.Home) {
                Text(householdName, color = mutedColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (isOffline) {
            Surface(color = Color(0xFF8B0000), shape = RoundedCornerShape(50)) {
                Text("🔌", fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        Box {
            RoundIconButton(icon = Icons.Filled.MoreVert, tint = if (isDark) Color.White else DeepGreen, onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Color.White) {
                DropdownMenuItem(
                    text = { Text(if (darkMode) "Mode clair" else "Mode sombre", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                    leadingIcon = { Text(if (darkMode) "☀️" else "🌙", fontSize = 18.sp) },
                    onClick = { menuOpen = false; onToggleDark() }
                )
                DropdownMenuItem(
                    text = { Text("Mise a jour", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreen) },
                    onClick = { menuOpen = false; onCheckUpdate() }
                )
                DropdownMenuItem(
                    text = { Text("Deconnexion", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                    leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = Muted) },
                    onClick = { menuOpen = false; onSignOut() }
                )
            }
        }
    }
}

@Composable
fun UpdateAvailableDialog(update: UpdateInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mise a jour disponible", fontWeight = FontWeight.Black, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version ${update.versionName}", fontWeight = FontWeight.Bold, color = DeepGreen)
                Text(update.notes.ifBlank { "Une nouvelle version de Mon Foyer est disponible." }, color = Muted)
                Text("Android demandera une confirmation avant l'installation.", color = Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
            ) {
                Text("Installer")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SoftGrey, contentColor = Ink)
            ) {
                Text("Plus tard")
            }
        },
        containerColor = Color.White
    )
}
