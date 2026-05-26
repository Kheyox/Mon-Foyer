package com.bibliostudio.monfoyer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        androidx.compose.material3.Button(
            onClick = { onAdd(label, amount) },
            enabled = canAdd,
            shape = RoundedCornerShape(FieldRadius),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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
