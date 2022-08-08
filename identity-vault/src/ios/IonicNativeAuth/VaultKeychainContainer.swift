//
//  VaultKeychainStorageContainer.swift
//  SessionTools
//
//  Copyright © 2018 Bottle Rocket Studios. All rights reserved.
//

import Foundation



// Credentials
public struct Credential: Codable {
    var data: Data?
    var passcode: String?
}

public struct VaultDescriptor: Codable {
    let username: String
    let vaultId: String

    public var key: String {
        return username + vaultId
    }
}

public struct VaultKeychainContainerConfig: Codable {
    var biometricsEnabled: Bool = true
    var passcodeEnabled: Bool = false
    var secureStorageModeEnabled: Bool = false
    var inUse: Bool = false
    let descriptor: VaultDescriptor
    var failedAttempts: Int = 0
    var version: Int = 0
    var lastSalt: Data? = nil

    init(_ descriptor: VaultDescriptor) {
        self.descriptor = descriptor
    }
    
    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        guard let descriptor = try container.decodeIfPresent(VaultDescriptor.self, forKey: .descriptor) else {
            throw VaultError.authFailed
        }
        self.biometricsEnabled = (try? container.decodeIfPresent(Bool.self, forKey: .biometricsEnabled) ?? true) ?? true
        self.passcodeEnabled = (try? container.decodeIfPresent(Bool.self, forKey: .passcodeEnabled) ?? false) ?? false
        self.secureStorageModeEnabled = (try? container.decodeIfPresent(Bool.self, forKey: .secureStorageModeEnabled) ?? false) ?? false
        self.inUse = (try? container.decodeIfPresent(Bool.self, forKey: .inUse) ?? false) ?? false
        self.descriptor = descriptor
        self.failedAttempts = (try? container.decodeIfPresent(Int.self, forKey: .failedAttempts) ?? 0) ?? 0
        self.version = (try? container.decodeIfPresent(Int.self, forKey: .version) ?? 0) ?? 0
        self.lastSalt = (try? container.decodeIfPresent(Data.self, forKey: .lastSalt)) ?? nil
    }
}

/// MARK: A container for storing data to the keychain.
public class VaultKeychainContainer {
    
    public static let CURRENT_VERSION: Int = 1

    public static var STORED_CONFIGS_KEY: String {
      return "_ionicIVStoredConfigKey-"
    }
    
    private var vaultConfig: VaultConfig
    private var config: VaultKeychainContainerConfig
    private let passwordItem: KeychainPasswordItem
    private var _credential: Credential
    private var jsonDecoder: JSONDecoder = JSONDecoder()
    private var jsonEncoder: JSONEncoder = JSONEncoder()
    private var allowedAttempts: Int = 5
    private let pinDialog: AuthPINDialog = AuthPINDialog.sharedInstance
    public var isShowingBiometrics: Bool = false

    private var _passcode: String? {
        set {
            guard let passcode = newValue else {
                _credential.passcode = newValue
                return
            }
            _credential.passcode = encryptPasscode(passcode, true)
        }

        get {
            return _credential.passcode
        }
    }

    private func encryptPasscode(_ passcode: String, _ setNew: Bool = false)  -> String {
        var salt = config.lastSalt
        if setNew || salt == nil {
            salt = AES256Crypter.randomSalt()
        }
        let saltedKey = try! AES256Crypter.createKey(password: passcode.data(using: .utf8)!, salt: salt!)
        if setNew {
            config.lastSalt = salt
            storeConfig()
        }
        return saltedKey.base64EncodedString()
    }

    private func aesEnCrypto(key: Data, iv: Data, token: String) -> Data? {
        do {
            let sourceData = token.data(using: .utf8)!
            let aes = try AES256Crypter(key: key, iv: iv)
            let encryptedData = try aes.encrypt(sourceData)
            return encryptedData
        } catch {
            print("Encryption failed! - \(error.localizedDescription)")
        }

        return nil
    }

    public var locked: Bool {
        return config.inUse && !secureStorageModeEnabled && _credential.data == nil
    }

    private func ensurePasscodeInMemory() throws {
        guard _passcode != nil else {
            throw VaultError.missingPasscode
        }
    }

    public func isPasscodeSetupNeeded() -> Bool {
        return passcodeEnabled && !locked && _passcode == nil
    }

