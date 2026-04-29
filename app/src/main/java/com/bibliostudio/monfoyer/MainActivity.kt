package com.bibliostudio.monfoyer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

data class Household(val id: String = "", val name: String = "Mon foyer", val inviteCode: String = "")
data class Member(val id: String = "", val name: String = "", val email: String = "")
data class ShoppingItem(val id: String = "", val name: String = "", val done: Boolean = false)
data class Bill(val id: String = "", val label: String = "", val amount: Double = 0.0, val paid: Boolean = false)
data class Event(val id: String = "", val title: String = "", val owner: String = "", val date: String = "")
data class Note(val id: String = "", val title: String = "", val body: String = "")
data class HouseholdTask(val id: String = "", val title: String = "", val assigneeId: String = "", val assigneeName: String = "", val done: Boolean = false, val color: Long = 0xFF174C43)
data class Birthday(val id: String = "", val name: String = "", val date: String = "", val birthYear: Int = 0)

data class AppUiState(
    val signedIn: Boolean = false,
    val userName: String = "",
    val household: Household? = null,
    val members: List<Member> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val events: List<Event> = emptyList(),
    val notes: List<Note> = emptyList(),
    val tasks: List<HouseholdTask> = emptyList(),
    val birthdays: List<Birthday> = emptyList(),
    val monthlyBudget: Double = 0.0,
    val selectedTab: Tab = Tab.Home,
    val loading: Boolean = true,
    val error: String? = null
)

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Accueil", Icons.Filled.Home),
    Shopping("Courses", Icons.Filled.ShoppingCart),
    Tasks("Taches", Icons.Filled.CheckCircle),
    Budget("Budget", Icons.Filled.Payments),
    Calendar("Agenda", Icons.Filled.CalendarMonth),
    Birthdays("Anniversaires", Icons.Filled.Group),
    Notes("Notes", Icons.Filled.EditNote),
    Members("Foyer", Icons.Filled.Group)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MonFoyerApp() }
    }
}

class MonFoyerViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var listeners = mutableListOf<ListenerRegistration>()
    var state by mutableStateOf(AppUiState())
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            clearListeners()
            if (user == null) {
                state = AppUiState(loading = false)
            } else {
                state = state.copy(
                    signedIn = true,
                    userName = user.displayName ?: "Mon compte",
                    loading = true,
                    error = null
                )
                listenHousehold(user.uid)
            }
        }
    }

    suspend fun signInWithGoogle(activity: ComponentActivity, webClientId: String) {
        runCatching {
            if (webClientId.startsWith("REMPLACE")) error("Configure d'abord web_client_id dans strings.xml.")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = CredentialManager.create(activity).getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential).await()
            } else {
                error("Connexion Google impossible avec ce compte.")
            }
        }.onFailure { setError(it.message ?: "Connexion Google impossible.") }
    }

    fun signOut(activity: ComponentActivity) {
        Identity.getSignInClient(activity).signOut()
        auth.signOut()
    }

    fun select(tab: Tab) {
        state = state.copy(selectedTab = tab)
    }

    fun createHousehold(name: String) {
        val user = auth.currentUser ?: return
        val code = inviteCode(user.uid)
        val householdRef = db.collection("households").document()
        val member = mapOf(
            "name" to (user.displayName ?: "Membre"),
            "email" to (user.email ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(householdRef, mapOf("name" to name.ifBlank { "Mon foyer" }, "ownerId" to user.uid, "inviteCode" to code))
        batch.set(householdRef.collection("members").document(user.uid), member)
        batch.set(db.collection("householdInvites").document(code), mapOf("householdId" to householdRef.id))
        batch.set(db.collection("users").document(user.uid), mapOf("householdId" to householdRef.id, "updatedAt" to FieldValue.serverTimestamp()))
        batch.commit().addOnFailureListener { setError(it.message ?: "Creation impossible.") }
    }

    fun joinHousehold(code: String) {
        val user = auth.currentUser ?: return
        val cleanedCode = code.trim().uppercase()
        db.collection("householdInvites").document(cleanedCode).get()
            .addOnSuccessListener { invite ->
                val householdId = invite.getString("householdId")
                if (householdId == null) {
                    setError("Code foyer introuvable.")
                } else {
                    db.collection("households").document(householdId)
                        .collection("members").document(user.uid).set(
                            mapOf(
                                "name" to (user.displayName ?: "Membre"),
                                "email" to (user.email ?: ""),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        ).addOnSuccessListener {
                            db.collection("users").document(user.uid)
                                .set(mapOf("householdId" to householdId, "updatedAt" to FieldValue.serverTimestamp()))
                        }
                }
            }
            .addOnFailureListener { setError(it.message ?: "Impossible de rejoindre ce foyer.") }
    }

    fun setMonthlyBudget(value: String) {
        val household = state.household ?: return
        val amount = value.toDoubleOrNull() ?: return
        db.collection("households").document(household.id).update("monthlyBudget", amount)
    }

    fun addShoppingItem(name: String) = add("shoppingItems", mapOf("name" to name, "done" to false))
    fun addBill(label: String, amount: String) = add("bills", mapOf("label" to label, "amount" to (amount.toDoubleOrNull() ?: 0.0), "paid" to false))
    fun addEvent(title: String, owner: String, date: String) {
        if (title.isBlank()) return
        add("events", mapOf("title" to title, "owner" to owner, "date" to date))
    }
    fun addNote(title: String, body: String) = add("notes", mapOf("title" to title, "body" to body))
    fun addTask(title: String, member: Member?) {
        if (title.isBlank()) return
        add(
            "tasks",
            mapOf(
                "title" to title,
                "assigneeId" to (member?.id ?: ""),
                "assigneeName" to (member?.name ?: "A assigner"),
                "color" to memberColorLong(member?.id ?: title),
                "done" to false
            )
        )
    }
    fun addBirthday(name: String, date: LocalDate, birthYear: String) {
        if (name.isBlank()) return
        add(
            "birthdays",
            mapOf(
                "name" to name,
                "date" to date.format(DateTimeFormatter.ISO_DATE),
                "birthYear" to (birthYear.toIntOrNull() ?: 0)
            )
        )
    }

    fun toggleShopping(item: ShoppingItem) = update("shoppingItems", item.id, "done", !item.done)
    fun toggleBill(bill: Bill) = update("bills", bill.id, "paid", !bill.paid)
    fun toggleTask(task: HouseholdTask) = update("tasks", task.id, "done", !task.done)
    fun delete(collection: String, id: String) {
        val household = state.household ?: return
        db.collection("households").document(household.id).collection(collection).document(id).delete()
    }

    fun deleteCheckedShoppingItems() {
        val household = state.household ?: return
        val checkedItems = state.shopping.filter { it.done }
        if (checkedItems.isEmpty()) return
        val batch = db.batch()
        checkedItems.forEach { item ->
            batch.delete(db.collection("households").document(household.id).collection("shoppingItems").document(item.id))
        }
        batch.commit().addOnFailureListener { setError(it.message ?: "Suppression impossible.") }
    }

    private fun add(collection: String, values: Map<String, Any>) {
        val household = state.household ?: return
        val cleanValues = values.filterValues { value -> value.toString().isNotBlank() }
        if (cleanValues.isEmpty()) return
        db.collection("households").document(household.id).collection(collection)
            .add(cleanValues + mapOf("createdAt" to FieldValue.serverTimestamp()))
    }

    private fun update(collection: String, id: String, field: String, value: Any) {
        val household = state.household ?: return
        db.collection("households").document(household.id).collection(collection).document(id).update(field, value)
    }

    private fun listenHousehold(uid: String) {
        listeners += db.collection("users").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    setError(error.message ?: "Chargement impossible.")
                    return@addSnapshotListener
                }
                val householdId = doc?.getString("householdId")
                if (householdId == null) {
                    state = state.copy(household = null, loading = false)
                } else {
                    listenHouseholdData(householdId)
                }
            }
    }

    private fun listenHouseholdData(householdId: String) {
        clearListeners(keepFirst = true)
        val householdRef = db.collection("households").document(householdId)
        listeners += householdRef.addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                state = state.copy(
                    household = Household(
                        id = doc.id,
                        name = doc.getString("name") ?: "Mon foyer",
                        inviteCode = doc.getString("inviteCode") ?: ""
                    ),
                    monthlyBudget = doc.getDouble("monthlyBudget") ?: 0.0,
                    loading = false
                )
            }
        }
        listeners += householdRef.collection("members").addSnapshotListener { snap, _ ->
            state = state.copy(members = snap?.documents?.map { Member(it.id, it.getString("name") ?: "", it.getString("email") ?: "") }.orEmpty())
        }
        listeners += householdRef.collection("shoppingItems").addSnapshotListener { snap, _ ->
            state = state.copy(shopping = snap?.documents?.map { ShoppingItem(it.id, it.getString("name") ?: "", it.getBoolean("done") ?: false) }.orEmpty())
        }
        listeners += householdRef.collection("bills").addSnapshotListener { snap, _ ->
            state = state.copy(bills = snap?.documents?.map { Bill(it.id, it.getString("label") ?: "", it.getDouble("amount") ?: 0.0, it.getBoolean("paid") ?: false) }.orEmpty())
        }
        listeners += householdRef.collection("events").addSnapshotListener { snap, _ ->
            state = state.copy(events = snap?.documents?.map { Event(it.id, it.getString("title") ?: "", it.getString("owner") ?: "", it.getString("date") ?: "") }.orEmpty())
        }
        listeners += householdRef.collection("notes").addSnapshotListener { snap, _ ->
            state = state.copy(notes = snap?.documents?.map { Note(it.id, it.getString("title") ?: "", it.getString("body") ?: "") }.orEmpty())
        }
        listeners += householdRef.collection("tasks").addSnapshotListener { snap, _ ->
            state = state.copy(tasks = snap?.documents?.map {
                HouseholdTask(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    assigneeId = it.getString("assigneeId") ?: "",
                    assigneeName = it.getString("assigneeName") ?: "",
                    done = it.getBoolean("done") ?: false,
                    color = it.getLong("color") ?: 0xFF174C43
                )
            }.orEmpty())
        }
        listeners += householdRef.collection("birthdays").addSnapshotListener { snap, _ ->
            state = state.copy(birthdays = snap?.documents?.map {
                Birthday(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    date = it.getString("date") ?: "",
                    birthYear = (it.getLong("birthYear") ?: 0).toInt()
                )
            }.orEmpty())
        }
    }

    private fun clearListeners(keepFirst: Boolean = false) {
        val kept = if (keepFirst) listeners.firstOrNull() else null
        listeners.filter { it != kept }.forEach { it.remove() }
        listeners = kept?.let { mutableListOf(it) } ?: mutableListOf()
    }

    private fun setError(message: String) {
        state = state.copy(error = message, loading = false)
    }

    private fun inviteCode(uid: String): String {
        val raw = (uid + System.currentTimeMillis()).hashCode().absoluteValue.toString(36).uppercase()
        return raw.take(6).padEnd(6, '0')
    }

    override fun onCleared() {
        clearListeners()
        super.onCleared()
    }
}

