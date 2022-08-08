# Ionic Identity Vault

Ionic Identity Vault is an all-in-one frontend identity management system that combines security best practices and the latest in biometric authentication options available on iOS and Android.

The Vault manages secure user identity and session tokens, ensuring sensitive tokens are encrypted at rest, stored only in secure locations on the device, and unlocked only with biometric identity (TouchID/FaceID).

Without Ionic Identity Vault, Ionic developers have to resort to combining third party Cordova plugins, often resulting in insecure setups due to the lack of correct implementation of biometric and at-rest encryption strategies. [Learn more.](https://ionicframework.com/identity-vault)

<native-ent-install plugin-id="identity-vault" variables=""></native-ent-install>

Update the native project config files:

```xml
// iOS - Info.plist
<key>NSFaceIDUsageDescription</key>
<string>Use Face ID to authenticate yourself and login</string>

// Android - No additional changes needed
```

## Reference App

A complete [login/logout experience](https://github.com/ionic-team/cs-demo-iv) that includes biometrics (Face ID with passcode as a fallback), secure token storage, background data hiding, and session timeouts.

## Configuring the Vault

The `IonicIdentityVaultUser` class takes a generic session type which represents the type of the session you'll store in the vault. You can use the [DefaultSession](#defaultsession) or extend the class to create a custom session. In the constructor of your `Identity` service, the vault is configured by providing options to the `super()` call:

```typescript
interface MyCustomSession extends DefaultSession {
  // username & token are inherited
  email: string;
  age: number;
  nicknames: string[];
}

export class IdentityService extends IonicIdentityVaultUser<MyCustomSession> {

constructor(private http: HttpClient, private router: Router, platform: Platform) {
  super(platform, {
    authMode: AuthMode.BiometricAndPasscode, // Use biometrics auth with passcode fallback
    restoreSessionOnReady: false, // whether or not to immediately attempt to restore the session when the vault is ready
    unlockOnReady: false, // set true to auto prompt the user to unlock when vault is ready
    unlockOnAccess: true, // set to true to auto prompt the user to unlock on first read access
    lockAfter: 5000, // lock after 5 seconds in the background
    hideScreenOnBackground: true // when in app launcher mode hide the current screen and display the splashscreen
  });

  onVaultUnlocked(config: VaultConfig) {
    //Route to my home page
  }

  onVaultLocked() {
    //Route to my login page
  }

  async onPasscodeRequest(isPasscodeSetRequest: boolean) {
    // Display a custom Passcode prompt and return the passcode as a string
    // or return undefined to use the build in native prompts. isPasscodeSetRequest
    // is true when attempting to set a new passcode on the vault, you can use
    // it to do something like prompt the user twice for the pin.
  }

}
```

## Automatically adding your token to requests

If you'd like to automatically add your authorization token from your identity service to every request, you can see a simple example at in our [demo repo](https://github.com/ionic-team/cs-demo-iv/blob/master/src/app/services/http-interceptors/auth-interceptor.ts).

## Upgrading to v4.0.0

If you have Identity Vault **<3.1.0**, please see [Upgrading from v3.0.0 to >=v3.1.0](https://ionicframework.com/docs/enterprise/identity-vault/3.6.X/identity-vault#upgrading-from-v3-0-0-to-v3-1-0) before following these upgrade instructions.

* Upgrade your app to use `cordova-android` 9.x (see the [9.0.0 milestone](https://github.com/apache/cordova-android/milestone/2) for progress) or Capacitor 2.x.
  * For Capacitor, please see the upgrade guide for [Android](https://capacitor.ionicframework.com/docs/android/updating) and [iOS](https://capacitor.ionicframework.com/docs/ios/updating).
* Install the new plugin version.

## API Documentation

You can find the API and interface documentation for everything below. The main classes to pay attention to are:

*   [IonicIdentityVaultUser](#identityvaultuser) - Subclass this when creating your identity service.
*   [DefaultSession](#defaultsession) - This is the generic type that represents your session. Extend this to implement a custom session.
*   [IdentityVault](#identityvault) - This is the lower level vault API. You can use this to implement advanced workflows including multi-tenant vaults.