    private func doPinDialog(_ title: String = "SetPIN", _ completion: @escaping (String?) -> Void) {
        pinDialog.displayPasscodePrompt(alertTitle: NSLocalizedString(title, tableName: nil, comment: ""),
                                        dispCancel: true, completion: {(pin_ok, pin) in
                                            if (pin_ok) {
                                                completion(pin)
                                            } else {
                                                completion(nil)
                                            }
        })
    }

    private func setPasscodeFromDialog(completion: @escaping (Error?) -> Void) {
        doPinDialog { (pin1) in
            guard pin1 != nil else {
                return completion(VaultError.userCanceledAuth)
            }
            self.doPinDialog("VerifyPIN") { (pin2) in
                guard pin2 != nil else {
                    return completion(VaultError.userCanceledAuth)
                }
                guard pin2 == pin1 else {
                    return completion(VaultError.mismatchedPasscode)
                }
                do {
                    // set the new passcode in memory
                    self._passcode = pin1
                    try self.reSaveCredentials()
                    completion(nil)
                } catch {
                    return completion(error)
                }
            }
        }
    }

    public var inUse: Bool {
        return config.inUse
    }

    public var biometricsEnabled: Bool {
        return config.biometricsEnabled
    }

    public var passcodeEnabled: Bool {
        return config.passcodeEnabled
    }

    public var secureStorageModeEnabled: Bool {
        return !config.passcodeEnabled && !config.biometricsEnabled && config.secureStorageModeEnabled
    }

    public var descriptor: VaultDescriptor {
        return config.descriptor
    }

    private var configKey: String {
        return VaultKeychainContainer.STORED_CONFIGS_KEY + descriptor.vaultId
    }

    private var bioKey: String {
        return "bio" + descriptor.vaultId
    }

    private var passcodeKey: String {
        return "passcode" + descriptor.vaultId
    }

    private var secureStorageKey: String {
        return "ss" + descriptor.vaultId
    }

    private func getReadKey(_ forcePasscode: Bool) -> String {
        if biometricsEnabled && !forcePasscode {
            return bioKey
        }
        return passcodeKey
    }

    public var remainingAttempts: Int {
        return allowedAttempts - config.failedAttempts
    }

    private static func getConfigKey(_ forDescriptor: VaultDescriptor) -> String {
        return VaultKeychainContainer.STORED_CONFIGS_KEY + forDescriptor.vaultId
    }

    public init(_ vaultConfig: VaultConfig) {
        self.vaultConfig = vaultConfig

        // create main password item
        passwordItem = KeychainPasswordItem(service: vaultConfig.descriptor.username)

        // search for previously configured vault
        // we'll use it's configuration if there was one to know whether bio and/or pin was enabled for it
        if let storedConfig = try? passwordItem.readUnlockedData(VaultKeychainContainer.getConfigKey(vaultConfig.descriptor)) {
            config = (try? jsonDecoder.decode(VaultKeychainContainerConfig.self, from: storedConfig)) ?? VaultKeychainContainer.createNewConfig(vaultConfig.descriptor)
        } else {
            config = VaultKeychainContainer.createNewConfig(vaultConfig.descriptor)
        }
        _credential = Credential()
        storeConfig()
        clearIfFirstRun()
        migrateVault()
    }

    private func storeConfig() {
        let encodedConfig = try! jsonEncoder.encode(config)
        try! passwordItem.saveUnlockedData(encodedConfig, configKey)
    }

    private static func createNewConfig(_ fromDescriptor: VaultDescriptor) -> VaultKeychainContainerConfig {
        return VaultKeychainContainerConfig(fromDescriptor)
    }

    public func hasItem(forIdentifier identifier: String) throws -> Bool {
        if locked { throw VaultError.vaultLocked }
        return try item(forIdentifier: identifier) != nil
    }

    public func item(forIdentifier identifier: String) throws -> Any? {
        if locked { throw VaultError.vaultLocked }
        let credential = try getCredential()
        return credential?[identifier]
    }

    public func getKeys() throws -> [String]? {
        if locked { throw VaultError.vaultLocked }
        let credential = try getCredential()
        if let lazyMapCollection = credential?.keys {
            let stringArray = Array<String>(lazyMapCollection)
            return stringArray
        }
        return []
    }

