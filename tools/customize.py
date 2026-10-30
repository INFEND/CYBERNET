#!/usr/bin/env python3
"""Reproducible CYBERNET branding and feature overlay onto pinned v2rayNG."""
from __future__ import annotations
import argparse
import os
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

APP_ID = "dev.xarkon.cybernet"
APP_NAME = "CYBERNET"
TOP = Path(__file__).resolve().parent.parent

def replace_one(s, old, new, label):
    if s.count(old) != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {s.count(old)}")
    return s.replace(old, new, 1)

def modify(path, operation):
    source = path.read_text(encoding="utf-8")
    result = operation(source)
    if source == result:
        raise RuntimeError(f"patch had no effect: {path}")
    path.write_text(result, encoding="utf-8")

def read_version():
    values = dict(line.strip().split("=", 1) for line in
        (TOP / "version.properties").read_text().splitlines()
        if line.strip() and not line.strip().startswith("#"))
    if not re.fullmatch(r"\d+\.\d+\.\d+", values["VERSION_NAME"]):
        raise ValueError("VERSION_NAME must be semver")
    n = int(values["VERSION_CODE"])
    if n < 1 or n > 1000000:
        raise ValueError("VERSION_CODE must be in [1,1000000]")
    return values["VERSION_NAME"], n

def patch(root):
    version, code = read_version()
    gradle = root / "app/build.gradle.kts"
    def build(s):
        s = replace_one(s, 'applicationId = "com.v2ray.ang"',
                        f'applicationId = "{APP_ID}"', "applicationId")
        s = re.sub(r'(versionName\s*=\s*)"[^"]+"', lambda m: m.group(1) + f'"{version}"', s, count=1)
        s = re.sub(r'(versionCode\s*=\s*)\d+', lambda m: m.group(1) + str(code), s, count=1)
        s = replace_one(s, "compileSdk = 37",
                        'compileSdk = 37\n    ndkVersion = "30.0.16248370"', "NDK")
        if f'applicationId = "{APP_ID}"' not in s or f'versionName = "{version}"' not in s:
            raise RuntimeError("version and applicationId patch failed")
        return s
    modify(gradle, build)

    for lang in ("values", "values-ru"):
        p = root / f"app/src/main/res/{lang}/strings.xml"
        def strings(s):
            for ident in ("app_name", "app_widget_name", "app_tile_name"):
                pat = rf'(<string name="{ident}"[^>]*>).*?(</string>)'
                s, n = re.subn(pat, lambda m: m.group(1) + APP_NAME + m.group(2), s, count=1)
                if n != 1: raise RuntimeError("missing string " + ident)
            return s.replace("Служба v2rayNG", "Служба CYBERNET").replace("v2rayNG service", "CYBERNET service")
        modify(p, strings)
        ET.parse(p)

    theme = root / "app/src/main/java/com/v2ray/ang/ui/compose/Theme.kt"
    def theme_patch(s):
        start = s.index("private val DarkColor = darkColorScheme(")
        end = s.index("\n\n// Semantic Colors", start)
        s = s[:start] + DARK_PALETTE + s[end:]
        s = s.replace("decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, true)",
                      "decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)")
        s = s.replace('decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"',
                      'decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "2") ?: "2"')
        marker = "object ThemeManager {"
        s = replace_one(s, marker, ACCENT_STATE + "\n" + marker, "accent insertion")
        s = replace_one(s, "object ThemeManager {",
            """object ThemeManager {
    private val _accent = MutableStateFlow(
        MmkvManager.decodeSettingsString("cybernet_accent", "neon") ?: "neon"
    )
    val accent: StateFlow<String> = _accent.asStateFlow()

    fun setAccent(value: String) {
        if (!CybernetAccents.containsKey(value)) return
        MmkvManager.encodeSettings("cybernet_accent", value)
        _accent.value = value
    }
""", "ThemeManager accent")
        s = replace_one(s, "    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()",
            """    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val accentName by ThemeManager.accent.collectAsState()
    val accent = CybernetAccents[accentName] ?: Color(0xFF40F4A4)""", "theme color state")
        s = replace_one(s, "darkTheme -> DarkColor",
            "darkTheme -> DarkColor.copy(primary = accent, secondary = accent, tertiary = accent)", "theme apply accent")
        s = s.replace("val colorPing = Color(0xFF009966)", "val colorPing = Color(0xFF40F4A4)")
        s = s.replace("val colorFabActive = Color(0xFFf97910)", "val colorFabActive = Color(0xFF40F4A4)")
        return s
    modify(theme, theme_patch)

    settings = root / "app/src/main/java/com/v2ray/ang/ui/settings/SettingsActivity.kt"
    def settings_patch(s):
        s = replace_one(s,
            'var uiModeNight by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "0")',
            '''var uiModeNight by rememberMmkvString(AppConfig.PREF_UI_MODE_NIGHT, "2")
    var cyberAccent by remember { mutableStateOf(ThemeManager.accent.value) }''', "theme default")
        s = replace_one(s,
            "            if (uiSettingsExpanded) {",
            '''            if (uiSettingsExpanded) {
                SettingsListItem(
                    title = "Цветовой акцент CYBERNET",
                    entries = listOf("Неоновый зелёный", "Изумруд", "Лазурь", "Фиолетовый", "Янтарь", "Красный"),
                    values = listOf("neon", "emerald", "cyan", "violet", "amber", "red"),
                    selectedValue = cyberAccent,
                    onSelected = {
                        cyberAccent = it
                        ThemeManager.setAccent(it)
                    }
                )''', "settings picker")
        return s
    modify(settings, settings_patch)

    locale = root / "app/src/main/java/com/v2ray/ang/handler/AppLocaleManager.kt"
    modify(locale, lambda s: replace_one(s,
        "MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code",
        'MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: "ru"',
        "Russian first launch"))
    for path in ("values/themes.xml", "values-night/themes.xml"):
        p = root / "app/src/main/res" / path
        modify(p, lambda s: s.replace("#FF1C1B1F", "#FF000000"))

    ui = root / "app/src/main/java/com/v2ray/ang/ui/main/MainScreen.kt"
    def main_patch(s):
        s = replace_one(s, "import androidx.compose.material3.Scaffold\n",
                        "import androidx.compose.material3.Scaffold\nimport androidx.compose.material3.Text\n", "Text import")
        s = replace_one(s, """                ) {
                    if (groups.size > 1) {""",
                        """                ) {
                    CybernetHero(
                        connected = isRunning,
                        canConnect = !selectedGuid.isNullOrEmpty(),
                        onToggle = { onAction(MainAction.ToggleService) },
                        onCheck = { onAction(MainAction.TestCurrentServer) }
                    )
                    if (groups.size > 1) {""", "connected screen")
        s = replace_one(s, """            if (groups.isNotEmpty()) {
                Column(""",
                        """            if (groups.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    CybernetHero(
                        connected = false,
                        canConnect = false,
                        onToggle = { onAction(MainAction.ToggleService) },
                        onCheck = { onAction(MainAction.TestCurrentServer) }
                    )
                    Text(
                        "Серверов пока нет. Импортируйте ключ, QR-код или подписку через меню.",
                        modifier = Modifier.padding(22.dp)
                    )
                }
            }
            if (groups.isNotEmpty()) {
                Column(""", "empty screen")
        return s
    modify(ui, main_patch)
    overlay = TOP / "overlay"
    main_dir = root / "app/src/main/java/com/v2ray/ang"
    shutil.copyfile(overlay / "CybernetHero.kt", main_dir / "ui/main/CybernetHero.kt")
    shutil.copyfile(overlay / "CheckUpdateActivity.kt", main_dir / "ui/checkupdate/CheckUpdateActivity.kt")

    manifest = root / "app/src/main/AndroidManifest.xml"
    def manifest_patch(s):
        s = replace_one(s, 'android:icon="@mipmap/ic_launcher"',
            'android:icon="@drawable/cybernet_icon"', "app icon")
        s = replace_one(s, '<uses-permission android:name="android.permission.INTERNET" />',
            '''<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />''', "installer permission")
        return s.replace('android:scheme="v2rayng"', 'android:scheme="cybernet"')
    modify(manifest, manifest_patch)
    dst = root / "app/src/main/res/drawable/cybernet_icon.xml"
    shutil.copyfile(overlay / "cybernet_icon.xml", dst)
    ET.parse(dst)
    ET.parse(manifest)
    print(f"CYBERNET patch applied: {APP_ID}, {version}, versionCode {code}")

