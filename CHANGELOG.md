# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.7.0] - 05-04-2020
### Added
- Added ability to export/import session and conf file (#95)

### Fixed
- Fixed WebView URL parse issue

### Changed
- Using Android logging now!
- Do not check aria2 version for In-App Downloader (#96)

## [5.6.6] - 24-02-2020
### Fixed
- Fixed crash at startup due to invalid font


## [5.6.5] - 23-02-2020
### Added
- Added preference to skip WebView dialog

### Changed
- Do not show confirmation dialog when sharing link


## [5.6.4] - 11-02-2020
### Fixed
- Fixed crash reporting and analytics (Google Play only)


## [5.6.3] - 29-01-2020
### Fixed
- Fixed payments being refunded automatically
- Fixed minor synchronization issue in WebSocketClient

## Changed
- Updated Material library


## [5.6.1] - 22-12-2019
### Changed
- Migrated to Firebase (Google Play users only)
- Fixed some crashes
- Improved WebView stability by enabling more features


## [5.6.0] - 30-11-2019
### Changed
- Request legacy storage permissions (fix In-App Downloader permission denied)
- Updated Material theme
- Minor bug fixes


## [5.5.1] - 29-11-2019
### Changed
- Fixed In-App Downloader not working on some devices


## [5.5.0] - 28-11-2019
### Changed
- aria2 binaries are now bundled inside the APK for compatibility reasons
- Fixed crash in WebView


## [5.4.5] - 27-11-2019
### Added
- Additional logging for aria2 service issues


## [5.4.4] - 23-11-2019
### Changed
- Fixed minor issue in WebView
- Disable non-Google payments on Google Play


## [5.4.3] - 04-11-2019
### Added 
- Translators list
- Brazilian Portuguese translation

### Changed
- Fixed issue with saving profiles
- Fixed drawer highlighting issue


## [5.4.2] - 22-10-2019
### Changed
- Fixed random crash when viewing servers in download info
- Updated 3rd part libraries


## [5.4.1] - 08-10-2019
### Changed
- Removed restriction for In-App Downloader on non-ARM devices


## [5.4.0] - 07-10-2019
### Added
- In-App Downloader now supports x86 devices and has expanded support for ARM devices
- Changing global options when using the In-App Downloader profile will make them persist across restarts


## [5.3.2] - 29-09-2019
### Added
- Change In-App Downloader aria2 version

## Changed
- Fixed app crashing when downloading aria2 executable
- Stability issues due to aria2 executable not existing
- Fixed occasional BitfieldVisualizer crash
- Fixed crash when editing profile


## [5.3.1] - 26-09-2019
### Added
- Better support for sharing links

## Changed
- Fixed issues with creating/editing profiles
- Fixed In-App downloader configuration layout


## [5.3.0] - 17-09-2019
### Added
- Import/export profiles feature

### Changed
- List unknown IPs in top download/upload countries
- Fixed issue with DirectDownload address in setup
- Fixed issue with In-App Downloader navigation
- Fixed issue where the user wasn't able to pick a profile


## [5.2.4] - 28-08-2019
### Changed
- Fixed WiFi condition not working on Android 8.1+ due to missing permission
- Refactored profile creation
- Fixed notification service restarting as "Not started"
- Improved In-App Downloader configuration page

### Removed
- Do not log GeoIP exceptions


## [5.2.3] - 19-08-2019
### Changed
- Fixed startup crash due to aria2 service (In-App downloader)


## [5.2.2] - 19-08-2019
### Changed
- Fixed crash on first startup due to missing notification channel


## [5.2.1] - 18-08-2019
### Changed
- Fixed notification service crash
- Moved aria2 execution to service thread
- Fixed "no downloads" message when doing search


## [5.2.0] - 09-08-2019
### Added
- Show which filters are currently applied

### Changed
- Better layout of download cards
- Fixed options export