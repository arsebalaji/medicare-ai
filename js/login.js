
"use strict";

const loginForm = document.getElementById("loginForm");
const userInput = document.getElementById("user");
const passwordInput = document.getElementById("pass");
const rememberInput = document.getElementById("remember");
const loginError = document.getElementById("loginError");
const userError = document.getElementById("userError");
const passError = document.getElementById("passError");
const togglePassword = document.getElementById("togglePassword");

const DEMO_EMAIL = "admin@gmail.com";
const DEMO_PASSWORD = "Admin@123";

function isLoggedIn() {
  return (
    localStorage.getItem("auroraLoggedIn") === "true" ||
    sessionStorage.getItem("auroraLoggedIn") === "true"
  );
}

// Avoid displaying Login to an already authenticated user.
if (isLoggedIn()) {
  window.location.replace("dashboard.html");
}

loginForm.addEventListener("submit", function (event) {
  event.preventDefault();

  const email = userInput.value.trim();
  const password = passwordInput.value;

  userError.textContent = "";
  passError.textContent = "";
  loginError.textContent = "";

  let hasError = false;

  if (!email) {
    userError.textContent = "Please enter your email.";
    hasError = true;
  }

  if (!password.trim()) {
    passError.textContent = "Please enter your password.";
    hasError = true;
  }

  if (hasError) {
    if (!email) {
      userInput.focus();
    } else {
      passwordInput.focus();
    }
    return;
  }

  if (email !== DEMO_EMAIL || password !== DEMO_PASSWORD) {
    loginError.textContent =
      "Invalid email or password. Please try again.";
    passwordInput.value = "";
    passwordInput.focus();
    return;
  }

  // Remove any previous login state.
  localStorage.removeItem("auroraLoggedIn");
  sessionStorage.removeItem("auroraLoggedIn");

  // Remember Me: persistent storage; otherwise tab-session storage.
  const storage = rememberInput.checked
    ? localStorage
    : sessionStorage;

  storage.setItem("auroraLoggedIn", "true");

  // Replace history so Back does not return to the old Login page.
  window.location.replace("dashboard.html");
});

// Show or hide password.
togglePassword.addEventListener("click", function () {
  const showPassword = passwordInput.type === "password";

  passwordInput.type = showPassword ? "text" : "password";
  togglePassword.textContent = showPassword ? "Hide" : "Show";
  togglePassword.setAttribute(
    "aria-label",
    showPassword ? "Hide password" : "Show password"
  );
});

// These links are placeholders for this assignment.
document.getElementById("forgotLink").addEventListener("click", function (event) {
  event.preventDefault();
  loginError.textContent =
    "Password recovery is not available in this demo.";
});

document.getElementById("signupLink").addEventListener("click", function (event) {
  event.preventDefault();
  loginError.textContent =
    "Account registration is not available in this demo.";
});
