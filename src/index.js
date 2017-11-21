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
 function getInboxMessages(){

  return RNAccengageModule.getInboxMessages();
 }

 /**
 * Get Inbox Message List
 * @param page index
 * @param limit
 * @return Promise with an Accengage Inbox Message or error
 */
 function getInboxMessagesWithPageIndex(pageIndex,limit){
  return RNAccengageModule.getInboxMessagesWithPageIndex(pageIndex,limit);
 }

 /**
 * Get Inbox Messages
 * @param index
 * @return Promise with an Accengage Inbox Message or error
 */
 function getMessage(index){
  return RNAccengageModule.getMessageAtIndex(index);
 }

 /**
 * Mark Message as Read
 * @param isRead
 * @return Promise with an Accengage Inbox Message or error
 */
 function markMessageAsRead(isRead){
  return RNAccengageModule.markMessageAsRead(isRead);
 }

 /**
 * Mark Message as Archived
 * @param isArchived
 * @return Promise with an Accengage Inbox Message or error
 */
 function markMessageAsArchived(isArchived){
  return RNAccengageModule.markMessageAsArchived(isArchived);
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
  getInboxMessagesWithPageIndex,
  getMessage,
  markMessageAsRead,
  markMessageAsArchived
}