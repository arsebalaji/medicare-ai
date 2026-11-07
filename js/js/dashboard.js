
"use strict";

(function () {
  function isLoggedIn() {
    return (
      localStorage.getItem("auroraLoggedIn") === "true" ||
      sessionStorage.getItem("auroraLoggedIn") === "true"
    );
  }

  function protectDashboard() {
    if (!isLoggedIn()) {
      window.location.replace("index.html");
      return false;
    }

    return true;
  }

  // Check authentication before displaying the dashboard.
  if (!protectDashboard()) {
    return;
  }

  // Recheck if a page is restored from browser history or cache.
  window.addEventListener("pageshow", function () {
    protectDashboard();
  });

  // Logout clears both storage options.
  document.getElementById("logoutBtn").addEventListener("click", function () {
    localStorage.removeItem("auroraLoggedIn");
    sessionStorage.removeItem("auroraLoggedIn");

    window.location.replace("index.html");
  });

  // Display current date.
  const dateElement = document.getElementById("currentDate");

  if (dateElement) {
    dateElement.textContent = new Intl.DateTimeFormat("en-IN", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric"
    }).format(new Date());
  }
})();
