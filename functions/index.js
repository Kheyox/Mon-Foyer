const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// ─── Helpers ────────────────────────────────────────────────────────────────

async function getTokens(db, householdId, excludeUids = []) {
  const membersSnap = await db
    .collection("households").doc(householdId).collection("members").get();
  const tokens = [];
  for (const memberDoc of membersSnap.docs) {
    if (excludeUids.includes(memberDoc.id)) continue;
    const userDoc = await db.collection("users").doc(memberDoc.id).get();
    const token = userDoc.data()?.fcmToken;
    if (token) tokens.push(token);
  }
  return tokens;
}

async function getTokenForUser(db, uid) {
  const userDoc = await db.collection("users").doc(uid).get();
  return userDoc.data()?.fcmToken || null;
}

async function sendToTokens(messaging, tokens, title, body) {
  if (tokens.length === 0) return;
  await Promise.allSettled(
    tokens.map((token) =>
      messaging.send({ token, notification: { title, body }, android: { priority: "high" } })
    )
  );
}

function formatDate(isoDate) {
  if (!isoDate) return "";
  const [, month, day] = isoDate.split("-");
  return `${day}/${month}`;
}

// ─── 1. Activité dans le foyer (existant) ────────────────────────────────────

exports.notifyActivity = onDocumentCreated(
  "households/{householdId}/activity/{activityId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const activity = snap.data();
    const db = getFirestore();
    const messaging = getMessaging();
    const tokens = await getTokens(db, event.params.householdId, [activity.actorId || ""]);
    await sendToTokens(messaging, tokens, "Mon Foyer", activity.text || "Nouvelle activite dans le foyer.");
  }
);

// ─── 2. Tâche assignée à un membre ───────────────────────────────────────────

exports.notifyTaskAssigned = onDocumentCreated(
  "households/{householdId}/tasks/{taskId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const task = snap.data();
    if (!task.assigneeId) return;

    const db = getFirestore();
    const messaging = getMessaging();
    const token = await getTokenForUser(db, task.assigneeId);
    if (!token) return;

    const emoji = task.emoji || "📋";
    const due = task.dueDate ? ` — avant le ${formatDate(task.dueDate)}` : "";
    await messaging.send({
      token,
      notification: {
        title: "Nouvelle tache assignee",
        body: `${emoji} ${task.title || "Une tache"}${due}`,
      },
      android: { priority: "high" },
    });
  }
);

// ─── 3. Article ajouté aux courses ───────────────────────────────────────────

exports.notifyShoppingAdded = onDocumentCreated(
  "households/{householdId}/shopping/{itemId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const item = snap.data();

    const db = getFirestore();
    const messaging = getMessaging();
    const tokens = await getTokens(db, event.params.householdId, []);

    const qty = item.quantity > 1 ? ` (x${item.quantity})` : "";
    await sendToTokens(
      messaging, tokens,
      "🛒 Liste de courses",
      `${item.name || "Un article"}${qty} a ete ajoute aux courses`
    );
  }
);

// ─── 4. Rappels quotidiens : anniversaires + événements + tâches en retard ───

exports.dailyReminders = onSchedule(
  { schedule: "0 8 * * *", timeZone: "Europe/Paris" },
  async () => {
    const db = getFirestore();
    const messaging = getMessaging();

    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const todayISO = today.toISOString().split("T")[0];
    const tomorrowISO = tomorrow.toISOString().split("T")[0];
    const todayMMDD = todayISO.substring(5);
    const tomorrowMMDD = tomorrowISO.substring(5);

    const householdsSnap = await db.collection("households").get();

    for (const householdDoc of householdsSnap.docs) {
      const householdId = householdDoc.id;
      const tokens = await getTokens(db, householdId, []);
      if (tokens.length === 0) continue;

      // Anniversaires aujourd'hui et demain
      const birthdaysSnap = await db
        .collection("households").doc(householdId).collection("birthdays").get();
      for (const bdDoc of birthdaysSnap.docs) {
        const bd = bdDoc.data();
        if (!bd.date) continue;
        const bdMMDD = bd.date.substring(5); // "YYYY-MM-DD" → "MM-DD"
        if (bdMMDD === todayMMDD) {
          const age = bd.birthYear > 0 ? ` — ${today.getFullYear() - bd.birthYear} ans` : "";
          await sendToTokens(messaging, tokens,
            "🎂 Anniversaire aujourd'hui !",
            `C'est l'anniversaire de ${bd.name}${age} !`
          );
        } else if (bdMMDD === tomorrowMMDD) {
          await sendToTokens(messaging, tokens,
            "🎂 Anniversaire demain",
            `${bd.name} fete son anniversaire demain !`
          );
        }
      }

      // Événements demain
      const eventsSnap = await db
        .collection("households").doc(householdId).collection("events")
        .where("date", "==", tomorrowISO).get();
      for (const evDoc of eventsSnap.docs) {
        const ev = evDoc.data();
        const timeStr = ev.allDay ? "toute la journee" : `a ${ev.time}`;
        await sendToTokens(messaging, tokens,
          "📅 Evenement demain",
          `${ev.typeIcon || "📅"} ${ev.title} — ${timeStr}`
        );
      }

      // Tâches en retard (non faites, dueDate passée)
      const allTasksSnap = await db
        .collection("households").doc(householdId).collection("tasks")
        .where("done", "==", false).get();
      const overdue = allTasksSnap.docs.filter((d) => {
        const due = d.data().dueDate;
        return due && due !== "" && due < todayISO;
      });
      if (overdue.length > 0) {
        const titles = overdue.slice(0, 2).map((d) => d.data().title).join(", ");
        const body = overdue.length === 1
          ? `⚠️ "${titles}" est en retard`
          : `⚠️ ${overdue.length} taches en retard : ${titles}${overdue.length > 2 ? "…" : ""}`;
        await sendToTokens(messaging, tokens, "Taches en retard", body);
      }
    }
  }
);

// ─── 5. Rappel budget le 1er du mois ─────────────────────────────────────────

exports.monthlyBudgetReminder = onSchedule(
  { schedule: "0 9 1 * *", timeZone: "Europe/Paris" },
  async () => {
    const db = getFirestore();
    const messaging = getMessaging();

    const householdsSnap = await db.collection("households").get();

    for (const householdDoc of householdsSnap.docs) {
      const householdId = householdDoc.id;
      const tokens = await getTokens(db, householdId, []);
      if (tokens.length === 0) continue;

      const billsSnap = await db
        .collection("households").doc(householdId).collection("bills")
        .where("paid", "==", false).get();
      if (billsSnap.empty) continue;

      const total = billsSnap.docs.reduce((sum, d) => sum + (d.data().amount || 0), 0);
      const count = billsSnap.size;
      await sendToTokens(messaging, tokens,
        "💰 Charges du mois",
        `${count} charge${count > 1 ? "s" : ""} a regler ce mois — ${total.toFixed(0)} € au total`
      );
    }
  }
);
