package com.bibliostudio.monfoyer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
import kotlin.math.absoluteValue

data class Household(val id: String = "", val name: String = "Mon foyer", val inviteCode: String = "")
data class Member(val id: String = "", val name: String = "", val email: String = "")
data class ShoppingItem(val id: String = "", val name: String = "", val done: Boolean = false)
data class Bill(val id: String = "", val label: String = "", val amount: Double = 0.0, val paid: Boolean = false)
data class Event(val id: String = "", val title: String = "", val owner: String = "", val date: String = "")
data class Note(val id: String = "", val title: String = "", val body: String = "")

data class AppUiState(
    val signedIn: Boolean = false,
    val userName: String = "",
    val household: Household? = null,
    val members: List<Member> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val events: List<Event> = emptyList(),
    val notes: List<Note> = emptyList(),
    val monthlyBudget: Double = 0.0,
    val selectedTab: Tab = Tab.Home,
    val loading: Boolean = true,
    val error: String? = null
)

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Accueil", Icons.Filled.Home),
    Shopping("Courses", Icons.Filled.ShoppingCart),
    Budget("Budget", Icons.Filled.Payments),
    Calendar("Agenda", Icons.Filled.CalendarMonth),
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
    fun addEvent(title: String, owner: String, date: String) = add("events", mapOf("title" to title, "owner" to owner, "date" to date))
    fun addNote(title: String, body: String) = add("notes", mapOf("title" to title, "body" to body))

    fun toggleShopping(item: ShoppingItem) = update("shoppingItems", item.id, "done", !item.done)
    fun toggleBill(bill: Bill) = update("bills", bill.id, "paid", !bill.paid)
    fun delete(collection: String, id: String) {
        val household = state.household ?: return
        db.collection("households").document(household.id).collection(collection).document(id).delete()
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

@Composable
fun MonFoyerApp(vm: MonFoyerViewModel = viewModel()) {
    val colors = lightColorScheme(
        primary = Color(0xFF2F6F6A),
        secondary = Color(0xFFE0A458),
        tertiary = Color(0xFF8A6FDF),
        background = Color(0xFFF7F4EE),
        surface = Color(0xFFFFFBF6),
        surfaceVariant = Color(0xFFE8E1D8)
    )
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mon Foyer", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Organise les courses, les factures, les agendas et les petites notes du foyer.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            scope.launch { vm.signInWithGoogle(context, context.getString(R.string.web_client_id)) }
        }) {
            Text("Continuer avec Google")
        }
        vm.state.error?.let { ErrorText(it) }
    }
}

