#!/bin/bash
# Build script para Linux/Mac - Pixel Engine Gabriel 2025

echo "==============================================="
echo "   PIXEL ENGINE - Build"
echo "==============================================="

# Limpa binários antigos
rm -rf bin
mkdir -p bin

# Compila engine
echo "Compilando engine..."
javac -d bin -sourcepath src \
    src/engine/math/*.java \
    src/engine/util/*.java \
    src/engine/core/*.java \
    src/engine/display/*.java \
    src/engine/input/*.java \
    src/engine/graphics/*.java \
    src/engine/assets/*.java

if [ $? -ne 0 ]; then
    echo "ERRO: Falha ao compilar engine!"
    exit 1
fi

# Compila game
echo "Compilando game..."
javac -d bin -sourcepath src -cp bin \
    src/game/test/*.java

if [ $? -ne 0 ]; then
    echo "ERRO: Falha ao compilar game!"
    exit 1
fi

echo "==============================================="
echo "   Build concluído com sucesso!"
echo "==============================================="
echo ""
echo "Para executar: ./run.sh"
