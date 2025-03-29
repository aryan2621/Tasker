# Tasker - Modern Task Management App

A comprehensive productivity app built with Jetpack Compose and modern Android architecture to help users efficiently organize, track, and complete their daily tasks.

<p align="center">
  <img width="386" alt="Tasker Home Screen" src="https://github.com/user-attachments/assets/827c76a5-3c68-4efb-af3b-36513f2285e4">
  <img width="386" alt="Tasker Task Detail" src="https://github.com/user-attachments/assets/9e6d94d1-1b2a-4cfc-894f-2a9576023174">
</p>

## ✨ App Preview

<p align="center">
  <img width="250" alt="Login Screen" src="https://github.com/user-attachments/assets/e56ad2ac-4178-45c7-935f-cc964573fccc">
  <img width="250" alt="Home Screen" src="https://github.com/user-attachments/assets/dd099e6c-b174-4cc9-9cf9-7c498143525a">
  <img width="250" alt="Task Detail" src="https://github.com/user-attachments/assets/9d5934ed-9b89-4917-a9ed-c4556f474873">
</p>

<p align="center">
  <img width="250" alt="Task Creation" src="https://github.com/user-attachments/assets/5519db31-bedb-4008-b3ca-6df6b6f38ffd">
  <img width="250" alt="Statistics" src="https://github.com/user-attachments/assets/e581dca6-6a35-4ac9-ad5a-6fda7a412180">
  <img width="250" alt="Achievements" src="https://github.com/user-attachments/assets/64f9de29-9f61-48dc-a822-b52e4179d0c6">
</p>

## 📱 Key Features

### Task Management
- **Create Tasks** with title, description, category, priority, and duration
- **Set Reminders** with customizable recurrence (daily, weekly, monthly, or once)
- **Task Categories** including Work, Study, Health, Personal, and Custom
- **Priority Levels** (High, Medium, Low) for better organization

### Task Execution & Analytics
- **Run Tasks** directly from the app with built-in timer and pause/resume functionality
- **Progress Dashboard** with visual charts and completion rate tracking
- **Category & Priority Distribution** analysis visualizations
- **Daily Statistics** to track productivity patterns

### Motivation & Engagement
- **Streaks** to maintain daily consistency
- **Achievements** system to reward productivity milestones
- **Category Mastery** rewards for completing tasks in specific categories

### Data Management
- **Offline-First Architecture** for reliability without internet
- **Firebase Integration** for secure data backup and synchronization
- **Conflict Resolution** for handling data differences
- **Background Sync** when connectivity returns

## 🛠️ Technology Stack

<table>
  <tr>
    <td>
      <strong>Frontend</strong>
      <ul>
        <li>Jetpack Compose</li>
        <li>Material 3 Design</li>
        <li>Navigation Component</li>
        <li>ViewModel & StateFlow</li>
      </ul>
    </td>
    <td>
      <strong>Backend & Data</strong>
      <ul>
        <li>Room Database</li>
        <li>Firebase (Auth & Firestore)</li>
        <li>WorkManager</li>
        <li>Coroutines</li>
        <li>Koin (DI)</li>
      </ul>
    </td>
  </tr>
</table>

## 📚 App Architecture

Tasker follows Clean Architecture principles with distinct layers:

<p align="center">
  <table>
    <tr>
      <th>Layer</th>
      <th>Components</th>
    </tr>
    <tr>
      <td>Presentation</td>
      <td>UI Components, ViewModels</td>
    </tr>
    <tr>
      <td>Domain</td>
      <td>Use Cases, Domain Models</td>
    </tr>
    <tr>
      <td>Data</td>
      <td>Repositories, Data Sources, Models</td>
    </tr>
  </table>
</p>

## 📂 Project Structure

```
app/
├── src/main/java/com/tasker/
│   ├── data/
│   │   ├── db/            # Room database implementation
│   │   ├── di/            # Dependency injection modules
│   │   ├── domain/        # Use cases for business logic
│   │   ├── model/         # Data models
│   │   ├── repository/    # Repository implementations
│   │   └── sync/          # Data synchronization logic
│   ├── service/           # Background services and receivers
│   ├── ui/
│   │   ├── components/    # Reusable UI components
│   │   ├── navigation/    # Navigation structure
│   │   ├── screens/       # App screens
│   │   └── theme/         # App theming
│   └── util/              # Utility classes
├── TaskerApp.kt           # Application class
└── MainActivity.kt        # Main activity
```

## 🔐 Features In-Depth

### Sync Architecture
- **SyncManager** orchestrates sync process for all data types
- **WorkManagerProvider** schedules periodic and on-demand sync operations
- **Conflict Resolution** logic for handling data inconsistencies
- **Offline Support** with status tracking for each entity

### Authentication
- **Email & Password** registration and login
- **Google Sign-In** integration
- **Profile Management** for user details
- **Password Reset** functionality

### Notifications System
- **Task Reminders** for upcoming tasks
- **Task Running** notifications with timer control
- **Completion Notifications** for feedback
- **Channel Customization** for user preferences

## 🚀 Getting Started

### Prerequisites
- Android Studio Flamingo (2022.2.1) or higher
- Android SDK 33 (min SDK 24)
- Kotlin 1.8.0 or higher

### Setup
1. Clone the repository:
```bash
git clone https://github.com/yourusername/tasker.git
```

2. Open in Android Studio
3. Connect Firebase project:
   - Create a Firebase project
   - Add Android app to your Firebase project
   - Download `google-services.json` and place it in the app module
   - Enable Authentication and Cloud Firestore in Firebase Console

4. Build and run the app:
```bash
./gradlew assembleDebug
```

## 🔨 Building for Production

To create a production release:

```bash
./gradlew bundleRelease
```

This will generate an AAB file in `app/build/outputs/bundle/release/`

## 👨‍💻 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgements

- [Material Design](https://material.io/) for design guidelines
- [Firebase](https://firebase.google.com/) for backend services
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit
- [Room](https://developer.android.com/jetpack/androidx/releases/room) for local database
- [Koin](https://insert-koin.io/) for dependency injection