private val Cream = Color(0xFFF3F1EC)
private val DeepGreen = Color(0xFF174C43)
private val Mint = Color(0xFFAEEBD8)
private val Lemon = Color(0xFFFFE58F)
private val Coral = Color(0xFFFF9AA7)
private val Sky = Color(0xFFA9DDEA)
private val SoftGrey = Color(0xFFF0EFEC)
private val Ink = Color(0xFF101010)
private val Muted = Color(0xFF8C8B88)

data class ModuleTile(
    val tab: Tab,
    val title: String,
    val subtitle: String,
    val count: String?,
    val colors: List<Color>,
    val icon: ImageVector
)

@Composable
fun MonFoyerApp(vm: MonFoyerViewModel = viewModel()) {
    val colors = lightColorScheme(
        primary = DeepGreen,
        secondary = Color(0xFFE8A64F),
        tertiary = Color(0xFFE86675),
        background = Cream,
        surface = Color.White,
        surfaceVariant = SoftGrey,
        onPrimary = Color.White,
        onSurface = Ink
    )
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
            when {
                vm.state.loading -> CenterMessage("Chargement...")
                !vm.state.signedIn -> SignInScreen(vm)
                vm.state.household == null -> HouseholdGate(vm)
                else -> HomeShell(vm)
            }
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
        Text("Courses, budget, agenda et petites notes au meme endroit.", fontSize = 18.sp, color = Muted)
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
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo()
        Spacer(Modifier.height(28.dp))
        Text("Ton espace commun", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, color = Ink)
        Text("Cree ton foyer ou rejoins celui d'un proche.", fontSize = 17.sp, color = Muted)
        Spacer(Modifier.height(24.dp))
        SoftInput(value = name, onValueChange = { name = it }, label = "Nom du foyer")
        Spacer(Modifier.height(10.dp))
        PrimaryButton(text = "Creer mon foyer", icon = Icons.Filled.Home) { vm.createHousehold(name) }
        Spacer(Modifier.height(24.dp))
        SoftInput(value = code, onValueChange = { code = it }, label = "Code d'invitation")
        Spacer(Modifier.height(10.dp))
        SecondaryButton(text = "Rejoindre", icon = Icons.Filled.Group) { vm.joinHousehold(code) }
        vm.state.error?.let { ErrorText(it) }
    }
}

