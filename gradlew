#!/bin/sh
# Codemagic builds use the Gradle 9.3.1 distribution configured in codemagic.yaml.
exec gradle "$@"
