#!/bin/sh
set -eu
GRADLE_VERSION=9.3.1
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_HOME="$ROOT/.gradle-local/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$ROOT/.gradle-local"
  ARCHIVE="$ROOT/.gradle-local/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ARCHIVE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -L --fail --retry 3 -o "$ARCHIVE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      wget -O "$ARCHIVE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    fi
  fi
  unzip -q "$ARCHIVE" -d "$ROOT/.gradle-local"
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
