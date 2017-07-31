import { NativeModules } from 'react-native';
const { RNAccengageModule } = NativeModules;
import { _hasPermissions, _requestPermissions } from './platform-specific';

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
 */
function requestPermissions() {
  _requestPermissions(RNAccengageModule);
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

module.exports = {
  hasPermissions,
  requestPermissions,
  trackEvent,
  trackEventWithCustomData,
  trackLead,
}
