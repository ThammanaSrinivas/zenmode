# ZenMode Android Client

Welcome to the open-source Android client for **ZenMode** - a mindful minimalist launcher designed to help you regain focus and reduce doomscrolling. 

This repository contains the frontend application and public API definitions. It acts as the core interface through which users interact with ZenMode's functionality.

## 🚀 Architecture Overview

ZenMode uses a modular composite build architecture to separate open-source definitions from proprietary backend implementations:

- **`app`**: The main Android application module containing all UI, views, ViewModels, and navigation logic.
- **`core-api`**: The public interface layer defining contracts for authentication, analytics, Firestore database interactions, and domain models. 
- **`core-mock`**: A mock implementation of `core-api` used for open-source contributors and local testing without requiring private backend keys.
- **`core-private`** *(Not in this repository)*: The closed-source implementation containing actual integrations with Firebase, PostHog, and other proprietary services.

## 🤝 How to Contribute

We welcome contributions from the community! To build and run ZenMode locally, you **do not** need access to the private backend keys. 

### Setting Up the Project

Our build system automatically detects if you are an open-source contributor and configures the app to use our mock environment.

UI Note: This project uses Cabinet Grotesque. To build the project with the intended styling, please download the free font files from Fontshare and place them in app/src/main/assets/fonts/.

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-org/ZenMode.git
   ```
2. **Open in Android Studio:**
   Open the `ZenMode` folder in Android Studio.
3. **Build the Project:**
   The `settings.gradle.kts` file will automatically detect that you do not have the `zenmode_core_private` repository. It will automatically route all backend calls to `core-mock`.
   
   *In `core-mock` mode:*
   - All logins will instantly succeed with a fake "Mock User".
   - Firestore reads/writes are mocked and will log to console without requiring network or Google Cloud accounts.
   - Analytics events are intercepted and logged locally instead of being sent to PostHog.

### Contributing Guidelines

1. Make sure your changes correspond only to the `app`, `core-api`, or `core-mock` modules.
2. If you add new backend requirements, you must define the interface in `core-api` and provide a mock implementation in `core-mock` so that other contributors' builds do not break.
3. Ensure all tests and lint checks pass before submitting a Pull Request.

## 📜 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3). See the [LICENSE](LICENSE) file for more details.
