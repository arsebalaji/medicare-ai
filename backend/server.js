require("dotenv").config();
const express = require("express");
const cors = require("cors");
const rateLimit = require("express-rate-limit");
const admin = require("firebase-admin");

const REQUIRE_AUTH = process.env.REQUIRE_AUTH !== "false";
if (REQUIRE_AUTH) admin.initializeApp({ credential: admin.credential.applicationDefault() });
if (!process.env.ANTHROPIC_API_KEY) { console.error("Missing ANTHROPIC_API_KEY"); process.exit(1); }

const SYSTEM = `You are MediCare AI, a general health and wellness education assistant for people in India.
Rules you must always follow:
- Never diagnose a condition or say what the user "has". Say what is commonly associated with a symptom and that only a clinician can assess it.
- Never prescribe, recommend, or give doses of any medicine. Never advise starting, stopping or changing a prescribed medicine; tell the user to ask their doctor or pharmacist.
- If the user describes possible emergency signs (chest pain, trouble breathing, stroke signs, severe bleeding, unconsciousness, seizure, poisoning, thoughts of self-harm), tell them to call 112 immediately before anything else.
- Be honest about uncertainty. Do not invent studies, statistics or references. If unsure, say so.
- You may explain medical terms, preventive care, healthy habits, and how to prepare for a doctor visit (symptoms timeline, medicines list, questions to ask, documents to bring).
- Encourage seeing a qualified doctor when symptoms are persistent, worsening, severe or unclear.
- Use plain, kind, simple language. Plain text only, no markdown. Keep answers under 200 words.
- Do not ask for or store personal identifiers.`;

const EMERGENCY = /(chest pain|heart attack|can'?t breathe|cannot breathe|difficulty breathing|severe bleeding|unconscious|seizure|overdose|suicid|kill myself|end my life|stroke|vomiting blood|anaphyla)/i;

async function auth(req, res, next) {
  if (!REQUIRE_AUTH) return next();
  const h = req.headers.authorization || "";
  const token = h.startsWith("Bearer ") ? h.slice(7) : null;
  if (!token) return res.status(401).json({ error: "Missing token" });
  try { req.user = await admin.auth().verifyIdToken(token); next(); }
  catch { res.status(401).json({ error: "Invalid token" }); }
}

const app = express();
app.use(express.json({ limit: "50kb" }));
app.use(cors());
app.use(rateLimit({ windowMs: 60_000, max: 20 }));

app.get("/health", (_, res) => res.json({ ok: true }));

app.post("/api/chat", auth, async (req, res) => {
  let msgs = Array.isArray(req.body.messages) ? req.body.messages : [];
  msgs = msgs.slice(-12)
    .filter(m => (m.role === "user" || m.role === "assistant") && typeof m.content === "string" && m.content.trim())
    .map(m => ({ role: m.role, content: m.content.slice(0, 2000) }));
  while (msgs.length && msgs[0].role !== "user") msgs.shift();
  if (!msgs.length || msgs[msgs.length - 1].role !== "user") return res.status(400).json({ error: "Invalid messages" });

  const last = msgs[msgs.length - 1].content;
  if (EMERGENCY.test(last) && !/^(what|define|explain)\b/i.test(last.trim()))
    return res.json({ emergency: true, reply: "This may be an emergency. Please call 112 now or go to the nearest emergency department. If you are thinking about harming yourself, call 112 or Tele-MANAS (14416) right away." });

  try {
    const r = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: { "content-type": "application/json", "x-api-key": process.env.ANTHROPIC_API_KEY, "anthropic-version": "2023-06-01" },
      body: JSON.stringify({ model: process.env.MODEL || "claude-sonnet-5-5", max_tokens: 600, system: SYSTEM, messages: msgs })
    });
    if (!r.ok) { console.error("Upstream status", r.status); return res.status(502).json({ error: "AI service unavailable" }); }
    const data = await r.json();
    const reply = (data.content || []).filter(b => b.type === "text").map(b => b.text).join("\n").trim();
    res.json({ reply: reply || "Sorry, I couldn't generate a reply." });   // note: message content is never logged
  } catch (e) { console.error("Upstream error"); res.status(502).json({ error: "AI service unavailable" }); }
});

app.listen(process.env.PORT || 3000, () => console.log("MediCare AI backend running"));