
import Foundation
import UIKit

public struct VaultConfig: Codable {

    // How long to wait before forcing the user to log in again (0 disables this)
    public var lockAfter: Int = 0

    // Whether to automatically obscure the app when backgrounded
    public var hideScreenOnBackground: Bool = false

    // After too many failed authentication attempts, should the vault be cleared?
    public var shouldClearVaultAfterTooManyFailedAttempts: Bool = true

    // If biometric auth fails, allow system pin fallback.
    public var allowSystemPinFallback: Bool = false

    public let descriptor: VaultDescriptor

    public init(_ config: [String:Any], _ descriptor: VaultDescriptor) {
        self.descriptor = descriptor
        lockAfter = config["lockAfter"] as? Int ?? lockAfter
        hideScreenOnBackground = config["hideScreenOnBackground"] as? Bool ?? hideScreenOnBackground
        shouldClearVaultAfterTooManyFailedAttempts = config["shouldClearVaultAfterTooManyFailedAttempts"] as? Bool ?? shouldClearVaultAfterTooManyFailedAttempts
        allowSystemPinFallback = config["allowSystemPinFallback"] as? Bool ?? allowSystemPinFallback
    }
}
