/*
 Copyright (C) 2016 Apple Inc. All Rights Reserved.
 See LICENSE.txt for this sample’s licensing information

 Abstract:
 A struct for accessing generic password keychain items.
 */

import Foundation
import LocalAuthentication


public struct KeychainPasswordItem {
    // MARK: Types

    // MARK: Properties

    let service: String
    let accessGroup: String?

    // MARK: Intialization

    public init(service: String, accessGroup: String? = nil) {
        self.service = service
        self.accessGroup = accessGroup
    }

    public func storePasscodeAuthPassword(_ password: Data, _ forKey: String, _ withPasscode: String) throws {
        // clear existing key
        try deleteItem(forKey)

        // create custom context and set application passcode to avoid the system prompt
        let context = LAContext()
        context.setCredential(withPasscode.data(using: .utf8), type: .applicationPassword)

        // create SAC for applicationPassword Only & don't require System Passcode to be set
        // NOTE: This will prompt a Native UI for the user to create a passcode if it is not set in the context above
        let sacObject = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            .applicationPassword,
            nil
        );

        // Store the password with the SAC & custom context
        var newItem = KeychainPasswordItem.keychainQuery(withService: service, forKey: forKey, accessGroup: accessGroup)
        newItem[kSecValueData as String] = password as AnyObject?
        newItem[kSecAttrAccessControl as String] = sacObject
        newItem[kSecUseAuthenticationContext as String] = context

        // Add a the new item to the keychain.
        let status = SecItemAdd(newItem as CFDictionary, nil)
        // Throw an error if security is not available
        if status == errSecNotAvailable { throw VaultError.securityNotAvailable }
        if status == errSecAuthFailed { throw VaultError.securityNotAvailable }

