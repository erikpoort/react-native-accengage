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
 */
function _requestPermissions(Module) {
  Module.requestPermissions();
}

module.exports = {
  _hasPermissions,
  _requestPermissions,
}
