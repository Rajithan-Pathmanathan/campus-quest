// POST /api/notify-new-quest   body: {"gameId":"..."}   header: Authorization: Bearer <Firebase ID token>
// Sends "A new quest has been hidden on campus!" to the FCM topic "new_games".
// Environment variable (Vercel -> Settings -> Environment Variables), never committed:
//   FIREBASE_SERVICE_ACCOUNT = the full JSON of a Firebase service-account key.
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
  });
}

module.exports = async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ error: "POST only" });

  // 1. Who is calling? Only signed-in Campus Quest users.
  const match = /^Bearer (.+)$/.exec(req.headers.authorization || "");
  if (!match) return res.status(401).json({ error: "Missing token" });
  let uid;
  try {
    uid = (await admin.auth().verifyIdToken(match[1])).uid;
  } catch {
    return res.status(401).json({ error: "Invalid token" });
  }

  // 2. The quest must exist, be published and belong to the caller.
  const gameId = typeof req.body?.gameId === "string" ? req.body.gameId : "";
  if (!gameId) return res.status(400).json({ error: "gameId required" });
  const db = admin.firestore();
  const gameRef = db.collection("games").doc(gameId);
  const game = await gameRef.get();
  if (!game.exists || game.get("status") !== "PUBLISHED" || game.get("creatorId") !== uid) {
    return res.status(403).json({ error: "Not your published quest" });
  }

  // 3. Send only once per quest.
  const sent = await db.runTransaction(async (tx) => {
    const marker = db.collection("notifications").doc(gameId);
    if ((await tx.get(marker)).exists) return false;
    tx.set(marker, { sentAt: admin.firestore.FieldValue.serverTimestamp(), by: uid });
    return true;
  });
  if (!sent) return res.status(200).json({ status: "already sent" });

  await admin.messaging().send({
    topic: "new_games",
    notification: {
      title: "A new quest has been hidden on campus!",
      body: `${game.get("title")} by ${game.get("creatorName") || "a fellow explorer"}`,
    },
    data: {
      gameId,
      EXTRA_GAME_ID: gameId,
      title: "A new quest has been hidden on campus!",
      body: String(game.get("title") || ""),
    },
    android: { priority: "high" },
  });
  return res.status(200).json({ status: "sent" });
};
