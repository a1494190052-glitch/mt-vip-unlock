#!/bin/bash
set -euxo pipefail

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
AJAR="$(ls "$SDK"/platforms/*/android.jar 2>/dev/null | sort -V | tail -1)"

if [ -z "$BT" ] || [ -z "$AJAR" ]; then
  echo "SDK not configured. ANDROID_HOME=$SDK"
  ls -la "$SDK" 2>/dev/null || true
  ls -la "$SDK"/build-tools 2>/dev/null || true
  ls -la "$SDK"/platforms 2>/dev/null || true
  exit 1
fi

echo "== build-tools: $BT"
echo "== android.jar : $AJAR"

rm -rf build out
mkdir -p build/stub build/classes build/dex out

# 1) compile Xposed API stubs (compile-time only, never packaged)
find stub -name '*.java' > build/stub.txt
javac -nowarn -cp "$AJAR" -d build/stub @build/stub.txt

# 2) compile the module against the stubs + android.jar
find src -name '*.java' > build/src.txt
javac -nowarn -cp "$AJAR:build/stub" -d build/classes @build/src.txt

# 3) dex only the module classes (stubs excluded)
find build/classes -name '*.class' > build/cls.txt
cat build/cls.txt
"$BT/d8" --min-api 21 --release --output build/dex @build/cls.txt

# 4) compile resources
"$BT/aapt2" compile --dir res -o build/res.zip

# 5) link a base apk
"$BT/aapt2" link \
  -o out/unsigned.apk \
  -I "$AJAR" \
  --manifest AndroidManifest.xml \
  -R build/res.zip \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code 1 \
  --version-name 1.0

# 6) inject dex into the apk root
(cd out && zip -q -j unsigned.apk ../build/dex/classes.dex)

# 7) sign
keytool -genkeypair \
  -keystore build/k.keystore -alias k \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass 123456 -keypass 123456 \
  -dname "CN=MTVIP, OU=Research, O=Local, L=X, ST=X, C=CN" >/dev/null 2>&1

"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk

"$BT/apksigner" sign \
  --ks build/k.keystore \
  --ks-pass pass:123456 \
  --key-pass pass:123456 \
  --out out/MTVIP-Unlock.apk out/aligned.apk

echo "== verify =="
"$BT/apksigner" verify --print-certs out/MTVIP-Unlock.apk | head -5
ls -la out/