@Composable
fun HouseholdGate(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("Mon foyer") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("Bienvenue ${vm.state.userName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Cree ton foyer ou rejoins celui d'un proche avec son code.")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du foyer") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.createHousehold(name) }, modifier = Modifier.fillMaxWidth()) { Text("Creer mon foyer") }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code d'invitation") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.joinHousehold(code) }, modifier = Modifier.fillMaxWidth()) { Text("Rejoindre") }
        vm.state.error?.let { ErrorText(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeShell(vm: MonFoyerViewModel) {
    val context = LocalContext.current as ComponentActivity
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vm.state.household?.name ?: "Mon Foyer") },
                actions = {
                    IconButton(onClick = { vm.signOut(context) }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Deconnexion")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = vm.state.selectedTab == tab,
                        onClick = { vm.select(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            vm.state.error?.let { ErrorText(it) }
            when (vm.state.selectedTab) {
                Tab.Home -> Dashboard(vm.state)
                Tab.Shopping -> ShoppingScreen(vm)
                Tab.Budget -> BudgetScreen(vm)
                Tab.Calendar -> AgendaScreen(vm)
                Tab.Notes -> NotesScreen(vm)
                Tab.Members -> MembersScreen(vm)
            }
        }
    }
}

@Composable
fun Dashboard(state: AppUiState) {
    val unpaid = state.bills.filterNot { it.paid }.sumOf { it.amount }
    val remaining = state.monthlyBudget - unpaid
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        item { MetricCard("Argent restant estime", "${"%.2f".format(remaining)} EUR", if (remaining >= 0) "Apres factures non payees" else "Budget depasse") }
        item { MetricCard("Courses a faire", state.shopping.count { !it.done }.toString(), "Articles restants") }
        item { MetricCard("Agenda", state.events.take(3).joinToString("\n") { "${it.date} - ${it.title}" }.ifBlank { "Rien de prevu" }, "Prochains evenements") }
        item { MetricCard("Notes", state.notes.size.toString(), "Notes partagees") }
    }
}

@Composable
fun ShoppingScreen(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("") }
    ModuleList(
        title = "Courses",
        input = {
            QuickAdd(value = name, onChange = { name = it }, label = "Ajouter un article") {
                vm.addShoppingItem(name)
                name = ""
            }
        }
    ) {
        items(vm.state.shopping) { item ->
            RowCard {
                IconButton(onClick = { vm.toggleShopping(item) }) {
                    Icon(if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Etat")
                }
                Text(item.name, modifier = Modifier.weight(1f))
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
    ModuleList(
        title = "Budget et factures",
        input = {
            OutlinedTextField(
                value = budget,
                onValueChange = { budget = it },
                label = { Text("Budget mensuel") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = { vm.setMonthlyBudget(budget) }) { Text("Mettre a jour") }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Facture") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Montant") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                vm.addBill(label, amount)
                label = ""
                amount = ""
            }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter") }
        }
    ) {
        items(vm.state.bills) { bill ->
            RowCard {
                IconButton(onClick = { vm.toggleBill(bill) }) {
                    Icon(if (bill.paid) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Payee")
                }
                Column(Modifier.weight(1f)) {
                    Text(bill.label, fontWeight = FontWeight.SemiBold)
                    Text("${"%.2f".format(bill.amount)} EUR")
                }
                DeleteButton { vm.delete("bills", bill.id) }
            }
        }
    }
}

@Composable
fun AgendaScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    ModuleList(
        title = "Agenda",
        input = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Evenement") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Pour qui ?") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                vm.addEvent(title, owner, date)
                title = ""
                owner = ""
                date = ""
            }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter") }
        }
    ) {
        items(vm.state.events) { event ->
            RowCard {
                Column(Modifier.weight(1f)) {
                    Text(event.title, fontWeight = FontWeight.SemiBold)
                    Text(listOf(event.date, event.owner).filter { it.isNotBlank() }.joinToString(" - "))
                }
                DeleteButton { vm.delete("events", event.id) }
            }
        }
    }
}

@Composable
fun NotesScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    ModuleList(
        title = "Notes",
        input = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Button(onClick = {
                vm.addNote(title, body)
                title = ""
                body = ""
            }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Ajouter") }
        }
    ) {
        items(vm.state.notes) { note ->
            RowCard {
                Column(Modifier.weight(1f)) {
                    Text(note.title, fontWeight = FontWeight.SemiBold)
                    Text(note.body)
                }
                DeleteButton { vm.delete("notes", note.id) }
            }
        }
    }
}

@Composable
fun MembersScreen(vm: MonFoyerViewModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
        item {
            MetricCard("Code foyer", vm.state.household?.inviteCode.orEmpty(), "A partager avec les membres du foyer")
        }
        items(vm.state.members) { member ->
            RowCard {
                Icon(Icons.Filled.Group, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(member.name.ifBlank { "Membre" }, fontWeight = FontWeight.SemiBold)
                    Text(member.email)
                }
            }
        }
    }
}

@Composable
fun ModuleList(title: String, input: @Composable () -> Unit, content: LazyListScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                input()
            }
        }
        content()
    }
}

@Composable
fun QuickAdd(value: String, onChange: (String) -> Unit, label: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.weight(1f))
        IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Ajouter") }
    }
}

@Composable
fun MetricCard(title: String, value: String, helper: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(helper, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RowCard(content: @Composable RowScope.() -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer") }
}

@Composable
fun ErrorText(message: String) {
    Spacer(Modifier.height(12.dp))
    Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
fun CenterMessage(message: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message)
    }
}
