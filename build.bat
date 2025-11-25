@echo off
REM Build script para Windows - Pixel Engine Gabriel 2025

echo ===============================================
echo    PIXEL ENGINE - Build
echo ===============================================

REM Limpa binarios antigos
if exist bin rmdir /s /q bin
mkdir bin

REM Compila todos os arquivos Java
echo Compilando engine...
javac -d bin -sourcepath src src/engine/math/*.java src/engine/util/*.java src/engine/core/*.java src/engine/display/*.java src/engine/input/*.java src/engine/graphics/*.java src/engine/assets/*.java

if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha ao compilar engine!
    exit /b 1
)

echo Compilando game...
javac -d bin -sourcepath src -cp bin src/game/test/*.java

if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha ao compilar game!
    exit /b 1
)

echo ===============================================
echo    Build concluido com sucesso!
echo ===============================================
echo.
echo Para executar: run.bat