    private func unencodeCredential() -> [String:Any]? {
        guard  let data = _credential.data else {
            return nil
        }
        return dataToDictionary(data)
    }

    private func getCredential(_ withPasscode: String? = nil) throws -> [String:Any]? {
        if !config.inUse {
            return nil
        }

        if _credential.data != nil {
            return unencodeCredential()
        }

        defer {
            self.isShowingBiometrics = false
            storeConfig()
        }

        do {
            let keychainData: Data
            if secureStorageModeEnabled {
                keychainData = try passwordItem.readUnlockedData(secureStorageKey)
            } else {
                let usingPasscode = withPasscode != nil
                // record the last time we prompted for biometrics
                if !usingPasscode {
                    self.isShowingBiometrics = true
                }
                keychainData = try passwordItem.readPassword(getReadKey(usingPasscode), withPasscode)
            }
            // successful auth reset failed attempts
            config.failedAttempts = 0
            _credential = try jsonDecoder.decode(Credential.self, from: keychainData)
            return unencodeCredential()
        } catch VaultError.keyNotFound {
            // either the user tried to unlock with passcode but it wasn't set
            // or the OS cleared the key due to bio/passcode changes
            // if they're authing with pin but it's not setup only clear if
            // biometrics isn't enabled as there is no hope for whatever might be saved
            if withPasscode != nil && biometricsEnabled {
                throw VaultError.missingPasscode
            }
            
            // if they're doing biometric auth but have
            // a passcode to fallback on we can let them try to use that.
            if withPasscode == nil && passcodeEnabled {
                throw VaultError.invalidatedCredentials
            }
            try? clear()
            throw VaultError.invalidatedCredentials
        } catch VaultError.authFailed {
            config.failedAttempts += 1
            if remainingAttempts == 0 {
                throw VaultError.tooManyFailedAttempts
            }
            throw VaultError.authFailed
        }
    }

    private func authByPinDialog(_ completion: ((Error?) -> Void)? ) {
        doPinDialog("VerifyPIN") { (pin) in
            guard pin != nil else {
                completion!(VaultError.userCanceledAuth)
                return
            }
            do {
                try self.getCredential(self.encryptPasscode(pin!))
                completion!(nil)
            } catch {
                return completion!(error)
            }
        }
    }

    public func unlock(_ usePasscode: Bool = false, _ withPasscode: String? = nil, _ completion: @escaping (Error?) -> Void) throws
    {
        if usePasscode && !self.passcodeEnabled { return completion(VaultError.passcodeNotEnabled) }
        if usePasscode {
            if withPasscode != nil {
                try self.getCredential(self.encryptPasscode(withPasscode!))
                return completion(nil)
            }
            return self.authByPinDialog(completion)
        }
        // use biometrics if it's enabled & no passcode provided
        // run async on in bacground so willResignActive handler fires correctly
        DispatchQueue.global().async {
            do {
                try self.getCredential()
                completion(nil)
            } catch {
                return completion(error)
            }
        }
    }

    public func removeItem(forIdentifier identifier: String) throws {
        guard var credential = try getCredentialsForModification() else {
            return
        }
        credential.removeValue(forKey: identifier)
        try updateCredential(credential)
    }

    public func clear() throws {
        _credential.data = nil
        _passcode = nil
        config.lastSalt = nil
        config.inUse = false
        config.failedAttempts = 0
        storeConfig()
        try passwordItem.deleteItem(bioKey)
        try passwordItem.deleteItem(passcodeKey)
        try? passwordItem.deleteItem(secureStorageKey)
    }

    public func clearConfig() {
        config = VaultKeychainContainer.createNewConfig(descriptor)
        storeConfig()
    }

    public func clearIfFirstRun() -> Void {
        if !UserDefaults.standard.bool(forKey: "HasRun-" + descriptor.key) && config.version != 0 {
            try? clear()
            clearConfig()
        }

        UserDefaults.standard.set(true, forKey: "HasRun-" + descriptor.key)
    }

    public func lock() {
        if secureStorageModeEnabled { return }
        if !passcodeEnabled && !biometricsEnabled { try? clear() }
        _credential.data = nil
        _passcode = nil
    }

    private func getCredentialsForModification() throws -> [String:Any]? {
        // verify a few things before allowing even in memory modifications
        if locked { throw VaultError.vaultLocked }
        let credential = try getCredential()
        if passcodeEnabled { try ensurePasscodeInMemory() }
        return credential
    }

