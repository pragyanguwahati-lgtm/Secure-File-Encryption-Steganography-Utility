@echo off
echo ===================================================
echo   Building SecTool (Secure Steganography Utility)  
echo ===================================================

if not exist bin mkdir bin

echo [1/2] Compiling source files...
javac -d bin src\com\stego\exception\*.java src\com\stego\crypto\*.java src\com\stego\util\*.java src\com\stego\core\*.java src\com\stego\cli\*.java src\com\stego\test\*.java src\MainCLI.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)

echo [2/2] Packaging into executable SecTool.jar...
jar cfe SecTool.jar com.stego.cli.MainCLI -C bin .

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JAR packaging failed.
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo [SUCCESS] Build completed successfully!
echo Executable JAR: SecTool.jar
echo Run: java -jar SecTool.jar --help
echo ===================================================