@Composable
fun HomeShell(vm: MonFoyerViewModel) {
    val context = LocalContext.current as ComponentActivity
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
                onSignOut = { vm.signOut(context) }
            )
            vm.state.error?.let { ErrorText(it, Modifier.padding(horizontal = 24.dp)) }
            when (vm.state.selectedTab) {
                Tab.Home -> Dashboard(vm)
                Tab.Shopping -> ShoppingScreen(vm)
                Tab.Tasks -> TasksScreen(vm)
                Tab.Budget -> BudgetScreen(vm)
                Tab.Calendar -> AgendaScreen(vm)
                Tab.Birthdays -> BirthdaysScreen(vm)
                Tab.Notes -> NotesScreen(vm)
                Tab.Members -> MembersScreen(vm)
            }
        }
        FloatingHomeButton(visible = vm.state.selectedTab != Tab.Home, onClick = { vm.select(Tab.Home) })
    }
}

@Composable
fun AppHeader(activeTab: Tab, householdName: String, onHome: () -> Unit, onSelect: (Tab) -> Unit, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BrandLogo(Modifier.weight(1f))
            Box {
                RoundIconButton(icon = Icons.Filled.MoreVert, tint = DeepGreen, onClick = { menuOpen = true })
                HomeMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    onSelect = {
                        menuOpen = false
                        onSelect(it)
                    },
                    onSignOut = {
                        menuOpen = false
                        onSignOut()
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            RoundIconButton(icon = Icons.Filled.Logout, tint = Muted, onClick = onSignOut)
        }
        Spacer(Modifier.height(16.dp))
        if (activeTab != Tab.Home) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = DeepGreen, shape = RoundedCornerShape(50), modifier = Modifier.clickable { onHome() }) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(activeTab.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(activeTab.label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    }
                }
                RoundIconButton(icon = Icons.Filled.Group, tint = Ink, onClick = {})
            }
        } else {
            Text(householdName, color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Dashboard(vm: MonFoyerViewModel) {
    val state = vm.state
    val modules = listOf(
        ModuleTile(Tab.Shopping, "Courses", "Liste partagee", state.shopping.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Mint, Color(0xFF86D6C3)), Icons.Filled.ShoppingCart),
        ModuleTile(Tab.Calendar, "Calendrier", "Agenda du foyer", state.events.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFF2B8), Lemon), Icons.Filled.CalendarMonth),
        ModuleTile(Tab.Tasks, "Taches", "Qui fait quoi", state.tasks.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFA9E8DD), Color(0xFFD5F3A5)), Icons.Filled.CheckCircle),
        ModuleTile(Tab.Notes, "Notes", "Pense-betes", state.notes.size.takeIf { it > 0 }?.toString(), listOf(Coral, Color(0xFFFFB6BF)), Icons.Filled.EditNote),
        ModuleTile(Tab.Budget, "Budget", "Factures et reste", null, listOf(Color(0xFFFFD6C8), Color(0xFFA7E0D2)), Icons.Filled.Payments),
        ModuleTile(Tab.Birthdays, "Anniversaires", "Dates importantes", state.birthdays.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFADEBDD), Color(0xFFFFE8A6)), Icons.Filled.Group),
        ModuleTile(Tab.Members, "Foyer", "Membres et code", state.members.size.toString(), listOf(Sky, Color(0xFFD7F0F5)), Icons.Filled.Group)
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Text("Mes applications", fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding()
        ) {
            gridItems(modules) { tile ->
                ModuleCard(tile = tile, onClick = { vm.select(tile.tab) })
            }
        }
    }
}

@Composable
fun ModuleCard(tile: ModuleTile, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(164.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(tile.colors))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Text(
            tile.title,
            fontSize = 27.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp).size(82.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(tile.icon, contentDescription = null, tint = DeepGreen, modifier = Modifier.size(46.dp))
        }
        tile.count?.let {
            Surface(color = Color.White.copy(alpha = 0.45f), shape = CircleShape, modifier = Modifier.align(Alignment.BottomStart)) {
                Text(it, modifier = Modifier.padding(horizontal = 17.dp, vertical = 13.dp), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
            }
        }
    }
}

