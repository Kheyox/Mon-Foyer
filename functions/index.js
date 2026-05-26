const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyActivity = onDocumentCreated(
  "households/{householdId}/activity/{activityId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const activity = snap.data();
    const householdId = event.params.householdId;

    const db = getFirestore();
    const messaging = getMessaging();

    // Fetch all members of the household
    const membersSnap = await db
      .collection("households")
      .doc(householdId)
      .collection("members")
      .get();

    const actorId = activity.actorId || "";

    // Collect FCM tokens from all members except the actor
    const tokens = [];
    for (const memberDoc of membersSnap.docs) {
      if (memberDoc.id === actorId) continue;
      const uid = memberDoc.id;
      const userDoc = await db.collection("users").doc(uid).get();
      const token = userDoc.data()?.fcmToken;
      if (token) tokens.push(token);
    }

    if (tokens.length === 0) return;

    const title = "Mon Foyer";
    const body = activity.text || "Nouvelle activite dans le foyer.";

    // Send to all tokens
    const messages = tokens.map((token) => ({
      token,
      notification: { title, body },
      android: { priority: "high" },
    }));

    await Promise.allSettled(
      messages.map((msg) => messaging.send(msg))
    );
  }
);