        // Throw an error if an unexpected status was returned.
        guard status == noErr else { throw VaultError.unhandledError(status.description) }
    }

    public func storeBiometricAuthPassword(_ password: Data, _ forKey: String, _ allowSystemPin: Bool = false) throws {
        // clear existing key
        try deleteItem(forKey)

        // Create SAC for biometricAuth & optionally allow System Pin Fallback
        // Require system Passcode to be set
        var accessFlags: [SecAccessControlCreateFlags] = []
        if allowSystemPin {
            accessFlags.append(SecAccessControlCreateFlags.or)
            accessFlags.append(SecAccessControlCreateFlags.devicePasscode)
        }
        if #available(iOS 11.3, *) {
            accessFlags.append(SecAccessControlCreateFlags.biometryCurrentSet)
        } else {
            accessFlags.append(SecAccessControlCreateFlags.touchIDCurrentSet)
        }
        let sacObject = SecAccessControlCreateWithFlags(kCFAllocatorDefault,
                                                    kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                                                    SecAccessControlCreateFlags(accessFlags),
                                                    nil);

        // Store the password with the SAC
        var newItem = KeychainPasswordItem.keychainQuery(withService: service, forKey: forKey, accessGroup: accessGroup)
        newItem[kSecValueData as String] = password as AnyObject?
        newItem[kSecAttrAccessControl as String] = sacObject

        // Add a the new item to the keychain.
        let status = SecItemAdd(newItem as CFDictionary, nil)
        // Throw an error if security is not available
        if status == errSecNotAvailable { throw VaultError.securityNotAvailable }
        if status == errSecAuthFailed { throw VaultError.securityNotAvailable }
        // Throw an error if an unexpected status was returned.
        guard status == noErr else { throw VaultError.unhandledError(status.description) }
    }

    public func updatePassword(_ password: Data, _ forKey: String, _ passcode: String? = nil) throws {
        var query = KeychainPasswordItem.keychainQuery(withService: service, forKey: forKey, accessGroup: accessGroup)

        // use the passcode if there is one
        if passcode != nil {
            let context = LAContext()
            context.setCredential(passcode!.data(using: .utf8), type: .applicationPassword)
            query[kSecUseAuthenticationContext as String] = context
        }

        // update the value
        var attributesToUpdate = [String : AnyObject]()
        attributesToUpdate[kSecValueData as String] = password as AnyObject

        // update the item
        let status = SecItemUpdate(query as CFDictionary, attributesToUpdate as CFDictionary)

        // Throw an error if security is not available
        if status == errSecItemNotFound { throw VaultError.keyNotFound }
        // Throw an error if an unexpected status was returned.
        guard status == noErr else { throw VaultError.unhandledError(status.description) }
    }

    public func readPassword(_ forKey: String, _ withPasscode: String? = nil, _ withPrompt: String? = "Authenticate to log in") throws -> Data  {
        let context = LAContext()
        // if the key was stored using an applicationPassword then set it via the context
        // to avoid a native UI prompt for the passcode
        if withPasscode != nil {
            context.setCredential(withPasscode!.data(using: .utf8), type: .applicationPassword)
        }

        var query = KeychainPasswordItem.keychainQuery(withService: service, forKey: forKey, accessGroup: accessGroup)
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        query[kSecReturnAttributes as String] = kCFBooleanTrue
        query[kSecReturnData as String] = kCFBooleanTrue
        query[kSecUseOperationPrompt as String] = withPrompt as AnyObject
        query[kSecUseAuthenticationContext as String] = context

        // Try to fetch the existing keychain item that matches the query.
        var queryResult: AnyObject?
        let status = withUnsafeMutablePointer(to: &queryResult) {
            SecItemCopyMatching(query as CFDictionary, UnsafeMutablePointer($0))
        }

        // Check the return status and throw an error if appropriate.
        guard status != errSecItemNotFound else { throw VaultError.keyNotFound }
        guard status != errSecAuthFailed else {
            let biometrics = IonicBiometrics()
            
            if (!biometrics.isAvailable && biometrics.error?.code == LAError.biometryLockout.rawValue) {
                throw VaultError.tooManyFailedAttempts
            }
            
            throw VaultError.authFailed
        }
        guard status != errSecUserCanceled else { throw VaultError.userCanceledAuth }
        guard status == noErr else { throw VaultError.unhandledError(status.description) }

        // Parse the password string from the query result.
        guard let existingItem = queryResult as? [String : AnyObject],
            let passwordData = existingItem[kSecValueData as String] as? Data
            else {
                throw VaultError.invalidatedCredentials
        }

        return passwordData
    }

    public func saveUnlockedData(_ data: Data, _ forKey: String) throws {
        // Delete the existing item from the keychain.
        var query: [String: Any] =
            [kSecAttrAccount as String: forKey,
             kSecAttrService as String: self.service]

        query[kSecClass as String] = kSecClassGenericPassword
        var status = SecItemDelete(query as CFDictionary)

        // Throw an error if an unexpected status was returned.
        guard status == noErr || status == errSecItemNotFound else {
            throw VaultError.unhandledError(status.description)
        }

        query[kSecValueData as String] = data as AnyObject?
        query[kSecUseAuthenticationUI as String] = kSecUseAuthenticationUIAllow
        query[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        // Add a the new item to the keychain.
        status = SecItemAdd(query as CFDictionary, nil)

        // Throw an error if an unexpected status was returned.
        guard status == noErr else {
            throw VaultError.unhandledError(status.description)
        }
    }

    public func readUnlockedData(_ forKey: String) throws -> Data {
        var query: [String: Any] =
            [kSecAttrAccount as String: forKey,
             kSecAttrService as String: self.service]
        query[kSecClass as String] = kSecClassGenericPassword
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        query[kSecReturnAttributes as String] = kCFBooleanTrue
        query[kSecReturnData as String] = kCFBooleanTrue

        // Fetch the existing keychain item that matches the query.
        var queryResult: AnyObject?
        let status = withUnsafeMutablePointer(to: &queryResult) {
            SecItemCopyMatching(query as CFDictionary, UnsafeMutablePointer($0))
        }

        // Check the return status and throw an error if appropriate.
        guard status != errSecItemNotFound else { throw VaultError.invalidatedCredentials }
        guard status == noErr else { throw VaultError.unhandledError(status.description) }

        // Parse the password string from the query result.
        guard let existingItem = queryResult as? [String : AnyObject],
            let itemData = existingItem[kSecValueData as String] as? Data
            else {
                throw VaultError.invalidatedCredentials
        }
        return itemData
    }

    public func deleteItem(_ forKey: String) throws {
        // Delete the existing item from the keychain.
        let query = KeychainPasswordItem.keychainQuery(withService: service, forKey: forKey, accessGroup: accessGroup)
        let status = SecItemDelete(query as CFDictionary)

        // Throw an error if an unexpected status was returned.
        guard status == noErr || status == errSecItemNotFound else { throw VaultError.unhandledError(status.description) }
    }

    // MARK: Convenience
    private static func keychainQuery(withService service: String, forKey: String? = nil, accessGroup: String? = nil) -> [String : AnyObject] {
        var query = [String : AnyObject]()
        query[kSecClass as String] = kSecClassGenericPassword
        query[kSecAttrService as String] = service as AnyObject?

        if let key = forKey {
            query[kSecAttrAccount as String] = key as AnyObject?
        }

        if let accessGroup = accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup as AnyObject?
        }

        return query
    }
}
