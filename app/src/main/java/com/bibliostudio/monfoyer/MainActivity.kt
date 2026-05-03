package com.bibliostudio.monfoyer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/Kheyox/Mon-Foyer/main/update.json"

data class Household(val id: String = "", val name: String = "Mon foyer", val inviteCode: String = "", val ownerId: String = "")
data class Member(val id: String = "", val name: String = "", val email: String = "", val role: String = "member", val color: Long = 0xFF174C43)
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val done: Boolean = false,
    val quantity: Int = 1,
    val category: String = "Epicerie",
    val favorite: Boolean = false
)
data class Bill(val id: String = "", val label: String = "", val amount: Double = 0.0, val paid: Boolean = false)
data class Event(
    val id: String = "",
    val title: String = "",
    val owner: String = "",
    val date: String = "",
    val description: String = "",
    val location: String = "",
    val typeName: String = "Repas",
    val typeIcon: String = "🍴",
    val typeColor: Long = 0xFFE86675,
    val allDay: Boolean = false,
    val time: String = "00:00",
    val recurrence: String = "Aucune"
)
data class EventType(val id: String = "", val name: String = "", val icon: String = "🍴", val color: Long = 0xFFE86675)
data class Note(val id: String = "", val title: String = "", val body: String = "")
data class HouseholdTask(val id: String = "", val title: String = "", val assigneeId: String = "", val assigneeName: String = "", val done: Boolean = false, val color: Long = 0xFF174C43, val description: String = "", val dueDate: String = "", val emoji: String = "🙂")
data class Birthday(val id: String = "", val name: String = "", val date: String = "", val birthYear: Int = 0)
data class MediaRequest(
    val id: String = "",
    val title: String = "",
    val kind: String = "Livre",
    val requesterId: String = "",
    val requesterName: String = "",
    val status: String = "pending",
    val adminNote: String = ""
)
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = ""
)

