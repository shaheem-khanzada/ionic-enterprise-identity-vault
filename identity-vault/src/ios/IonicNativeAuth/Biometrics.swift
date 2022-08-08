import Foundation
import LocalAuthentication

public class IonicBiometrics {

    private let context = LAContext()
    public var error: NSError?
    
    public enum HardwareType: String {
        case fingerprint = "fingerprint"
        case face = "face"
        case none = "none"
        
        var deprecatedType: String {
            switch self {
            case .fingerprint:
                return "touchID"
            case .face:
                return "faceID"
            case .none:
                return self.rawValue
            }
        }
    }
    
    public init() {}

    /**
     * Will the biometric prompt fail?
     */
    public var isAvailable: Bool {
        return context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    public var isLockedOut: Bool {
        return !isAvailable && error!.code == LAError.biometryLockout.rawValue
    }
    
    /**
     * The prompt could fail if they're not enrolled in biometrics or if passcode isn't set.
     */
    public var isSupported: Bool {
        if isAvailable {
            return true
        } else {
            switch error!.code{
            case LAError.biometryNotEnrolled.rawValue:
                return true
            case LAError.biometryLockout.rawValue:
                return true
            case LAError.passcodeNotSet.rawValue:
                print("can't know if biometric is supported unless passcode is set")
                return false
            default:
                return false
            }
        }
    }

    public var type: HardwareType {
        if #available(iOS 11.0, *) {
            // biometryType is not set until canEvaluatePolicy is called at least once, so this empty call is required
            let _ = isAvailable
            switch context.biometryType {
            case .faceID:
                return .face
            case .touchID:
                return .fingerprint
            case .none:
                return .none
            default:
                return .none
            }
        } else {
            return isAvailable ? .fingerprint : .none
        }
    }
}
