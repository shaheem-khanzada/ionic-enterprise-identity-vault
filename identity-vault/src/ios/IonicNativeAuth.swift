@objc(IonicNativeAuthPlugin)
class IonicNativeAuthPlugin: CDVPlugin {

    private var vaultStore: [IdentityVault]?

    public override func pluginInitialize() {
        vaultStore = []
    }

    private func ensureVault(_ command: CDVInvokedUrlCommand) -> IdentityVault? {
        do {
            let descriptor = try getDescriptorFromCommand(command)
            guard let vault = getVaultFromStore(descriptor.key) else {
                sendResult(command, VaultError.vaultUnavailable)
                return nil
            }
            return vault
        } catch {
            sendResult(command, error)
            return nil
        }
    }

    private func getVaultFromStore(_ key: String) -> IdentityVault? {
        for vault in vaultStore! {
            if vault.descriptor.key == key {
                return vault
            }
        }
        return nil
    }

    private func storeVault(_ vault: IdentityVault) {
        vaultStore!.append(vault)
    }

    private func getDescriptorFromCommand(_ command: CDVInvokedUrlCommand) throws -> VaultDescriptor {
        let descriptors = command.arguments[0] as? [String:Any] ?? [:]
        guard let username = descriptors["username"] as? String else { throw VaultError.invalidArguments("missing username")}
        guard let vaultId = descriptors["vaultId"] as? String else { throw VaultError.invalidArguments("missing vaultId")}
        return VaultDescriptor(username: username, vaultId: vaultId)
    }

    @objc func setup(_ command: CDVInvokedUrlCommand) {
        let setupOptions = command.arguments[0] as? [String:Any] ?? [:]
        guard let descriptor = try? getDescriptorFromCommand(command) else {
            sendResult(command, VaultError.invalidArguments("missing username or vaultId"))
            return
        }
        var authVault = getVaultFromStore(descriptor.key)
        if authVault == nil {
            authVault = IdentityVault(setupOptions, descriptor, eventHandler: sendEvents)
            storeVault(authVault!)
        }
        guard let vault = authVault else { return }

        vault.addEventHandler(command)

        vault.sendConfigEvent()
    }

    @objc func getConfig(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        // send the config event as well as the result
        sendResult(command, vault.getConfig())
    }

