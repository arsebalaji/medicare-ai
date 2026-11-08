"use strict";

const STORAGE_KEY = "medicarePreviewState";
const toDateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
const today = () => toDateKey(new Date());
const makeId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`;
const defaultState = () => ({
  route: "home",
  name: "Balaji",
  profile: {},
  meds: [
    { id: makeId(), name: "Paracetamol", schedule: "As prescribed", times: "08:00", status: "PENDING" },
    { id: makeId(), name: "Vitamin D", schedule: "As prescribed", times: "13:30", status: "PENDING" }
  ],
  appointments: [],
  entries: [
    { id: makeId(), type: "WATER", value: 1800, day: today(), source: "Manual" },
    { id: makeId(), type: "SLEEP", value: 7.4, day: today(), source: "Manual" },
    { id: makeId(), type: "STEPS", value: 8200, day: today(), source: "Manual" }
  ],
  goals: { WATER: 2500, SLEEP: 8, STEPS: 10000 },
  chat: [],
  saveChat: true,
  theme: "light",
  symptom: "",
  notice: ""
});

let state = defaultState();
try {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved) state = { ...defaultState(), ...JSON.parse(saved) };
} catch {
  state.notice = "Saved demo data could not be read. New changes will be saved from this session.";
}

const app = document.getElementById("app");
const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
  "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
})[char]);
const dateLabel = (value) => value ? new Intl.DateTimeFormat("en-IN", { dateStyle: "medium" }).format(new Date(`${value}T12:00:00`)) : "";
const typeInfo = {
  WATER: { title: "Water intake", unit: "ml", icon: "💧" },
  SLEEP: { title: "Sleep", unit: "hours", icon: "🌙" },
  STEPS: { title: "Activity", unit: "steps", icon: "👟" }
};

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    state.notice = "Couldn't save changes in this browser. Check available local storage.";
  }
}

if (["afroz", "balaji"].includes(String(state.name).toLowerCase())) {
  state.name = "Balaji";
  persist();
}

function go(route) {
  state.route = route;
  state.notice = "";
  persist();
  render();
}

function button(label, action, extra = "") {
  return `<button type="button" class="primary-btn ${extra}" data-action="${esc(action)}">${label}</button>`;
}

function secondary(label, action, extra = "") {
  return `<button type="button" class="secondary-btn ${extra}" data-action="${esc(action)}">${label}</button>`;
}

function card(title, body, extra = "") {
  return `<section class="feature-card ${extra}"><h2>${title}</h2>${body}</section>`;
}

function navButton(route, icon, label) {
  return `<button type="button" class="nav-item ${state.route === route ? "active" : ""}" data-route="${route}" aria-current="${state.route === route ? "page" : "false"}"><span>${icon}</span><small>${label}</small></button>`;
}

function summaryValue(type) {
  return state.entries.filter((entry) => entry.type === type && entry.day === today())
    .reduce((sum, entry) => sum + Number(entry.value), 0);
}

function ring(type) {
  const value = summaryValue(type);
  const goal = Number(state.goals[type]) || 1;
  const ratio = Math.min(value / goal, 1);
  const display = type === "WATER" ? `${(value / 1000).toFixed(1)}L` : type === "SLEEP" ? `${value.toFixed(1)}h` : `${(value / 1000).toFixed(1)}k`;
  return `<button class="summary-ring" type="button" data-route="${type.toLowerCase()}">
    <span class="ring" style="--progress:${Math.round(ratio * 100)}%"><span>${esc(display)}</span></span>
    <small>${typeInfo[type].title}</small>
  </button>`;
}

function homeScreen() {
  const pending = state.meds.filter((med) => med.status === "PENDING").slice(0, 3);
  const upcoming = state.appointments.filter((item) => item.date >= today()).sort((a, b) => a.date.localeCompare(b.date)).slice(0, 2);
  return `<section class="hero-card">
    <div class="eyebrow">${new Intl.DateTimeFormat("en", { weekday: "long" }).format(new Date())}</div>
    <div class="greeting-row"><div><h1>Hello, ${esc(state.name || "there")}</h1><p>${dateLabel(today())}</p></div><button class="icon-btn" type="button" data-route="profile" aria-label="Health profile">●</button></div>
    <div class="summary-title">Today's wellness summary</div>
    <div class="rings-row">${ring("WATER")}${ring("SLEEP")}${ring("STEPS")}</div>
  </section>
  ${card("✦ Health assistant", `<p>Ask general health questions or prepare for a doctor visit.</p>${button("Open assistant", "go-chat")}`)}
  ${card("Upcoming medicine reminders", `${pending.length ? `<ul class="plain-list">${pending.map((med) => `<li><strong>${esc(med.times)}</strong> · ${esc(med.name)}</li>`).join("")}</ul>` : "<p>No pending reminders.</p>"}<button type="button" class="text-btn" data-route="medicines">Manage medicines</button>`)}
  ${card("Upcoming appointments", `${upcoming.length ? `<ul class="plain-list">${upcoming.map((item) => `<li>${dateLabel(item.date)} · Dr. ${esc(item.doctor)}</li>`).join("")}</ul>` : "<p>No upcoming appointments.</p>"}<button type="button" class="text-btn" data-route="appointments">Manage appointments</button>`)}
  ${card("Emergency", `<p>In an emergency, call 112.</p><a class="danger-btn" href="tel:112">Call 112</a>`, "warning-card")}
  <p class="disclaimer">Demo only. General wellness information is not a diagnosis or a substitute for professional care.</p>`;
}

function chatScreen() {
  const messages = state.chat.map((message) => `<div class="chat-row ${message.role === "user" ? "mine" : ""}"><div class="chat-bubble">${esc(message.text)}</div></div>`).join("");
  return `<h1 class="page-title">Health assistant</h1>
    <p class="disclaimer">For general information only—not diagnosis, treatment, or emergency care. Don't enter private medical details in this demo.</p>
    ${!state.chat.length ? card("Try asking", `<div class="chip-row">${["What does hypertension mean?", "How can I prepare for a doctor visit?", "Why is hydration important?"].map((text) => `<button type="button" class="chip" data-suggestion="${esc(text)}">${esc(text)}</button>`).join("")}</div>`) : ""}
    <section class="chat-log" aria-live="polite">${messages || `<p class="muted">Your conversation will appear here.</p>`}</section>
    <form class="feature-card form-stack" id="chat-form"><label for="chat-message">Your question</label><textarea id="chat-message" name="message" rows="3" maxlength="800" required placeholder="Ask a general health question"></textarea><button class="primary-btn" type="submit">Send message</button></form>
    ${state.chat.some((item) => item.emergency) ? `<div class="feature-card warning-card"><strong>Possible emergency</strong><p>Call emergency services now. Don't wait for an online response.</p><a class="danger-btn" href="tel:112">Call 112</a></div>` : ""}`;
}

function medicinesScreen() {
  const list = state.meds.length ? state.meds.map((med) => `<article class="feature-card data-card">
    <div class="card-heading"><div><h2>${esc(med.name)}</h2><p>${esc(med.schedule || "Follow your prescription")} · ${esc(med.times)}</p><span class="status ${med.status === "PENDING" ? "" : "complete"}">${med.status === "PENDING" ? "Upcoming" : esc(med.status.toLowerCase())}</span></div>
    <div class="button-stack">${secondary("Edit", `edit-med:${med.id}`)}${secondary("Delete", `delete-med:${med.id}`, "quiet")}</div></div>
    <div class="button-row">${secondary("Taken", `mark-med:${med.id}:TAKEN`)}${secondary("Missed", `mark-med:${med.id}:MISSED`)}</div></article>`).join("") : `<p class="muted">No medicines yet. Add one to keep a demo reminder list.</p>`;
  return `<h1 class="page-title">Medicine reminders</h1><p class="disclaimer">This demo does not send notifications or recommend medicines or doses. Follow your clinician's prescription.</p>
    ${card("Add a medicine", `<form class="form-grid" id="med-form"><input type="hidden" name="id"><label>Medicine name<input name="name" required maxlength="80"></label><label>Prescribed schedule<input name="schedule" maxlength="100" placeholder="As written by your doctor"></label><label>Reminder time<input name="times" type="time" value="08:00" required></label><button class="primary-btn" type="submit">Save medicine</button><button type="button" class="text-btn hidden" data-action="cancel-med-edit">Cancel edit</button></form>`)}
    <h2 class="section-title">Your medicines</h2>${list}`;
}

function appointmentsScreen() {
  const items = state.appointments.slice().sort((a, b) => a.date.localeCompare(b.date));
  const list = items.length ? items.map((item) => `<article class="feature-card data-card"><div class="card-heading"><div><h2>Dr. ${esc(item.doctor)}</h2><p>${dateLabel(item.date)}${item.time ? ` · ${esc(item.time)}` : ""}</p><p>${esc(item.place || "")}</p>${item.notes ? `<p>${esc(item.notes)}</p>` : ""}</div><div class="button-stack">${secondary("Edit", `edit-appt:${item.id}`)}${secondary("Delete", `delete-appt:${item.id}`, "quiet")}</div></div></article>`).join("") : `<p class="muted">No appointments added yet.</p>`;
  return `<h1 class="page-title">Appointments</h1>
    ${card("Add an appointment", `<form class="form-grid" id="appt-form"><input type="hidden" name="id"><label>Doctor name<input name="doctor" required maxlength="80"></label><label>Clinic or hospital<input name="place" maxlength="100"></label><div class="form-row"><label>Date<input name="date" type="date" min="${today()}" required></label><label>Time<input name="time" type="time"></label></div><label>Notes<textarea name="notes" rows="2" maxlength="300"></textarea></label><button class="primary-btn" type="submit">Save appointment</button><button type="button" class="text-btn hidden" data-action="cancel-appt-edit">Cancel edit</button></form>`)}
    <h2 class="section-title">Your visits</h2>${list}`;
}

function trackerScreen(type) {
  const info = typeInfo[type];
  const goal = Number(state.goals[type]) || 1;
  const daily = summaryValue(type);
  const entries = state.entries.filter((entry) => entry.type === type).sort((a, b) => b.day.localeCompare(a.day));
  const quick = type === "WATER" ? [150, 250, 500].map((amount) => secondary(`+${amount} ml`, `quick-add:${type}:${amount}`)).join("") : "";
  const bars = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() - (6 - index));
    const day = toDateKey(date);
    const value = state.entries.filter((entry) => entry.type === type && entry.day === day).reduce((sum, entry) => sum + Number(entry.value), 0);
    return `<div class="chart-column"><div class="chart-bar" style="height:${Math.max(4, Math.min(100, value / goal * 100))}%"></div><small>${new Intl.DateTimeFormat("en", { weekday: "short" }).format(date)}</small></div>`;
  }).join("");
  return `<h1 class="page-title">${info.icon} ${info.title}</h1>
    ${card("Today's progress", `<div class="progress-track"><span style="width:${Math.min(100, daily / goal * 100)}%"></span></div><p class="progress-copy"><strong>${esc(daily.toLocaleString())} ${info.unit}</strong> of ${esc(goal.toLocaleString())} ${info.unit}</p><form class="inline-form" id="goal-form"><label>Daily goal<input type="number" name="goal" min="1" step="${type === "SLEEP" ? "0.5" : "1"}" value="${esc(goal)}" required></label><button class="secondary-btn" type="submit">Update goal</button></form>`)}
    ${card(`Add ${info.unit}`, `<div class="button-row">${quick}</div><form class="inline-form" id="entry-form"><label>Amount (${info.unit})<input type="number" name="value" min="${type === "SLEEP" ? "0.1" : "1"}" step="${type === "SLEEP" ? "0.1" : "1"}" required></label><button class="primary-btn" type="submit">Add entry</button></form>${type === "STEPS" ? `<p class="muted">Phone sensor sync is available in the Android app; enter steps manually in this browser demo.</p>` : ""}`)}
    ${card("Last 7 days", `<div class="chart">${bars}</div><p class="muted">Bars show daily totals; the goal is ${esc(goal)} ${info.unit} per day.</p>`)}
    <h2 class="section-title">Entries</h2>${entries.length ? entries.map((entry) => `<article class="feature-card data-card compact"><div><strong>${esc(entry.value)} ${info.unit}</strong><p>${dateLabel(entry.day)} · ${esc(entry.source)}</p></div>${secondary("Delete", `delete-entry:${entry.id}`, "quiet")}</article>`).join("") : `<p class="muted">No entries yet.</p>`}`;
}

function moreScreen() {
  const links = [
    ["water", "💧", "Water intake", "Track daily water"],
    ["sleep", "🌙", "Sleep", "Track hours slept"],
    ["steps", "👟", "Activity", "Track steps"],
    ["symptoms", "＋", "Symptom guide", "General educational information"],
    ["profile", "◉", "Health profile", "Optional details stored in this browser"],
    ["history", "▤", "History & reports", "Review or share a summary"],
    ["emergency", "!", "Emergency", "Emergency number and guidance"],
    ["settings", "⚙", "Settings", "Theme, privacy, and demo data"]
  ];
  return `<h1 class="page-title">More</h1><div class="menu-list">${links.map(([route, icon, title, sub]) => `<button type="button" class="menu-item" data-route="${route}"><span class="menu-icon">${icon}</span><span><strong>${title}</strong><small>${sub}</small></span><span class="chevron">›</span></button>`).join("")}</div>`;
}

const symptoms = {
  "Headache": ["Sudden severe pain, weakness, confusion, vision loss, or fainting", "A clinician can help assess your headache and its triggers."],
  "Cough": ["Severe trouble breathing, blue lips, or coughing blood", "Note how long the cough has lasted and any other symptoms."],
  "Fever": ["Difficulty breathing, confusion, seizure, or inability to stay awake", "Track temperature, duration, and other symptoms."],
  "Stomach pain": ["Severe or rapidly worsening pain, fainting, or blood in vomit/stool", "Note where the pain is and when it started."],
  "Nausea": ["Severe dehydration, fainting, or blood in vomit", "Note how long it has lasted and whether you can keep fluids down."],
  "Chest discomfort": ["Any chest pain, pressure, or tightness can be an emergency", "Do not use this guide to assess chest discomfort; seek emergency help."]
};

function symptomsScreen() {
  const opts = Object.keys(symptoms).map((item) => `<option ${state.symptom === item ? "selected" : ""}>${item}</option>`).join("");
  const selected = symptoms[state.symptom];
  return `<h1 class="page-title">Symptom guide</h1><p class="disclaimer">Educational information only. This does not diagnose a condition or replace professional care.</p>
    ${card("Choose a symptom", `<form class="form-stack" id="symptom-form"><label>Symptom<select name="symptom" required><option value="">Select one</option>${opts}</select></label><label>How long has it lasted?<select name="duration"><option>Today</option><option>1–3 days</option><option>More than 3 days</option></select></label><fieldset><legend>Does any of this apply?</legend>${selected ? `<label class="check-row"><input type="checkbox" name="redFlag"> ${esc(selected[0])}</label>` : `<span class="muted">Select a symptom to see relevant warning signs.</span>`}</fieldset><label>Anything else to note (optional)<textarea name="note" rows="2" maxlength="300"></textarea></label><button class="primary-btn" type="submit">Show general guidance</button></form>`)}
    ${selected ? card(`About ${esc(state.symptom.toLowerCase())}`, `<p>${esc(selected[1])}</p><p class="muted">If you are worried or symptoms are worsening, contact a healthcare professional.</p>`) : ""}
    ${state.notice ? card("Guidance", `<p>${esc(state.notice)}</p>${state.notice.includes("emergency") ? `<a class="danger-btn" href="tel:112">Call 112</a>` : ""}`) : ""}`;
}

function profileScreen() {
  const p = state.profile;
  return `<h1 class="page-title">Health profile</h1><p class="disclaimer">All fields are optional. This demo stores information in this browser only; don't enter sensitive details on a shared device.</p>
    ${card("Your details", `<form class="form-grid" id="profile-form"><label>Name<input name="name" value="${esc(state.name)}" maxlength="60"></label><div class="form-row"><label>Age<input name="age" type="number" min="0" max="120" value="${esc(p.age || "")}"></label><label>Height (cm)<input name="height" type="number" min="30" max="260" value="${esc(p.height || "")}"></label></div><label>Weight (kg)<input name="weight" type="number" min="1" max="500" step="0.1" value="${esc(p.weight || "")}"></label><label>Allergies<input name="allergies" value="${esc(p.allergies || "")}" maxlength="300"></label><label>Existing conditions<input name="conditions" value="${esc(p.conditions || "")}" maxlength="300"></label><div class="form-row"><label>Emergency contact name<input name="contactName" value="${esc(p.contactName || "")}" maxlength="80"></label><label>Emergency phone<input name="contactPhone" type="tel" value="${esc(p.contactPhone || "")}" maxlength="30"></label></div><button class="primary-btn" type="submit">Save profile</button><button class="secondary-btn" type="button" data-action="delete-profile">Delete profile</button></form>`)}`;
}

function historyScreen() {
  const meds = state.meds.map((med) => `<li>${esc(med.name)} · ${esc(med.status.toLowerCase())}</li>`).join("");
  const visits = state.appointments.map((item) => `<li>${dateLabel(item.date)} · Dr. ${esc(item.doctor)}</li>`).join("");
  const wellness = ["WATER", "SLEEP", "STEPS"].map((type) => `<li>${typeInfo[type].title}: ${esc(summaryValue(type))} ${typeInfo[type].unit} today</li>`).join("");
  return `<h1 class="page-title">History & reports</h1>${card("Summary", `<div id="report-text"><h3>${esc(state.name)}'s wellness summary</h3><p>Generated ${dateLabel(today())}</p><ul>${wellness}${meds}${visits}</ul>${state.profile.allergies ? `<p>Allergies: ${esc(state.profile.allergies)}</p>` : ""}</div><label class="check-row"><input id="include-profile" type="checkbox"> Include optional profile details</label><button class="primary-btn" type="button" data-action="share-report">Share summary</button>`)}
    ${card("Recent assistant conversation", state.chat.length ? state.chat.map((message) => `<p><strong>${message.role === "user" ? "You" : "Assistant"}:</strong> ${esc(message.text)}</p>`).join("") : "<p>No chat history.</p>")}`;
}

function emergencyScreen() {
  return `<h1 class="page-title">Emergency</h1>${card("Emergency number (India)", `<div class="emergency-number">112</div><a class="danger-btn full-width" href="tel:112">Call 112</a><p class="muted">This preview cannot place a call. Use your phone's dialer if you need emergency services.</p>`, "warning-card")}
    ${card("What to do", `<ol class="plain-list"><li>Call 112 and clearly tell the operator where you are.</li><li>Describe what happened and whether the person is awake and breathing.</li><li>Stay on the line and follow the operator's instructions.</li><li>Ask someone nearby to guide responders to you if possible.</li></ol>`)}
    ${state.profile.contactPhone ? card("Emergency contact", `<p>${esc(state.profile.contactName || "Saved contact")} · ${esc(state.profile.contactPhone)}</p><a class="secondary-btn" href="tel:${encodeURIComponent(state.profile.contactPhone)}">Call contact</a>`) : ""}<p class="disclaimer">This app cannot send help automatically. Contact local emergency services directly.</p>`;
}

function settingsScreen() {
  return `<h1 class="page-title">Settings</h1>${card("Appearance", `<div class="button-row">${["light", "dark"].map((theme) => `<button type="button" class="secondary-btn ${state.theme === theme ? "selected" : ""}" data-action="theme:${theme}">${theme[0].toUpperCase()}${theme.slice(1)}</button>`).join("")}</div>`)}
    ${card("Privacy", `<label class="check-row"><input type="checkbox" data-action="toggle-chat" ${state.saveChat ? "checked" : ""}> Save assistant chat history in this browser</label><p class="muted">Chat here uses local demo responses and doesn't contact the AI service.</p><button class="secondary-btn" type="button" data-action="clear-chat">Clear chat history</button>`)}
    ${card("Demo data", `<p>Health data and settings are stored in this browser only.</p><button class="secondary-btn danger-outline" type="button" data-action="wipe-data">Delete all demo data</button>`)}`;
}

const screens = {
  home: homeScreen, chat: chatScreen, medicines: medicinesScreen, appointments: appointmentsScreen,
  more: moreScreen, water: () => trackerScreen("WATER"), sleep: () => trackerScreen("SLEEP"),
  steps: () => trackerScreen("STEPS"), symptoms: symptomsScreen, profile: profileScreen,
  history: historyScreen, emergency: emergencyScreen, settings: settingsScreen
};

function render() {
  document.body.dataset.theme = state.theme;
  const screen = (screens[state.route] || homeScreen)();
  app.innerHTML = `<div class="phone-frame">
    <header class="topbar"><div class="status-icons"><span>9:41</span><span>● ● ●</span></div><span class="demo-label">Interactive browser demo · changes saved here</span></header>
    <main class="screen" id="screen"><div class="notice" role="status">${esc(state.notice)}</div>${screen}</main>
    <nav class="bottom-nav" aria-label="Main navigation">${navButton("home", "⌂", "Home")}${navButton("chat", "✦", "Assistant")}${navButton("medicines", "✚", "Medicines")}${navButton("appointments", "◫", "Visits")}${navButton("more", "☰", "More")}</nav>
  </div>`;
}

function setNotice(text) {
  state.notice = text;
  persist();
  render();
}

function handleClick(event) {
  const routeButton = event.target.closest("[data-route]");
  if (routeButton) {
    go(routeButton.dataset.route);
    return;
  }
  const suggestion = event.target.closest("[data-suggestion]");
  if (suggestion) {
    sendMessage(suggestion.dataset.suggestion);
    return;
  }
  const control = event.target.closest("[data-action]");
  if (!control) return;
  const [action, ...args] = control.dataset.action.split(":");
  if (action.startsWith("go-")) {
    go(action.slice(3));
  } else if (action === "mark-med") {
    const med = state.meds.find((item) => item.id === args[0]);
    if (med) med.status = args[1];
    persist(); render();
  } else if (action === "delete-med" || action === "delete-appt" || action === "delete-entry") {
    if (!window.confirm("Delete this item?")) return;
    if (action === "delete-med") state.meds = state.meds.filter((item) => item.id !== args[0]);
    if (action === "delete-appt") state.appointments = state.appointments.filter((item) => item.id !== args[0]);
    if (action === "delete-entry") state.entries = state.entries.filter((item) => item.id !== args[0]);
    persist(); render();
  } else if (action === "edit-med") {
    const med = state.meds.find((item) => item.id === args[0]);
    const form = document.getElementById("med-form");
    if (med && form) {
      form.elements.id.value = med.id;
      form.elements.name.value = med.name;
      form.elements.schedule.value = med.schedule;
      form.elements.times.value = med.times.split(",")[0] || "08:00";
      form.querySelector('[type="submit"]').textContent = "Update medicine";
      form.querySelector('[data-action="cancel-med-edit"]').classList.remove("hidden");
      form.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  } else if (action === "edit-appt") {
    const item = state.appointments.find((appt) => appt.id === args[0]);
    const form = document.getElementById("appt-form");
    if (item && form) {
      for (const key of ["id", "doctor", "place", "date", "time", "notes"]) form.elements[key].value = item[key] || "";
      form.querySelector('[type="submit"]').textContent = "Update appointment";
      form.querySelector('[data-action="cancel-appt-edit"]').classList.remove("hidden");
      form.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  } else if (action === "quick-add") {
    state.entries.push({ id: makeId(), type: args[0], value: Number(args[1]), day: today(), source: "Manual" });
    persist(); render();
  } else if (action === "theme") {
    state.theme = args[0]; persist(); render();
  } else if (action === "toggle-chat") {
    state.saveChat = control.checked;
    if (!state.saveChat) state.chat = [];
    persist(); render();
  } else if (action === "clear-chat") {
    if (window.confirm("Clear all saved demo chat messages?")) {
      state.chat = []; persist(); render();
    }
  } else if (action === "wipe-data") {
    if (window.confirm("Delete all data saved by this browser demo?")) {
      state = defaultState(); persist(); render();
    }
  } else if (action === "delete-profile") {
    if (window.confirm("Delete the saved health profile?")) {
      state.profile = {}; persist(); render();
    }
  } else if (action === "cancel-med-edit") {
    render();
  } else if (action === "cancel-appt-edit") {
    render();
  } else if (action === "share-report") {
    shareReport();
  }
}

function sendMessage(text) {
  const question = text.trim();
  if (!question) return;
  const emergency = /(chest pain|can't breathe|cannot breathe|trouble breathing|unconscious|severe bleeding|not breathing)/i.test(question);
  const reply = emergency
    ? "This could be an emergency. Call 112 or your local emergency number now. Don't wait for an online reply."
    : "I can offer general information, but I can't diagnose or recommend treatment. For personal medical advice, contact a qualified healthcare professional. For emergencies, call 112.";
  if (state.saveChat) state.chat.push({ role: "user", text: question }, { role: "assistant", text: reply, emergency });
  state.notice = emergency ? "Possible emergency — call emergency services now." : "";
  persist(); render();
}

function shareReport() {
  const report = `${state.name}'s wellness summary (${dateLabel(today())})\nWater: ${summaryValue("WATER")} ml\nSleep: ${summaryValue("SLEEP")} hours\nSteps: ${summaryValue("STEPS")}\nMedicines: ${state.meds.length}\nAppointments: ${state.appointments.length}`;
  if (navigator.share) {
    navigator.share({ title: "Wellness summary", text: report }).catch(() => setNotice("Sharing was cancelled or isn't available."));
  } else if (navigator.clipboard) {
    navigator.clipboard.writeText(report).then(() => setNotice("Summary copied to clipboard.")).catch(() => setNotice("Couldn't copy the summary; try selecting the report text."));
  } else {
    setNotice("Sharing isn't available in this browser. Select and copy the summary text.");
  }
}

