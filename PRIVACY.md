# TapForge privacy

TapForge is designed to work locally.

- The app does not request the `INTERNET` permission.
- TapForge does not require an account.
- NDEF/HCE profiles are stored in Android app preferences on the device.
- The Profile Library is stored locally. Export happens only after the user chooses a destination through Android's document picker; import happens only from a file the user selects.
- Contact access is optional and is requested only when the user chooses to import a contact.
- NFC history is local and stores operation metadata such as time, action and scanned tag UID; it is not transmitted anywhere.
- The clone workflow stores the last captured NDEF locally.
- Undo Last Write stores one previous readable NDEF message, its tag UID and timestamp locally so that it can be restored to the same tag.
- APDU Lab stores only a summary entry in NFC history; TapForge does not transmit APDU sessions to a server.
- TapForge does not include analytics or advertising SDKs in this project.

Deleting TapForge's app data removes its profiles, Profile Library, NFC history, clone buffer and rollback snapshot. Files explicitly exported through Android's document picker remain wherever the user saved them.
