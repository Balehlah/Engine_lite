@echo off
REM Run script para Windows - Pixel Engine Gabriel 2025

echo ===============================================
echo    PIXEL ENGINE - Executando
echo ===============================================

java -cp bin game.test.Main

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERRO: Falha ao executar! Execute build.bat primeiro.
)

