# Ruuvi Station

[![License][license-image]][license-url]
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](https://github.com/ruuvi/com.ruuvi.station/pulls)
[![Android Test Build](https://github.com/ruuvi/com.ruuvi.station/actions/workflows/buildTest.yml/badge.svg?branch=master)](https://github.com/ruuvi/com.ruuvi.station/actions/workflows/buildTest.yml)

Ruuvi Station is an open-source Android app [available](https://play.google.com/store/apps/details?id=com.ruuvi.station) on the Google Play. You can observe the weather parameters, such as temperature, humidity, air pressure (and more) right on your Android device. Ruuvi Station is a companion app for open-source IoT BLE devices made by [Ruuvi](https://ruuvi.com).

<p align="center">
  <img src="/docs/v2_screenshot0.jpg?raw=true" alt="Ruuvi Station for Android" height="400"/>
  <img src="/docs/v2_screenshot1.jpg?raw=true" alt="Ruuvi Station for Android" height="400"/>
  <img src="/docs/v2_screenshot2.jpg?raw=true" alt="Ruuvi Station for Android" height="400"/>
  <img src="/docs/v2_screenshot3.jpg?raw=true" alt="Ruuvi Station for Android" height="400"/>
</p>

<p align="center">
  <a href='https://play.google.com/store/apps/details?id=com.ruuvi.station'><img alt='Get it on Google Play' height='60' src='docs/google-play-badge.png'/></a>
</p>

## RuuviTag

<p align="center">
  <a href="https://shop.ruuvi.com"><img src="/docs/ruuvitag-enclosure-open.jpg?raw=true" alt="RuuviTag" height="200"/></a>
  <a href="https://shop.ruuvi.com"><img src="/docs/ruuvitag1.jpg?raw=true" alt="RuuviTag" height="200"/></a>
  <a href="https://shop.ruuvi.com"><img src="/docs/ruuvitag2.jpg?raw=true" alt="RuuviTag" height="200"/></a>
</p>

[RuuviTag](https://ruuvi.com) is an advanced open-source sensor beacon platform designed to fulfill the needs of business customers, developers, makers, students, and can even be used in your home and as part of your personal endeavours. The device is set up to work as soon as you take it out of its box and is ready to be deployed to where you need it. Whether you need a beehive monitor in your backyard, or an industrial mesh network asset tracking system, [RuuviTag](https://ruuvi.com) gets you covered. 

## Features

- [x] Temperature (°C, °F, K)
- [x] Humidity (%, g/m3, °C)
- [x] Air Pressure (Pa, hPa, mmHg, inHg)
- [x] Acceleration (g)
- [x] 10 days sensor data storage
- [x] Charts
- [x] Cloud Features
- [x] Data Forwarding

## Get in touch

Join our [Slack](https://slack.ruuvi.com) community. 

Join our [Telegram](https://t.me/ruuvicom) community. 

## How to buy

You can order RuuviTag sensors [online](https://shop.ruuvi.com). Find more info about the devices on [Ruuvi.com](https://ruuvi.com). 

## Contribute

We would love you for the contribution to **Ruuvi Station**, check the ``LICENSE`` file for more info.

## Release automation

Maintainer release workflows are documented in
[`docs/android-release-automation.md`](docs/android-release-automation.md).

## How to build

1. Open Android Studio (4.0 or newer)
2. Select 'Get from Version Control
3. Enter repository URL: https://github.com/ruuvi/com.ruuvi.station
4. Clone repository
5. Select a branch you want to build
6. Sync project with Gradle file
7. Now you can run project on emulator or phone

### Push Notifications Setup (Firebase)

To receive Cloud Push Notifications during development, you must register your local debug SHA-1 fingerprint:

1. **Get your SHA-1 Fingerprint:**
   - Run `./gradlew signingReport` in the Android Studio terminal.
   - Copy the SHA1 string from the `debug` variant.

2. **Firebase Console:**
   - Go to [Firebase Console](https://console.firebase.google.com/) -> Project Settings.
   - Add your SHA-1 fingerprint to the Android app.
   - Download the updated `google-services.json` and replace the one in your `/app/` folder.

3. **Google Cloud Console:**
   - Go to [Google Cloud Credentials](https://console.cloud.google.com/apis/credentials).
   - Find the **Android key (auto created by Firebase)**.
   - Ensure the **Firebase Installations API** is added to the list of allowed APIs for this key.
   - Verify that the **Firebase Installations API** is enabled in the [API Library](https://console.cloud.google.com/apis/library).

4. **Finalize:**
   - Clean and Rebuild the project in Android Studio.

Watch video showing build process:

<a href="https://www.youtube.com/watch?v=1sXIASGXaaw"><img src="/docs/playvideo.png?raw=true" alt="Watch video" height="400"/></a>


<!-- Please don't remove this: Grab your social icons from https://github.com/carlsednaoui/gitsocial -->

[![Twitter][twitter-image]][twitter]
[![Facebook][facebook-image]][facebook]
[![Github][github-image]][github]

[github-image]:http://i.imgur.com/0o48UoR.png
[github]:https://github.com/ruuvi
[facebook-image]:http://i.imgur.com/P3YfQoD.png
[facebook]:https://www.facebook.com/ruuvi.cc/
[twitter-image]:http://i.imgur.com/tXSoThF.png
[twitter]:https://twitter.com/ruuvicom
[license-image]: https://img.shields.io/badge/License-BSD-blue.svg
[license-url]: LICENSE
