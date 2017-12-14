# react-native-accengage
ReactNative module for Accengage 6.0.0+
Version 1.1.4

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
 * @param userAction Boolean When this is true, the settings app will be opened if the user didn't 
 *                           grant permissions. 
 */
Accengage.requestPermissions(userAction);

/**
 * Update push tokens
 * This should be called every time you open the app
 * It will never trigger a dialog or open the settings app
 */
Accengage.updateTokens();

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
Accengage.trackLead(labelLabel, leadValue);

/**
* Update device info
* The object keys and values should be strings.
* Date values should be strings formatted like: yyyy-MM-dd HH:mm:ss zzz
* @param object
*/
Accengage.updateDeviceInfo(object);

/**
* Get Inbox Messages
* Returns an array of Messages.
* @param object
*/
Accengage.getInboxMessages();

/**
* Get Message
* Returns a single message given an index.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
*/
Accengage.getMessage(index);

/**
* Mark Message As Archived
* Archive a message. Returns the message with the new value.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
* @param bool
*/
Accengage.markMessageAsArchived(index, bool);

/**
* Mark Message As Read
* Read a message. Returns the message with the new value.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
* @param bool
*/
Accengage.markMessageAsRead(index, bool);
```
