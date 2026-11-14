#!/usr/bin/env bash
set -euo pipefail

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Install JDK 25 and add it to PATH." >&2
  exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version \"\([0-9]*\).*/\1/p')"
if [[ "$JAVA_MAJOR" != "25" ]]; then
  echo "ERROR: Minecraft 26.1.2 requires JDK 25; found Java ${JAVA_MAJOR:-unknown}." >&2
  exit 1
fi
if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Install Gradle 9.4.1 and add it to PATH, or open the project in IntelliJ IDEA." >&2
  exit 1
fi

gradle build

echo "BUILD COMPLETE. Jar: build/libs/fps-stabilizer-1.1.0.jar"
