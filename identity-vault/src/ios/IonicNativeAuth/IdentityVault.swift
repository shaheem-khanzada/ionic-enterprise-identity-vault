//
//  IdentityVault.swift
//  auth-play2
//
//  Created by Nicholas Hyatt on 3/22/19.
//

import Foundation

public typealias LockedHandler = (Bool, Bool, Bool) -> Void
public typealias EventHandler = ([String:Any], IdentityVault) -> Void

/**
 * Reusable authentication vault
 */
public class IdentityVault {
    // MARK: Configuration
    public var config: VaultConfig

    private var backgroundStart: Date?
    private var screenProtectView: UIImageView?
    private var eventHandler: EventHandler
    private var keychainContainer: VaultKeychainContainer
    private let bio = IonicBiometrics()
    private var handlers: [String:CDVInvokedUrlCommand] = [:]
    public var descriptor: VaultDescriptor {
        return config.descriptor
    }

    public init(_ setupOptions: [String:Any], _ descriptor: VaultDescriptor, eventHandler: @escaping EventHandler) {
        config = VaultConfig(setupOptions, descriptor)
        keychainContainer = VaultKeychainContainer(config)
        self.eventHandler = eventHandler
        #if swift(>=4.2)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationDidFinishLaunching(_:)),
        name:UIApplication.didFinishLaunchingNotification,
        object: nil)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationWillTerminate(_:)),
        name:UIApplication.willTerminateNotification,
        object: nil)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationWillEnterForeground(_:)),
        name:UIApplication.willEnterForegroundNotification,
        object: nil)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationDidBecomeActive(_:)),
        name:UIApplication.didBecomeActiveNotification,
        object: nil)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationWillResignActive(_:)),
        name:UIApplication.willResignActiveNotification,
        object: nil)
        NotificationCenter.default.addObserver(self,
        selector:#selector(applicationDidEnterBackground(_:)),
        name:UIApplication.didEnterBackgroundNotification,
        object: nil)


        #else
        //For compatability with older cordova plugins, compile with Swift 4 or lower.

        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationDidFinishLaunching(_:)),
                                               name:NSNotification.Name.UIApplicationDidFinishLaunching,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationWillTerminate(_:)),
                                               name:NSNotification.Name.UIApplicationWillTerminate,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationWillEnterForeground(_:)),
                                               name:NSNotification.Name.UIApplicationWillEnterForeground,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationDidBecomeActive(_:)),
                                               name:.UIApplicationDidBecomeActive,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationWillResignActive(_:)),
                                               name:.UIApplicationWillResignActive,
                                               object: nil)
        NotificationCenter.default.addObserver(self,
                                               selector:#selector(applicationDidEnterBackground(_:)),
                                               name:.UIApplicationDidEnterBackground,
                                               object: nil)
        #endif
    }

    @objc func applicationDidBecomeActive(_ notification: NSNotification) {
        unobscureScreen()
    }

    @objc func applicationDidFinishLaunching(_ notification: NSNotification) {

    }

    @objc func applicationWillTerminate(_ notification: NSNotification) {

    }

    @objc func applicationWillEnterForeground(_ notification: NSNotification) {
        guard let bgStart = backgroundStart else {
            return
        }

        // If we have an idle timeout, check if we were backgrounded for too
        // long, then trigger a timeout and clear session
        if config.lockAfter > 0 {
            let elapsed = Date().timeIntervalSince(bgStart) * 1000

            if !elapsed.isLessThanOrEqualTo(Double(config.lockAfter)) {
                lock(true)
            }
            backgroundStart = nil
        }
    }

    @objc func applicationWillResignActive(_ notification: NSNotification) {
        if keychainContainer.isShowingBiometrics {
            return
        }

        backgroundStart = Date()
        if config.hideScreenOnBackground {
            obscureScreen()
        }
    }

    @objc func applicationDidEnterBackground(_ notification: NSNotification) {

    }

    private func obscureScreen() {
        if screenProtectView == nil {
            screenProtectView = UIImageView()
            if let launchImage = UIImage(named: "LaunchImage") {
                screenProtectView!.image = launchImage
                screenProtectView!.frame = UIScreen.main.bounds
                screenProtectView!.contentMode = .scaleAspectFill
                screenProtectView!.isUserInteractionEnabled = false
            } else if let launchImage = UIImage(named: "Splash") {
                screenProtectView!.image = launchImage
                screenProtectView!.frame = UIScreen.main.bounds
                screenProtectView!.contentMode = .scaleAspectFill
                screenProtectView!.isUserInteractionEnabled = false
            }
        }
        guard let vc = UIApplication.shared.windows.last?.rootViewController else {
            return
        }

        vc.view.addSubview(screenProtectView!)
    }

    private func unobscureScreen() {
        screenProtectView?.removeFromSuperview()
    }

    public func getHandlers() -> Dictionary<String, CDVInvokedUrlCommand>.Values {
        return handlers.values
    }

    public func addEventHandler(_ handler: CDVInvokedUrlCommand) {
        handlers[handler.callbackId] = handler
    }

    public func removeEventHandler(_ handlerId: String) {
        handlers.removeValue(forKey: handlerId)
    }

    public func isBiometricsAvailable() -> Bool {
        return bio.isAvailable
    }

    public func isBiometricsSupported() -> Bool {
        return bio.isSupported
    }

    public func getBiometricType() -> IonicBiometrics.HardwareType {
        return bio.type
    }

    public func getConfig() -> [String:Any] {
        var currentConfig = (try? JSONSerialization.jsonObject(with: JSONEncoder().encode(config))) as? [String:Any] ?? [:]
        currentConfig["isBiometricsEnabled"] = keychainContainer.biometricsEnabled
        currentConfig["isPasscodeEnabled"] = keychainContainer.passcodeEnabled
        currentConfig["isPasscodeSetupNeeded"] = keychainContainer.isPasscodeSetupNeeded()
        currentConfig["isSecureStorageModeEnabled"] = keychainContainer.secureStorageModeEnabled
        return currentConfig
    }

    public func isPasscodeSetupNeeded() -> Bool {
        return keychainContainer.isPasscodeSetupNeeded()
    }

    public func isLocked() -> Bool {
        return keychainContainer.locked
    }
    
    public func isLockedOutOfBiometrics() -> Bool {
        return bio.isLockedOut
    }

    public func lock(_ wasTimeout: Bool = false) {
        if isSecureStorageModeEnabled() { return }
        let wasLocked = keychainContainer.locked
        // If locked, clear the in-memory credential
        keychainContainer.lock()
        // If we are locking and previously it was unlocked, post the lock status
        if !wasLocked {
            sendEvent("lock", ["timeout": wasTimeout, "saved": keychainContainer.inUse])
        }
    }

    public func sendEvent(_ eventName: String, _ data: [String:Any]) {
        let event: [String:Any] = ["event": eventName, "data": data ]
        eventHandler(event, self)
    }

    public func sendConfigEvent() {
        sendEvent("config", getConfig())
    }

    public func isInUse() -> Bool {
        return keychainContainer.inUse
    }

    public func remainingAttempts() -> Int {
        return keychainContainer.remainingAttempts
    }

    public func getStoredValue(forKey: String) throws -> Any? {
        return try keychainContainer.item(forIdentifier: forKey)
    }

    public func getStoredKeys() throws -> [String]? {
        return try keychainContainer.getKeys()
    }

    public func storeValue(_ key: String, _ data: Any) throws {
        try keychainContainer.storeItem(data, forIdentifier: key)
    }

    public func removeValue(_ key: String) throws {
        try keychainContainer.removeItem(forIdentifier: key)
    }

    public func getUsername() -> String {
        return keychainContainer.descriptor.username
    }

    public func getVaultId() -> String {
        return keychainContainer.descriptor.vaultId
    }

    public func setBiometricsEnabled(_ enabled: Bool) throws {
        guard enabled != isBiometricsEnabled() else { return }
        if (!bio.isAvailable && enabled) { throw VaultError.securityNotAvailable }
        try keychainContainer.setBiometricsEnabled(enabled)
        sendConfigEvent()
    }

    public func isBiometricsEnabled() -> Bool {
        return keychainContainer.biometricsEnabled
    }

    public func setPasscodeEnabled(_ enabled: Bool) throws {
        guard enabled != isPasscodeEnabled() else { return }
        try keychainContainer.setPasscodeEnabled(enabled)
        sendConfigEvent();
    }

    public func setSecureStorageModeEnabled(_ enabled: Bool) throws {
        try keychainContainer.setSecureStorageModeEnabled(enabled)
        sendConfigEvent()
    }

    public func isSecureStorageModeEnabled() -> Bool {
        return keychainContainer.secureStorageModeEnabled
    }

    public func setPasscode(_ passcode: String? = nil, _ completion: @escaping (Error?) -> Void) throws {
        try keychainContainer.setPasscode(passcode, completion)
    }

    public func unlock(_ usePasscode: Bool = false, _ passcode: String? = nil, _ completion: @escaping (Error?) -> Void) throws {
        try keychainContainer.unlock(usePasscode, passcode, completion)
    }

    public func isPasscodeEnabled() -> Bool {
        return keychainContainer.passcodeEnabled
    }

    public func clear() throws {
        try keychainContainer.clear()
    }
}