@Composable
fun HomeMenu(expanded: Boolean, onDismiss: () -> Unit, onSelect: (Tab) -> Unit, onSignOut: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Color.White) {
        listOf(Tab.Home, Tab.Shopping, Tab.Calendar, Tab.Tasks, Tab.Birthdays, Tab.Budget, Tab.Notes, Tab.Members).forEach { tab ->
            DropdownMenuItem(
                text = { Text(tab.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
                leadingIcon = { Icon(tab.icon, contentDescription = null, tint = DeepGreen) },
                onClick = { onSelect(tab) }
            )
        }
        DropdownMenuItem(
            text = { Text("Deconnexion", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
            leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = Muted) },
            onClick = onSignOut
        )
    }
}

@Composable
fun ShoppingScreen(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("") }
    val checkedCount = vm.state.shopping.count { it.done }
    ModulePanel(title = "Liste de course") {
        item {
            QuickAdd(value = name, onChange = { name = it }, label = "Ajouter un article...") {
                vm.addShoppingItem(name)
                name = ""
            }
            if (checkedCount > 0) {
                SecondaryButton(text = "Supprimer elements coches ($checkedCount)", icon = Icons.Filled.Delete) {
                    vm.deleteCheckedShoppingItems()
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        items(vm.state.shopping) { item ->
            ListRow {
                IconButton(onClick = { vm.toggleShopping(item) }, modifier = Modifier.size(48.dp)) {
                    Icon(if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Etat", tint = if (item.done) DeepGreen else Color(0xFFDADADA), modifier = Modifier.size(34.dp))
                }
                Text(item.name, modifier = Modifier.weight(1f), fontSize = 24.sp, color = Ink)
                DeleteButton { vm.delete("shoppingItems", item.id) }
            }
        }
    }
}

@Composable
fun BudgetScreen(vm: MonFoyerViewModel) {
    var budget by remember(vm.state.monthlyBudget) { mutableStateOf(if (vm.state.monthlyBudget == 0.0) "" else vm.state.monthlyBudget.toString()) }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val unpaid = vm.state.bills.filterNot { it.paid }.sumOf { it.amount }
    val remaining = vm.state.monthlyBudget - unpaid
    ModulePanel(title = "Budget") {
        item {
            StatBubble("Reste estime", "${"%.2f".format(remaining)} EUR")
            Spacer(Modifier.height(14.dp))
            SoftInput(value = budget, onValueChange = { budget = it }, label = "Budget mensuel", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(10.dp))
            SecondaryButton(text = "Mettre a jour", icon = Icons.Filled.Payments) { vm.setMonthlyBudget(budget) }
            Spacer(Modifier.height(18.dp))
            SoftInput(value = label, onValueChange = { label = it }, label = "Nouvelle facture")
            Spacer(Modifier.height(10.dp))
            SoftInput(value = amount, onValueChange = { amount = it }, label = "Montant", keyboardType = KeyboardType.Decimal)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Ajouter", icon = Icons.Filled.Add) {
                vm.addBill(label, amount)
                label = ""
                amount = ""
            }
        }
        items(vm.state.bills) { bill ->
            ListRow {
                IconButton(onClick = { vm.toggleBill(bill) }, modifier = Modifier.size(48.dp)) {
                    Icon(if (bill.paid) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Payee", tint = if (bill.paid) DeepGreen else Color(0xFFDADADA), modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(bill.label, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("${"%.2f".format(bill.amount)} EUR", fontSize = 16.sp, color = Muted)
                }
                DeleteButton { vm.delete("bills", bill.id) }
            }
        }
    }
}

@Composable
fun AgendaScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var selectedMemberId by remember(vm.state.members) { mutableStateOf(vm.state.members.firstOrNull()?.id.orEmpty()) }
    val selectedMember = vm.state.members.firstOrNull { it.id == selectedMemberId }
    val selectedDateText = selectedDate.format(DateTimeFormatter.ISO_DATE)
    val selectedEvents = vm.state.events.filter { it.date == selectedDateText }
    ModulePanel(title = "Calendrier") {
        item {
            CalendarMonthView(
                month = visibleMonth,
                selectedDate = selectedDate,
                events = vm.state.events,
                onPrevious = {
                    visibleMonth = visibleMonth.minusMonths(1)
                    selectedDate = visibleMonth.atDay(1)
                },
                onNext = {
                    visibleMonth = visibleMonth.plusMonths(1)
                    selectedDate = visibleMonth.atDay(1)
                },
                onDateSelected = {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                }
            )
            Spacer(Modifier.height(18.dp))
            if (selectedEvents.isEmpty()) {
                Text("Aucun evenement ce jour", color = Muted, fontSize = 18.sp)
            } else {
                selectedEvents.forEach { event ->
                    CalendarEventPill(event)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            SoftInput(value = title, onValueChange = { title = it }, label = "Nom de l'evenement")
            Spacer(Modifier.height(10.dp))
            MemberPicker(
                members = vm.state.members,
                selectedMemberId = selectedMemberId,
                onSelect = { selectedMemberId = it }
            )
            Spacer(Modifier.height(10.dp))
            DateChip(selectedDate)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Ajouter", icon = Icons.Filled.Add) {
                vm.addEvent(title, selectedMember?.name ?: "Tout le foyer", selectedDateText)
                title = ""
            }
        }
        items(vm.state.events) { event ->
            ListRow {
                Column(Modifier.weight(1f)) {
                    Text(event.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(listOf(event.date, event.owner).filter { it.isNotBlank() }.joinToString(" - "), fontSize = 16.sp, color = Muted)
                }
                DeleteButton { vm.delete("events", event.id) }
            }
        }
    }
}

@Composable
fun TasksScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var selectedMemberId by remember(vm.state.members) { mutableStateOf(vm.state.members.firstOrNull()?.id.orEmpty()) }
    val selectedMember = vm.state.members.firstOrNull { it.id == selectedMemberId }
    ModulePanel(title = "Taches") {
        item {
            SoftInput(value = title, onValueChange = { title = it }, label = "Nouvelle tache")
            Spacer(Modifier.height(10.dp))
            MemberPicker(
                members = vm.state.members,
                selectedMemberId = selectedMemberId,
                onSelect = { selectedMemberId = it }
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Ajouter", icon = Icons.Filled.Add) {
                vm.addTask(title, selectedMember)
                title = ""
            }
        }
        items(vm.state.tasks) { task ->
            ListRow {
                val color = Color(task.color)
                Surface(color = color.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(50.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = { vm.toggleTask(task) }) {
                            Icon(if (task.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Etat", tint = color, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(task.assigneeName.ifBlank { "A assigner" }, fontSize = 16.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
                DeleteButton { vm.delete("tasks", task.id) }
            }
        }
    }
}

@Composable
fun BirthdaysScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var birthdayToDelete by remember { mutableStateOf<Birthday?>(null) }
    ModulePanel(title = "Anniversaires") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.ViewList, tint = Muted, onClick = {})
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        items(vm.state.birthdays.sortedBy { it.nextBirthday() }) { birthday ->
            BirthdayRow(birthday) { birthdayToDelete = birthday }
        }
    }
    if (showAddSheet) {
        AddBirthdaySheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, date, year ->
                vm.addBirthday(name, date, year)
                showAddSheet = false
            }
        )
    }
    birthdayToDelete?.let { birthday ->
        ConfirmDeleteDialog(
            title = "Supprimer cet anniversaire ?",
            message = "L'anniversaire de ${birthday.name} sera retire du foyer.",
            onConfirm = {
                vm.delete("birthdays", birthday.id)
                birthdayToDelete = null
            },
            onDismiss = { birthdayToDelete = null }
        )
    }
}

@Composable
fun NotesScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    ModulePanel(title = "Notes") {
        item {
            SoftInput(value = title, onValueChange = { title = it }, label = "Titre")
            Spacer(Modifier.height(10.dp))
            SoftInput(value = body, onValueChange = { body = it }, label = "Note", minLines = 3)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Ajouter", icon = Icons.Filled.Add) {
                vm.addNote(title, body)
                title = ""
                body = ""
            }
        }
        items(vm.state.notes) { note ->
            ListRow {
                Column(Modifier.weight(1f)) {
                    Text(note.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(note.body, fontSize = 16.sp, color = Muted)
                }
                DeleteButton { vm.delete("notes", note.id) }
            }
        }
    }
}

@Composable
fun MembersScreen(vm: MonFoyerViewModel) {
    ModulePanel(title = "Mon foyer") {
        item {
            StatBubble("Code foyer", vm.state.household?.inviteCode.orEmpty())
        }
        items(vm.state.members) { member ->
            ListRow {
                Surface(color = SoftGrey, shape = CircleShape, modifier = Modifier.size(50.dp), content = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Group, contentDescription = null, tint = DeepGreen)
                    }
                })
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(member.name.ifBlank { "Membre" }, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(member.email, fontSize = 14.sp, color = Muted)
                }
            }
        }
    }
}

