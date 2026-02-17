AireVentureAuthTabs (Login + Signup in ONE Activity with Segmented Tabs)

How to open:
1) Unzip
2) Android Studio -> Open -> select the AireVentureAuthTabs folder
3) Let Gradle sync
4) Run (AuthActivity is launcher)

Pixel-perfect notes:
- Layout and colors follow your Figma closely (segmented tabs + indicator animation).
- SVG icons were rasterized to PNG for maximum compatibility.

Fonts:
- This project currently uses Android system fonts (sans-serif) to avoid missing TTF build errors.
- If you want true pixel-perfect typography:
  - Download Inter + Plus Jakarta Sans as .ttf
  - Put them into app/src/main/res/font/
  - Update the XML layouts to reference @font/... again (I can do this for you once you upload the .ttf files).
