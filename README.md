# SMS Guard - Smishing Detection App

## Overview

SMS Guard is an Android application that receives and processes SMS messages to detect potential smishing (SMS phishing) attempts. The app uses a local language model (Gemma 3n) to classify messages as either "benign" or "smishing" and provides users with visual indicators and explanations.

## Features

### ✅ Implemented Features

1. **SMS Reception**: The app can receive incoming SMS messages through a BroadcastReceiver
2. **Message Display**: Clean, modern UI using Jetpack Compose and Material 3
3. **Classification Indicators**: 
   - 🟢 Green checkmark for benign messages
   - 🔴 Red warning for smishing messages
   - ⏳ Loading indicator for messages being processed
4. **Explanation Dialog**: Detailed explanation when users tap on smishing messages
5. **Real-time Updates**: Messages appear immediately when received
6. **MVVM Architecture**: Clean separation of concerns with ViewModels and Repository pattern
7. **Dependency Injection**: Hilt for managing dependencies

### 🔄 In Progress

- AI Classification with Gemma 3n (currently using placeholder logic)
- Permission handling for SMS access
- Persistent storage for SMS messages

## Architecture

The app follows the MVVM (Model-View-ViewModel) architecture pattern:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   UI Layer      │    │  ViewModel Layer │    │  Data Layer     │
│                 │    │                  │    │                 │
│ • SMSScreen     │◄──►│ • SMSViewModel   │◄──►│ • SMSRepository │
│ • SMSItem       │    │ • SMSUiState     │    │ • SMSMessage    │
│ • Components    │    │                  │    │ • RepositoryImpl│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌──────────────────┐
                       │  SMS Receiver    │
                       │                  │
                       │ • BroadcastReceiver
                       │ • SMS Parsing    │
                       └──────────────────┘
```

## Key Components

### Data Models
- `SMSMessage`: Represents an SMS with sender, message, timestamp, and classification
- `SMSClassification`: Enum for PENDING, BENIGN, and SMISHING states

### UI Components
- `SMSScreen`: Main screen displaying the list of SMS messages
- `SMSItem`: Individual SMS message card with classification indicators
- `ExplanationDialog`: Dialog showing smishing details and warnings

### Business Logic
- `SMSViewModel`: Manages UI state and coordinates between UI and data layer
- `SMSRepository`: Interface for SMS operations
- `SMSRepositoryImpl`: In-memory implementation using StateFlow

### Infrastructure
- `SMSReceiver`: BroadcastReceiver for intercepting incoming SMS
- `SMSGuardApplication`: Hilt application class
- `AppModule`: Dependency injection configuration

## Setup and Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 28+ (API level 28)
- Kotlin 2.0.21+

### Dependencies
The app uses the following key dependencies:
- **Jetpack Compose**: Modern UI toolkit
- **Material 3**: Design system
- **Hilt**: Dependency injection
- **ViewModel & LiveData**: Architecture components
- **Coroutines**: Asynchronous programming

### Permissions
The app requires the following permissions:
- `RECEIVE_SMS`: To receive incoming SMS messages
- `READ_SMS`: To read existing SMS messages (for testing)

## Usage

1. **Launch the App**: The app starts with the main SMS screen
2. **View Messages**: SMS messages are displayed in a scrollable list
3. **Classification Indicators**: 
   - Green checkmark = Safe message
   - Red warning = Potential smishing
   - Clock icon = Processing
4. **Tap Smishing Messages**: Tap on red warning indicators to see detailed explanations
5. **Real-time Updates**: New messages appear automatically when received

## Testing

The app includes test messages to demonstrate functionality:
- **Smishing Examples**: 
  - Package delivery scam with suspicious link
  - Bank account suspension scam
- **Benign Examples**:
  - Personal message from family
  - Legitimate order confirmation

## Future Enhancements

1. **AI Integration**: Implement Gemma 3n for actual smishing detection
2. **Database Storage**: Add Room database for persistent SMS storage
3. **Settings Screen**: Allow users to configure detection sensitivity
4. **Notification System**: Push notifications for detected smishing
5. **Message Filtering**: Filter messages by classification
6. **Export Functionality**: Export smishing reports

## Technical Notes

- The app uses `StateFlow` for reactive UI updates
- SMS processing is simulated (placeholder for AI classification)
- In-memory storage is used (will be replaced with Room database)
- Material 3 theming provides consistent design language
- Edge-to-edge display for modern Android experience

## Contributing

This is a development project for smishing detection. The current implementation provides the foundation for SMS reception and display, with the AI classification component ready for integration. 