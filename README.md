# AssetHub Mobile — Asset Management App for Android

A native Android prototype for tracking company assets: who holds each item, where it is, and what has happened
to it. It is the mobile companion of the AssetHub web app.

## Features

- **Asset register** with search, photos, category, location, and purchase date.
- **Lifecycle status** for every asset (active, maintenance, borrowed, broken, retired), with a per-asset timeline of
  assignments and returns.
- **Role-based access** for three roles: admin, staff, and viewer. Admins change user roles and manage accounts.
- **Admin invitation codes** with an expiry date and a usage limit, used to grant admin rights or recover admin
  access.
- **Activity log** that records who did what, such as creating an asset or changing a role.
- **Dashboard** with total, active, borrowed, maintenance, and broken asset counts, and layouts that adapt to
  phones and tablets.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · Kotlin coroutines · MVVM with a single `AssetViewModel` ·
Coil for images. Minimum Android version: 7.0 (API 24).

| Path | Role |
| --- | --- |
| `app/src/main/java/com/example/data/` | Room entities, DAOs, repositories, and the seeded database |
| `app/src/main/java/com/example/ui/` | Compose screens, theme, and the view model |

## Install the demo APK

1. Download `.build-outputs/app-debug.apk` to an Android phone.
2. Allow installs from unknown sources when Android asks, then open the file.
3. Sign in with one of the demo accounts created on first launch (admin, staff, viewer); their details are in
   `app/src/main/java/com/example/data/AppDatabase.kt`.

To build it yourself, open the project in Android Studio and run the `app` configuration.

## Limitations

This is a prototype. All data lives in a local database on the device, there is no server sync, and demo passwords
are stored in plain text, so it is not meant for real company data.
