# Privacy Policy

This privacy policy describes what data is collected and how it is processed by the app *Social Distance Meter*.

This information of the privacy policy is valid for version **0.1.2 (from 27.12.2020)**. 

This version of the privacy policy applies also for newer versions of the app, as long as there were no fundamental changes. Fundamental changes to the app may require a modification of this privacy policy.

## What data is collected by this app?
The main function of the app is scanning and capturing of transmitted Bluetooth Low Energy (BLE) tokens of the Exposure Notification Framework (ENF) by nearby devices.
Those tokens are stored together with a timestamp, the quality of reception (RSSI) and the source MAC address. Scanning will take place every minute but the period can be increased by the user.

The user can activate the recording of the position (opt in). The position will then be determined every 5 to 15 minutes and stored together with the captured tokens.

## What data is collected by external services?
The app synchronized diagnosis keys of the official national warn apps. Therefor the diagnosis keys of the official warn apps get downloaded via HTTPS.

When downloading the diagnosis keys, data can get recorded and processed by the provider. You can find more information in the privacy policy of the corresponding provider:
* Germany: https://www.coronawarn.app/assets/documents/cwa-privacy-notice-en.pdf

## Where is collected data stored?
All data captured by the app is stored in the protected app storage on the device.

## How long is collected data stored?
The captured data is normally stored for up to 15 days on the device.

Once a day the stored data get purged. All data older than 14 days get cleared.

## Will collected data get transferred?
The app doesn't transfer any data. All captured data are exclusively stored on the device.

## What permissions requires the app and why?
* *Fine location*: 
This permission is necessary to be able to capture Bluetooth Low Energy (BLE) tokens. Google requires this permission since some BLE tokens (not the ones of the Exposure Notification Framework (ENF)) may reveal information about the exact location of the device. Scanning BLE tokens without this permission is not possible.
Recording locations within the app only takes place when the corresponding setting is set to active.

* *Background location*:
Since the app should capture BLE tokens even if it is not active in foreground, the permission to access the location in background is required. Without this permission the app may only capture BLE tokens when it is running in foreground.

* *Foreground service*:
BLE tokens are captured at regular intervals. To ensure those regular intervals and prevent the service for capturing in background gets getting killed by the operating system, this permission is mandatory.

* *Bluetooth*:
Without the permission to access Bluetooth no BLE tokens may get captured.

* *Run at start*:
To start the service for capturing BLE tokens after a reboot of the device this permission is required.
Otherwise the app has to be started manually after a reboot.

* *Network*:
To be able to download diagnosis keys the app requires permission to access the internet.

## Why is the fine location permission required although I don't want to capture my position?
Google requires this permission since some BLE tokens (not the ones of the Exposure Notification Framework (ENF)) may reveal information about the exact location of the device. Scanning BLE tokens without this permission is not possible.
Recording locations within the app only takes place when the corresponding setting is set to active.

# Contact
If you have further questions concerning this privacy policy you can contact the author at https://github.com/BaaaZen/social-distance-meter or via baaazen@gmail.com.
