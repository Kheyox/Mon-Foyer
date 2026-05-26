package com.bibliostudio.monfoyer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/Kheyox/Mon-Foyer/main/update.json"

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
    androidx.compose.runtime.SideEffect {
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
        modifier = Modifier
            .fillMaxSize()
            .background(DeepGreen),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mon Foyer",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Chargement…",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(40.dp)
            )
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
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
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

@Composable
fun HomeShell(vm: MonFoyerViewModel) {
    val context = LocalContext.current as ComponentActivity
    NotificationPermissionEffect()
    LaunchedEffect(Unit) {
        vm.setAppContext(context)
        vm.checkForUpdate(context, silent = true, notify = true)
    }
    LaunchedEffect(vm.state.events, vm.state.tasks, vm.state.birthdays) {
        ReminderScheduler.refresh(context, vm.state.events, vm.state.tasks, vm.state.birthdays)
    }
    MediaRequestNotificationEffect(vm.state)
    BackHandler(enabled = vm.state.selectedTab != Tab.Home) {
        vm.select(Tab.Home)
    }
    Box(Modifier.fillMaxSize().background(Cream).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                activeTab = vm.state.selectedTab,
                householdName = vm.state.household?.name ?: "Mon Foyer",
                onHome = { vm.select(Tab.Home) },
                onSelect = { vm.select(it) },
                onCheckUpdate = { vm.checkForUpdate(context) },
                onSignOut = { vm.signOut(context) }
            )
            vm.state.error?.let { ErrorText(it, Modifier.padding(horizontal = 24.dp)) }
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
                    Tab.Requests -> RequestsScreen(vm)
                    Tab.Activity -> ActivityScreen(vm)
                    Tab.Birthdays -> BirthdaysScreen(vm)
                    Tab.Notes -> NotesScreen(vm)
                    Tab.Members -> MembersScreen(vm)
                }
            }
        }
        FloatingHomeButton(visible = vm.state.selectedTab != Tab.Home, onClick = { vm.select(Tab.Home) })
        vm.state.updateInfo?.let { update ->
            UpdateAvailableDialog(
                update = update,
                onDismiss = { vm.clearUpdateInfo() }
            )
        }
    }
}

@Composable
fun AppHeader(
    activeTab: Tab,
    householdName: String,
    onHome: () -> Unit,
    onSelect: (Tab) -> Unit,
    onCheckUpdate: () -> Unit,
    onSignOut: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BrandLogo(Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Box {
                RoundIconButton(icon = Icons.Filled.MoreVert, tint = DeepGreen, onClick = { menuOpen = true })
                HomeMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onSelect = {
                        menuOpen = false
                        onSelect(it)
                    },
                    onCheckUpdate = {
                        menuOpen = false
                        onCheckUpdate()
                    },
                    onSignOut = {
                        menuOpen = false
                        onSignOut()
                    }
                )
            }
            Spacer(Modifier.width(10.dp))
            RoundIconButton(icon = Icons.Filled.Logout, tint = Muted, onClick = onSignOut)
        }
        Spacer(Modifier.height(16.dp))
        if (activeTab != Tab.Home) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = DeepGreen, shape = RoundedCornerShape(50), shadowElevation = 2.dp, modifier = Modifier.clickable { onHome() }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(activeTab.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(activeTab.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            Surface(color = Paper.copy(alpha = 0.78f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, CardBorder)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Leaf))
                    Spacer(Modifier.width(8.dp))
                    Text(householdName, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun HomeMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Tab) -> Unit,
    onCheckUpdate: () -> Unit,
    onSignOut: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Color.White) {
        listOf(Tab.Home, Tab.Calendar, Tab.Tasks, Tab.Shopping, Tab.Requests, Tab.Activity, Tab.Birthdays, Tab.Notes, Tab.Members).forEach { tab ->
            DropdownMenuItem(
                text = { Text(tab.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                leadingIcon = { Icon(tab.icon, contentDescription = null, tint = DeepGreen) },
                onClick = { onSelect(tab) }
            )
        }
        DropdownMenuItem(
            text = { Text("Mise a jour", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
            leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreen) },
            onClick = onCheckUpdate
        )
        DropdownMenuItem(
            text = { Text("Deconnexion", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
            leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = Muted) },
            onClick = onSignOut
        )
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
