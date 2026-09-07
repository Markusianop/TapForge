# Security

TapForge works with NFC input that may be untrusted. Treat tags and APDU responses as external data.

When reporting a security issue, include:

- TapForge version;
- Android version and device model;
- NFC technology involved (`Ndef`, `IsoDep`, etc.);
- whether the issue requires a specially crafted physical tag;
- a minimal reproduction if it is safe to share.

Do not include payment-card data, access credentials, private keys or other sensitive secrets in public reports.

TapForge is not intended to bypass cryptographic protections or security controls on payment, transit or access-control systems.
