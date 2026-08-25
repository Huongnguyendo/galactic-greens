# Galactic Greens 🚀🌱

Hungry in deep space? Galactic Greens has you covered. This Android app turns food ordering into a tiny interplanetary adventure: browse quirky cosmic kitchens, fill your cart, choose a destination, track your order across the map, and collect rewards along the way. No spacesuit required.

## Demo

> 🎬 **Demo video**
> [![Watch the Galactic Greens demo]](https://youtube.com/shorts/jvFgD0AeZKI)

## Features

- Space-themed Material 3 interface built entirely with Jetpack Compose
- Firebase email/password authentication
- Ingredient-aware menu filtering using TheMealDB recipe details
- Breakfast, dessert, pasta, side, starter, vegan, and vegetarian categories
- Multiple themed restaurants per category with Firestore menu caching
- Search, sorting, category browsing, and detailed recipe views
- Firestore-backed shopping cart and per-user order history
- Order confirmation, scheduling, ETA calculation, and tracking
- Google Maps delivery view with current-location support
- CameraX delivery-photo capture
- Firebase Storage uploads
- Points and badge rewards based on delivery photos

## Technology

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | ViewModel, repository pattern, StateFlow |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Photo storage | Firebase Storage and app-private local storage |
| Food data | [TheMealDB](https://www.themealdb.com/api.php) |
| Maps and location | Google Maps Compose, Google Play Services Location |
| Camera | CameraX |
| Networking | Retrofit, Moshi, OkHttp |
| Images | Coil |
| Preferences | Jetpack DataStore |
| Testing | JUnit, AndroidX Test, Espresso, Compose UI Test |

## Project Structure

```text
app/src/main/
├── data/          # API, Firebase, local-photo and preference data
│   └── repository # Cart, order, user, reward and vendor repositories
├── domain/        # Pricing, rewards and delivery-time calculations
├── location/      # Location permission and distance utilities
├── models/        # Application data models
├── nav/           # Compose navigation graph
├── network/       # Retrofit service and TheMealDB response models
├── ui/
│   ├── screens/   # Compose screens and reusable feature cards
│   └── theme/     # Galactic Greens colors, typography and theme
└── viewmodels/    # UI state and application workflows
```

## Local Setup

### Requirements

- Android Studio
- JDK 11 or newer; Android Studio's bundled JDK is supported
- Android SDK 35
- A Firebase project with an Android app
- A Google Maps Platform key with Maps SDK for Android enabled

### 1. Clone the repository

```bash
git clone YOUR_REPOSITORY_URL
cd YOUR_REPOSITORY_DIRECTORY
```

### 2. Add Firebase configuration

Download `google-services.json` from:

```text
Firebase Console → Project settings → Your apps → Android app
```

Place it at:

```text
app/google-services.json
```

The file is intentionally excluded from Git.

In Firebase Authentication, enable **Email/Password** sign-in.

### 3. Add the Google Maps key

Add the following to the project-level `local.properties` file:

```properties
MAPS_API_KEY=YOUR_RESTRICTED_MAPS_API_KEY
```

Enable Maps SDK for Android and protect the key with appropriate application and API restrictions in Google Cloud Console.

### 4. Publish Firebase rules

Publish the included rules through Firebase Console or the Firebase CLI:

```bash
firebase deploy --only firestore:rules,storage
```

Rule files:

- `firestore.rules`
- `storage.rules`

### 5. Build and test

On macOS or Linux:

```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
```

On Windows:

```powershell
gradlew.bat :app:assembleDebug
gradlew.bat testDebugUnitTest
```

## Build Configuration

- Minimum Android SDK: 24
- Target/compile Android SDK: 35
- Android Gradle Plugin: 8.5.2
- Gradle wrapper: 8.7
- Kotlin: 2.0.21

## Acknowledgements

- Meal data and food imagery: [TheMealDB](https://www.themealdb.com/)
- Backend services: [Firebase](https://firebase.google.com/)
- Maps: [Google Maps Platform](https://mapsplatform.google.com/)
