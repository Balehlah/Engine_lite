#!/bin/bash
echo "Compilando o motor PIXEL_ENGINE..."
mkdir -p bin
javac -d bin $(find src -name "*.java")
echo "Compilação finalizada com sucesso."

