# CircuitCraft – Guide (v3)

## 1. Run it
1. Unzip → Android Studio → **File → Open** → select the `CircuitCraft` folder → wait for Gradle sync.
2. Copy **your own** `google-services.json` into `app/` (replace the placeholder).
3. Firebase console → enable **Authentication → Email/Password**, **Firestore Database**, and **Realtime Database** (section 3).
4. If your quiz questions are hosted online, paste your raw JSON link into `QuizRepository.QUIZ_URL` (otherwise the offline questions are used).
5. **Uninstall the old app** from the emulator/phone once, then press **Run ▶**.

## 2. What is implemented (checklist for the presentation)
| Requirement | Where |
|---|---|
| Overview & features | Splash, Login/Register, Components Library, Ohm's calculator, Circuit Lab simulator, Quiz, Leaderboard, Favourites |
| Multi-Activity navigation | 11 activities, `AndroidManifest.xml` (`parentActivityName` = Up arrow) |
| Explicit intents | `MainActivity.open()`, Splash → Login, Favourites → Lab (circuit JSON as extra) |
| Implicit intents | browser (`ACTION_VIEW`), share (`ACTION_SEND`), email to a contact (`ACTION_SENDTO`) |
| View Binding / events | every Activity (`ActivityXxxBinding`), click listeners with lambdas |
| Layouts | **ConstraintLayout** (Splash, Login, Register), LinearLayout, ScrollView, FrameLayout, MaterialCardView |
| RecyclerView | `ComponentAdapter`, `FavouriteAdapter`, `LeaderboardAdapter` (+ ViewHolders, item layouts) |
| Custom View | `sim/SimulatorView` (Canvas, touch, pinch-zoom) |
| System broadcast events | `SystemEventReceiver`: **low battery** + **airplane mode** (Dashboard banner, Lab stops simulating); `NetworkReceiver`: internet on/off |
| Custom broadcast | `QuizTimerService` → `QuizActivity` (`ACTION_TICK`) |
| **Foreground service + notification** | `QuizTimerService`: `startForeground()`, notification updated every second ("Elapsed time 00:45") |
| **Content Provider (shared device data)** | Favourites → Share reads the phone's **Contacts** (`ContactsContract`) with runtime permission `READ_CONTACTS` |
| Own Content Provider | `FavouriteProvider` (+ `DbHelper` SQLite) |
| Permissions | INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, POST_NOTIFICATIONS (runtime on Android 13), READ_CONTACTS (runtime) |
| HTTP + JSON | `QuizRepository` (HttpURLConnection on a background thread, `org.json`, offline fallback) |
| Firebase Authentication | `LoginActivity`, `RegisterActivity` (signed out after sign-up) |
| Cloud Firestore | profile `users/{uid}`, favourites mirror, quiz history `scores` |
| **Realtime Database** | `ScoreRepository` writes `leaderboard/{uid}` (best result); `LeaderboardActivity` listens live (`ValueEventListener`) |
| Pictures / media | not used by the app (the logo is bundled in the APK), so no Firebase Storage |
| Rotation / responsive | no orientation lock; `layout-land` for Login and Lab; ConstraintLayout percent sizing on Splash; `ViewModel` keeps the circuit and the quiz alive on rotation; foreground service keeps counting |

## 2b. Share this app + Dark mode
- **Share this app** (dashboard): copies the installed APK to the cache, exposes it with a `FileProvider`, and opens the share sheet with the APK file + a short message + the GitHub link (`AppSharer`). Works with WhatsApp, Telegram, Bluetooth, Nearby Share, Drive. Gmail blocks .apk attachments (use Drive or a messenger).
- **Dark mode**: a sun / moon icon in the top bar of every screen (☀ = light is on, 🌙 = dark is on; tap to switch). Until you tap it the app follows the phone. Saved in SharedPreferences (`ThemeHelper`, applied in `CircuitCraftApp`, icon in `BaseActivity`). Colours have night variants in `values-night/colors.xml`; the circuit workbench keeps a light "paper" so parts stay readable.

