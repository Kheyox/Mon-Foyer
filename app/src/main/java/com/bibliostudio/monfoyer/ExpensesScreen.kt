package com.bibliostudio.monfoyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val EXPENSE_CATEGORIES = listOf("Courses", "Loyer", "Factures", "Transport", "Loisirs", "Sante", "Autre")
private val CATEGORY_EMOJI = mapOf("Courses" to "🛒", "Loyer" to "🏠", "Factures" to "💡", "Transport" to "🚗", "Loisirs" to "🎉", "Sante" to "💊", "Autre" to "💶")

private data class Settlement(val fromId: String, val fromName: String, val toId: String, val toName: String, val fromColor: Long, val toColor: Long, val amount: Double)

private fun calculateBalances(expenses: List<Expense>, members: List<Member>): Map<String, Double> {
    val balances = members.associate { it.id to 0.0 }.toMutableMap()
    for (expense in expenses) {
        val involved = expense.splitWith.ifEmpty { members.map { it.id } }
        if (involved.isEmpty()) continue
        val share = expense.amount / involved.size
        balances[expense.payerId] = (balances[expense.payerId] ?: 0.0) + expense.amount
        involved.forEach { id -> balances[id] = (balances[id] ?: 0.0) - share }
    }
    return balances
}

private fun calculateSettlements(balances: Map<String, Double>, members: List<Member>): List<Settlement> {
    data class Entry(val id: String, var amount: Double)
    val creditors = balances.entries.filter { it.value > 0.01 }.map { Entry(it.key, it.value) }.sortedByDescending { it.amount }.toMutableList()
    val debtors = balances.entries.filter { it.value < -0.01 }.map { Entry(it.key, -it.value) }.sortedByDescending { it.amount }.toMutableList()
    val results = mutableListOf<Settlement>()
    var ci = 0; var di = 0
    while (ci < creditors.size && di < debtors.size) {
        val transfer = minOf(creditors[ci].amount, debtors[di].amount)
        val from = members.find { it.id == debtors[di].id }
        val to = members.find { it.id == creditors[ci].id }
        if (from != null && to != null) results.add(Settlement(from.id, from.name, to.id, to.name, from.color, to.color, transfer))
        creditors[ci].amount -= transfer; debtors[di].amount -= transfer
        if (creditors[ci].amount < 0.01) ci++
        if (debtors[di].amount < 0.01) di++
    }
    return results
}

