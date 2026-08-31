@echo off
setlocal
set GRADLE_VERSION=9.3.1
set DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip
set BASE_DIR=%USERPROFILE%\.gradle\wrapper\custom-gradle-%GRADLE_VERSION%
set GRADLE_HOME=%BASE_DIR%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
  if not exist "%BASE_DIR%\gradle-%GRADLE_VERSION%-bin.zip" (
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%BASE_DIR%\gradle-%GRADLE_VERSION%-bin.zip'"
  )
  powershell -NoProfile -Command "Expand-Archive -Force '%BASE_DIR%\gradle-%GRADLE_VERSION%-bin.zip' '%BASE_DIR%'"
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