@Composable
fun ModulePanel(title: String, content: LazyListScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp).navigationBarsPadding()
        ) {
            item {
                Text(title, fontSize = 34.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(18.dp))
            }
            content()
            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}

@Composable
fun QuickAdd(value: String, onChange: (String) -> Unit, label: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SoftInput(value = value, onValueChange = onChange, label = label, modifier = Modifier.weight(1f))
        Surface(color = DeepGreen, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(64.dp).clickable(onClick = onAdd)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
fun CalendarMonthView(
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val monthTitle = month.month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("$monthTitle ${month.year}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
            RoundIconButton(icon = Icons.Filled.ChevronLeft, tint = Muted, onClick = onPrevious)
            Spacer(Modifier.width(8.dp))
            RoundIconButton(icon = Icons.Filled.ChevronRight, tint = DeepGreen, onClick = onNext)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.").forEach {
                Text(it, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(356.dp)
        ) {
            gridItems(cells) { date ->
                if (date == null) {
                    Spacer(Modifier.height(58.dp))
                } else {
                    val dateKey = date.format(DateTimeFormatter.ISO_DATE)
                    val dayEvents = events.filter { it.date == dateKey }
                    val selected = date == selectedDate
                    Box(
                        Modifier
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) DeepGreen else SoftGrey)
                            .clickable { onDateSelected(date) }
                            .padding(7.dp)
                    ) {
                        Text(date.dayOfMonth.toString(), color = if (selected) Color.White else Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (dayEvents.isNotEmpty()) {
                            Column(Modifier.align(Alignment.BottomStart)) {
                                dayEvents.take(2).forEach { event ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) Color.White.copy(alpha = 0.85f) else DeepGreen.copy(alpha = 0.75f))
                                    )
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventPill(event: Event) {
    Surface(color = DeepGreen.copy(alpha = 0.09f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(DeepGreen))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(event.owner.ifBlank { "Tout le foyer" }, fontSize = 14.sp, color = DeepGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MemberPicker(members: List<Member>, selectedMemberId: String, onSelect: (String) -> Unit) {
    Text("Pour qui ?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (members.isEmpty()) {
            MemberChip(label = "Tout le foyer", selected = selectedMemberId.isBlank(), color = DeepGreen) { onSelect("") }
        } else {
            members.take(4).forEach { member ->
                MemberChip(
                    label = member.name.ifBlank { "Membre" },
                    selected = selectedMemberId == member.id,
                    color = memberColor(member.id)
                ) { onSelect(member.id) }
            }
        }
    }
}

@Composable
fun MemberChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        color = if (selected) color else SoftGrey,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DateChip(date: LocalDate) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(2.dp, DeepGreen), modifier = Modifier.fillMaxWidth()) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)),
            color = DeepGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
fun BirthdayRow(birthday: Birthday, onDelete: () -> Unit) {
    val next = birthday.nextBirthday()
    val age = if (birthday.birthYear > 0) next.year - birthday.birthYear else null
    val monthsAway = ((next.year - LocalDate.now().year) * 12 + next.monthValue - LocalDate.now().monthValue).coerceAtLeast(0)
    ListRow {
        Surface(color = Color(0xFFEAF8EE), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(58.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF54B568), modifier = Modifier.size(31.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(birthday.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Text(next.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)), fontSize = 15.sp, color = Muted)
            Text("Dans $monthsAway mois", fontSize = 15.sp, color = Color(0xFF4CAF50))
        }
        age?.let {
            Text("$it ans", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink)
        }
        DeleteButton(onDelete)
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black, color = Ink) },
        text = { Text(message, color = Muted) },
        confirmButton = {
            Text(
                "Supprimer",
                color = Color(0xFFE86675),
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onConfirm).padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                "Annuler",
                color = DeepGreen,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp)
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdaySheet(onDismiss: () -> Unit, onAdd: (String, LocalDate, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 14.dp).width(70.dp).height(6.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF9B9B9B))
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Ajouter une personne", fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(color = SoftGrey, shape = CircleShape, modifier = Modifier.size(132.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = Muted, modifier = Modifier.size(68.dp))
                    }
                }
            }
            Spacer(Modifier.height(34.dp))
            Text("Nom", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = name, onValueChange = { name = it }, label = "Entre le nom de la personne", leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(26.dp))
            Text("Date de naissance", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            DateField(selectedDate, onClick = { showDateSheet = true })
            Spacer(Modifier.height(34.dp))
            val canAdd = name.isNotBlank()
            Button(
                onClick = { onAdd(name, selectedDate, selectedDate.year.toString()) },
                enabled = canAdd,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGreen,
                    disabledContainerColor = Color(0xFFE1E1E1),
                    disabledContentColor = Muted
                ),
                modifier = Modifier.fillMaxWidth().height(66.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Ajouter", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    if (showDateSheet) {
        BirthdayDateSheet(
            initialDate = selectedDate,
            onDismiss = { showDateSheet = false },
            onConfirm = {
                selectedDate = it
                showDateSheet = false
            }
        )
    }
}

@Composable
fun DateField(date: LocalDate, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD9D9D9)),
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE)),
                color = Muted,
                fontSize = 21.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Muted, modifier = Modifier.size(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDateSheet(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var year by remember { mutableStateOf(initialDate.year) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    if (day > maxDay) day = maxDay

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 14.dp).width(70.dp).height(6.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF9B9B9B))
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Selectionner la date", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                Text(
                    "OK",
                    color = DeepGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { onConfirm(LocalDate.of(year, month, day)) }.padding(12.dp)
                )
            }
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                PickerColumn(
                    values = (1..maxDay).toList(),
                    selected = day,
                    label = { it.toString() },
                    onSelect = { day = it },
                    modifier = Modifier.weight(0.8f)
                )
                PickerColumn(
                    values = (1..12).toList(),
                    selected = month,
                    label = { LocalDate.of(2026, it, 1).month.getDisplayName(TextStyle.FULL, Locale.FRANCE).replaceFirstChar { char -> char.titlecase(Locale.FRANCE) } },
                    onSelect = { month = it },
                    modifier = Modifier.weight(1.7f)
                )
                PickerColumn(
                    values = (LocalDate.now().year downTo 1920).toList(),
                    selected = year,
                    label = { it.toString() },
                    onSelect = { year = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun <T> PickerColumn(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(values, selected) {
        val index = values.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(index)
    }
    Box(modifier.height(238.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(values) { value ->
                val isSelected = value == selected
                Surface(
                    color = if (isSelected) SoftGrey else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onSelect(value) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label(value),
                            fontSize = if (isSelected) 26.sp else 22.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Ink else Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun memberColorLong(seed: String): Long {
    val palette = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95)
    return palette[(seed.hashCode().absoluteValue) % palette.size]
}

fun memberColor(seed: String): Color = Color(memberColorLong(seed))

fun Birthday.markerEvent(year: Int): Event? {
    val dateValue = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val markerDate = runCatching { LocalDate.of(year, dateValue.month, dateValue.dayOfMonth) }.getOrNull() ?: return null
    return Event(id = id, title = name, owner = "Anniversaire", date = markerDate.format(DateTimeFormatter.ISO_DATE))
}

fun Birthday.nextBirthday(): LocalDate {
    val value = runCatching { LocalDate.parse(date) }.getOrNull() ?: LocalDate.now()
    val now = LocalDate.now()
    val thisYear = runCatching { LocalDate.of(now.year, value.month, value.dayOfMonth) }.getOrDefault(now)
    return if (thisYear.isBefore(now)) thisYear.plusYears(1) else thisYear
}

@Composable
fun SoftInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Muted, fontSize = 18.sp) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, tint = Muted) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepGreen,
            unfocusedBorderColor = Color(0xFFE3E3E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
fun ListRow(content: @Composable RowScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun StatBubble(label: String, value: String) {
    Surface(color = SoftGrey, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(label, color = Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = DeepGreen, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MonthPreview() {
    val days = (1..30).toList()
    Column {
        Text("Avril 2026", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            gridItems(days) { day ->
                Box(
                    Modifier.height(58.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (day == 29) DeepGreen else SoftGrey)
                        .padding(8.dp)
                ) {
                    Text(day.toString(), color = if (day == 29) Color.White else Ink, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
        modifier = Modifier.fillMaxWidth().height(62.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey, contentColor = DeepGreen),
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoxScope.FloatingHomeButton(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    Surface(
        color = Color.White,
        shape = CircleShape,
        shadowElevation = 10.dp,
        modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(28.dp).size(76.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Home, contentDescription = "Accueil", tint = DeepGreen, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Surface(color = Color.White, shape = CircleShape, shadowElevation = 2.dp, modifier = Modifier.size(46.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Text(
        "Mon\nFoyer",
        modifier = modifier,
        color = DeepGreen,
        fontSize = 33.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun FlowerMark() {
    Canvas(Modifier.size(38.dp)) {
        val colors = listOf(DeepGreen, Color(0xFFE86675), Color(0xFFE8A64F), Color(0xFF7BC6D4), Color(0xFF9AD7C2), Color(0xFFF2C55A))
        colors.forEachIndexed { index, color ->
            val angle = (index * 60f) * Math.PI.toFloat() / 180f
            val center = Offset(
                x = size.width / 2 + kotlin.math.cos(angle.toDouble()).toFloat() * 12.dp.toPx(),
                y = size.height / 2 + kotlin.math.sin(angle.toDouble()).toFloat() * 12.dp.toPx()
            )
            drawCircle(color = color, radius = 4.5.dp.toPx(), center = center)
        }
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
    }
}

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted) }
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Spacer(Modifier.height(12.dp))
    Text(message, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
fun CenterMessage(message: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandLogo()
        Spacer(Modifier.height(18.dp))
        Text(message, color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
