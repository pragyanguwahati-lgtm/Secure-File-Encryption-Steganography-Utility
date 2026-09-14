#!/usr/bin/env bash
set -e

echo "==================================================="
echo "  Building SecTool (Secure Steganography Utility)  "
echo "==================================================="

mkdir -p bin

echo "[1/2] Compiling source files..."
javac -d bin $(find src -name "*.java")

echo "[2/2] Packaging into executable SecTool.jar..."
jar cfe SecTool.jar com.stego.cli.MainCLI -C bin .

echo "==================================================="
echo "[SUCCESS] Build completed successfully!"
echo "Executable JAR: SecTool.jar"
echo "Run: java -jar SecTool.jar --help"
echo "==================================================="
