@echo off
REM Compatibility entrypoint. Gradle Wrapper is the source of truth.
call "%~dp0gradlew.bat" clean test %*
exit /b %ERRORLEVEL%
