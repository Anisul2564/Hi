/**
 * BD PARTY Cloud Functions (2nd Gen) - Secure LiveKit Token Provider
 *
 * Implements: POST /livekit/token (or /token) using 2nd Gen HTTPS functions
 *
 * Security Requirements:
 * 1. Firebase Authentication: Must present Bearer <Firebase ID Token>
 * 2. Room Verification: Room document /rooms/{roomId} must exist and status must be LIVE.
 * 3. Participant Role: Derived exclusively from /rooms/{roomId}/participants/{uid}. Client supplied roles are ignored.
 * 4. LiveKit Secrets: Read strictly from backend secrets via defineSecret:
 *    - LIVEKIT_API_KEY
 *    - LIVEKIT_API_SECRET
 *    - LIVEKIT_URL
 *    Bound only to functions that need them (token endpoint). Never exposed to Android client, APK, or public repos.
 * 5. Minimum permissions:
 *    - LISTENER: canJoin=true, canSubscribe=true, canPublish=false
 *    - SPEAKER, CO_HOST, MODERATOR, HOST: canJoin=true, canSubscribe=true, canPublish=true
 * 6. Short-lived token TTL: 1 hour (3600s) default.
 * 7. Identity format: "bdp_{uid}" with name set to verified username.
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { AccessToken } = require("livekit-server-sdk");

// Secret Manager references for LiveKit credentials
const liveKitApiKey = defineSecret("LIVEKIT_API_KEY");
const liveKitApiSecret = defineSecret("LIVEKIT_API_SECRET");
const liveKitUrl = defineSecret("LIVEKIT_URL");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

/**
 * Validates bearer token from Authorization header and decodes Firebase user.
 */
async function authenticateRequest(req) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    throw {
      status: 401,
      code: "unauthenticated",
      message: "Authorization header with Bearer token is required.",
    };
  }

  const idToken = authHeader.split("Bearer ")[1].trim();
  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    return decodedToken;
  } catch (err) {
    throw {
      status: 401,
      code: "invalid-token",
      message: "Invalid or expired Firebase ID token.",
    };
  }
}

/**
 * Health / Readiness check endpoint (2nd Gen).
 * Does NOT bind or expose LiveKit secrets.
 */
exports.health = onRequest({ cors: true }, async (req, res) => {
  const hasKey = Boolean(process.env.LIVEKIT_API_KEY);
  const hasSecret = Boolean(process.env.LIVEKIT_API_SECRET);
  const hasUrl = Boolean(process.env.LIVEKIT_URL);

  res.status(200).json({
    service: "bdparty-livekit-token-provider",
    version: "2nd-gen",
    status: "ok",
    livekitConfigured: hasKey && hasSecret && hasUrl,
    message: hasKey && hasSecret && hasUrl
      ? "LiveKit credentials configured and ready."
      : "LiveKit credentials pending deployment/configuration.",
  });
});

/**
 * POST /token (or /livekit/token)
 * 2nd Generation HTTPS function with secrets explicitly bound.
 * Handles token generation for BD PARTY voice stages.
 */
