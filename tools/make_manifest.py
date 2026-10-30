#!/usr/bin/env python3
"""Generate OTA manifest from actual signed universal APK metadata (not guessed codes)."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
apk = root / "output/CYBERNET.apk"
metadata = root / "upstream/V2rayNG/app/build/outputs/apk/playstore/release/output-metadata.json"
version = dict(line.strip().split("=", 1) for line in
    (root / "version.properties").read_text().splitlines()
    if line.strip() and not line.startswith("#"))
assert apk.is_file() and metadata.is_file(), "release APK or metadata missing"
meta = json.loads(metadata.read_text())
universal = [
    entry for entry in meta.get("elements", [])
    if "universal" in entry.get("outputFile", "").lower()
]
assert len(universal) == 1, f"expected one universal APK: {universal}"
code = int(universal[0]["versionCode"])
assert code >= int(version["VERSION_CODE"]), "unexpected manifest versionCode"
assert universal[0]["versionName"] == version["VERSION_NAME"], "version name mismatch"
tag = "v" + version["VERSION_NAME"]
sha = hashlib.sha256(apk.read_bytes()).hexdigest()
manifest = {
    "applicationId": "dev.xarkon.cybernet",
    "versionName": version["VERSION_NAME"],
    "versionCode": code,
    "apkUrl": "https://github.com/ANTIFERMENT/CYBERNET/releases/download/"
              + tag + "/CYBERNET.apk",
    "sha256": sha,
    "size": apk.stat().st_size,
    "notes": "Обновление CYBERNET: улучшения интерфейса, стабильности и безопасности.",
}
destination = root / "output/update.json"
destination.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
print("Validated OTA manifest:", tag, "versionCode", code, "SHA-256", sha)
