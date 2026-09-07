# NFC compatibility notes

TapForge uses Android NFC framework APIs. Actual behavior depends on the device NFC controller, Android version and the target tag/reader.

## Phone-side HCE

TapForge emulates NFC Forum Type 4 NDEF using Android Host Card Emulation. Readers that select the standard NDEF application can read compatible TapForge profiles. Android HCE availability is device-dependent.

A reader receiving NDEF is not guaranteed to present every NDEF record type to the user. URLs are widely surfaced by mobile operating systems; text and vCard records may be read successfully without a system-level UI. TapForge's Receive mode exists to provide a predictable receiver experience on compatible Android devices.

## Physical tags

Read/write support depends on the tag technology and whether Android exposes it as `Ndef` or `NdefFormatable`. Capacity, lock state and vendor behavior vary.

`Make read-only` can be irreversible. TapForge must not imply that every tag can be unlocked or reformatted afterward.

## NDEF cloning

Clone copies readable NDEF content. It does not clone secure-element state, cryptographic keys, protected sectors, payment cards, transit credentials or access-control credentials.

## ISO-DEP / APDU Lab

APDU Lab sends user-supplied commands to compatible `IsoDep` targets and displays raw responses. It intentionally ships without payment/access credential bypass presets.
