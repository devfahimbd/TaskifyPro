<div align="center">

# Taskify Pro
### Production-Ready Android To-Do Reminder App

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Firebase](https://img.shields.io/badge/Firebase-32.7.3-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-green)]()

</div>

---

## Overview

Taskify Pro is a feature-rich, production-ready Android to-do list and reminder application built from the ground up with modern Android development best practices. It uses **Kotlin** as the primary language, **Firebase** for backend services (Authentication + Cloud Firestore), and follows the **MVVM (Model-View-ViewModel)** architecture pattern with a clean Repository layer.

The application allows users to create, edit, delete, and manage tasks with scheduled reminders, real-time data synchronization across devices, and automatic categorisation into Pending and Completed sections.

## Features

### Authentication
- Email & Password Sign Up with form validation
- Secure Login with Firebase Authentication
- Logout with session clearing
- Forgot Password with email reset link
- Automatic login persistence across app restarts
- Proper error handling with user-friendly SnackBar messages
- Loading indicators during authentication operations

### Task Management
- Create tasks with title (required) and optional description
- Date picker for scheduling task reminders
- Time picker for precise reminder timing
- Edit existing tasks with pre-filled form
- Delete tasks with confirmation dialog
- Toggle task completion status (Pending / Completed)
- Separate visual sections for Pending and Completed tasks
- Real-time updates via Firestore snapshot listeners
- Overdue task indicators with color-coded status badges
- Time remaining display (e.g., "2 hr left", "Overdue")

### Notification System
- Schedule exact local notifications at selected date & time
- Notification includes task title in the content
- AlarmManager with setExactAndAllowWhileIdle() (bypasses Doze mode)
- Automatic alarm rescheduling after device reboot
- Android 13+ POST_NOTIFICATIONS permission handling
- Android 12+ SCHEDULE_EXACT_ALARM permission support
- Notification channel with high-importance priority
- Tap notification to open the app

### Database & Backend
- Firebase Cloud Firestore for persistent data storage
- Real-time data synchronization across devices
- Per-user data isolation (users only see their own tasks)
- Server-side timestamps for accurate creation dates
- Firestore security rules for production-grade access control

### UI/UX
- Material Design 3 theming
- RecyclerView with DiffUtil for efficient list updates
- Floating Action Button for quick task creation
- Empty state with illustration and call-to-action
- Loading progress indicators
- Strikethrough styling for completed tasks
- Smooth checkbox animations
- Pull-to-refresh feel with real-time data
- Responsive layout for all screen sizes

## Architecture

Taskify Pro follows clean MVVM architecture with clear separation of concerns:

```
UI Layer (Activities, Adapters, XML Layouts)
       |
       | observes LiveData / StateFlow
       v
ViewModel Layer (AuthViewModel, TaskViewModel)
       |
       | calls repository methods
       v
Repository Layer (AuthRepository, TaskRepository)
       |
       | wraps Firebase operations
       v
Data Source (FirebaseSource - Auth + Firestore)
```

### Design Decisions

| Decision | Rationale |
|----------|-----------|
| MVVM | Separates UI logic from business logic for testability |
| Repository Pattern | Abstracts data sources; easy to swap Firebase for another backend |
| Coroutines + Flow | Modern async programming with structured concurrency |
| ViewBinding | Type-safe view access; no runtime findViewById calls |
| Hilt DI | Compile-time dependency injection for testable, scalable code |
| DiffUtil | Efficient RecyclerView updates with minimal rebinds |
| callbackFlow | Bridges callback-based Firebase APIs to Kotlin Flow |

## Project Structure

```
TaskifyPro/
├── build.gradle.kts                    # Root Gradle config
├── settings.gradle.kts                 # Module declarations
├── gradle.properties                   # Project properties
├── LICENSE                             # MIT License
├── README.md                           # This file
└── app/
    ├── build.gradle.kts                # App dependencies (19 libraries)
    ├── proguard-rules.pro              # R8/ProGuard rules
    └── src/main/
        ├── AndroidManifest.xml         # Permissions, activities, receivers
        ├── java/com/taskify/pro/
        │   ├── TaskifyApplication.kt   # Hilt entry point
        │   ├── model/
        │   │   └── Task.kt             # Firestore-mapped data class
        │   ├── firebase/
        │   │   └── FirebaseSource.kt   # Firebase SDK wrapper
        │   ├── repository/
        │   │   ├── AuthRepository.kt   # Auth operations
        │   │   └── TaskRepository.kt   # Task CRUD + real-time listener
        │   ├── viewmodel/
        │   │   ├── AuthViewModel.kt    # Auth state (LiveData)
        │   │   └── TaskViewModel.kt    # Task state (StateFlow)
        │   ├── ui/
        │   │   ├── auth/
        │   │   │   ├── LoginActivity.kt
        │   │   │   └── SignUpActivity.kt
        │   │   └── task/
        │   │       ├── MainActivity.kt
        │   │       ├── AddEditTaskActivity.kt
        │   │       └── TaskAdapter.kt
        │   ├── receiver/
        │   │   ├── NotificationReceiver.kt
        │   │   └── BootReceiver.kt
        │   └── utils/
        │       ├── Resource.kt
        │       ├── ValidationUtils.kt
        │       └── NotificationHelper.kt
        └── res/
            ├── layout/ (5 layouts)
            ├── drawable/ (3 vector icons)
            ├── values/ (colors, strings, themes)
            └── menu/ (toolbar menus)
```

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 1.9.22 | Primary language |
| Android SDK | 26 (min) / 34 (target) | Platform compatibility |
| Firebase Auth | BOM 32.7.3 | Email/password authentication |
| Cloud Firestore | BOM 32.7.3 | Real-time cloud database |
| Material Design 3 | 1.11.0 | UI components & theming |
| AndroidX Lifecycle | 2.7.0 | ViewModel + LiveData + Runtime |
| Navigation Component | 2.7.7 | In-app navigation |
| Coroutines | 1.7.3 | Asynchronous programming |
| Hilt (Dagger) | 2.50 | Dependency injection |
| WorkManager | 2.9.0 | Background task scheduling |
| Glide | 4.16.0 | Image loading |

## Getting Started

### Prerequisites

- **Android Studio**: Flamingo (2023.2.1) or newer
- **Android SDK**: API Level 34
- **JDK**: 17
- **Firebase Account**: Free-tier project with Auth + Firestore enabled
- **Device**: Physical device (recommended) or emulator running Android 8.0+

### Step 1: Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Click **"Create a project"** and name it `Taskify Pro`
3. Click the **Android icon** and register your app with package name `com.taskify.pro`
4. Download `google-services.json` and place it in `app/` directory
5. Enable **Authentication > Email/Password** sign-in method
6. Create a **Firestore Database** (start in production mode)
7. Apply the following security rules in Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /tasks/{taskId} {
      allow read, write: if request.auth != null
        && request.auth.uid == resource.data.user_id;
      allow create: if request.auth != null
        && request.auth.uid == request.resource.data.user_id;
    }
  }
}
```

### Step 2: Clone & Build

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/TaskifyPro.git
cd TaskifyPro

# Open in Android Studio
# File > Open > select the TaskifyPro folder

# Sync Gradle
# File > Sync Project with Gradle Files

# Build the project
# Build > Rebuild Project

# Run on device/emulator
# Run > Run 'app'
```

