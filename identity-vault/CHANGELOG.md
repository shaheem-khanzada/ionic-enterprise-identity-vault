## Change Log



### [4.2.1] (2020-05-27)


### Bug Fixes

* **android:** avoid crash on detecting gesture navigation when using hideScreen 



### [4.2.0] (2020-05-13)


### Bug Fixes

* **android:** Added transparent theme for biometric auth activity SE-188  
* **android:** make hideScreen work when using gesture navigation  


### Features

* added method getAvailableHardware to return list of biometrics options 



### [4.1.0] (2020-04-29)


### Bug Fixes

* **cordova:** remove full paths in config file targets  


### Features

* `allowSystemPinFallback`, `shouldClearVaultAfterTooManyFailedAttempts`, and `isLockedOutOfBiometrics 



### [4.0.1] (2020-04-17)


### Bug Fixes

* **android:** clear vault when there are too many failed bio unlock attempts  
* **ios:** clear vault when there are too many failed bio unlock attempts 
* allow install in cordova-android 9-dev 



### [4.0.0] (2020-04-08)


### Bug Fixes

* **ios:** swift 4.2 compilation issue  


### Features

* **android:** AndroidX upgrade, Android Face ID support  


### BREAKING CHANGES

* **android:** AndroidX is now required in projects with IV v4.


### [3.6.4] (2020-05-13)


### Bug Fixes

* **android:** avoid KeyPermanentlyInvalidatedException problem on SDK 19 [SE-183]
* **ios:** swift 4.2 compilation issue


### [3.6.3] (2020-04-01)


### Bug Fixes

* **ios:** remove old vault upon reinstall  



### [3.6.2] (2020-02-28)


### Bug Fixes

* **ios:** clear the vault on lock when using InMemoryOnly mode  



### [3.6.1] (2020-02-05)


### Bug Fixes

* **Android, iOS:** fix an issue where if auto unlock or restore session fails the vault fails to fire the onVaultReady event 



### [3.6.0] (2019-12-20)


### Features

* add getKeys to IdentityVault 
* add removeValue to IdentityVault 



### [3.5.1] (2019-12-18)


### Bug Fixes

* **android:** properly call onVaultLocked after lock  
* **ios:** add screenProtectView on top window  



### [3.5.0] (2019-11-27)


### Bug Fixes

* **Android:** Fix issue where vault would crash if Android device only supported FaceMatch 
* **vault-user:** use the vault user methods to set the auth mode  


### Features

* add isBiometricsSupported function 



### [3.4.8] (2019-11-08)


### Bug Fixes

* **vault-user:** use the vault user methods to set the auth mode  



### [3.4.7] (2019-09-09)


### Bug Fixes

* **Android:** Fix an issue where the vault would not be cleared when fingerprints were added or all fingerprints were removed on Android.. 



### [3.4.6] (2019-08-07)


### Bug Fixes

* **Android:** fix an issue where adding a fingerprint to device after the app was open would not refresh whether biometrics was available or not 



### [3.4.5] (2019-07-27)


### Bug Fixes

* **Android, iOS:** getSession return type and default IonicIdentityVaultUser generic to DefaultSession 



### [3.4.4] (2019-07-25)


### Bug Fixes

* **Android:** Fixes an issue on Android where getBiometricType would return none if Biometrics was not enabled even though the device had biometric hardware. 



### [3.4.3] (2019-06-14)


### Bug Fixes

* **Android:** Fixed issue where when hideScreenInBackground feature was enabled screenshots would be disabled. 



### [3.4.2] (2019-06-14)


### Bug Fixes

* **iOS:** Fixed an issue where the hide screen in background functionality was broken 



### [3.4.1] (2019-06-06)


### Bug Fixes

* **Android:** fix issue where setBiometricsEnabled(false) would throw an error if biometrics was unavailable  



### [3.4.0] (2019-06-06)


### Bug Fixes

* **iOS:** fix an issue where if a user removed fingerprints after authentication storing the session would return an error rather than default to passcode only mode 
* **iOS:** Fix issue where `getBiometricType` would return `none` if TouchID or FaceID was present on device but the user was not enrolled.  
* **iOS:** fix issue with getBiometricType and issue where lock event was triggered when lock was called in secure storage mode 


### Features

* Added android side of Secure Storage Mode 
* update Typescript/JS layer to support Secure Storage mode 



### [3.3.0] (2019-05-10)


### Bug Fixes

* **Android, iOS:** make the setting of the auth mode fault tolerant  


### Features

* **Android. iOS:** add Biometric or Passcode mode  



### [3.2.3] (2019-04-29)


### Bug Fixes

* **Android:** fix bug in Android where FingerprintManager import was missing 



### [3.2.2] (2019-04-29)


### Bug Fixes

* fix release configuration issue where xlmns:android was incorrectly add to manifest 



### [3.2.1] (2019-04-27)


### Bug Fixes

* fix bug where plugin id was incorrect and didn't include scope 



### [3.2.0] (2019-04-26)

### Features

- Added [getPlugin](#identityvaultuser.getplugin) method which can be overridden in advanced use cases to provide custom implementations for PWA compatibility etc.

### Bug Fixes

* **iOS:** Fixed a bug on iOS where when using the [hideScreenOnBackground](#vaultoptions.hidescreenonbackground) flag the splashscreen may temporarily flash during biometric prompts.
* **Android:** Fixed a bug on Android where [isBiometricsAvailable](#identityvaultuser.isbiometricsavailable) would return true is some cases if No fingerprints were enrolled or fingerprint hardware wasn't available.
* **Android, iOS:** Fixed a bug where [getSession](#identityvaultuser.getsession) may incorrectly return `undefined` due to failing to wait for the plugin to be ready before returning.

### [3.1.0] (2019-04-19)

### Features

* Added [login](#identityvaultuser.login) method which clears the vault and stores the session passed to it.

### [3.0.0] (2019-04-08)

### Features

* Added the ability to use [onPasscodeRequest](#identityvaultuser.onpasscoderequest) to use a custom pin prompt screen.
* Made [IdentityVaultUser](#identityvaultuser) a generic class to allow using the [DefaultSession](#defaultsession) or extending it to type and store the session object.
* Added support for advanced usages such as multi-tenant vaults by using the [IonicNativeAuthPlugin](#ionicnativeauthplugin.getvault) and [IdentityVault](#identityvault) APIs directly.
