var exec = require('cordova/exec');

var PLUGIN_NAME = 'IonicNativeAuth';

var IonicNativeAuth = {
  getVault: function(options) {
    var instance = new AuthVault(options);
    var resolver;
    var rejecter;
    var p = new Promise(function(resolve, reject) { resolver = resolve; rejecter = reject});
    instance._setupPromise = p;
    exec(function(event) {
      instance.handle(event);
      options.onReady && options.onReady(instance);
      resolver();
    }, function(err) {
      options.onError && options.onError(err);
      rejecter();
    }, PLUGIN_NAME, 'setup', [options]);
    return instance;
  },
};


function unwrapData(key, resolve) {
  return function(data) {
    resolve(data && data[key]);
  }
}

function AuthVault(options) {
  return {
    onLock: options.onLock,
    onUnlock: options.onUnlock,
    onConfig: options.onConfig,
    descriptor: {
      username: options.username,
      vaultId: options.vaultId,
    },
    _setupPromise: null,
    config: null,
    handlerId: null,
    getConfig: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'getConfig', [descriptor]);
        });
      });
    },
    unsubscribe: function() {
      var descriptor = this.descriptor
      var handlerId = this.handlerId
      return this._setupPromise.then(function() {
        return new Promise(function (resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'close', [descriptor, handlerId]);
        });
      });
    },
    clear: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'clear', [descriptor]);
        });
      });
    },
    isLocked: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isLocked', [descriptor]);
        });
      });
    },
    isLockedOutOfBiometrics: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isLockedOutOfBiometrics', [descriptor]);
        });
      });
    },
    isInUse: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isInUse', [descriptor]);
        });
      });
    },
    remainingAttempts: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'remainingAttempts', [descriptor]);
        });
      });
    },
    getToken: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(unwrapData('token', resolve), reject, PLUGIN_NAME, 'getValue', [descriptor, 'token']);
        });
      });
    },
    storeToken: function(token) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'storeValue', [descriptor, "token", token]);
        });
      });
    },
    getValue: function(key) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(unwrapData(key, resolve), reject, PLUGIN_NAME, 'getValue', [descriptor, key]);
        });
      });
    },
    getKeys: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'getKeys', [descriptor]);
        });
      });
    },
    storeValue: function(key, value) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'storeValue', [descriptor, key, value]);
        });
      });
    },
    removeValue: function(key) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'removeValue', [descriptor, key]);
        });
      });
    },
    getUsername: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'getUsername', [descriptor]);
        });
      });
    },
    lock: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'lock', [descriptor]);
        });
      });
    },
    getBiometricType: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'getBiometricType', [descriptor]);
        });
      });
    },
    getAvailableHardware: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'getAvailableHardware', [descriptor]);
        });
      });
    },
    isBiometricsAvailable: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isBiometricsAvailable', [descriptor]);
        });
      });
    },
    isBiometricsSupported: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isBiometricsSupported', [descriptor]);
        });
      });
    },
    setBiometricsEnabled: function(isBiometricsEnabled) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'setBiometricsEnabled', [descriptor, isBiometricsEnabled]);
        });
      });
    },
    isBiometricsEnabled: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isBiometricsEnabled', [descriptor]);
        });
      });
    },
    isSecureStorageModeEnabled: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isSecureStorageModeEnabled', [descriptor]);
        });
      });
    },
    setSecureStorageModeEnabled: function(enable) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'setSecureStorageModeEnabled', [descriptor, enable]);
        });
      });
    },
    isPasscodeEnabled: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isPasscodeEnabled', [descriptor]);
        });
      });
    },
    setPasscodeEnabled: function(enable) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'setPasscodeEnabled', [descriptor, enable]);
        });
      });
    },
    setPasscode: function(passcode) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'setPasscode', [descriptor, passcode]);
        });
      });
    },
    isPasscodeSetupNeeded: function() {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'isPasscodeSetupNeeded', [descriptor]);
        });
      });
    },
    unlock: function(withPasscode, passcode) {
      var descriptor = this.descriptor
      return this._setupPromise.then(function() {
        return new Promise(function(resolve, reject) {
          exec(resolve, reject, PLUGIN_NAME, 'unlock', [descriptor, withPasscode, passcode]);
        });
      });
    },
    handle: function(event) {
      if (event && event.event) {
        switch (event.event) {
          case 'lock': {
            this.onLock && this.onLock(event.data);
            this.handlerId = event.handlerId
            break;
          }
          case 'config': {
            this.config = event.data;
            this.onConfig && this.onConfig(event.data);
            this.handlerId = event.handlerId
            break;
          }
          case 'unlock': {
            this.onUnlock && this.onUnlock(event.data);
            this.handlerId = event.handlerId
            break;
          }
        }
      }
    },
  };
}

module.exports = IonicNativeAuth;
