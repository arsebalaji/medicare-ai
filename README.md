# MediCare AI

Android wellness assistant: Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Firebase Auth · Node.js AI proxy.

## Setup
1. **Firebase**: console.firebase.google.com → new project → add Android app `com.medicare.ai`
   → Authentication → enable *Email/Password* → download `google-services.json` into `app/`.
   Project settings → Service accounts → generate key → save as `backend/serviceAccount.json`.
2. **Backend locally** (Node 18+), from PowerShell:
   ```powershell
   cd backend
   npm install
   Copy-Item .env.example .env
   ```
   Add your Anthropic API key to `backend/.env`, save your Firebase service-account key as
   `backend/serviceAccount.json`, then run `npm start` from `backend/`. Check http://localhost:3000/health.

## Deploy the backend to Render
1. Push this repository to GitHub. In Render, choose **New → Blueprint** and connect the repository.
   Set the Blueprint Path to `MediCareAI/render.yaml` (the repository also contains the older web project).
2. Enter your Anthropic API key when prompted for `ANTHROPIC_API_KEY`.
3. After the service is created, open **Environment → Secret Files**, add a file named
   `serviceAccount.json`, and paste the Firebase service-account JSON into it. Save the change and
   redeploy. Do not commit this file or put its contents in the Android app.
4. Wait for the deploy to finish, then open `https://<your-render-service>.onrender.com/health`.
   It should return `{"ok":true}`. The free service may take a little time to wake after inactivity.
5. In `gradle.properties`, change `BACKEND_URL` to your Render URL with a trailing slash, for example
   `https://medicare-ai-backend.onrender.com/`, then rebuild the Android app.

## Run on Android
Open the folder in Android Studio, sync, and run on an emulator. For local backend development, the
emulator reaches your PC at `10.0.2.2` (the default in `gradle.properties`). For a physical phone or
the Render backend, set `BACKEND_URL` to the server URL with a trailing slash. Deployed backends must
use HTTPS.

## Security
- The AI key stays in `backend/.env` locally or Render's secret environment variable; the app sends a
  Firebase ID token, which the server verifies.
- Health data stays in a local Room DB; the AI receives only the last 10 chat messages (never your profile).
- Chat history can be turned off or cleared; "Delete all my data" wipes everything.
- android:allowBackup=false; HTTPS required except the dev emulator host.

## Build APK
Windows PowerShell: `.\gradlew.bat assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
Other platforms: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
Release: Build → Generate Signed Bundle/APK → APK → create keystore → release.
Install: adb install app-debug.apk

## Tests
Windows PowerShell: `.\gradlew.bat test`
Other platforms: `./gradlew test`
Manual checklist (run on emulator + one real phone):
| Area | Steps | Expected |
|---|---|---|
| Register/Login | Sign up with consent, log out, log in; wrong password | Dashboard shown; friendly error on bad login |
| Navigation | Tap every bottom tab and every item in More | No crashes, back works |
| Chatbot | "What does hypertension mean?" / "I have chest pain" | Answer / immediate 112 card, no API call |
| Reminder | Add medicine 2 minutes ahead, lock phone | Notification fires, Taken/Missed updates, next-day alarm re-armed |
| Edit/delete | Edit times, delete medicine | Old alarms cancelled |
| Appointments | Add, edit, delete, past/upcoming split | Lists update |
| Wellness | Add water/sleep, edit goals, sync steps | Rings + chart update; source label correct |
| Persistence | Force-stop and reopen; reboot phone | Data kept; alarms restored |
| Offline | Airplane mode | Local features work; chat shows friendly error |
| Errors | Empty fields, invalid age, 0 ml | Validation messages |
| Accessibility | TalkBack on; font size = Largest; dark mode | Icons labelled, no clipped text |

## Screenshots
Run the app, then: adb exec-out screencap -p > docs/home.png (repeat per screen),
or use Android Studio's Logcat/Running Devices camera button.

## Known limitations
- Symptom guide content and emergency keywords are general and must be reviewed by a qualified clinician before real-world use.
- Device step sync counts steps from the first sync of each day (Android's step counter is cumulative since boot).
- Appointment reminders are not implemented (medicine reminders are).