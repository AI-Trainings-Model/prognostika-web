# Weather With You — Android alpha

An Android application and launcher widget, rebuilt without legacy code.

- Native host (Java/WebView), responsive offline-ready UI and modular editor
- Weather from Open-Meteo (no user account)
- Per-module visibility/order; drag to reorder; per-parameter refresh
- Household composition: male, female, child, pet
- APK update check via public signed release metadata
- Screenshot verification using a real Android emulator in GitHub Actions

The design reference is the October 21 module/editor renders. This is an alpha implementation; do not describe it as pixel-identical until image comparison passes.

See `.github/workflows/android.yml` for the APK and screenshot artifacts.
