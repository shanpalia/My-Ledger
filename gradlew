#!/usr/bin/env sh
set -eu
GRADLE_VERSION="9.3.1"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/custom-gradle-${GRADLE_VERSION}"
GRADLE_HOME="${BASE_DIR}/gradle-${GRADLE_VERSION}"
if [ ! -x "${GRADLE_HOME}/bin/gradle" ]; then
  mkdir -p "${BASE_DIR}"
  ZIP_FILE="${BASE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
  if [ ! -f "${ZIP_FILE}" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 --connect-timeout 20 "${DIST_URL}" -o "${ZIP_FILE}"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "${ZIP_FILE}" "${DIST_URL}"
    else
      echo "curl or wget is required to download Gradle ${GRADLE_VERSION}." >&2
      exit 1
    fi
  fi
  unzip -q -o "${ZIP_FILE}" -d "${BASE_DIR}"
fi
exec "${GRADLE_HOME}/bin/gradle" "$@"