exports.token = onRequest(
  {
    cors: true,
    secrets: [liveKitApiKey, liveKitApiSecret, liveKitUrl],
  },
  async (req, res) => {
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }

    if (req.method !== "POST") {
      res.status(405).json({
        error: "method-not-allowed",
        message: "Only POST requests are accepted.",
      });
      return;
    }

    try {
      // 1. Authenticate caller using Firebase ID Token
      const decodedUser = await authenticateRequest(req);
      const callerUid = decodedUser.uid;

      // 2. Validate request payload (accept ONLY roomId from body)
      const { roomId } = req.body || {};
      if (!roomId || typeof roomId !== "string" || roomId.trim().length === 0) {
        res.status(400).json({
          error: "invalid-argument",
          message: "roomId is required.",
        });
        return;
      }
      const cleanRoomId = roomId.trim();

      // 3. Access bound secrets through process.env or secret param .value()
      const apiKey = liveKitApiKey.value ? liveKitApiKey.value() : process.env.LIVEKIT_API_KEY;
      const apiSecret = liveKitApiSecret.value ? liveKitApiSecret.value() : process.env.LIVEKIT_API_SECRET;
      const serverUrl = liveKitUrl.value ? liveKitUrl.value() : process.env.LIVEKIT_URL;

      if (!apiKey || !apiSecret || !serverUrl) {
        res.status(503).json({
          error: "livekit-not-configured",
          message: "LiveKit credentials pending deployment/configuration.",
        });
        return;
      }

      // 4. Validate Room existence and LIVE status in Firestore
      const roomRef = db.collection("rooms").doc(cleanRoomId);
      const roomDoc = await roomRef.get();

      if (!roomDoc.exists) {
        res.status(404).json({
          error: "room-not-found",
          message: `Room ${cleanRoomId} does not exist.`,
        });
        return;
      }

      const roomData = roomDoc.data();
      if (roomData.status !== "LIVE") {
        res.status(403).json({
          error: "room-ended",
          message: `Room status is ${roomData.status}. Only LIVE rooms can generate voice tokens.`,
        });
        return;
      }

      // 5. Read participant identity & roomRole from Firestore (NEVER client-supplied)
      const participantRef = roomRef.collection("participants").doc(callerUid);
      const participantDoc = await participantRef.get();

      let roomRole = "LISTENER";
      let participantUsername = decodedUser.name || `User_${callerUid.substring(0, 6)}`;
      let seatIndex = -1;

      if (participantDoc.exists) {
        const pData = participantDoc.data();
        roomRole = pData.roomRole || "LISTENER";
        seatIndex = typeof pData.seatIndex === "number" ? pData.seatIndex : -1;
        if (pData.username) {
          participantUsername = pData.username;
        }
      } else {
        // If caller is host, default to HOST
        if (roomData.hostUid === callerUid) {
          roomRole = "HOST";
        }
      }

      // 6. Enforce least-privilege LiveKit grants
      // LISTENER: canJoin = true, canSubscribe = true, canPublish = false
      // SPEAKER / CO_HOST / MODERATOR / HOST: canJoin = true, canSubscribe = true, canPublish = true
      const canPublish = [
        "SPEAKER",
        "CO_HOST",
        "MODERATOR",
        "HOST",
      ].includes(roomRole) && (seatIndex >= 0 || roomRole === "HOST");

      const grant = {
        room: cleanRoomId,
        roomJoin: true,
        canSubscribe: true,
        canPublish: canPublish,
        canPublishData: true,
      };

      // 7. Token expiration: 1 hour (3600 seconds)
      const tokenTtlSeconds = 3600;
      const nowEpochSeconds = Math.floor(Date.now() / 1000);
      const expiresAt = (nowEpochSeconds + tokenTtlSeconds) * 1000; // ms

      // Identity strictly formatted as bdp_{uid}
      const participantIdentity = `bdp_${callerUid}`;

      const at = new AccessToken(apiKey, apiSecret, {
        identity: participantIdentity,
        name: participantUsername,
        ttl: `${tokenTtlSeconds}s`,
      });

      at.addGrant(grant);
      const jwtToken = await at.toJwt();

      // 8. Return response contract
      res.status(200).json({
        token: jwtToken,
        roomName: cleanRoomId,
        identity: participantIdentity,
        expiresAt: expiresAt,
        serverUrl: serverUrl,
        roomRole: roomRole,
        canPublish: canPublish,
      });
    } catch (error) {
      if (error.status) {
        res.status(error.status).json({
          error: error.code || "error",
          message: error.message,
        });
        return;
      }
      console.error("Error generating LiveKit token:", error);
      res.status(500).json({
        error: "internal-error",
        message: "Internal server error generating LiveKit access token.",
      });
    }
  }
);
