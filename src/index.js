import { NativeModules } from 'react-native';
const { RNAccengageModule } = NativeModules;
// import {} from './platform-specific';

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
  RNAccengageModule.trackEvent(key, customData);
}

module.exports = {
  trackEvent,
}
