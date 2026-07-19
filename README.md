# WeChat Class Scanner

Standalone CLI tool to scan WeChat APK(s) for class names and method signatures. Designed for hook development — finds exact `com.tencent.mm.*` classes and their public methods.

## Features

- **Multi-APK input**: Scans `base.apk` + all `split_config.*.apk` in one run
- **dexlib2 backend**: Pure bytecode parsing, no class loading, no runtime crashes
- **Keyword filtering**: Targets `msg`, `conversation`, `contact`, `storage`, `modelmulti`, etc.
- **Method signatures**: Outputs full descriptors (e.g., `insert(Landroid/content/ContentValues;)J`)
- **Zero dependencies at runtime**: Shadow JAR bundles dexlib2 + picocli (~3 MB)

## Quick Start

```bash
# Download latest release
wget https://github.com/wxmyyds/wechat-class-scanner/releases/latest/download/scanner-all.jar

# Scan WeChat APKs (base + splits)
java -jar scanner-all.jar \
  --apks base.apk,split_config.arm64_v8a.apk,split_config.x86_64.apk \
  --out classes.txt

# Or scan a directory containing all APKs
java -jar scanner-all.jar \
  --apks-dir /path/to/wechat-apks \
  --out classes.txt
```

## CLI Options

| Flag | Description | Default |
|------|-------------|---------|
| `--apks <files>` | Comma-separated APK paths (required unless `--apks-dir` used) | — |
| `--apks-dir <dir>` | Directory containing `base.apk` + `split_config.*.apk` | — |
| `-o, --output <file>` | Output file (default: stdout) | stdout |
| `-k, --keywords <csv>` | Keywords to filter class names | `msg,conversation,conv,contact,storage,store,info,message,modelmulti` |
| `-p, --package-prefix <pkg>` | Package prefix filter | `com.tencent` |
| `--no-methods` | Disable method signature output | false |

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
- **Filtering**: Only classes containing at least one keyword AND having public methods

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
        run: wget -q https://github.com/wxmyyds/wechat-class-scanner/releases/latest/download/scanner-all.jar -O scanner.jar
      - name: Find APKs in release assets
        id: apks
        run: |
          mkdir apks
          # GitHub Actions automatically downloads release assets to ./release-assets/
          # Adjust pattern as needed
          find . -name '*.apk' -exec cp {} apks/ \;
      - name: Run scanner
        run: |
          java -jar scanner.jar --apks-dir apks --out classes.txt
      - name: Upload classes.txt to release
        uses: softprops/action-gh-release@v2
        with:
          files: classes.txt
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