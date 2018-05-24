function _getDeviceID(Module, callback) {
  Module.getDeviceID(result => {
    callback(result);
  });
}

/**
 * Android doesn't see push as a dangerous permission, so permissions are always granted
 * @param callback Called with true
 */
function _hasPermissions(Module, callback) {
  callback(true);
}

/**
 * Request push permissions, android will ignore this.
 */
function _requestPermissions(Module, userAction) {
}

/**
 * Update push token, android will ignore this.
 */
function _updateTokens(Module) {
}

module.exports = {
  _getDeviceID, 
  _hasPermissions,
  _requestPermissions,
  _updateTokens,
}