DARK_PALETTE = """private val DarkColor = darkColorScheme(
    primary = Color(0xFF40F4A4),
    onPrimary = Color(0xFF002516),
    primaryContainer = Color(0xFF10452E),
    onPrimaryContainer = Color(0xFFB8FFD9),
    secondary = Color(0xFF8FFFD0),
    onSecondary = Color(0xFF073525),
    secondaryContainer = Color(0xFF173A2C),
    onSecondaryContainer = Color(0xFFC2FFE3),
    tertiary = Color(0xFF6BC9FF),
    onTertiary = Color(0xFF00354F),
    tertiaryContainer = Color(0xFF154560),
    onTertiaryContainer = Color(0xFFC4EAFF),
    error = Color(0xFFFF697A),
    errorContainer = Color(0xFF7B142D),
    onError = Color(0xFF430511),
    onErrorContainer = Color(0xFFFFD8DC),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE4F7EC),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE4F7EC),
    surfaceVariant = Color(0xFF172B24),
    onSurfaceVariant = Color(0xFFAACAB8),
    outline = Color(0xFF436F58),
    outlineVariant = Color(0xFF1E3B2B),
    inverseSurface = Color(0xFFE1FAE9),
    inverseOnSurface = Color(0xFF13281D),
    inversePrimary = Color(0xFF087D4B),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF40F4A4),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF07130E),
    surfaceContainer = Color(0xFF0C1A13),
    surfaceContainerHigh = Color(0xFF11251A),
    surfaceContainerHighest = Color(0xFF183024),
)"""

ACCENT_STATE = """val CybernetAccents = mapOf(
    "neon" to Color(0xFF40F4A4),
    "emerald" to Color(0xFF16C784),
    "cyan" to Color(0xFF41D5F8),
    "violet" to Color(0xFFB58CFF),
    "amber" to Color(0xFFFFBF54),
    "red" to Color(0xFFFF6677)
)"""

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream_project", type=Path)
    args = parser.parse_args()
    patch(args.upstream_project.resolve())