### Step 3: Verify

1. Launch the app on your device or emulator
2. Tap "Sign Up" and create a new account
3. Add a task with a title, date, and time
4. Verify the task appears in the "Pending Tasks" section
5. Tap the checkbox to mark it as completed
6. Wait for the notification to fire at the scheduled time

## Firestore Schema

```javascript
// Collection: tasks
{
  user_id: string,        // Firebase Auth UID
  title: string,          // Task title (max 200 chars)
  description: string,    // Optional description (max 1000 chars)
  timestamp: Timestamp,   // Reminder date/time
  completed: boolean,     // Task completion status
  created_at: Timestamp   // Auto server timestamp
}
```

## Notification System

The app uses Android's `AlarmManager` to schedule exact alarms:

1. When a task is created/updated, `NotificationHelper.scheduleTaskReminder()` is called
2. The alarm triggers `NotificationReceiver` at the exact scheduled time
3. The receiver calls `NotificationHelper.showNotification()` to display the notification
4. On device reboot, `BootReceiver` queries Firestore and reschedules all pending alarms
5. Android 13+ requires `POST_NOTIFICATIONS` runtime permission

## Screens

| Screen | Description |
|--------|-------------|
| Login | Email/password sign-in with forgot password |
| Sign Up | Account registration with password confirmation |
| Task List | Pending & Completed sections with empty state |
| Add/Edit Task | Title, description, date picker, time picker |

## Code Quality

- Kotlin coroutines for all async operations
- ViewBinding for type-safe view access
- Proper try/catch error handling
- Sealed class `Resource<T>` for consistent state management
- DiffUtil for efficient RecyclerView updates
- Hilt dependency injection throughout
- ProGuard/R8 minification for release builds
- Comprehensive inline comments

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Built with Kotlin, Firebase, and Material Design 3**

Made with dedication to clean architecture and production-ready code.

</div>