## 2c Quiz questions, bigger workbench, PNP and Clock
- **Quiz questions** are downloaded from `https://raw.githubusercontent.com/amontilladoD/resource/main/electronics_quiz_questions.json` (30 questions). If the phone is offline the same 30 questions are used from `assets/questions.json`. The toast tells you which source was used (and the reason if it fell back). The parser also accepts a top-level array and the keys `question`, `choices`, `correct`, `explanation`.
- **Workbench**: the canvas is now 900 x 1300 units (about 5x the free space). Drag empty space to move around, pinch to zoom, tap **🔍 View** to see the whole workbench and tap again to return to the breadboard. New parts are placed in the first free spot below, then to the right of, the breadboard.
- **PNP Transistor** (`PNP`): emitter to the + supply, ON when the base is pulled low; solved with the same Ebers-Moll model as the NPN with all signs reversed.
- **Clock**: pins OUT and GND; 0 V / 5 V square wave, 50 % duty, frequency set with Value (0.05 - 1000 Hz, default 1 Hz). Wire OUT to the flip-flop CK; connect Q-bar to D for a toggle (divide-by-2) flip-flop. Works without a battery (its GND is the reference).

## 3. Add Firebase Realtime Database (step by step)
1. Firebase console → your project → **Build → Realtime Database → Create Database**.
2. Choose a location (any) → **Start in test mode** → Enable.
3. Open the **Rules** tab, replace everything with the rules below, press **Publish**:
```
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "leaderboard": { ".indexOn": ["score"] }
  }
}
```
4. **Project settings (gear) → Your apps → Android app → download `google-services.json` again** and replace `app/google-services.json`. (The new file now contains your database URL. If you skip this, you get "Specify DatabaseURL".)
5. If the error persists, copy the URL shown at the top of the Realtime Database page (e.g. `https://xxxx-default-rtdb.firebaseio.com`) into `RealtimeDb.DB_URL`.
6. Run the app → finish a quiz → open **Leaderboard**. In the console the **Data** tab shows `leaderboard → <uid> → name, score, total, seconds`. Finish another quiz with a better score on a second account and watch the leaderboard update live.

The dependency (`firebase-database`) is already in `gradle/libs.versions.toml` and `app/build.gradle`.

## 4. How to demonstrate each feature
| Feature | How |
|---|---|
| Low battery | Emulator: `...` (Extended controls) → **Battery** → Charge level **10 %**, state **Discharging** → banner on the dashboard; in the Lab the simulation stops |
| Airplane mode | Pull down quick settings → toggle airplane mode → banner on the dashboard |
| Foreground service | Start a quiz → pull down the notification shade → "CircuitCraft quiz in progress – Elapsed time 00:12", updates every second. Android 13: allow the notification permission |
| Contacts provider | Add a contact **with an email** in the emulator's Contacts app → Favourites → **Share** → allow permission → pick the contact → email app opens |
| Rotation | Rotate the phone (enable auto-rotate) in Lab / Quiz / Login: the circuit, quiz progress and timer stay |
| Realtime DB | See section 3, step 6 |

## 5. Package map
```
com.circuitcraft.app
├── MainActivity
├── model     ComponentItem, Favourite, QuizQuestion, LeaderboardEntry
├── logic     CircuitCalculator (Ohm), Units
├── sim       PartType, Part, CircuitModel, CircuitEngine, CircuitIO, SimulatorView
├── data      DbHelper, FavouriteProvider, FavouriteRepository, QuizRepository,
│             ScoreRepository, RealtimeDb, ComponentRepository
├── service   QuizTimerService (foreground)
├── receiver  NetworkReceiver, SystemEventReceiver
└── ui        Splash/Login/Register/Components/Calculator/Lab/Quiz/Leaderboard/Favourites activities,
              adapters, QuizViewModel, LabViewModel
```

## 6. Limits (be honest in the demo)
NPN transistor only; gates use an implicit 5 V supply and the battery − as ground; breadboard rails are not split; in landscape the Lab workbench is smaller (pinch to zoom).
