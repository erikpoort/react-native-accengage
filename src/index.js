import { NativeModules } from 'react-native';
const { RNAccengageModule } = NativeModules;
import { _hasPermissions, _requestPermissions, _updateTokens } from './platform-specific';

/**
 * Check if user has granted push permissions
 * @param callback Called with boolean
 */
function hasPermissions(callback) {
  _hasPermissions(RNAccengageModule, result => {
    callback(result);
  });
}

/**
 * Request push permissions, android will ignore this.
 * @param userAction Boolean Setting to true can open settings app
 */
function requestPermissions(userAction = false) {
  _requestPermissions(RNAccengageModule, userAction);
}

/**
 * Update push tokens
 * This should be called every time you open the app
 * It will never trigger a dialog or open the settings app
 */
function updateTokens() {
  _updateTokens(RNAccengageModule);
}

/**
 * Track a custom event to enable segmentation in Accengage.
 * The key used should be setup in Accengage dashboard before use.
 * @param key Custom key for Accengage tracking
 */
function trackEvent(key) {
  RNAccengageModule.trackEvent(key);
}

/**
 * Track a custom event to enable segmentation in Accengage.
 * The key used should be setup in Accengage dashboard before use.
 * @param key Custom key for Accengage tracking
 * @param customData An object with custom data to send along
 */
function trackEventWithCustomData(key, customData) {
  RNAccengageModule.trackEventWithCustomData(key, customData);
}

/**
 * Track a lead
 * @param label
 * @param value
 */
function trackLead(label, value) {
  RNAccengageModule.trackLead(label, value);
}

/**
 * Update device info
 * The object keys and values should be strings.
 * Date values should be strings formatted like: yyyy-MM-dd HH:mm:ss zzz
 * @param object
 */
function updateDeviceInfo(object) {
  RNAccengageModule.updateDeviceInfo(object);
}

/**
 * Get Inbox Messages
 * @param callback returns an inbox messages list
 */
function getInboxMessages() {
  return RNAccengageModule.getInboxMessages();
}

/**
* Get Inbox Messages
* @param index
* @return Promise with an Accengage Inbox Message or error
*/
function getMessage(index) {
  return RNAccengageModule.getMessageAtIndex(index);
}

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
* @param index
* @param isRead
* @return Promise with an Accengage Inbox Message or error
*/
function markMessageAsRead(index, isRead) {
  return RNAccengageModule.markMessageAsRead(index, isRead);
}

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
* @param index
* @param isArchived
* @return Promise with an Accengage Inbox Message or error
*/
function markMessageAsArchived(index, isArchived) {
  return RNAccengageModule.markMessageAsArchived(index, isArchived);
}

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

module.exports = {
  hasPermissions,
  requestPermissions,
  updateTokens,
  trackEvent,
  trackEventWithCustomData,
  trackLead,
  updateDeviceInfo,
  getInboxMessages,
  getMessage,
  interactWithButton,
  markMessageAsRead,
  markMessageAsDisplayed,
  markMessageAsArchived,
  trackDisplay,
  trackOpening,
  clearMessages,
}