function handleSubmit(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;
  event.preventDefault();
  const formId = form.getAttribute("id");
  const data = new FormData(form);
  if (formId === "chat-form") {
    sendMessage(String(data.get("message") || ""));
  } else if (formId === "med-form") {
    const id = String(data.get("id") || "");
    const existing = state.meds.find((item) => item.id === id);
    const medicine = { id: id || makeId(), name: String(data.get("name")).trim(), schedule: String(data.get("schedule") || "").trim(), times: String(data.get("times")), status: existing?.status || "PENDING" };
    state.meds = existing ? state.meds.map((item) => item.id === id ? medicine : item) : [...state.meds, medicine];
    persist(); render();
  } else if (formId === "appt-form") {
    const id = String(data.get("id") || "");
    const appointment = Object.fromEntries(["doctor", "place", "date", "time", "notes"].map((key) => [key, String(data.get(key) || "").trim()]));
    appointment.id = id || makeId();
    state.appointments = id ? state.appointments.map((item) => item.id === id ? appointment : item) : [...state.appointments, appointment];
    persist(); render();
  } else if (formId === "entry-form") {
    const value = Number(data.get("value"));
    if (!Number.isFinite(value) || value <= 0) return setNotice("Enter a value greater than zero.");
    const type = state.route === "water" ? "WATER" : state.route === "sleep" ? "SLEEP" : "STEPS";
    state.entries.push({ id: makeId(), type, value, day: today(), source: "Manual" });
    persist(); render();
  } else if (formId === "goal-form") {
    const goal = Number(data.get("goal"));
    if (!Number.isFinite(goal) || goal <= 0) return setNotice("Enter a goal greater than zero.");
    const type = state.route === "water" ? "WATER" : state.route === "sleep" ? "SLEEP" : "STEPS";
    state.goals[type] = goal; persist(); render();
  } else if (formId === "profile-form") {
    const age = String(data.get("age") || "");
    const height = String(data.get("height") || "");
    const weight = String(data.get("weight") || "");
    if (age && (+age < 0 || +age > 120)) return setNotice("Age must be between 0 and 120.");
    if (height && (+height < 30 || +height > 260)) return setNotice("Height must be between 30 and 260 cm.");
    if (weight && (+weight < 1 || +weight > 500)) return setNotice("Weight must be between 1 and 500 kg.");
    state.name = String(data.get("name") || "there").trim() || "there";
    state.profile = Object.fromEntries(["age", "height", "weight", "allergies", "conditions", "contactName", "contactPhone"].map((key) => [key, String(data.get(key) || "").trim()]));
    persist(); render();
  } else if (formId === "symptom-form") {
    const symptom = String(data.get("symptom") || "");
    state.symptom = symptom;
    const urgent = Boolean(data.get("redFlag")) || symptom === "Chest discomfort";
    const duration = String(data.get("duration") || "");
    state.notice = urgent
      ? "Seek emergency care now. Call 112 or go to the nearest emergency department. Don't wait."
      : duration === "More than 3 days"
        ? "Consider contacting a healthcare professional soon, especially if symptoms worsen."
        : "Monitor how you feel and contact a healthcare professional if symptoms worsen or concern you.";
    persist(); render();
  }
}

app.addEventListener("click", handleClick);
app.addEventListener("submit", handleSubmit);
app.addEventListener("change", (event) => {
  if (event.target.name === "symptom") {
    state.symptom = event.target.value;
    state.notice = "";
    render();
  }
});
render();