data class AppUiState(
    val signedIn: Boolean = false,
    val currentUserId: String = "",
    val userName: String = "",
    val household: Household? = null,
    val members: List<Member> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val events: List<Event> = emptyList(),
    val eventTypes: List<EventType> = emptyList(),
    val notes: List<Note> = emptyList(),
    val tasks: List<HouseholdTask> = emptyList(),
    val birthdays: List<Birthday> = emptyList(),
    val mediaRequests: List<MediaRequest> = emptyList(),
    val monthlyBudget: Double = 0.0,
    val selectedTab: Tab = Tab.Home,
    val loading: Boolean = true,
    val error: String? = null,
    val checkingUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null
)

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Accueil", Icons.Filled.Home),
    Shopping("Courses", Icons.Filled.ShoppingCart),
    Tasks("Taches", Icons.Filled.CheckCircle),
    Calendar("Agenda", Icons.Filled.CalendarMonth),
    Requests("Demandes", Icons.Filled.ViewList),
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
                    currentUserId = user.uid,
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

    fun checkForUpdate(context: Context? = null, silent: Boolean = false, notify: Boolean = false) {
        if (state.checkingUpdate) return
        state = state.copy(checkingUpdate = true, error = if (silent) state.error else null)
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val json = URL(UPDATE_MANIFEST_URL).readText()
                val manifest = JSONObject(json)
                UpdateInfo(
                    versionCode = manifest.optInt("versionCode", 0),
                    versionName = manifest.optString("versionName"),
                    apkUrl = manifest.optString("apkUrl"),
                    notes = manifest.optString("notes")
                )
            }.onSuccess { info ->
                withContext(Dispatchers.Main) {
                    val update = info.takeIf { it.versionCode > BuildConfig.VERSION_CODE && it.apkUrl.isNotBlank() }
                    if (update != null && notify && context != null) {
                        notifyUpdateOnce(context, update)
                    }
                    state = state.copy(
                        checkingUpdate = false,
                        updateInfo = update,
                        error = if (update == null && !silent) "Mon Foyer est deja a jour." else state.error
                    )
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    state = state.copy(
                        checkingUpdate = false,
                        error = if (silent) state.error else "Verification impossible. Le fichier de mise a jour doit etre public."
                    )
                }
            }
        }
    }

    private fun notifyUpdateOnce(context: Context, update: UpdateInfo) {
        val prefs = context.getSharedPreferences("mon_foyer_updates", Context.MODE_PRIVATE)
        val key = "notified_version_code"
        if (prefs.getInt(key, 0) >= update.versionCode) return
        ReminderReceiver.showNow(
            context,
            90000 + update.versionCode,
            "Mise a jour Mon Foyer",
            "La version ${update.versionName} est disponible."
        )
        prefs.edit().putInt(key, update.versionCode).apply()
    }

    fun clearUpdateInfo() {
        state = state.copy(updateInfo = null)
    }

    fun createHousehold(name: String) {
        val user = auth.currentUser ?: return
        val code = inviteCode(user.uid)
        val householdRef = db.collection("households").document()
        val member = mapOf(
            "name" to (user.displayName ?: "Membre"),
            "email" to (user.email ?: ""),
            "role" to "admin",
            "color" to memberColorLong(user.uid),
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
                                "role" to "member",
                                "color" to memberColorLong(user.uid),
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
        val amount = value.parseMoneyOrNull() ?: return
        db.collection("households").document(household.id).update("monthlyBudget", amount)
    }

    fun addShoppingItem(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val match = Regex("""^(\d+)\s+(.+)$""").find(clean)
        val quantity = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        val itemName = match?.groupValues?.getOrNull(2)?.trim().orEmpty().ifBlank { clean }
        add(
            "shoppingItems",
            mapOf(
                "name" to itemName,
                "done" to false,
                "quantity" to quantity,
                "category" to shoppingCategory(itemName),
                "favorite" to false
            )
        )
    }
    fun addBill(label: String, amount: String) {
        val cleanLabel = label.trim()
        val cleanAmount = amount.parseMoneyOrNull() ?: return
        if (cleanLabel.isBlank() || cleanAmount <= 0.0) return
        add("bills", mapOf("label" to cleanLabel, "amount" to cleanAmount, "paid" to false))
    }
    fun addEvent(
        title: String,
        description: String,
        location: String,
        owner: String,
        date: String,
        time: String,
        allDay: Boolean,
        recurrence: String,
        type: EventType
    ) {
        if (title.isBlank()) return
        add(
            "events",
            mapOf(
                "title" to title,
                "description" to description,
                "location" to location,
                "owner" to owner,
                "date" to date,
                "time" to time,
                "allDay" to allDay,
                "recurrence" to recurrence,
                "typeName" to type.name,
                "typeIcon" to type.icon,
                "typeColor" to type.color
            )
        )
    }
    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        location: String,
        owner: String,
        date: String,
        time: String,
        allDay: Boolean,
        recurrence: String,
        type: EventType
    ) {
        if (title.isBlank()) return
        val household = state.household ?: return
        db.collection("households").document(household.id).collection("events").document(eventId)
            .update(
                mapOf(
                    "title" to title.trim(),
                    "description" to description,
                    "location" to location,
                    "owner" to owner,
                    "date" to date,
                    "time" to time,
                    "allDay" to allDay,
                    "recurrence" to recurrence,
                    "typeName" to type.name,
                    "typeIcon" to type.icon,
                    "typeColor" to type.color,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }
    fun addEventType(name: String, icon: String, color: Long) {
        if (name.isBlank()) return
        add("eventTypes", mapOf("name" to name, "icon" to icon, "color" to color))
    }
    fun addNote(title: String, body: String) = add("notes", mapOf("title" to title, "body" to body))
    fun addTask(title: String, description: String, dueDate: String, emoji: String, member: Member?) {
        if (title.isBlank()) return
        add(
            "tasks",
            mapOf(
                "title" to title,
                "description" to description,
                "dueDate" to dueDate,
                "emoji" to emoji,
                "assigneeId" to (member?.id ?: ""),
                "assigneeName" to (member?.name ?: "A assigner"),
                "color" to (member?.color ?: memberColorLong(title)),
                "done" to false
            )
        )
    }
    fun updateTask(taskId: String, title: String, description: String, dueDate: String, emoji: String, member: Member?) {
        if (title.isBlank()) return
        val household = state.household ?: return
        db.collection("households").document(household.id).collection("tasks").document(taskId)
            .update(
                mapOf(
                    "title" to title.trim(),
                    "description" to description,
                    "dueDate" to dueDate,
                    "emoji" to emoji,
                    "assigneeId" to (member?.id ?: ""),
                    "assigneeName" to (member?.name ?: "A assigner"),
                    "color" to (member?.color ?: memberColorLong(title)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
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
    fun updateBirthday(birthdayId: String, name: String, date: LocalDate, birthYear: String) {
        val household = state.household ?: return
        if (name.isBlank()) return
        db.collection("households").document(household.id).collection("birthdays").document(birthdayId)
            .update(
                mapOf(
                    "name" to name.trim(),
                    "date" to date.format(DateTimeFormatter.ISO_DATE),
                    "birthYear" to (birthYear.toIntOrNull() ?: date.year),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }

    fun addMediaRequest(title: String, kind: String) {
        val household = state.household ?: return
        val user = auth.currentUser ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val requesterName = state.members.firstOrNull { it.id == user.uid }?.name
            ?: state.userName.ifBlank { "Membre" }
        db.collection("households").document(household.id).collection("requests")
            .add(
                mapOf(
                    "title" to cleanTitle,
                    "kind" to kind,
                    "requesterId" to user.uid,
                    "requesterName" to requesterName,
                    "status" to "pending",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Demande impossible.") }
    }

    fun updateMediaRequestStatus(request: MediaRequest, status: String) {
        val household = state.household ?: return
        if (!state.isCurrentUserAdmin()) return
        db.collection("households").document(household.id).collection("requests").document(request.id)
            .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }

    fun toggleShopping(item: ShoppingItem) = update("shoppingItems", item.id, "done", !item.done)
    fun toggleShoppingFavorite(item: ShoppingItem) = update("shoppingItems", item.id, "favorite", !item.favorite)
    fun toggleBill(bill: Bill) = update("bills", bill.id, "paid", !bill.paid)
    fun toggleTask(task: HouseholdTask) = update("tasks", task.id, "done", !task.done)
    fun updateShoppingItem(itemId: String, name: String, quantity: String, category: String) {
        val household = state.household ?: return
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        db.collection("households").document(household.id).collection("shoppingItems").document(itemId)
            .update(
                mapOf(
                    "name" to cleanName,
                    "quantity" to (quantity.toIntOrNull() ?: 1).coerceAtLeast(1),
                    "category" to category.ifBlank { shoppingCategory(cleanName) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }
    fun updateBill(billId: String, label: String, amount: String) {
        val household = state.household ?: return
        if (label.isBlank()) return
        val cleanAmount = amount.parseMoneyOrNull() ?: 0.0
        db.collection("households").document(household.id).collection("bills").document(billId)
            .update(mapOf("label" to label.trim(), "amount" to cleanAmount, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }
    fun updateNote(noteId: String, title: String, body: String) {
        val household = state.household ?: return
        if (title.isBlank() && body.isBlank()) return
        db.collection("households").document(household.id).collection("notes").document(noteId)
            .update(mapOf("title" to title.trim(), "body" to body.trim(), "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }
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

    fun updateMember(memberId: String, name: String, color: Long) {
        val household = state.household ?: return
        val cleanName = name.trim().ifBlank { "Membre" }
        db.collection("households").document(household.id).collection("members").document(memberId)
            .update(mapOf("name" to cleanName, "color" to color, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }

    fun leaveHousehold() {
        val household = state.household ?: return
        val user = auth.currentUser ?: return
        val batch = db.batch()
        batch.delete(db.collection("households").document(household.id).collection("members").document(user.uid))
        batch.delete(db.collection("users").document(user.uid))
        batch.commit()
            .addOnSuccessListener {
                clearListeners()
                state = state.copy(household = null, members = emptyList(), selectedTab = Tab.Home)
            }
            .addOnFailureListener { setError(it.message ?: "Impossible de quitter le foyer.") }
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
                        inviteCode = doc.getString("inviteCode") ?: "",
                        ownerId = doc.getString("ownerId") ?: ""
                    ),
                    monthlyBudget = doc.getDouble("monthlyBudget") ?: 0.0,
                    loading = false
                )
            }
        }
        listeners += householdRef.collection("members").addSnapshotListener { snap, _ ->
            val ownerId = state.household?.ownerId.orEmpty()
            state = state.copy(members = snap?.documents?.map {
                Member(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    email = it.getString("email") ?: "",
                    role = it.getString("role") ?: if (it.id == ownerId) "admin" else "member",
                    color = it.getLong("color") ?: memberColorLong(it.id)
                )
            }.orEmpty())
        }
        listeners += householdRef.collection("shoppingItems").addSnapshotListener { snap, _ ->
            state = state.copy(shopping = snap?.documents?.map {
                ShoppingItem(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    done = it.getBoolean("done") ?: false,
                    quantity = (it.getLong("quantity") ?: 1).toInt(),
                    category = it.getString("category") ?: shoppingCategory(it.getString("name") ?: ""),
                    favorite = it.getBoolean("favorite") ?: false
                )
            }.orEmpty())
        }
        listeners += householdRef.collection("bills").addSnapshotListener { snap, _ ->
            state = state.copy(bills = snap?.documents?.map { Bill(it.id, it.getString("label") ?: "", it.getDouble("amount") ?: 0.0, it.getBoolean("paid") ?: false) }.orEmpty())
        }
        listeners += householdRef.collection("events").addSnapshotListener { snap, _ ->
            state = state.copy(events = snap?.documents?.map {
                Event(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    owner = it.getString("owner") ?: "",
                    date = it.getString("date") ?: "",
                    description = it.getString("description") ?: "",
                    location = it.getString("location") ?: "",
                    typeName = it.getString("typeName") ?: "Repas",
                    typeIcon = it.getString("typeIcon") ?: "🍴",
                    typeColor = it.getLong("typeColor") ?: 0xFFE86675,
                    allDay = it.getBoolean("allDay") ?: false,
                    time = it.getString("time") ?: "00:00",
                    recurrence = it.getString("recurrence") ?: "Aucune"
                )
            }.orEmpty())
        }
        listeners += householdRef.collection("eventTypes").addSnapshotListener { snap, _ ->
            val savedTypes = snap?.documents?.map {
                EventType(
                    id = it.id,
                    name = it.getString("name") ?: "",
                    icon = it.getString("icon") ?: "🍴",
                    color = it.getLong("color") ?: 0xFFE86675
                )
            }.orEmpty()
            state = state.copy(eventTypes = defaultEventTypes() + savedTypes)
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
                    color = it.getLong("color") ?: 0xFF174C43,
                    description = it.getString("description") ?: "",
                    dueDate = it.getString("dueDate") ?: "",
                    emoji = it.getString("emoji") ?: "🙂"
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
        listeners += householdRef.collection("requests").addSnapshotListener { snap, _ ->
            state = state.copy(mediaRequests = snap?.documents?.map {
                MediaRequest(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    kind = it.getString("kind") ?: "Livre",
                    requesterId = it.getString("requesterId") ?: "",
                    requesterName = it.getString("requesterName") ?: "",
                    status = it.getString("status") ?: "pending",
                    adminNote = it.getString("adminNote") ?: ""
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

private val Cream = Color(0xFFFFFAF1)
private val DeepGreen = Color(0xFF103F37)
private val Leaf = Color(0xFF42A47D)
private val Mint = Color(0xFFD5F4E8)
private val Lemon = Color(0xFFFFD86B)
private val Coral = Color(0xFFFF7E6E)
private val Sky = Color(0xFFCDEBFF)
private val Lilac = Color(0xFFE5D8FF)
private val Apricot = Color(0xFFFFD0A8)
private val SoftGrey = Color(0xFFF2EEE6)
private val CardBorder = Color(0xFFE7DDCF)
private val Ink = Color(0xFF17201D)
private val Muted = Color(0xFF7F776D)
private val Paper = Color(0xFFFFFFFB)
private val Clay = Color(0xFFC96D52)

private val AppRadius = 22.dp
private val PanelRadius = 34.dp
private val FieldRadius = 18.dp

data class ModuleTile(
    val tab: Tab,
    val title: String,
    val subtitle: String,
    val count: String?,
    val colors: List<Color>,
    val icon: ImageVector,
    val emoji: String,
    val accent: Color
)

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
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    NotificationPermissionEffect()
    LaunchedEffect(Unit) {
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
            when (vm.state.selectedTab) {
                Tab.Home -> Dashboard(vm)
                Tab.Shopping -> ShoppingScreen(vm)
                Tab.Tasks -> TasksScreen(vm)
                Tab.Calendar -> AgendaScreen(vm)
                Tab.Requests -> RequestsScreen(vm)
                Tab.Birthdays -> BirthdaysScreen(vm)
                Tab.Notes -> NotesScreen(vm)
                Tab.Members -> MembersScreen(vm)
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
            Surface(color = Paper.copy(alpha = 0.78f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)) {
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
fun Dashboard(vm: MonFoyerViewModel) {
    val state = vm.state
    val modules = listOf(
        ModuleTile(Tab.Calendar, "Agenda", "Rendez-vous", state.events.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFE5A3), Color(0xFFFFBE73)), Icons.Filled.CalendarMonth, "📅", Color(0xFFE28B21)),
        ModuleTile(Tab.Tasks, "Taches", "A faire", state.tasks.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFC9EFFF), Color(0xFF92D7F6)), Icons.Filled.CheckCircle, "✅", Color(0xFF2E89C9)),
        ModuleTile(Tab.Shopping, "Courses", "Liste commune", state.shopping.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFD3F7DE), Color(0xFF85DFAF)), Icons.Filled.ShoppingCart, "🛒", Color(0xFF139567)),
        ModuleTile(Tab.Requests, "Demandes", "Films & livres", state.pendingRequestCount().takeIf { it > 0 }?.toString(), listOf(Color(0xFFE5D8FF), Color(0xFFC9D9FF)), Icons.Filled.ViewList, "🎬", Color(0xFF6B63D8)),
        ModuleTile(Tab.Birthdays, "Anniversaires", "A ne pas oublier", state.birthdays.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFD6E3), Color(0xFFD8CBFF)), Icons.Filled.Group, "🎂", Color(0xFFB256B4)),
        ModuleTile(Tab.Notes, "Notes", "Pense-betes", state.notes.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFD9B8), Color(0xFFFFB8A8)), Icons.Filled.EditNote, "📝", Clay),
        ModuleTile(Tab.Members, "Foyer", "Membres & code", state.members.size.toString(), listOf(Color(0xFFD6F4EF), Color(0xFFBCE8F5)), Icons.Filled.Group, "🏡", DeepGreen)
    )
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Tableau", fontSize = 40.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black, color = Ink)
                Text("du foyer", fontSize = 40.sp, lineHeight = 39.sp, fontWeight = FontWeight.Black, color = DeepGreen)
            }
            Surface(color = Lemon, shape = RoundedCornerShape(22.dp), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✨", fontSize = 27.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Les petites choses du quotidien, rangees au meme endroit.", fontSize = 15.sp, lineHeight = 19.sp, color = Muted, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        HomeInsightStrip(state)
        Spacer(Modifier.height(16.dp))
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
    val titleSize = if (tile.title.length > 12) 21.sp else 24.sp
    Box(
        modifier = Modifier
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(tile.colors))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.24f),
                radius = 76.dp.toPx(),
                center = Offset(size.width - 8.dp.toPx(), size.height + 10.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = 34.dp.toPx(),
                center = Offset(18.dp.toPx(), 24.dp.toPx())
            )
            drawCircle(
                color = tile.accent.copy(alpha = 0.16f),
                radius = 42.dp.toPx(),
                center = Offset(size.width - 26.dp.toPx(), 32.dp.toPx())
            )
        }
        Surface(color = Paper.copy(alpha = 0.84f), shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(tile.icon, contentDescription = null, tint = tile.accent, modifier = Modifier.size(27.dp))
            }
        }
        Text(
            tile.emoji,
            fontSize = 46.sp,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 4.dp, y = 6.dp)
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 30.dp)
        ) {
            Text(tile.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Black, color = tile.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                tile.title,
                fontSize = titleSize,
                lineHeight = (titleSize.value + 2).sp,
                fontWeight = FontWeight.Black,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        tile.count?.let {
            Surface(color = tile.accent, shape = CircleShape, shadowElevation = 3.dp, modifier = Modifier.align(Alignment.TopEnd)) {
                Text(it, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
fun HomeInsightStrip(state: AppUiState) {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    val todayEvents = state.events.count { it.date == today }
    val openTasks = state.tasks.count { !it.done }
    val nextBirthday = state.birthdays.minByOrNull { it.nextBirthday() }
    val remainingShopping = state.shopping.count { !it.done }
    val birthdayLabel = nextBirthday?.let {
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it.nextBirthday()).coerceAtLeast(0)
        "J-$days ${it.name}"
    } ?: "Aucun"
    val insights = listOf(
        "📅 $todayEvents aujourd'hui",
        "✅ $openTasks a faire",
        "🛒 $remainingShopping courses",
        "🎂 $birthdayLabel"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(116.dp)
    ) {
        gridItems(insights) { insight ->
            Surface(color = Paper, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), shadowElevation = 1.dp) {
                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
                    Text(insight, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, body: String) {
    Surface(color = SoftGrey, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 34.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(body, fontSize = 14.sp, lineHeight = 17.sp, color = Muted, fontWeight = FontWeight.Medium)
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
        listOf(Tab.Home, Tab.Calendar, Tab.Tasks, Tab.Shopping, Tab.Requests, Tab.Birthdays, Tab.Notes, Tab.Members).forEach { tab ->
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

@Composable
fun ShoppingScreen(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    val checkedCount = vm.state.shopping.count { it.done }
    val favoriteItems = vm.state.shopping.filter { it.favorite }.distinctBy { it.name }.take(4)
    val sortedItems = vm.state.shopping.sortedWith(compareBy<ShoppingItem> { it.done }.thenBy { it.category }.thenBy { it.name })
    ModulePanel(title = "Liste de course") {
        item {
            QuickAdd(value = name, onChange = { name = it }, label = "Ajouter un article...") {
                vm.addShoppingItem(name)
                name = ""
            }
            if (favoriteItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    favoriteItems.forEach { item ->
                        TaskFilterChip(item.name, false) {
                            vm.addShoppingItem("${item.quantity} ${item.name}")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (checkedCount > 0) {
                SecondaryButton(text = "Supprimer elements coches ($checkedCount)", icon = Icons.Filled.Delete) {
                    confirmClear = true
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (sortedItems.isEmpty()) {
            item { EmptyState("🛒", "Liste vide", "Ajoute un article, avec une quantite si besoin : 2 lait.") }
        }
        items(sortedItems) { item ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingItem = item }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.toggleShopping(item) }, modifier = Modifier.size(48.dp)) {
                    Icon(if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Etat", tint = if (item.done) DeepGreen else Color(0xFFDADADA), modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Text("${item.quantity} x - ${item.category}", fontSize = 15.sp, color = Muted)
                }
                Text(
                    if (item.favorite) "★" else "☆",
                    color = if (item.favorite) Color(0xFFE8A64F) else Muted,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { vm.toggleShoppingFavorite(item) }.padding(horizontal = 8.dp)
                )
                DeleteButton { vm.delete("shoppingItems", item.id) }
                }
            }
        }
    }
    if (confirmClear) {
        ConfirmDeleteDialog(
            title = "Vider les elements coches ?",
            message = "$checkedCount article(s) coche(s) vont etre supprimes de la liste.",
            onConfirm = {
                vm.deleteCheckedShoppingItems()
                confirmClear = false
            },
            onDismiss = { confirmClear = false }
        )
    }
    editingItem?.let { item ->
        EditShoppingSheet(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { itemName, quantity, category ->
                vm.updateShoppingItem(item.id, itemName, quantity, category)
                editingItem = null
            }
        )
    }
}

@Composable
fun BudgetScreen(vm: MonFoyerViewModel) {
    var budget by remember(vm.state.monthlyBudget) { mutableStateOf(if (vm.state.monthlyBudget == 0.0) "" else vm.state.monthlyBudget.toString()) }
    var showAddBill by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<Bill?>(null) }
    var confirmDeleteBill by remember { mutableStateOf<Bill?>(null) }
    var filter by remember { mutableStateOf("A payer") }
    val total = vm.state.bills.sumOf { it.amount }
    val paid = vm.state.bills.filter { it.paid }.sumOf { it.amount }
    val unpaidBills = vm.state.bills.filterNot { it.paid }
    val unpaid = unpaidBills.sumOf { it.amount }
    val remaining = vm.state.monthlyBudget - total
    val progress = if (vm.state.monthlyBudget <= 0.0) 0f else (total / vm.state.monthlyBudget).coerceIn(0.0, 1.0).toFloat()
    val filteredBills = when (filter) {
        "Payees" -> vm.state.bills.filter { it.paid }
        "Toutes" -> vm.state.bills
        else -> unpaidBills
    }.sortedWith(compareBy<Bill> { it.paid }.thenByDescending { it.amount })
    ModulePanel(title = "Budget") {
        item {
            BudgetHero(
                budget = vm.state.monthlyBudget,
                unpaid = unpaid,
                paid = paid,
                remaining = remaining,
                progress = progress
            )
            Spacer(Modifier.height(14.dp))
            Text("Budget du mois", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftInput(
                    value = budget,
                    onValueChange = { budget = it },
                    label = "Ex: 1800",
                    keyboardType = KeyboardType.Decimal,
                    leadingIcon = Icons.Filled.Payments,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = DeepGreen,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(62.dp).clickable { vm.setMonthlyBudget(budget) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Enregistrer", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("1200", "1600", "2000").forEach { value ->
                    BudgetQuickAmount(value) {
                        budget = value
                        vm.setMonthlyBudget(value)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Factures", fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("${vm.state.bills.size} ligne(s) - ${moneyText(total)} au total", fontSize = 14.sp, color = Muted, fontWeight = FontWeight.Bold)
                }
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(58.dp).clickable { showAddBill = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("A payer", filter == "A payer") { filter = "A payer" }
                TaskFilterChip("Payees", filter == "Payees") { filter = "Payees" }
                TaskFilterChip("Toutes", filter == "Toutes") { filter = "Toutes" }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (filteredBills.isEmpty()) {
            item {
                EmptyState(
                    emoji = "💶",
                    title = if (vm.state.bills.isEmpty()) "Aucune facture" else "Rien dans ce filtre",
                    body = if (vm.state.bills.isEmpty()) "Ajoute ton loyer, tes abonnements ou tes factures du mois." else "Change le filtre pour retrouver les autres lignes."
                )
            }
        }
        items(filteredBills) { bill ->
            BudgetBillRow(
                bill = bill,
                onToggle = { vm.toggleBill(bill) },
                onEdit = { editingBill = bill },
                onDelete = { confirmDeleteBill = bill }
            )
        }
    }
    if (showAddBill) {
        AddBillSheet(
            onDismiss = { showAddBill = false },
            onAdd = { billLabel, billAmount ->
                vm.addBill(billLabel, billAmount)
                showAddBill = false
            }
        )
    }
    confirmDeleteBill?.let { bill ->
        ConfirmDeleteDialog(
            title = "Supprimer cette facture ?",
            message = "${bill.label} sera retiree du budget.",
            onConfirm = {
                vm.delete("bills", bill.id)
                confirmDeleteBill = null
            },
            onDismiss = { confirmDeleteBill = null }
        )
    }
    editingBill?.let { bill ->
        EditBillSheet(
            bill = bill,
            onDismiss = { editingBill = null },
            onSave = { billLabel, billAmount ->
                vm.updateBill(bill.id, billLabel, billAmount)
                editingBill = null
            }
        )
    }
}

@Composable
fun BudgetHero(budget: Double, unpaid: Double, paid: Double, remaining: Double, progress: Float) {
    val remainingColor = if (remaining < 0) Coral else DeepGreen
    Surface(
        color = Color(0xFFFFF0D9),
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Reste a vivre", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF9B6C2C))
                    Text(moneyText(remaining), fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, color = remainingColor)
                    Text(
                        if (remaining < 0) "Budget depasse, on garde un oeil dessus." else "Apres les factures non payees.",
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        color = Muted,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("💶", fontSize = 48.sp)
            }
            Spacer(Modifier.height(16.dp))
            BudgetProgressBar(progress)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BudgetMetricCard("Budget", moneyText(budget), Color.White.copy(alpha = 0.72f), Modifier.weight(1f))
                BudgetMetricCard("A payer", moneyText(unpaid), Color.White.copy(alpha = 0.72f), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            BudgetMetricCard("Deja paye", moneyText(paid), Color.White.copy(alpha = 0.72f), Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun BudgetProgressBar(progress: Float) {
    Box(
        Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.7f))
    ) {
        Box(
            Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(18.dp).clip(RoundedCornerShape(50))
                .background(if (progress > 0.92f) Coral else DeepGreen)
        )
    }
}

@Composable
fun BudgetMetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color, shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Text(value, color = Ink, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun BudgetQuickAmount(value: String, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text("$value EUR", color = DeepGreen, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

@Composable
fun BudgetBillRow(bill: Bill, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = if (bill.paid) Color(0xFFF2FAF5) else Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (bill.paid) Color(0xFFCFE8D8) else CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (bill.paid) DeepGreen else SoftGrey,
                shape = CircleShape,
                modifier = Modifier.size(52.dp).clickable(onClick = onToggle)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (bill.paid) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Payee",
                        tint = if (bill.paid) Color.White else Muted,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(bill.label.ifBlank { "Facture" }, fontSize = 21.sp, lineHeight = 23.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (bill.paid) "Payee" else "A payer", fontSize = 14.sp, color = if (bill.paid) DeepGreen else Muted, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(moneyText(bill.amount), fontSize = 17.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Modifier", tint = Muted, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted, modifier = Modifier.size(21.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsScreen(vm: MonFoyerViewModel) {
    var selectedMemberId by remember(vm.state.members) {
        mutableStateOf(vm.state.members.firstOrNull { it.id == vm.state.currentUserId }?.id ?: vm.state.members.firstOrNull()?.id.orEmpty())
    }
    var kind by remember { mutableStateOf("Livre") }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<MediaRequest?>(null) }
    val members = vm.state.members.ifEmpty { listOf(Member(vm.state.currentUserId, vm.state.userName.ifBlank { "Moi" })) }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId } ?: members.first()
    val requests = vm.state.mediaRequests
        .filter { it.requesterId == selectedMember.id && it.kind == kind }
        .sortedWith(compareBy<MediaRequest> { it.status != "pending" }.thenBy { it.title.lowercase(Locale.FRANCE) })
    val isAdmin = vm.state.isCurrentUserAdmin()

    ModulePanel(title = "Demandes") {
        item {
            Text("Par personne", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height((((members.size + 1) / 2) * 54).dp)
            ) {
                gridItems(members) { member ->
                    MemberRequestTab(
                        member = member,
                        selected = selectedMember.id == member.id,
                        pendingCount = vm.state.mediaRequests.count { it.requesterId == member.id && it.status == "pending" },
                        onClick = { selectedMemberId = member.id }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("Livres", kind == "Livre") { kind = "Livre" }
                TaskFilterChip("Films / series", kind == "FilmSerie") { kind = "FilmSerie" }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(selectedMember.name.ifBlank { "Membre" }, fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text(if (kind == "Livre") "Demandes de livres" else "Demandes de films et series", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (selectedMember.id == vm.state.currentUserId) {
                    Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(58.dp).clickable { showAdd = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (requests.isEmpty()) {
            item {
                EmptyState(
                    emoji = if (kind == "Livre") "📚" else "🎬",
                    title = "Aucune demande",
                    body = if (selectedMember.id == vm.state.currentUserId) "Ajoute une idee, elle apparaitra ici pour l'admin." else "Cette personne n'a rien demande dans cette categorie."
                )
            }
        }
        items(requests) { request ->
            MediaRequestCard(
                request = request,
                canModerate = isAdmin && request.status == "pending",
                canDelete = isAdmin || request.requesterId == vm.state.currentUserId,
                onApprove = { vm.updateMediaRequestStatus(request, "approved") },
                onReject = { vm.updateMediaRequestStatus(request, "rejected") },
                onDelete = { confirmDelete = request }
            )
        }
    }
    if (showAdd) {
        AddMediaRequestSheet(
            kind = kind,
            onDismiss = { showAdd = false },
            onAdd = { title ->
                vm.addMediaRequest(title, kind)
                showAdd = false
            }
        )
    }
    confirmDelete?.let { request ->
        ConfirmDeleteDialog(
            title = "Supprimer la demande ?",
            message = "${request.title} sera retire de la liste.",
            onConfirm = {
                vm.delete("requests", request.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
fun MemberRequestTab(member: Member, selected: Boolean, pendingCount: Int, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DeepGreen else CardBorder),
        modifier = Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(member.color), shape = CircleShape, modifier = Modifier.size(26.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.name.take(1).uppercase().ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(member.name.ifBlank { "Membre" }, color = if (selected) Color.White else Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (pendingCount > 0) {
                Text(pendingCount.toString(), color = if (selected) Lemon else DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun MediaRequestCard(
    request: MediaRequest,
    canModerate: Boolean,
    canDelete: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (request.status) {
        "approved" -> DeepGreen
        "rejected" -> Coral
        else -> Color(0xFFE39318)
    }
    val statusText = when (request.status) {
        "approved" -> "Valide"
        "rejected" -> "Refuse"
        else -> "En attente"
    }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(if (request.kind == "Livre") "📚" else "🎬", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.title, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("Demande de ${request.requesterName.ifBlank { "Membre" }}", fontSize = 14.sp, color = Muted, fontWeight = FontWeight.Bold)
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            if (canModerate || canDelete) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canModerate) {
                        RequestActionButton("Valider", DeepGreen, Modifier.weight(1f), onApprove)
                        RequestActionButton("Refuser", Coral, Modifier.weight(1f), onReject)
                    }
                    if (canDelete) {
                        Surface(color = SoftGrey, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp).clickable(onClick = onDelete)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestActionButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(14.dp), modifier = modifier.height(48.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun AgendaScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val selectedDateText = selectedDate.format(DateTimeFormatter.ISO_DATE)
    val selectedEvents = vm.state.events.filter { it.date == selectedDateText }
    ModulePanel(title = "Calendrier") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
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
                EmptyState("🌤️", "Journee libre", "Aucun evenement prevu ce jour.")
            } else {
                selectedEvents.forEach { event ->
                    CalendarEventPill(event, onClick = { editingEvent = event })
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(vm.state.events) { event ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingEvent = event }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(listOf(event.date, if (event.allDay) "Toute la journee" else event.time, event.owner).filter { it.isNotBlank() }.joinToString(" - "), fontSize = 16.sp, color = Muted)
                }
                DeleteButton { vm.delete("events", event.id) }
                }
            }
        }
    }
    if (showAddSheet) {
        AddEventSheet(
            members = vm.state.members,
            eventTypes = vm.state.eventTypes.ifEmpty { defaultEventTypes() },
            initialDate = selectedDate,
            onDismiss = { showAddSheet = false },
            onAddType = { name, icon, color -> vm.addEventType(name, icon, color) },
            onAdd = { title, description, location, owner, date, time, allDay, recurrence, type ->
                vm.addEvent(title, description, location, owner, date.format(DateTimeFormatter.ISO_DATE), time, allDay, recurrence, type)
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                showAddSheet = false
            }
        )
    }
    editingEvent?.let { event ->
        AddEventSheet(
            members = vm.state.members,
            eventTypes = vm.state.eventTypes.ifEmpty { defaultEventTypes() },
            initialDate = runCatching { LocalDate.parse(event.date) }.getOrDefault(selectedDate),
            event = event,
            onDismiss = { editingEvent = null },
            onAddType = { name, icon, color -> vm.addEventType(name, icon, color) },
            onAdd = { title, description, location, owner, date, time, allDay, recurrence, type ->
                vm.updateEvent(event.id, title, description, location, owner, date.format(DateTimeFormatter.ISO_DATE), time, allDay, recurrence, type)
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                editingEvent = null
            }
        )
    }
}

@Composable
fun TasksScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<HouseholdTask?>(null) }
    var taskToDelete by remember { mutableStateOf<HouseholdTask?>(null) }
    var statusFilter by remember { mutableStateOf("todo") }
    var memberFilter by remember { mutableStateOf("") }
    val filteredTasks = vm.state.tasks
        .filter { task ->
            when (statusFilter) {
                "todo" -> !task.done
                "done" -> task.done
                else -> true
            }
        }
        .filter { task -> memberFilter.isBlank() || task.assigneeId == memberFilter }
        .sortedWith(compareBy<HouseholdTask> { it.done }.thenBy { it.dueDate.ifBlank { "9999-12-31" } }.thenBy { it.title })
    ModulePanel(title = "Taches") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Spacer(Modifier.width(10.dp))
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("A faire", statusFilter == "todo") { statusFilter = "todo" }
                TaskFilterChip("Terminees", statusFilter == "done") { statusFilter = "done" }
                TaskFilterChip("Toutes", statusFilter == "all") { statusFilter = "all" }
            }
            if (vm.state.members.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TaskFilterChip("Tous", memberFilter.isBlank()) { memberFilter = "" }
                    vm.state.members.take(3).forEach { member ->
                        TaskFilterChip(member.name.ifBlank { "Membre" }, memberFilter == member.id) { memberFilter = member.id }
                    }
                }
            }
        }
        if (filteredTasks.isEmpty()) {
            item { EmptyState("✅", "Rien a faire ici", "Les taches apparaitront selon ton filtre.") }
        }
        items(filteredTasks) { task ->
            TaskCard(
                task = task,
                onToggle = { vm.toggleTask(task) },
                onEdit = { editingTask = task },
                onDelete = { taskToDelete = task }
            )
        }
    }
    if (showAddSheet) {
        AddTaskSheet(
            members = vm.state.members,
            onDismiss = { showAddSheet = false },
            onAdd = { title, description, dueDate, emoji, member ->
                vm.addTask(title, description, dueDate, emoji, member)
                showAddSheet = false
            }
        )
    }
    editingTask?.let { task ->
        AddTaskSheet(
            members = vm.state.members,
            task = task,
            onDismiss = { editingTask = null },
            onAdd = { title, description, dueDate, emoji, member ->
                vm.updateTask(task.id, title, description, dueDate, emoji, member)
                editingTask = null
            }
        )
    }
    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            title = "Supprimer cette tache ?",
            message = "La tache \"${task.title}\" sera supprimee pour tout le foyer.",
            onConfirm = {
                vm.delete("tasks", task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
}

@Composable
fun BirthdaysScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var birthdayToDelete by remember { mutableStateOf<Birthday?>(null) }
    var editingBirthday by remember { mutableStateOf<Birthday?>(null) }
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
        if (vm.state.birthdays.isEmpty()) {
            item { EmptyState("🎂", "Aucun anniversaire", "Ajoute les dates importantes du foyer.") }
        }
        items(vm.state.birthdays.sortedBy { it.nextBirthday() }) { birthday ->
            BirthdayRow(birthday, onClick = { editingBirthday = birthday }) { birthdayToDelete = birthday }
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
    editingBirthday?.let { birthday ->
        AddBirthdaySheet(
            birthday = birthday,
            onDismiss = { editingBirthday = null },
            onAdd = { name, date, year ->
                vm.updateBirthday(birthday.id, name, date, year)
                editingBirthday = null
            }
        )
    }
}

@Composable
fun NotesScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<Note?>(null) }
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
        if (vm.state.notes.isEmpty()) {
            item { EmptyState("📝", "Aucune note", "Note une idee, un code, ou une petite info a garder.") }
        }
        items(vm.state.notes) { note ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingNote = note }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(note.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(note.body, fontSize = 16.sp, color = Muted)
                }
                DeleteButton { vm.delete("notes", note.id) }
                }
            }
        }
    }
    editingNote?.let { note ->
        EditNoteSheet(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { noteTitle, noteBody ->
                vm.updateNote(note.id, noteTitle, noteBody)
                editingNote = null
            }
        )
    }
}

@Composable
fun MembersScreen(vm: MonFoyerViewModel) {
    val state = vm.state
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var remindersEnabled by remember { mutableStateOf(ReminderScheduler.remindersEnabled(context)) }
    val currentMember = state.members.firstOrNull { it.id == state.currentUserId }
    val isAdmin = currentMember?.role == "admin"

    ModulePanel(title = "Mon foyer") {
        item {
            Surface(color = SoftGrey, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Code d'invitation", fontSize = 16.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            state.household?.inviteCode.orEmpty(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepGreen,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp).clickable {
                                clipboard.setText(AnnotatedString(state.household?.inviteCode.orEmpty()))
                                copied = true
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copier", tint = DeepGreen)
                            }
                        }
                    }
                    if (copied) {
                        Spacer(Modifier.height(6.dp))
                        Text("Code copie", fontSize = 14.sp, color = DeepGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(state.members.sortedWith(compareByDescending<Member> { it.role == "admin" }.thenBy { it.name.ifBlank { it.email } })) { member ->
            val canEdit = isAdmin || member.id == state.currentUserId
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit) { editingMember = member }
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(member.color), shape = CircleShape, modifier = Modifier.size(54.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                member.name.memberInitial(),
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.name.ifBlank { "Membre" }, fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(member.email.ifBlank { "Compte Google" }, fontSize = 14.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        color = if (member.role == "admin") DeepGreen else SoftGrey,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            if (member.role == "admin") "Admin" else "Membre",
                            color = if (member.role == "admin") Color.White else Muted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        item {
            Surface(color = Color(0xFFFFF3C9), shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("Anniversaires, rendez-vous et taches proches.", fontSize = 14.sp, color = Muted)
                    }
                    androidx.compose.material3.Switch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            ReminderScheduler.setRemindersEnabled(context, it)
                            ReminderScheduler.refresh(context, state.events, state.tasks, state.birthdays)
                        }
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Quitter le foyer", icon = Icons.Filled.Logout) {
                confirmLeave = true
            }
        }
    }

    editingMember?.let { member ->
        MemberEditorSheet(
            member = member,
            onDismiss = { editingMember = null },
            onSave = { name, color ->
                vm.updateMember(member.id, name, color)
                editingMember = null
            }
        )
    }
    if (confirmLeave) {
        ConfirmDeleteDialog(
            title = "Quitter le foyer",
            message = "Ton compte ne verra plus ce foyer. Tu pourras le rejoindre plus tard avec le code d'invitation.",
            confirmLabel = "Quitter",
            onConfirm = {
                confirmLeave = false
                vm.leaveHousehold()
            },
            onDismiss = { confirmLeave = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditorSheet(member: Member, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    var name by remember(member.id) { mutableStateOf(member.name.ifBlank { "Membre" }) }
    var color by remember(member.id) { mutableStateOf(member.color) }
    val colors = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95, 0xFF54B568, 0xFFB56AE8)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(Modifier.padding(top = 14.dp).width(70.dp).height(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFF9B9B9B)))
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Modifier le membre", fontSize = 31.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(color), shape = CircleShape, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.memberInitial(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(member.email.ifBlank { "Compte Google" }, color = Muted, fontSize = 15.sp)
                    Text(if (member.role == "admin") "Administrateur" else "Membre", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Nom affiche", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = name, onValueChange = { name = it }, label = "Nom du membre", leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(24.dp))
            Text("Couleur", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                colors.forEach { value ->
                    Surface(
                        color = Color(value),
                        shape = CircleShape,
                        modifier = Modifier.size(if (value == color) 58.dp else 48.dp).clickable { color = value }
                    ) {
                        if (value == color) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
            PrimaryButton(text = "Enregistrer", icon = Icons.Filled.CheckCircle) {
                onSave(name, color)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModulePanel(title: String, content: LazyListScope.() -> Unit) {
    Surface(
        color = Paper,
        shape = RoundedCornerShape(topStart = PanelRadius, topEnd = PanelRadius),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp).navigationBarsPadding()
        ) {
            item {
                Box(Modifier.width(58.dp).height(5.dp).clip(RoundedCornerShape(50)).background(CardBorder))
                Spacer(Modifier.height(18.dp))
                val mood = moduleMood(title)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(mood.second)
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = 64.dp.toPx(),
                            center = Offset(size.width - 18.dp.toPx(), 16.dp.toPx())
                        )
                        drawCircle(
                            color = DeepGreen.copy(alpha = 0.08f),
                            radius = 44.dp.toPx(),
                            center = Offset(16.dp.toPx(), size.height - 6.dp.toPx())
                        )
                    }
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Paper.copy(alpha = 0.82f), shape = RoundedCornerShape(20.dp)) {
                            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                                Text(mood.first, fontSize = 32.sp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontSize = 31.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(mood.third, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = DeepGreen.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShoppingSheet(item: ShoppingItem, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    EditSheetScaffold(title = "Modifier l'article", emoji = "🛒", onDismiss = onDismiss) {
        SoftInput(name, { name = it }, "Nom de l'article")
        Spacer(Modifier.height(10.dp))
        SoftInput(quantity, { quantity = it }, "Quantite", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Frais", "Epicerie", "Hygiene", "Maison").forEach { value ->
                TaskFilterChip(value, category == value) { category = value }
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Enregistrer", Icons.Filled.CheckCircle) { onSave(name, quantity, category) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillSheet(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val canAdd = label.isNotBlank() && (amount.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0
    EditSheetScaffold(title = "Nouvelle facture", emoji = "💶", onDismiss = onDismiss) {
        Text("Nom", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(8.dp))
        SoftInput(label, { label = it }, "Ex: Loyer, EDF, Netflix", leadingIcon = Icons.Filled.EditNote)
        Spacer(Modifier.height(18.dp))
        Text("Montant", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(8.dp))
        SoftInput(amount, { amount = it }, "Ex: 49.99", keyboardType = KeyboardType.Decimal, leadingIcon = Icons.Filled.Payments)
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { onAdd(label, amount) },
            enabled = canAdd,
            shape = RoundedCornerShape(FieldRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeepGreen,
                disabledContainerColor = Color(0xFFE1E1E1),
                disabledContentColor = Muted
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Text("Ajouter", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaRequestSheet(kind: String, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val label = if (kind == "Livre") "Nom du livre" else "Nom du film ou de la serie"
    EditSheetScaffold(title = if (kind == "Livre") "Demander un livre" else "Demander un film", emoji = if (kind == "Livre") "📚" else "🎬", onDismiss = onDismiss) {
        Text("Titre", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(8.dp))
        SoftInput(title, { title = it }, label, leadingIcon = Icons.Filled.EditNote)
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { onAdd(title) },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(FieldRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeepGreen,
                disabledContainerColor = Color(0xFFE1E1E1),
                disabledContentColor = Muted
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Text("Envoyer la demande", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBillSheet(bill: Bill, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var label by remember(bill.id) { mutableStateOf(bill.label) }
    var amount by remember(bill.id) { mutableStateOf(if (bill.amount == 0.0) "" else bill.amount.toString()) }
    EditSheetScaffold(title = "Modifier la facture", emoji = "💶", onDismiss = onDismiss) {
        SoftInput(label, { label = it }, "Nom de la facture")
        Spacer(Modifier.height(10.dp))
        SoftInput(amount, { amount = it }, "Montant", keyboardType = KeyboardType.Decimal)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Enregistrer", Icons.Filled.CheckCircle) { onSave(label, amount) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteSheet(note: Note, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    EditSheetScaffold(title = "Modifier la note", emoji = "📝", onDismiss = onDismiss) {
        SoftInput(title, { title = it }, "Titre")
        Spacer(Modifier.height(10.dp))
        SoftInput(body, { body = it }, "Note", minLines = 4)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Enregistrer", Icons.Filled.CheckCircle) { onSave(title, body) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheetScaffold(title: String, emoji: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = PanelRadius, topEnd = PanelRadius)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(emoji, fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(18.dp))
        }
    }
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
fun CalendarEventPill(event: Event, onClick: () -> Unit = {}) {
    Surface(color = Color(event.typeColor).copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(event.typeIcon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(listOf(event.typeName, if (event.allDay) "Toute la journee" else event.time, event.owner.ifBlank { "Tout le foyer" }).joinToString(" - "), fontSize = 14.sp, color = Color(event.typeColor), fontWeight = FontWeight.SemiBold)
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
fun MemberChip(label: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) color else SoftGrey,
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            maxLines = 2,
            lineHeight = 16.sp,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun DateChip(date: LocalDate) {
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(2.dp, DeepGreen), modifier = Modifier.fillMaxWidth()) {
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
fun BirthdayRow(birthday: Birthday, onClick: () -> Unit = {}, onDelete: () -> Unit) {
    val next = birthday.nextBirthday()
    val age = if (birthday.birthYear > 0) next.year - birthday.birthYear else null
    val monthsAway = ((next.year - LocalDate.now().year) * 12 + next.monthValue - LocalDate.now().monthValue).coerceAtLeast(0)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
fun TaskFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else SoftGrey,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun TaskCard(task: HouseholdTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = Color(task.color)
    val overdue = task.isOverdue()
    Surface(
        color = if (overdue && !task.done) Color(0xFFFFF6F1) else Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (overdue && !task.done) Coral else CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(task.title, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (task.done) DeepGreen else CardBorder),
                    modifier = Modifier.size(48.dp).clickable(onClick = onToggle)
                ) {
                    if (task.done) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreen)
                        }
                    }
                }
            }
            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(task.description, color = Muted, fontSize = 15.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.78f), shape = CircleShape, modifier = Modifier.size(50.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(task.assigneeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(task.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFFF0A000), modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(5.dp))
                Text(task.dueDate.taskDueLabel(), color = Color(0xFFF0A000), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("⋮⋮", color = Color(0xFFB9B9B9), fontSize = 25.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted)
                }
            }
            if (overdue && !task.done) {
                Spacer(Modifier.height(8.dp))
                Text("En retard", color = Coral, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    members: List<Member>,
    task: HouseholdTask? = null,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Member?) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var description by remember(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var emoji by remember(task?.id) { mutableStateOf(task?.emoji ?: "🙂") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedDate by remember(task?.id) {
        mutableStateOf(task?.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusDays(1))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMemberId by remember(task?.id, members) {
        mutableStateOf(task?.assigneeId?.ifBlank { null } ?: members.firstOrNull()?.id.orEmpty())
    }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (task == null) "Nouvelle tache" else "Modifier la tache", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = DeepGreen, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Nom de la tache", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
                    modifier = Modifier.size(66.dp).clickable { showEmojiPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
                }
                SoftInput(value = title, onValueChange = { title = it }, label = "Saisir le nom de la tache", modifier = Modifier.weight(1f))
            }
            if (showEmojiPicker) {
                Spacer(Modifier.height(10.dp))
                EmojiPicker(selected = emoji, onSelect = {
                    emoji = it
                    showEmojiPicker = false
                })
            }
            Spacer(Modifier.height(22.dp))
            Text("Description", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = description, onValueChange = { description = it }, label = "Ajouter une description (optionnel)", minLines = 3)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Echeance", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    CompactField(text = selectedDate.taskDueLabel(), icon = Icons.Filled.CalendarMonth) { showDatePicker = true }
                }
                Column(Modifier.weight(1f)) {
                    Text("Assigner a", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    CompactField(text = selectedMember?.name ?: "Personne", icon = Icons.Filled.Person) {
                        val index = members.indexOfFirst { it.id == selectedMemberId }
                        selectedMemberId = members.getOrNull(index + 1)?.id ?: members.firstOrNull()?.id.orEmpty()
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
            PrimaryButton(text = if (task == null) "Ajouter" else "Enregistrer", icon = Icons.Filled.CheckCircle) {
                onAdd(title, description, selectedDate.format(DateTimeFormatter.ISO_DATE), emoji, selectedMember)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showDatePicker) {
        TaskDateDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }
}

@Composable
fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val emojis = listOf("🙂", "🏠", "🛒", "📞", "🧹", "🍽", "💸", "📦", "🎂", "📝")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        emojis.forEach { item ->
            Surface(
                color = if (item == selected) DeepGreen.copy(alpha = 0.14f) else SoftGrey,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp).clickable { onSelect(item) }
            ) {
                Box(contentAlignment = Alignment.Center) { Text(item, fontSize = 22.sp) }
            }
        }
    }
}

@Composable
fun CompactField(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().height(66.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Muted, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun TaskDateDialog(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Mois precedent", tint = Ink)
                    }
                    val title = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                        .replaceFirstChar { it.titlecase(Locale.FRANCE) }
                    Text(
                        "$title ${visibleMonth.year}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Mois suivant", tint = Ink)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.").forEach {
                        Text(it, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TaskDateGrid(month = visibleMonth, selectedDate = selectedDate) {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepGreen),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Annuler", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onConfirm(selectedDate) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Ok", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(AppRadius)
    )
}

@Composable
fun TaskDateGrid(month: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val previousMonth = month.minusMonths(1)
    val previousStart = previousMonth.lengthOfMonth() - leading + 1
    val previousDays = if (leading == 0) emptyList() else (previousStart..previousMonth.lengthOfMonth()).map { previousMonth.atDay(it) }
    val currentDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailingCount = (42 - previousDays.size - currentDays.size).coerceAtLeast(0)
    val nextDays = (1..trailingCount).map { month.plusMonths(1).atDay(it) }
    val days = previousDays + currentDays + nextDays
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(300.dp)
    ) {
        gridItems(days) { date ->
            val isCurrent = YearMonth.from(date) == month
            val selected = date == selectedDate
            Box(
                Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) DeepGreen else Color.Transparent)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                    color = when {
                        selected -> Color.White
                        isCurrent -> Ink
                        else -> Muted.copy(alpha = 0.65f)
                    }
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(initialTime: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val parts = initialTime.split(":")
    var hour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0) }
    var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Text("Selectionner l'heure", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                    PickerColumn(
                        values = (0..23).toList(),
                        selected = hour,
                        label = { "%02d".format(it) },
                        onSelect = { hour = it },
                        modifier = Modifier.weight(1f)
                    )
                    PickerColumn(
                        values = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55),
                        selected = minute - (minute % 5),
                        label = { "%02d".format(it) },
                        onSelect = { minute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepGreen),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Annuler", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onConfirm("%02d:%02d".format(hour, minute)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Ok", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(AppRadius)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(
    members: List<Member>,
    eventTypes: List<EventType>,
    initialDate: LocalDate,
    event: Event? = null,
    onDismiss: () -> Unit,
    onAddType: (String, String, Long) -> Unit,
    onAdd: (String, String, String, String, LocalDate, String, Boolean, String, EventType) -> Unit
) {
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var description by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var selectedMemberIds by remember(event?.id, members) {
        mutableStateOf(
            event?.owner?.let { owner ->
                members.filter { member -> owner.contains(member.name.ifBlank { "Membre" }, ignoreCase = true) }.map { it.id }.toSet()
            }?.takeIf { it.isNotEmpty() } ?: members.map { it.id }.toSet()
        )
    }
    var selectedType by remember(event?.id, eventTypes) {
        mutableStateOf(
            event?.let { current ->
                eventTypes.firstOrNull { it.name == current.typeName } ?: EventType(name = current.typeName, icon = current.typeIcon, color = current.typeColor)
            } ?: eventTypes.firstOrNull() ?: defaultEventTypes().first()
        )
    }
    var showTypeSheet by remember { mutableStateOf(false) }
    var selectedDate by remember(event?.id) { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var time by remember(event?.id) { mutableStateOf(event?.time ?: "00:00") }
    var showTimePicker by remember { mutableStateOf(false) }
    var allDay by remember(event?.id) { mutableStateOf(event?.allDay ?: false) }
    var recurrence by remember(event?.id) { mutableStateOf(event?.recurrence ?: "Aucune") }
    var showRecurrence by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val owner = members.filter { it.id in selectedMemberIds }.joinToString(", ") { it.name.ifBlank { "Membre" } }.ifBlank { "Tout le foyer" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(if (event == null) "Nouvel evenement" else "Modifier l'evenement", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = DeepGreen, modifier = Modifier.size(34.dp)) }
                }
                Spacer(Modifier.height(18.dp))
                FieldLabel("Titre")
                SoftInput(title, { title = it }, "Nom de l'evenement")
                Spacer(Modifier.height(20.dp))
                FieldLabel("Description")
                SoftInput(description, { description = it }, "Description (optionnel)", minLines = 3)
                Spacer(Modifier.height(20.dp))
                FieldLabel("Lieu")
                SoftInput(location, { location = it }, "Adresse ou lieu (optionnel)", leadingIcon = Icons.Filled.LocationOn)
                Spacer(Modifier.height(20.dp))
                FieldLabel("Participants")
                ParticipantsField(members, selectedMemberIds) { selectedMemberIds = it }
                Spacer(Modifier.height(20.dp))
                FieldLabel("Type d'evenement")
                EventTypeField(selectedType) { showTypeSheet = true }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Toute la journee")
                        Text("L'evenement a lieu sur la journee complete", color = Muted, fontSize = 16.sp)
                    }
                    androidx.compose.material3.Switch(checked = allDay, onCheckedChange = { allDay = it })
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Date")
                        CompactField(selectedDate.format(DateTimeFormatter.ofPattern("d/M/yyyy", Locale.FRANCE)), Icons.Filled.CalendarMonth) { showDatePicker = true }
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Heure")
                        CompactField(time, Icons.Filled.Schedule) { showTimePicker = true }
                    }
                }
                Spacer(Modifier.height(20.dp))
                FieldLabel("Recurrence")
                RecurrenceField(recurrence) { showRecurrence = !showRecurrence }
                if (showRecurrence) {
                    Spacer(Modifier.height(10.dp))
                    RecurrencePicker(recurrence) {
                        recurrence = it
                        showRecurrence = false
                    }
                }
                Spacer(Modifier.height(34.dp))
                PrimaryButton(if (event == null) "Ajouter" else "Enregistrer", Icons.Filled.CheckCircle) {
                    onAdd(title, description, location, owner, selectedDate, time, allDay, recurrence, selectedType)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    if (showDatePicker) {
        TaskDateDialog(initialDate = selectedDate, onDismiss = { showDatePicker = false }) {
            selectedDate = it
            showDatePicker = false
        }
    }
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            }
        )
    }
    if (showTypeSheet) {
        EventTypeSheet(
            types = eventTypes,
            selected = selectedType,
            onDismiss = { showTypeSheet = false },
            onSelect = {
                selectedType = it
                showTypeSheet = false
            },
            onAddType = { name, icon, color ->
                onAddType(name, icon, color)
                selectedType = EventType(name = name, icon = icon, color = color)
            }
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ParticipantsField(members: List<Member>, selectedIds: Set<String>, onChange: (Set<String>) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            if (members.isEmpty()) {
                Text("Tout le foyer", color = Muted, fontSize = 18.sp)
            } else {
                members.forEach { member ->
                    val selected = member.id in selectedIds
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        onChange(if (selected) selectedIds - member.id else selectedIds + member.id)
                    }.padding(vertical = 8.dp)) {
                        Icon(if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = if (selected) DeepGreen else Muted)
                        Spacer(Modifier.width(10.dp))
                        Text(member.name.ifBlank { "Membre" }, fontSize = 18.sp, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
fun EventTypeField(type: EventType, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().height(66.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(type.icon, fontSize = 25.sp)
            Spacer(Modifier.width(12.dp))
            Text(type.name, fontSize = 20.sp, color = Ink, modifier = Modifier.weight(1f))
            Text("×", color = Muted, fontSize = 28.sp)
        }
    }
}

@Composable
fun RecurrenceField(value: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Repeat, contentDescription = null, tint = Muted)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Recurrence", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(value, color = Muted, fontSize = 17.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
fun RecurrencePicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "Aucune",
        "Tous les jours",
        "Tous les jours ouvrés",
        "Toutes les semaines",
        "Toutes les 2 semaines",
        "Tous les mois",
        "Tous les ans"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { value ->
                    MemberChip(label = value, selected = selected == value, color = DeepGreen, modifier = Modifier.weight(1f)) { onSelect(value) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeSheet(types: List<EventType>, selected: EventType, onDismiss: () -> Unit, onSelect: (EventType) -> Unit, onAddType: (String, String, Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showNewType by remember { mutableStateOf(false) }
    val visibleTypes = types.filter { it.name.contains(query, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Type d'evenement", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null, tint = DeepGreen) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftInput(query, { query = it }, "Rechercher un type ...", leadingIcon = Icons.Filled.Search, modifier = Modifier.weight(1f))
                Surface(color = DeepGreen, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(62.dp).clickable { showNewType = true }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(260.dp)) {
                gridItems(visibleTypes) { type ->
                    Surface(color = if (type.name == selected.name) Color(type.color) else SoftGrey, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(88.dp).clickable { onSelect(type) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(type.icon, fontSize = 28.sp)
                            Text(type.name, color = if (type.name == selected.name) Color.White else Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    if (showNewType) {
        NewEventTypeSheet(onDismiss = { showNewType = false }, onAdd = { name, icon, color ->
            onAddType(name, icon, color)
            showNewType = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventTypeSheet(onDismiss: () -> Unit, onAdd: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🍴") }
    var color by remember { mutableStateOf(0xFF174C43) }
    var showIcons by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Text("Nouveau type", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(26.dp))
            FieldLabel("Nom du type")
            SoftInput(name, { name = it }, "Ex: Medical, Ecole, Loisirs...")
            Spacer(Modifier.height(22.dp))
            FieldLabel("Icone du type")
            CompactTextField("$icon  Choisir une icone") { showIcons = true }
            Spacer(Modifier.height(22.dp))
            FieldLabel("Couleur du type")
            CompactTextField("●  Choisir une couleur", Color(color)) { showColors = true }
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Annuler", color = Muted, fontSize = 19.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
                Spacer(Modifier.weight(1f))
                Button(onClick = { onAdd(name, icon, color) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepGreen), modifier = Modifier.width(180.dp).height(58.dp)) {
                    Text("Ajouter", fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showIcons) {
        IconBankSheet(onDismiss = { showIcons = false }, onSelect = {
            icon = it
            showIcons = false
        })
    }
    if (showColors) {
        ColorBankSheet(onDismiss = { showColors = false }, onSelect = {
            color = it
            showColors = false
        })
    }
}

@Composable
fun CompactTextField(text: String, tint: Color = Muted, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = tint, fontSize = 19.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconBankSheet(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val icons = listOf("🍎", "🥩", "🌾", "🍬", "🥕", "🍒", "🥐", "🥚", "🐟", "🍇", "🍪", "🍉", "🥛", "🍕", "🥤", "🥗", "🍗", "🥪", "🍜", "🍷", "🛒", "📦", "🎁", "🍴", "☕", "🎂", "🍌", "💧", "🏥", "🏫", "⚽", "🎬")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Choisir une icone", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null, tint = Muted) }
            }
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.height(430.dp)) {
                gridItems(icons) { icon ->
                    Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(58.dp).clickable { onSelect(icon) }) {
                        Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 34.sp) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorBankSheet(onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val colors = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF54B568, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95, 0xFF111111)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Text("Choisir une couleur", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                colors.forEach { value ->
                    Surface(color = Color(value), shape = CircleShape, modifier = Modifier.size(54.dp).clickable { onSelect(value) }) {}
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Supprimer",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black, color = Ink) },
        text = { Text(message, color = Muted) },
        confirmButton = {
            Text(
                confirmLabel,
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
fun AddBirthdaySheet(birthday: Birthday? = null, onDismiss: () -> Unit, onAdd: (String, LocalDate, String) -> Unit) {
    var name by remember(birthday?.id) { mutableStateOf(birthday?.name.orEmpty()) }
    var selectedDate by remember(birthday?.id) {
        mutableStateOf(birthday?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
    }
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
                Text(if (birthday == null) "Ajouter une personne" else "Modifier la personne", fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(34.dp))
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
                Text(if (birthday == null) "Ajouter" else "Enregistrer", fontSize = 22.sp, fontWeight = FontWeight.Black)
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
        shape = RoundedCornerShape(FieldRadius),
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

fun String.memberInitial(): String {
    val clean = trim()
    return clean.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
}

fun shoppingCategory(name: String): String {
    val value = name.lowercase(Locale.FRANCE)
    return when {
        listOf("lait", "fromage", "yaourt", "beurre", "creme", "salade", "tomate", "pomme", "banane", "viande", "poisson", "oeuf").any { it in value } -> "Frais"
        listOf("shampoing", "savon", "dentifrice", "gel douche", "coton", "rasoir", "lessive").any { it in value } -> "Hygiene"
        listOf("sopalin", "papier", "sac", "eponge", "produit", "nettoyant", "ampoule").any { it in value } -> "Maison"
        else -> "Epicerie"
    }
}

fun moduleMood(title: String): Triple<String, Color, String> = when {
    "course" in title.lowercase(Locale.FRANCE) -> Triple("🛒", Color(0xFFE0F8E7), "La liste commune, claire et rapide.")
    "agenda" in title.lowercase(Locale.FRANCE) || "calendrier" in title.lowercase(Locale.FRANCE) -> Triple("📅", Color(0xFFFFF0C8), "Les rendez-vous du foyer.")
    "tache" in title.lowercase(Locale.FRANCE) -> Triple("✅", Color(0xFFE1F4FF), "Qui fait quoi, sans flou.")
    "demande" in title.lowercase(Locale.FRANCE) -> Triple("🎬", Color(0xFFE9E1FF), "Films, series et livres a valider.")
    "anniversaire" in title.lowercase(Locale.FRANCE) -> Triple("🎂", Color(0xFFFFE0EA), "Les dates qui comptent.")
    "note" in title.lowercase(Locale.FRANCE) -> Triple("📝", Color(0xFFFFE7D4), "Les idees et pense-betes.")
    "foyer" in title.lowercase(Locale.FRANCE) -> Triple("🏡", Color(0xFFE0F5F1), "Membres, couleurs et invitation.")
    else -> Triple("✨", SoftGrey, "Un espace simple et vivant.")
}

fun defaultEventTypes(): List<EventType> = listOf(
    EventType(id = "meal", name = "Repas", icon = "🍴", color = 0xFFE86675),
    EventType(id = "medical", name = "Medical", icon = "🏥", color = 0xFF54B568),
    EventType(id = "school", name = "Ecole", icon = "🏫", color = 0xFF5C8EE6),
    EventType(id = "leisure", name = "Loisirs", icon = "⚽", color = 0xFFE8A64F)
)

fun nextTimeValue(value: String): String {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val nextMinute = if (minute == 0) 30 else 0
    val nextHour = if (minute == 0) hour else (hour + 1) % 24
    return "%02d:%02d".format(nextHour, nextMinute)
}

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

fun String.taskDueLabel(): String {
    val date = runCatching { LocalDate.parse(this) }.getOrNull() ?: return "Sans date"
    val today = LocalDate.now()
    return when (date) {
        today -> "Aujourd'hui"
        today.plusDays(1) -> "Demain"
        today.minusDays(1) -> "Hier"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE))
    }
}

fun LocalDate.taskDueLabel(): String = format(DateTimeFormatter.ISO_DATE).taskDueLabel()

fun moneyText(value: Double): String = String.format(Locale.FRANCE, "%.2f EUR", value)

fun String.parseMoneyOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun AppUiState.isCurrentUserAdmin(): Boolean =
    members.firstOrNull { it.id == currentUserId }?.role == "admin" || household?.ownerId == currentUserId

fun AppUiState.pendingRequestCount(): Int =
    if (isCurrentUserAdmin()) mediaRequests.count { it.status == "pending" }
    else mediaRequests.count { it.requesterId == currentUserId && it.status == "pending" }

@Composable
fun MediaRequestNotificationEffect(state: AppUiState) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(state.mediaRequests, state.currentUserId, state.members) {
        val current = state.mediaRequests.associate { it.id to it.status }
        if (ready) {
            val isAdmin = state.isCurrentUserAdmin()
            state.mediaRequests.forEach { request ->
                val oldStatus = previous[request.id]
                if (oldStatus == null && isAdmin && request.requesterId != state.currentUserId && request.status == "pending") {
                    ReminderReceiver.showNow(
                        context,
                        request.id.hashCode().absoluteValue,
                        "Nouvelle demande ${if (request.kind == "Livre") "livre" else "film/serie"}",
                        "${request.requesterName.ifBlank { "Un membre" }} demande : ${request.title}"
                    )
                } else if (oldStatus != null && oldStatus != request.status && request.requesterId == state.currentUserId && request.status != "pending") {
                    ReminderReceiver.showNow(
                        context,
                        request.id.hashCode().absoluteValue,
                        "Demande ${requestStatusLabel(request.status).lowercase(Locale.FRANCE)}",
                        request.title
                    )
                }
            }
        } else {
            ready = true
        }
        previous = current
    }
}

fun requestStatusLabel(status: String): String = when (status) {
    "approved" -> "Validee"
    "rejected" -> "Refusee"
    else -> "En attente"
}

fun HouseholdTask.isOverdue(): Boolean {
    val date = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return false
    return date.isBefore(LocalDate.now()) && !done
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
        shape = RoundedCornerShape(FieldRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepGreen,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
fun ListRow(content: @Composable RowScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun StatBubble(label: String, value: String) {
    Surface(color = SoftGrey, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(label, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(value, color = DeepGreen, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
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
        shape = RoundedCornerShape(FieldRadius),
        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(FieldRadius),
        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey, contentColor = DeepGreen),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
    Surface(
        color = Paper,
        shape = CircleShape,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder.copy(alpha = 0.65f)),
        modifier = Modifier.size(52.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(52.dp)) {
            drawCircle(color = Mint, radius = 24.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
            drawCircle(color = Lemon, radius = 5.dp.toPx(), center = Offset(42.dp.toPx(), 10.dp.toPx()))
            drawCircle(color = Coral, radius = 4.dp.toPx(), center = Offset(44.dp.toPx(), 22.dp.toPx()))
            val roof = Path().apply {
                moveTo(12.dp.toPx(), 28.dp.toPx())
                lineTo(26.dp.toPx(), 15.dp.toPx())
                lineTo(40.dp.toPx(), 28.dp.toPx())
            }
            drawPath(roof, color = DeepGreen, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
            drawLine(DeepGreen, Offset(16.dp.toPx(), 27.dp.toPx()), Offset(16.dp.toPx(), 38.dp.toPx()), strokeWidth = 4.dp.toPx())
            drawLine(DeepGreen, Offset(36.dp.toPx(), 27.dp.toPx()), Offset(36.dp.toPx(), 38.dp.toPx()), strokeWidth = 4.dp.toPx())
            drawLine(DeepGreen, Offset(16.dp.toPx(), 38.dp.toPx()), Offset(36.dp.toPx(), 38.dp.toPx()), strokeWidth = 4.dp.toPx())
            drawLine(DeepGreen, Offset(25.dp.toPx(), 38.dp.toPx()), Offset(25.dp.toPx(), 31.dp.toPx()), strokeWidth = 3.dp.toPx())
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Mon\nFoyer",
            color = DeepGreen,
            fontSize = 30.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Black
        )
    }
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