    @objc func isBiometricsAvailable(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isBiometricsAvailable())
    }

    @objc func isBiometricsSupported(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isBiometricsSupported())
    }

    @objc func isPasscodeSetupNeeded(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isPasscodeSetupNeeded())
    }

    @objc func getBiometricType(_ command: CDVInvokedUrlCommand) {
        // deprecated, getAvailableHardware should be preferred
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.getBiometricType().deprecatedType)
    }
    
    @objc func getAvailableHardware(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        let types: [String] = [vault.getBiometricType()].compactMap { (type) -> String? in
            return (type == .none) ? nil : type.rawValue
        }
        sendResult(command, types)
    }
    
    @objc func lock(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        vault.lock()
        sendResult(command)
    }

    @objc func isLocked(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isLocked())
    }

    @objc func isLockedOutOfBiometrics(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isLockedOutOfBiometrics())
    }

    @objc func isInUse(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isInUse())
    }

    @objc func remainingAttempts(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.remainingAttempts())
    }

    @objc func getValue(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }

        guard let key = command.arguments[1] as? String else {
            return sendResult(command, VaultError.invalidArguments("key missing"))
        }

        do {
            guard let value = try vault.getStoredValue(forKey: key) else {
                return sendResult(command)
            }
            return sendResult(command, [key:value])
        } catch {
            return sendResult(command, error)
        }
    }
    
    @objc func getKeys(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        do {
            guard let value = try vault.getStoredKeys() else {
                return sendResult(command)
            }
            return sendResult(command, value)
        } catch {
            return sendResult(command, error)
        }
    }

    @objc func storeValue(_ command: CDVInvokedUrlCommand) {
        guard command.arguments.count == 3 else {
            return sendResult(command, VaultError.invalidArguments("wrong number of args, missing key or data"))
        }

        guard let key = command.arguments[1] as? String else {
            return sendResult(command, VaultError.invalidArguments("missing key"))
        }

        let data = command.arguments[2]

        guard let vault = ensureVault(command) else { return }

        do {
            try vault.storeValue(key, data)
            sendResult(command)
        } catch {
            sendResult(command, error)
        }
    }

    @objc func removeValue(_ command: CDVInvokedUrlCommand) {
        guard command.arguments.count == 2 else {
            return sendResult(command, VaultError.invalidArguments("wrong number of args, missing key or data"))
        }

        guard let key = command.arguments[1] as? String else {
            return sendResult(command, VaultError.invalidArguments("missing key"))
        }

        guard let vault = ensureVault(command) else { return }

        do {
            try vault.removeValue(key)
            sendResult(command)
        } catch {
            sendResult(command, error)
        }
    }

    @objc func getUsername(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.getUsername())
    }

    @objc func getVaultId(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.getVaultId())
    }

    @objc func setBiometricsEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        guard let enabled = command.arguments[1] as? Bool else {
            return sendResult(command, VaultError.invalidArguments("enabled arg missing or not a boolean"))
        }

        do {
            try vault.setBiometricsEnabled(enabled)
        } catch {
            return sendResult(command, error)
        }
        sendResult(command)
    }

    @objc func isBiometricsEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isBiometricsEnabled())
    }

    @objc func setSecureStorageModeEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        guard let enabled = command.arguments[1] as? Bool else {
            return sendResult(command, VaultError.invalidArguments("enabled arg missing or not a boolean"))
        }

        do {
            try vault.setSecureStorageModeEnabled(enabled)
        } catch {
            return sendResult(command, error)
        }
        sendResult(command)
    }

    @objc func isSecureStorageModeEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isSecureStorageModeEnabled())
    }

    @objc func isPasscodeEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        sendResult(command, vault.isPasscodeEnabled())
    }

    @objc func clear(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        do {
            try vault.clear()
            sendResult(command)
        } catch {
            return sendResult(command, error)
        }
    }

    @objc func close(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        guard let handlerId = command.arguments[1] as? String else {
            return sendResult(command, VaultError.invalidArguments("handlerId missing"))
        }
        vault.removeEventHandler(handlerId)
        if vault.getHandlers().count == 0 {
        }
        sendResult(command)
    }

    @objc func setPasscodeEnabled(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        guard let enabled = command.arguments[1] as? Bool else {
            return sendResult(command, VaultError.invalidArguments("enabled arg missing or not a boolean"))
        }

        do {
            try vault.setPasscodeEnabled(enabled)
        } catch {
            return sendResult(command, error)
        }
        sendResult(command)
    }

    @objc func setPasscode(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        let passcode = command.arguments[1] as? String

        do {
            let wasNeeded = vault.isPasscodeSetupNeeded()
            try vault.setPasscode(passcode) { error in
                guard let err = error else {
                    if (wasNeeded) { vault.sendConfigEvent() }
                    return self.sendResult(command)
                }
                return self.sendResult(command, err)
            }
        } catch {
            return sendResult(command, error)
        }
    }

    @objc func unlock(_ command: CDVInvokedUrlCommand) {
        guard let vault = ensureVault(command) else { return }
        let withPasscode = command.arguments[1] as? Bool ?? false
        let passcode = command.arguments[2] as? String
        if (!vault.isLocked()) { return sendResult(command) }
        do {
            try vault.unlock(withPasscode, passcode) { error in
                guard let err = error else {
                    vault.sendEvent("unlock", vault.getConfig())
                    return self.sendResult(command)
                }
                return self.sendResult(command, err)
            }
        } catch {
            return sendResult(command, error)
        }
    }

    private func sendEvents(_ event: [String:Any] = [:], _ vault: IdentityVault) {
        for command in vault.getHandlers() {
            var eventToSend: [String:Any] = ["handlerId":command.callbackId]
            for (k, v) in event {
                eventToSend[k] = v
            }
            sendResult(command, eventToSend, keepCallback: true)
        }
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, _ error: Error) {
        if let descriptor = try? getDescriptorFromCommand(command) {
            if let vault = getVaultFromStore(descriptor.key) {
                switch (error) {
                case VaultError.tooManyFailedAttempts:
                    if vault.config.shouldClearVaultAfterTooManyFailedAttempts {
                        try? vault.clear()
                    }
                    break;
                default:
                    break;
                }
            }
        }
        
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: getVaultErrorObject(error).asDictionary)
        result?.setKeepCallbackAs(true)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, keepCallback: Bool = false) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        result?.setKeepCallbackAs(keepCallback)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, _ data: [String:Any], keepCallback: Bool = false) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: data)
        result?.setKeepCallbackAs(keepCallback)
        commandDelegate.send(result, callbackId: command.callbackId)
    }
    
    private func sendResult(_ command: CDVInvokedUrlCommand, _ data: [String], keepCallback: Bool = false) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: data)
        result?.setKeepCallbackAs(keepCallback)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, _ data: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: data)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, _ data: Int) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: data)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func sendResult(_ command: CDVInvokedUrlCommand, _ data: Bool) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: data)
        commandDelegate.send(result, callbackId: command.callbackId)
    }
}
