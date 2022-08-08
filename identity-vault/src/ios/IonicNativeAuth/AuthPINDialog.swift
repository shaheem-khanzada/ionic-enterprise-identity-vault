//
//  AuthPINDialog.swift
//  PIN Dialog Sample
//
//  Created by Mano on 14/11/18.
//

import UIKit

class AuthPINDialog: NSObject {

    var rootWindow: UIWindow!

    // Singleton.
    class var sharedInstance: AuthPINDialog {
        struct Static {
            static let instance: AuthPINDialog = AuthPINDialog()
        }
        return Static.instance
    }

    public override init() {}

    // show alert.
    func showAlertView(
        title: String? = nil,
        message: String,
        actionTitles: [String],
        actions: [(UIAlertController) -> ()]?) {
        // create new window.
        let window = UIWindow(frame: UIScreen.main.bounds)
        window.backgroundColor = UIColor.clear
        window.rootViewController = UIViewController()
        AuthPINDialog.sharedInstance.rootWindow = UIApplication.shared.windows[0]
        #if swift(>=4.2)
        let style = UIAlertController.Style.alert
        #else
        //For compatability with older cordova plugins, compile with Swift 4 or lower.
        let style = UIAlertControllerStyle.alert
        #endif
        //create alertview.
        let alert = UIAlertController(title: title, message: message, preferredStyle : style)
        alert.addTextField(configurationHandler: { (textField) in
            textField.placeholder = ""
            textField.font = UIFont(name: "Courier", size: 48)
            textField.isSecureTextEntry = true
            textField.textAlignment = .center
            textField.keyboardType = UIKeyboardType.numberPad
        })
        for title in actionTitles {
            //add action.
            let action = UIAlertAction(title: title, style: .default, handler: {[weak alert] (action : UIAlertAction) -> Void in
                if let acts = actions {
                    if acts.count >= actionTitles.count {
                        acts[actionTitles.index(of: title)!](alert!)
                    }
                }
                DispatchQueue.main.async(execute: { () -> Void in
                    alert?.dismiss(animated: true, completion: nil)
                    window.isHidden = true
                    window.removeFromSuperview()
                    AuthPINDialog.sharedInstance.rootWindow.makeKeyAndVisible()
                })
            })
            alert.addAction(action)
        }

        #if swift(>=4.2)
        window.windowLevel = UIWindow.Level.alert
        #else
        //For compatability with older cordova plugins, compile with Swift 4 or lower.
        window.windowLevel = UIWindowLevelAlert
        #endif

        window.makeKeyAndVisible()
        window.rootViewController?.present(alert, animated: true, completion: nil)
    }

    public func displayPasscodePrompt(alertTitle: String, dispCancel: Bool, completion: @escaping (Bool, String)->()) {
        let alertMessage = NSLocalizedString("PePtc", tableName: nil, comment: "");
        let positiveButtonText = NSLocalizedString("OK", tableName: nil, comment: "");
        let negativeButtonText = NSLocalizedString("Cancel", tableName: nil, comment: "");

        let aTitles = (dispCancel) ? [negativeButtonText, positiveButtonText] :
            [positiveButtonText]
        showAlertView(title: alertTitle , message: alertMessage,
                      actionTitles: aTitles,
                      actions: (dispCancel) ? [
                        {(alert)->() in
                            print(negativeButtonText)
                            completion(false, "")
                        },
                        {(alert)->() in
                            let textField = alert.textFields![0]
                            completion(true, textField.text!)
                        }] : [
                            {(alert)->() in
                                let textField = alert.textFields![0]
                                completion(true, textField.text!)
                            }]
        )
    }
}