@Composable
fun ExpensesScreen(vm: MonFoyerViewModel) {
    var tab by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Expense?>(null) }
    val expenses = vm.state.expenses.sortedByDescending { it.createdAtMillis }
    val members = vm.state.members
    val balances = remember(expenses, members) { calculateBalances(expenses, members) }
    val settlements = remember(balances, members) { calculateSettlements(balances, members) }
    val total = expenses.sumOf { it.amount }

    ModulePanel(title = "Depenses") {
        item {
            StatBubble("Total des depenses", moneyText(total))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    TaskFilterChip("Depenses", tab == 0) { tab = 0 }
                    TaskFilterChip("Solde", tab == 1) { tab = 1 }
                }
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(52.dp).clickable { showAdd = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (tab == 0) {
            if (expenses.isEmpty()) {
                item { EmptyState("⚖️", "Aucune depense", "Ajoute les achats du foyer pour partager les frais equitablement.") }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(expense = expense, members = members, onDelete = { toDelete = expense })
                }
            }
        } else {
            item {
                if (members.isEmpty()) {
                    EmptyState("⚖️", "Aucun membre", "Ajoute des membres au foyer pour voir les soldes.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Solde par personne", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                        Spacer(Modifier.height(2.dp))
                        members.forEach { member ->
                            val balance = balances[member.id] ?: 0.0
                            BalanceCard(member = member, balance = balance)
                        }
                        if (settlements.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Pour equilibrer :", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                            Spacer(Modifier.height(2.dp))
                            settlements.forEach { s -> SettlementRow(settlement = s) }
                        } else if (expenses.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅", fontSize = 24.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Tout le monde est quitte !", fontSize = 16.sp, fontWeight = FontWeight.Black, color = DeepGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    toDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Supprimer cette depense ?",
            message = "${expense.label} (${moneyText(expense.amount)}) sera supprimee.",
            onConfirm = { vm.deleteExpense(expense.id); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
    if (showAdd) {
        AddExpenseSheet(
            members = members,
            currentUserId = vm.state.currentUserId,
            onDismiss = { showAdd = false },
            onAdd = { label, amount, payerId, payerName, splitWith, category ->
                vm.addExpense(label, amount, payerId, payerName, splitWith, category)
                showAdd = false
            }
        )
    }
}

@Composable
fun ExpenseCard(expense: Expense, members: List<Member>, onDelete: () -> Unit) {
    val payer = members.find { it.id == expense.payerId }
    val payerColor = payer?.let { Color(it.color) } ?: DeepGreen
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = payerColor.copy(alpha = 0.13f), shape = CircleShape, modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(CATEGORY_EMOJI[expense.category] ?: "💶", fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = payerColor.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                        Text(
                            payer?.name?.ifBlank { "Membre" } ?: expense.payerName.ifBlank { "Membre" },
                            color = payerColor, fontSize = 12.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(expense.category, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(moneyText(expense.amount), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Ink)
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun BalanceCard(member: Member, balance: Double) {
    val color = Color(member.color)
    val isPositive = balance >= -0.01
    val bgColor = if (isPositive) color.copy(alpha = 0.08f) else Color(0xFFFFF0F0)
    Surface(color = bgColor, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (member.avatar.isNotBlank()) Text(member.avatar, fontSize = 20.sp)
                    else Text(member.name.memberInitial(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name.ifBlank { "Membre" }, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(if (balance > 0.01) "On vous doit" else if (balance < -0.01) "Vous devez" else "Compte a jour", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Bold)
            }
            Text(
                if (abs(balance) < 0.01) "✓" else (if (isPositive) "+" else "-") + moneyText(abs(balance)),
                fontSize = 17.sp, fontWeight = FontWeight.Black,
                color = if (balance > 0.01) DeepGreen else if (balance < -0.01) Color(0xFFE53935) else Muted
            )
        }
    }
}

@Composable
fun SettlementRow(settlement: Settlement) {
    Surface(color = Color(0xFFFAFAFA), shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(settlement.fromColor), shape = CircleShape, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(settlement.fromName.memberInitial(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("${settlement.fromName} → ${settlement.toName}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink)
            }
            Surface(color = Color(settlement.toColor).copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                Text(moneyText(settlement.amount), color = Color(settlement.toColor), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    members: List<Member>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, List<String>, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var payerId by remember { mutableStateOf(currentUserId.ifBlank { members.firstOrNull()?.id ?: "" }) }
    var category by remember { mutableStateOf("Courses") }
    var splitAll by remember { mutableStateOf(true) }
    var selectedIds by remember { mutableStateOf(members.map { it.id }.toSet()) }
    val canAdd = label.isNotBlank() && (amount.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0.0 && payerId.isNotBlank()

    EditSheetScaffold(title = "Nouvelle depense", emoji = "⚖️", onDismiss = onDismiss) {
        FieldLabel("Description")
        SoftInput(label, { label = it }, "Ex: Courses Leclerc, Loyer...")
        Spacer(Modifier.height(14.dp))
        FieldLabel("Montant (EUR)")
        SoftInput(amount, { amount = it }, "Ex: 85.50", keyboardType = KeyboardType.Decimal, leadingIcon = Icons.Filled.AccountBalance)
        Spacer(Modifier.height(14.dp))
        FieldLabel("Paye par")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            members.take(5).forEach { m ->
                MemberChip(label = m.name.ifBlank { "Membre" }, selected = payerId == m.id, color = memberColor(m.id)) { payerId = m.id }
            }
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Categorie")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EXPENSE_CATEGORIES.take(4).forEach { cat ->
                TaskFilterChip(cat, category == cat) { category = cat }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EXPENSE_CATEGORIES.drop(4).forEach { cat ->
                TaskFilterChip(cat, category == cat) { category = cat }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Partage", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(if (splitAll) "Tout le foyer" else "${selectedIds.size} personne(s)", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Bold)
            }
            TaskFilterChip("Tout le foyer", splitAll) { splitAll = true; selectedIds = members.map { it.id }.toSet() }
        }
        if (!splitAll && members.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                members.take(5).forEach { m ->
                    MemberChip(label = m.name.ifBlank { "Membre" }, selected = selectedIds.contains(m.id), color = memberColor(m.id)) {
                        selectedIds = if (selectedIds.contains(m.id)) selectedIds - m.id else selectedIds + m.id
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        val payerName = members.find { it.id == payerId }?.name ?: ""
        val splitWith = if (splitAll) emptyList() else selectedIds.toList()
        PrimaryButton("Ajouter", Icons.Filled.Check) {
            if (canAdd) onAdd(label, amount, payerId, payerName, splitWith, category)
        }
    }
}
