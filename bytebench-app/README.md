# Bytebench for Android

A native Android app (Kotlin + Jetpack Compose) of the Bytebench website: home page,
all 24 free tools, blog, services, case study, shop, about, contact and call booking —
in the website's three themes (Porcelain, Ink & Brass, Emerald).

## Get the APK

Every push that changes this folder builds a signed APK on GitHub Actions:

- **Latest download:** the [“Bytebench Android app” release](../../releases/tag/bytebench-app-latest)
  (open it on your phone, tap the `.apk`, allow “Install unknown apps” for your browser).
- Each run also keeps the APK as a workflow artifact under **Actions → Bytebench Android APK**.

Updates install over the previous version because every build is signed with the same key
(`keystore/debug.keystore`). That key is fine for sharing the APK yourself; for Google Play,
see below.

## What’s inside

| | |
|---|---|
| **Free tools** (offline, nothing uploaded) | Word & character counter, case converter, age, percentage, BMI, unit and time-zone converters, profit margin, loan/EMI with schedule + CSV, discount & tax, invoice PDF, image compressor / resizer / converter, PDF → JPG, password, QR code (PNG/SVG), random names, lorem ipsum, YouTube titles, Instagram captions, hashtags, meta descriptions |
| **Site content** | Home, services, blog with Markdown posts, case study, about/experience/skills, shop, legal pages, search |
| **Contact** | WhatsApp and email, contact form, “Book a free call” with your real hours (Mon–Fri 5–10 pm PKT) shown in the visitor’s time zone |
| **Extras** | Theme picker (or automatic light/dark), share and save files, launcher shortcuts for QR, compressor, invoice and word counter |

## Keeping content up to date

The app ships with the content from the Laravel project (`assets/content.json`).
When the website is live:

1. Add the endpoint in [`laravel-api/`](laravel-api/README.md) to the website.
2. In the app, **More → Settings & theme → Website sync**, enter the site address and tap **Save & sync now**.

To refresh the built-in copy instead (for example before a new release):

```bash
php scripts/export_content.php /path/to/bytebench app/src/main/assets/content.json
```

## Build it yourself

Needs JDK 17 and the Android SDK (API 37). From this folder:

```bash
./gradlew assembleRelease      # → app/build/outputs/apk/release/app-release.apk
```

## Publishing on Google Play

1. Create your own upload key: `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Build with it by setting `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS` and
   `RELEASE_KEY_PASSWORD` (for CI, store them as repository secrets), and build an App Bundle with `./gradlew bundleRelease`.
3. Keep `upload.jks` and its passwords safe — never commit them.