    public func migrateVault() -> Void {
//        if config.version < 3 {
//            // TODO: migration code
//        }

        config.version = VaultKeychainContainer.CURRENT_VERSION
        storeConfig()
    }

    public func storeItem(_ item: Any, forIdentifier identifier: String) throws {
        var credential = try getCredentialsForModification() ?? [:]
        credential.updateValue(item, forKey: identifier)
        return try updateCredential(credential)
    }

    private func encodeCredential(_ credential: [String:Any]) throws -> Data {
        _credential.data = dictionaryToData(credential)
        return try jsonEncoder.encode(_credential)
    }

    private func updateCredential(_ credential: [String:Any]) throws {
        let encodedCredential = try encodeCredential(credential)
        try updateBioCredential(encodedCredential)
        try updatePasscodeCredential(encodedCredential)
        try updateSecureStorageCredential(encodedCredential)
        config.inUse = true
        storeConfig()
    }

    private func updateSecureStorageCredential(_ credential: Data) throws {
        if secureStorageModeEnabled {
            try passwordItem.saveUnlockedData(credential, secureStorageKey)
        } else {
            try passwordItem.deleteItem(secureStorageKey)
        }
    }

    private func updateBioCredential(_ credential: Data) throws {
        if biometricsEnabled {
            // make sure the passcode is embedded in the credential if passcode is enabled
            // so that we can use it on next bio auth to update the passcode key as well
            if passcodeEnabled { try ensurePasscodeInMemory() }
            try passwordItem.storeBiometricAuthPassword(credential, bioKey, vaultConfig.allowSystemPinFallback)
        } else {
            try passwordItem.deleteItem(bioKey)
        }
    }

    private func updatePasscodeCredential(_ credential: Data) throws {
        if passcodeEnabled {
            try ensurePasscodeInMemory()
            try passwordItem.storePasscodeAuthPassword(credential, passcodeKey, _passcode!)
        } else {
            try passwordItem.deleteItem(passcodeKey)
        }
    }

    public func setBiometricsEnabled(_ enabled: Bool) throws {
        guard enabled != biometricsEnabled else { return }
        guard !locked else { throw VaultError.vaultLocked }
        if enabled {
            try setSecureStorageModeEnabled(false)
        }
        config.biometricsEnabled = enabled
        storeConfig()
        try reSaveCredentials()
    }

    public func setPasscodeEnabled(_ enabled: Bool) throws {
        guard enabled != passcodeEnabled else { return }
        guard !locked else { throw VaultError.vaultLocked }
        if enabled {
            try setSecureStorageModeEnabled(false)
        }
        config.passcodeEnabled = enabled
        storeConfig()
        if !enabled {
            _passcode = nil
            config.lastSalt = nil
            try reSaveCredentials()
        }
    }

    public func setSecureStorageModeEnabled(_ enabled: Bool) throws {
        guard enabled != secureStorageModeEnabled else { return }
        guard !locked else { throw VaultError.vaultLocked }
        if enabled {
            try setPasscodeEnabled(false)
            try setBiometricsEnabled(false)
        }
        config.secureStorageModeEnabled = enabled
        storeConfig()
        try reSaveCredentials()
    }

    public func setPasscode(_ passcode: String? = nil, _ completion: @escaping (Error?) -> Void) throws {
        guard !locked else { throw VaultError.vaultLocked }
        if !passcodeEnabled { throw VaultError.passcodeNotEnabled }
        if passcode != nil {
            // set the passcode in memory & config to be saved
            _passcode = passcode
            try reSaveCredentials()
            return completion(nil)
        }
        setPasscodeFromDialog(completion: completion)
    }

    // re-save the current credentials in case of configuration update
    private func reSaveCredentials() throws {
        if locked { throw VaultError.vaultLocked }
        guard let credential = (try? getCredential()) ?? nil else { return } // nothing to re-save
        try updateCredential(credential)
    }


    private func dictionaryToData(_ dictionary: [String:Any]) -> Data {
        return NSKeyedArchiver.archivedData(withRootObject: dictionary)
    }

    private func dataToDictionary(_ data: Data) -> [String:Any] {
        return NSKeyedUnarchiver.unarchiveObject(with: data) as! [String:Any]
    }
}
