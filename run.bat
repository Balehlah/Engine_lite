@echo off
REM Compatibility entrypoint. Use the documented transitional Gradle task.
call "%~dp0gradlew.bat" legacyDemo %*
exit /b %ERRORLEVEL%

