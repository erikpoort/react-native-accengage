function _getDeviceID(Module, callback) {
  Module.getDeviceID(result => {
    callback(result);
  });
}

/**
 * Check if user has granted push permissions
 * @param callback Called with boolean
 */
function _hasPermissions(Module, callback) {
  Module.hasPermissions(result => {
    callback(result);
  });
}

/**
 * Request push permissions
 * This will trigger the permission dialog
 * If userAction is set to true and permission was denied before, this will open the settings app.
 * Set userAction to false if you request permissions on page load, otherwise the settings app
 * will open every time you load the page.
 * @param userAction Boolean user triggered request
 */
function _requestPermissions(Module, userAction) {
  Module.updatePermissions(true, userAction);
}

/**
 * Update push tokens
 * This should be called every time you open the app
 * It will never trigger a dialog or open the settings app
 */
function _updateTokens(Module) {
  Module.updatePermissions(false, false);
}

module.exports = {
  _getDeviceID,
  _hasPermissions,
  _requestPermissions,
  _updateTokens,
}
