# react-native-accengage
ReactNative module for Accengage 6.0.0+
Version 1.0.1

## Installation

```bash
npm install --save react-native-accengage
```
```bash
react-native link react-native-accengage
```

### iOS
This module acts purely as a bridge for calling Accengage methods in ReactNative. Please follow the 
Accengage documentation on their website, on how to setup with appId and privateKey. This will 
mean making changes to your AppDelegate.

### Android
This module acts purely as a bridge for calling Accengage methods in ReactNative, but the gradle 
file will link to the A4S SDK as well. Please follow the Accengage documentation on their 
website, on how to setup with appId and privateKey. This will mean making changes to your 
Application.java.

## Usage
```js
// Import
import Accengage from 'react-native-accengage';
```

Right now hasPermissions, requestPermission, trackEvent, trackEventWithCustomData and trackLead are 
implemented. 
```js
/**
 * Check if user has granted push permissions
 * @param callback Called with boolean
 */
Accengage.hasPermissions(RNAccengageModule, result => {
  // result
});

/**
 * Request push permissions, android will ignore this.
 */
Accengage.requestPermissions();

/**
 * Track a custom event to enable segmentation in Accengage.
 * The key used should be setup in Accengage dashboard before use.
 * @param key Custom key for Accengage tracking
 */
Accengage.trackEvent(key);

/**
 * Track a custom event to enable segmentation in Accengage.
 * The key used should be setup in Accengage dashboard before use.
 * @param key Custom key for Accengage tracking
 * @param customData An object with custom data to send along
 */
Accengage.trackEventWithCustomData(key, customData);

/**
 * Track a lead
 * @param label
 * @param value
 */
Accengage.trackLead(label, value);
```
