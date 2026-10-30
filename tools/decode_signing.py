#!/usr/bin/env python3
"""Decode user-owned stable signing bundle from GitHub Actions secret.
CYBERNET_SIGNING_BUNDLE = base64(JSON{keystore,storePassword,keyAlias,keyPassword}).
Never commit this bundle or the keystore to a public repository.
"""
import base64
import json
import os
from pathlib import Path

raw = os.environ.get("CYBERNET_SIGNING_BUNDLE", "")
if not raw:
    raise SystemExit("CYBERNET_SIGNING_BUNDLE GitHub Actions secret is missing")

obj = json.loads(base64.b64decode(raw, validate=True))
fields = ("keystore", "storePassword", "keyAlias", "keyPassword")
if any(not obj.get(k) for k in fields):
    raise SystemExit("Signing bundle is incomplete")
keystore = base64.b64decode(obj["keystore"], validate=True)
if not keystore or len(keystore) > 1024 * 1024:
    raise SystemExit("Invalid keystore size")

destination = Path(os.environ.get("RUNNER_TEMP", "/tmp")) / "cybernet-release.jks"
destination.write_bytes(keystore)
destination.chmod(0o600)
environment = Path(os.environ["GITHUB_ENV"])
with environment.open("a", encoding="utf-8") as out:
    for k, value in (
        ("CYBERNET_KEYSTORE_PATH", str(destination)),
        ("CYBERNET_STORE_PASSWORD", obj["storePassword"]),
        ("CYBERNET_KEY_ALIAS", obj["keyAlias"]),
        ("CYBERNET_KEY_PASSWORD", obj["keyPassword"]),
    ):
        if "\n" in value or "\r" in value:
            raise SystemExit("Newlines in signing fields are not allowed")
        out.write(f"{k}={value}\n")
print("Stable release signing configured (private key contents are not logged).")
