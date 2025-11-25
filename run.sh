#!/bin/bash
# Run script para Linux/Mac - Pixel Engine Gabriel 2025

echo "==============================================="
echo "   PIXEL ENGINE - Executando"
echo "==============================================="

java -cp bin game.test.Main

if [ $? -ne 0 ]; then
    echo ""
    echo "ERRO: Falha ao executar! Execute ./build.sh primeiro."
fi
