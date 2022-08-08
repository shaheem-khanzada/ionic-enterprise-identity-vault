//
//  VaultErrors.swift
//  auth-play2
//
//  Created by Nicholas Hyatt on 3/24/19.
//

import Foundation

public enum VaultError: Error {
    case vaultLocked
    case invalidArguments(_ description: String)
    case invalidatedCredentials
    case vaultUnavailable
    case securityNotAvailable
    case authFailed
    case userCanceledAuth
    case tooManyFailedAttempts
    case keyNotFound
    case mismatchedPasscode
    case missingPasscode
    case passcodeNotEnabled
    case unhandledError(_ description: String)
}

public struct VaultErrorObject {
    let code: Int
    let message: String
    init(_ code: Int, _ message: String) {
        self.code = code
        self.message = message
    }

    var asDictionary: [String:Any] {
        return ["code": code, "message": message]
    }
}

public func getVaultErrorObject(_ error: Error) -> VaultErrorObject {
    switch error {
    case VaultError.vaultLocked:
        return VaultErrorObject(1, "Operation not allowed while vault locked.")
    case VaultError.vaultUnavailable:
        return VaultErrorObject(2, "Vault Unavailable: Make sure you've configured the vault.")
    case VaultError.invalidArguments(let message):
        return VaultErrorObject(3, "Invalid Arguments Provided: " + message)
    case VaultError.invalidatedCredentials:
        return VaultErrorObject(4, "Credentials invalidated or expired. Vault cleared.")
    case VaultError.securityNotAvailable:
        return VaultErrorObject(5, "Biometric Security unavailable.")
    case VaultError.authFailed:
        return VaultErrorObject(6, "Failed authorization attempt")
    case VaultError.tooManyFailedAttempts:
        return VaultErrorObject(7, "Too many failed attempts. Vault cleared.")
    case VaultError.userCanceledAuth:
        return VaultErrorObject(8, "User canceled auth attempt.")
    case VaultError.mismatchedPasscode:
        return VaultErrorObject(9, "Passcodes did not match.")
    case VaultError.missingPasscode:
        return VaultErrorObject(10, "Passcode not setup yet. You must call setPasscode prior to storing values if passcode is enabled")
    case VaultError.passcodeNotEnabled:
        return VaultErrorObject(11, "Passcode not enabled.")
    case VaultError.keyNotFound:
        return VaultErrorObject(12, "Key Not Found")
    case VaultError.unhandledError(let message):
        return VaultErrorObject(0, "Unhandled Error: " + message)
    default:
        return VaultErrorObject(0, "Unhandled Error: " + error.localizedDescription)
    }
}

