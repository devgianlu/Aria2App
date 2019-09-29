# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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