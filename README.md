# react-native-accengage
ReactNative module for Accengage 6.0.0+
Version 1.2.4

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

Implemented calls:
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
* Returns an array of a maximum of 20 Messages.
* @param object
*/
Accengage.getInboxMessages();

/**
* Get Message
* Returns a single message given an index.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
* @return Promise with either a message or null (in which case a webview is opened)
*/
Accengage.getMessage(index);

/**
* Interact with button in message content
* @param messageIndex
* @param buttonIndex
* @return Promise with an Accengage Inbox Message or error
*/
function interactWithButton(messageIndex, buttonIndex) {
  return RNAccengageModule.interactWithButton(buttonIndex, messageIndex);
}

/**
* Mark message as read
* Read a message. Returns the message with the new value.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
* @param bool
*/
Accengage.markMessageAsRead(index, bool);

/**
* Mark message as displayed
* @param index
* @param isDisplayed
* @return Promise with an Accengage Inbox Message or error
*/
function markMessageAsDisplayed(index, isDisplayed) {
return RNAccengageModule.markMessageAsDisplayed(index, isDisplayed);
}

/**
* Mark message as archived
* Archive a message. Returns the message with the new value.
* Before calling this method, getInboxMessages() should be invocated.
* @param index
* @param bool
*/
Accengage.markMessageAsArchived(index, bool);

/**
* Track display
* @param index
* @return Promise with an Accengage Inbox Message or error
*/
function trackDisplay(index) {
return RNAccengageModule.trackDisplay(index);
}

/**
* Track opening
* @param index
* @return Promise with an Accengage Inbox Message or error
*/
function trackOpening(index) {
return RNAccengageModule.trackOpening(index);
}

/**
* Clear the message cache
*/
function clearMessages() {
RNAccengageModule.clearMessages();
}
```

## Message Format
When a message was succefully retrieved, it will have the following structure:

```
{
  type: "message",
  index: Integer,
  subject: String,
  category: String,
  summary: String,
  timestamp: Timestamp/null
  sender: String,
  read: Boolean,
  archived: Boolean,
  customParameters: Object,
}
```

Due too the way the Accengage SDK is setup, you need to do seperate message detail calls to be
able to fill a list. As some of those can fail, the list can contain messages of the following
structure as well. In which case you can show a row with a retry button for example.
```
{
  type: "error",
  index: Integer
}
```

## Error Handling
When an inbox call fails, it will reject a promise. These are the codes you can handle:

`loading_inbox_failed`
When the very first call to Accengage fails

`loading_message_failed`
Only in case of retrieving a single message

`already_loading`
When you try to load a list, while a previous one was still loading

`general_error`
These are errors that shouldn't happen. Think of memory issues or async calls finishing after
cleanup.
Examples:
- Inbox was null
- Inbox doesn't exist anymore
- Messages disappeared
- Couldn't find the message to mark read/archived
