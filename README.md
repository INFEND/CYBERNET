# CYBERNET

**CYBERNET** is an independently branded Android VPN application based on the open-source [v2rayNG](https://github.com/2dust/v2rayNG) client, with a Russian-first AMOLED interface, built-in OTA update checks, and GitHub Actions builds.

- Android application ID: `dev.xarkon.cybernet`
- Current app version: `0.2.0` (base versionCode `2`, universal APK versionCode is derived by upstream).
- Reproducible upstream source revision: `aa4b05503a504a3f2e43818e6ee8ebf1caaa3df0`.
- Built-in OTA endpoint: https://github.com/ANTIFERMENT/CYBERNET/releases/latest/download/update.json
- Binary endpoint: https://github.com/ANTIFERMENT/CYBERNET/releases/latest/download/CYBERNET.apk

## Build and release

The workflow fetches the pinned upstream revision and submodules, builds native libraries, applies the CYBERNET overlay, and produces a **debug APK artifact**. When signing secrets exist, it also builds a release-signed universal APK and publishes `CYBERNET.apk`, `update.json`, checksums, and corresponding build sources in GitHub Releases.

**ONE signing secret is needed to publish OTA-installable releases:** `CYBERNET_SIGNING_BUNDLE`.
The developer has a private one-line base64 signing bundle generated separately. Copy it into **Settings → Secrets and variables → Actions → New repository secret** with this exact name; do not paste it in issues, commits, or chats.

After adding the secret, open [Actions](https://github.com/ANTIFERMENT/CYBERNET/actions/workflows/android.yml), select **Run workflow**. GitHub will sign the APK with the stable certificate and publish `CYBERNET.apk`, `update.json`, and corresponding GPL sources into Releases. Without the secret, the workflow builds only a debug APK artifact. Never commit private signing keys or passwords to this public repository.

For regular use open [Actions](https://github.com/ANTIFERMENT/CYBERNET/actions/workflows/android.yml), run the workflow, and install the artifact/release. Future releases use the same signing certificate.

## Source and licensing

This repository holds the **CYBERNET build overlay**, source patches, and workflow. It fetches the full corresponding v2rayNG source at a pinned revision. v2rayNG is GPL-3.0 licensed. Retain the GPL source licensing, credits, and corresponding source distribution when sharing derivative binary releases.

Not affiliated with the INCY app or its publisher. No proprietary INCY code or assets are included. User-supplied VPN profiles and servers are required.

## Status

Compilation and on-device connection tests are independently verified only once an Actions run passes; commits alone do not establish successful builds.
