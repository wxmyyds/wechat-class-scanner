# WeChat Class Scanner

Standalone CLI tool to scan WeChat APK(s) for class names and method signatures. Designed for hook development — finds exact `com.tencent.mm.*` classes and their public methods.

## Features

- **Multi-DEX**: Scans `base.apk` and iterates **all** `classes.dex` entries in the container (WeChat splits code across many DEX files — `classes.dex`, `classes2.dex`, …)
- **Multi-APK input**: Pass `base.apk` + any `split_config.*.apk` as positional args; resource/config splits without a DEX are skipped gracefully
- **dexlib2 backend**: Pure bytecode parsing, no class loading, no runtime crashes
- **Keyword filtering**: Targets `msg`, `conversation`, `contact`, `storage`, etc.
- **Method signatures**: Outputs full descriptors (e.g., `insert(Landroid/content/ContentValues;)J`)
- **Zero dependencies at runtime**: Shadow JAR bundles dexlib2 + picocli (~3 MB)

> Note: classes only *referenced* (e.g. in method parameter types) but not *defined* in the scanned DEX files will not have their own method list. A full scan of `base.apk` covers the vast majority of `com.tencent.mm.*` definitions.

## Quick Start

```bash
# Download latest release
wget https://github.com/wxmyyds/wechat-class-scanner/releases/latest/download/scanner-all.jar

# Scan WeChat APKs (positional args) — outputs classes.txt
java -jar scanner-all.jar -o classes.txt base.apk split_config.arm64_v8a.apk

# Or just base.apk (it contains the multi-DEX container)
java -jar scanner-all.jar -o classes.txt base.apk
```

## CLI Options

| Flag | Description | Default |
|------|-------------|---------|
| `<apk>...` | Positional APK file paths (required) | — |
| `-o, --output <file>` | Output file (default: stdout) | stdout |
| `-k, --keywords <csv>` | Keywords to filter class names | `msg,conversation,conv,contact,storage,store,info,message` |
| `-p, --package-prefix <pkg>` | Package prefix filter | `com.tencent` |
| `-m, --include-methods` | Include method signatures in output (boolean flag) | `true` |

## Output Format

```
com.tencent.mm.modelmulti.ae
  insert(Landroid/content/ContentValues;)J
  query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
  delete(Landroid/net/Uri;Ljava/lang/String;[Ljava/lang/String;)I
com.tencent.mm.modelmulti.af
  ...
com.tencent.mm.storage.MsgInfoStorage
  addMsg(Lcom/tencent/mm/protocal/protobuf/Msg;)J
  getMsgBySvrId(J)Lcom/tencent/mm/protocal/protobuf/Msg;
...
```

- **Classes**: One per line, sorted
- **Methods**: Indented, full JVM descriptors (parameter types + return type)
- **Filtering**: Only classes whose name contains at least one keyword under the package prefix, and that have at least one method (when `-m` is on)

## GitHub Actions: Auto-scan on Release

Add `.github/workflows/scan.yml` to your WeChat APK repo:

```yaml
name: Scan WeChat APK
on:
  release:
    types: [published]
  workflow_dispatch:

jobs:
  scan:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Download scanner
        run: wget -q https://github.com/wxmyyds/wechat-class-scanner/releases/latest/download/scanner-all.jar -O scanner.jar || true
      - name: Prepare APKs
        id: apks
        run: |
          mkdir -p apks
          gh release download --dir apks --pattern "*.apk" --pattern "*.apks" || true
          for bundle in apks/*.apks; do [ -f "$bundle" ] && unzip -o -q "$bundle" -d apks/; done
          rm -f apks/*.apks
      - name: Run scanner
        run: java -jar scanner.jar -o classes.txt apks/*.apk
      - name: Upload classes.txt to release
        run: gh release upload "$(gh release list --limit 1 --json tagName -q '.[0].tagName')" classes.txt --clobber
```

**Workflow**: Push Release → Action downloads APKs from assets → Runs scanner → Uploads `classes.txt` back to same Release.

## Building from Source

```bash
git clone https://github.com/wxmyyds/wechat-class-scanner.git
cd wechat-class-scanner
./gradlew shadowJar --no-daemon
# Output: build/libs/scanner-all.jar
```

Requires: JDK 17+, Gradle (wrapper included)

## Why Not DexKit / ClassLoader?

| Approach | Problem |
|----------|---------|
| DexKit | Requires native `libdexkit.so` → `UnsatisfiedLinkError` in Zygisk/InMemoryDexClassLoader |
| ClassLoader reflection | `InMemoryDexClassLoader` loads from memory, no `DexFile` to enumerate |
| **This scanner** | **Parses DEX directly from APK files on disk — no loading, no natives** |

## License

MIT — Use freely for research / hook development.