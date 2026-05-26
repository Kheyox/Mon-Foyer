package com.bibliostudio.monfoyer

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

private const val UPDATE_MANIFEST_URL_VM = "https://raw.githubusercontent.com/Kheyox/Mon-Foyer/main/update.json"

class MonFoyerViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    private var listeners = mutableListOf<ListenerRegistration>()
    var state by mutableStateOf(AppUiState())
        private set
    private var appContext: Context? = null

    fun setAppContext(context: Context) {
        appContext = context.applicationContext
    }

    init {
        // Point 2: Mode offline Firestore
        FirebaseFirestore.getInstance().firestoreSettings = com.google.firebase.firestore.firestoreSettings { isPersistenceEnabled = true }

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
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                // Point 3d: update FCM token after sign-in
                authResult.user?.uid?.let { uid ->
                    MonFoyerMessagingService.updateToken(db, uid)
                }
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
                val json = URL(UPDATE_MANIFEST_URL_VM).readText()
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
        batch.commit()
            .addOnSuccessListener { logActivity(householdRef.id, "a cree le foyer ${name.ifBlank { "Mon foyer" }}") }
            .addOnFailureListener { setError(it.message ?: "Creation impossible.") }
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
                            logActivity(householdId, "a rejoint le foyer")
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
        ) { "a ajoute $itemName aux courses" }
    }

    fun addBill(label: String, amount: String) {
        val cleanLabel = label.trim()
        val cleanAmount = amount.parseMoneyOrNull() ?: return
        if (cleanLabel.isBlank() || cleanAmount <= 0.0) return
        add("bills", mapOf("label" to cleanLabel, "amount" to cleanAmount, "paid" to false)) { "a ajoute une facture $cleanLabel" }
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
        ) { "a ajoute l'evenement $title" }
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
        add("eventTypes", mapOf("name" to name, "icon" to icon, "color" to color)) { "a cree le type d'evenement ${name.trim()}" }
        refreshEventTypes()
    }

    fun addNote(title: String, body: String) = add("notes", mapOf("title" to title, "body" to body)) { "a ajoute une note" }

    // Point 4f: addTask accepts repeatInterval and priority
    fun addTask(title: String, description: String, dueDate: String, emoji: String, member: Member?, repeatInterval: String = "none", priority: String = "normal") {
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
                "done" to false,
                "repeatInterval" to repeatInterval,
                "priority" to priority
            )
        ) { "a ajoute la tache $title" }
    }

    // Point 4d: updateTask accepts repeatInterval and priority
    fun updateTask(taskId: String, title: String, description: String, dueDate: String, emoji: String, member: Member?, repeatInterval: String = "none", priority: String = "normal") {
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
                    "repeatInterval" to repeatInterval,
                    "priority" to priority,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }

    fun addBirthday(name: String, date: LocalDate, birthYear: String) {
        if (name.isBlank()) return
        val householdId = state.household?.id ?: return
        add(
            "birthdays",
            mapOf(
                "name" to name,
                "date" to date.format(DateTimeFormatter.ISO_DATE),
                "birthYear" to (birthYear.toIntOrNull() ?: 0)
            )
        ) { "a ajoute l'anniversaire de ${name.trim()}" }
        // Auto-create a recurring annual event for the birthday
        val eventData = hashMapOf<String, Any>(
            "title" to "${name.trim()} 🎂",
            "date" to date.format(DateTimeFormatter.ISO_DATE),
            "allDay" to true,
            "time" to "00:00",
            "owner" to "Tout le foyer",
            "typeName" to "Anniversaire",
            "typeIcon" to "🎂",
            "typeColor" to 0xFFFF6B6BL,
            "recurrence" to "Annuelle",
            "description" to "Anniversaire de ${name.trim()}"
        )
        db.collection("households").document(householdId)
            .collection("events").add(eventData)
        refreshBirthdays()
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
            .addOnSuccessListener { refreshBirthdays() }
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
            .addOnSuccessListener { logActivity("a ajoute une demande ${kind.lowercase()}: $cleanTitle") }
            .addOnFailureListener { setError(it.message ?: "Demande impossible.") }
    }

    fun updateMediaRequestStatus(request: MediaRequest, status: String) {
        val household = state.household ?: return
        if (!state.isCurrentUserAdmin()) return
        db.collection("households").document(household.id).collection("requests").document(request.id)
            .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { logActivity("a marque ${request.title} comme ${status.mediaStatusLabel().lowercase()}") }
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
    }

    fun toggleShopping(item: ShoppingItem) = update("shoppingItems", item.id, "done", !item.done)
    fun toggleShoppingFavorite(item: ShoppingItem) = update("shoppingItems", item.id, "favorite", !item.favorite)
    fun toggleBill(bill: Bill) = update("bills", bill.id, "paid", !bill.paid)

    // Point 4e: toggleTask with recurrence support and completedAt tracking
    fun toggleTask(task: HouseholdTask) {
        val newValue = !task.done
        val household = state.household ?: return
        val updates: Map<String, Any> = if (newValue) {
            mapOf("done" to true, "completedAt" to System.currentTimeMillis())
        } else {
            mapOf("done" to false, "completedAt" to 0L)
        }
        db.collection("households").document(household.id).collection("tasks").document(task.id)
            .update(updates)
        logActivity(if (newValue) "a termine la tache ${task.title}" else "a remis la tache ${task.title} a faire")
        if (newValue && task.repeatInterval != "none") {
            scheduleNextRecurrence(task)
        }
    }

    private fun scheduleNextRecurrence(task: HouseholdTask) {
        val currentDue = runCatching { LocalDate.parse(task.dueDate) }.getOrNull() ?: LocalDate.now()
        val nextDue = when (task.repeatInterval) {
            "daily" -> currentDue.plusDays(1)
            "weekly" -> currentDue.plusWeeks(1)
            "biweekly" -> currentDue.plusWeeks(2)
            "monthly" -> currentDue.plusMonths(1)
            else -> return
        }
        add(
            "tasks",
            mapOf(
                "title" to task.title,
                "description" to task.description,
                "dueDate" to nextDue.format(DateTimeFormatter.ISO_DATE),
                "emoji" to task.emoji,
                "assigneeId" to task.assigneeId,
                "assigneeName" to task.assigneeName,
                "color" to task.color,
                "done" to false,
                "repeatInterval" to task.repeatInterval
            )
        )
    }

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

    fun updateHouseholdName(name: String) {
        val household = state.household ?: return
        if (!state.isCurrentUserAdmin()) return
        val cleanName = name.trim().ifBlank { "Mon foyer" }
        db.collection("households").document(household.id)
            .update(mapOf("name" to cleanName, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { logActivity("a renomme le foyer en $cleanName") }
            .addOnFailureListener { setError(it.message ?: "Modification impossible.") }
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

    fun addSuggestion(item: ShoppingItem) {
        val householdId = state.household?.id ?: return
        val newItem = hashMapOf<String, Any>(
            "name" to item.name,
            "done" to false,
            "quantity" to item.quantity,
            "category" to item.category,
            "favorite" to true
        )
        db.collection("households").document(householdId)
            .collection("shoppingItems").add(newItem)
    }

    private fun writeWidgetData(context: Context?) {
        val ctx = context ?: appContext ?: return
        val today = java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val pendingTasks = state.tasks.filter { !it.done }.map { it.title }
        val todayEvents = state.events.filter { it.date == today }.map { it.title }
        val prefs = ctx.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("tasks_today", JSONArray(pendingTasks).toString())
            .putString("events_today", JSONArray(todayEvents).toString())
            .apply()
    }

    fun updateMember(memberId: String, name: String, color: Long, role: String = "", avatar: String = "") {
        val household = state.household ?: return
        val canEdit = state.isCurrentUserAdmin() || memberId == state.currentUserId
        if (!canEdit) return
        val cleanName = name.trim().ifBlank { "Membre" }
        val values = mutableMapOf<String, Any>(
            "name" to cleanName,
            "color" to color,
            "avatar" to avatar,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (state.isCurrentUserAdmin() && memberId != state.currentUserId && role in listOf("admin", "member")) {
            values["role"] = role
        }
        db.collection("households").document(household.id).collection("members").document(memberId)
            .update(values)
            .addOnSuccessListener { logActivity("a mis a jour le profil de $cleanName") }
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

    internal fun add(collection: String, values: Map<String, Any>, activityText: (() -> String)? = null) {
        val household = state.household ?: return
        val cleanValues = values.filterValues { value -> value.toString().isNotBlank() }
        if (cleanValues.isEmpty()) return
        db.collection("households").document(household.id).collection(collection)
            .add(cleanValues + mapOf("createdAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { activityText?.let { logActivity(it()) } }
    }

    private fun activityPayload(text: String): Map<String, Any> {
        val user = auth.currentUser
        val actorId = user?.uid ?: state.currentUserId
        val member = state.members.firstOrNull { it.id == actorId }
        val actorName = member?.name?.ifBlank { null } ?: user?.displayName ?: state.userName.ifBlank { "Membre" }
        return mapOf(
            "text" to text,
            "actorId" to actorId,
            "actorName" to actorName,
            "color" to (member?.color ?: memberColorLong(actorId)),
            "createdAt" to FieldValue.serverTimestamp(),
            "createdAtMillis" to System.currentTimeMillis()
        )
    }

    private fun logActivity(text: String) {
        val household = state.household ?: return
        logActivity(household.id, text)
    }

    private fun logActivity(householdId: String, text: String) {
        db.collection("households").document(householdId).collection("activity")
            .add(activityPayload(text))
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
                    color = it.getLong("color") ?: memberColorLong(it.id),
                    avatar = it.getString("avatar") ?: ""
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
            writeWidgetData(null)
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
                    emoji = it.getString("emoji") ?: "🙂",
                    repeatInterval = it.getString("repeatInterval") ?: "none",
                    priority = it.getString("priority") ?: "normal",
                    completedAt = it.getLong("completedAt") ?: 0L
                )
            }.orEmpty())
            writeWidgetData(null)
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
        listeners += householdRef.collection("activity")
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                state = state.copy(activity = snap?.documents?.map {
                    ActivityItem(
                        id = it.id,
                        text = it.getString("text") ?: "",
                        actorId = it.getString("actorId") ?: "",
                        actorName = it.getString("actorName") ?: "Membre",
                        color = it.getLong("color") ?: 0xFF174C43,
                        createdAtMillis = it.getLong("createdAtMillis") ?: 0L
                    )
                }.orEmpty())
            }

        // Point 7: birthdays and eventTypes as one-shot get() calls
        refreshBirthdays()
        refreshEventTypes()
    }

    // Point 7: Manual refresh for birthdays
    fun refreshBirthdays() {
        val household = state.household ?: return
        val householdRef = db.collection("households").document(household.id)
        householdRef.collection("birthdays").get()
            .addOnSuccessListener { snap ->
                state = state.copy(birthdays = snap.documents.map {
                    Birthday(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        date = it.getString("date") ?: "",
                        birthYear = (it.getLong("birthYear") ?: 0).toInt()
                    )
                })
            }
    }

    // Point 7: Manual refresh for eventTypes
    fun refreshEventTypes() {
        val household = state.household ?: return
        val householdRef = db.collection("households").document(household.id)
        householdRef.collection("eventTypes").get()
            .addOnSuccessListener { snap ->
                val savedTypes = snap.documents.map {
                    EventType(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        icon = it.getString("icon") ?: "🍴",
                        color = it.getLong("color") ?: 0xFFE86675
                    )
                }
                state = state.copy(eventTypes = defaultEventTypes() + savedTypes)
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
