<img alt="Github Repo Header" src="https://github.com/user-attachments/assets/b1dca957-e336-443b-abd5-d3eb69c4a3fb" />
<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.zenlauncher.zenmode&hl=en_IN">
    <img src="https://img.shields.io/badge/Download%20on%20Play%20Store-00C700?style=for-the-badge&logo=google-play&logoColor=white"/>
  </a>
</p>

<p align="center">

  <!-- Stickers / Badges -->
  <img src="https://img.shields.io/badge/India%20FOSS%20United-2026-00C700?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Open%20Source-Love-00C700?style=for-the-badge&logo=opensourceinitiative&logoColor=white" />
  <img src="https://img.shields.io/badge/Built%20for-Focus-00C700?style=for-the-badge" />

</p>

## 🎬 Experience ZenMode (Click to watch demo)

<p align="center">
  <a href="https://youtu.be/48M1x2ryhpI">
    <img src="https://img.youtube.com/vi/48M1x2ryhpI/maxresdefault.jpg" width="100%" />
  </a>
</p>

## Our story

![Github](https://github.com/user-attachments/assets/de9a13cc-d8a0-472d-9e5c-1ca7e57d7b3f)

In 2025, **[Kamal](https://github.com/Kamal007OLica)**, and **[Srinivas](https://github.com/ThammanaSrinivas)** met at India FOSS United 2025 Con,

Today, we're building **ZenMode** a movement toward mindful technology that improves the lives of the next generation.



<p align="center">
  <b>Try Now!</b>
</p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.zenlauncher.zenmode&hl=en_IN">
    <img src="https://img.shields.io/badge/Download%20on%20Play%20Store-00C700?style=for-the-badge&logo=google-play&logoColor=white"/>
  </a>
</p>

---

## ✨ Philosophy
Less scrolling.
More living.

Less noise.
More clarity.


---

## 🎥 Motion Experiences

### 🌊 Mindful Scrolling  
https://github.com/user-attachments/assets/c1bc3c23-9277-41d3-979e-64ebbec470f1


### 🔓 Mindful Unlocks  
https://github.com/user-attachments/assets/fc09f3cf-1519-428a-ae9e-6a5520182527


### ⏳ Mindful Usage  
https://github.com/user-attachments/assets/45d793b8-8db1-4083-b815-80cc4fbca0be


---
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



## 💚 Support
<p align="center"> Need help, feedback, or just want to say hi? </p> <p align="center"> 📩 <b>zenmode.help@gmail.com</b> </p> <p align="center"> Built with calm, care & clarity <br/> <span style="color:#00C700;">● ZenMode</span> </p>

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.zenlauncher.zenmode&hl=en_IN">
    <img src="https://img.shields.io/badge/Download%20on%20Play%20Store-00C700?style=for-the-badge&logo=google-play&logoColor=white"/>
  </a>
</p>


## 📜 License

This project is licensed under the **GNU General Public License v3.0** (GPLv3). See the [LICENSE](LICENSE) file for more details.

![image](https://github.com/user-attachments/assets/00800890-b75c-4970-b0fa-c72e8e574384)
