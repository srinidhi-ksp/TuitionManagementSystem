@echo off
setlocal

set CP=lib\bson-4.11.1.jar;lib\bson-record-codec-4.11.1.jar;lib\itextpdf-5.5.13.3.jar;lib\mongodb-driver-core-4.11.1.jar;lib\mongodb-driver-sync-4.11.1.jar

if not exist out mkdir out

echo Compiling all Java sources...
javac -cp "%CP%" -d out -encoding UTF-8 @java_files.txt 2>compile_errors.txt

if %ERRORLEVEL% EQU 0 (
    echo.
    echo BUILD SUCCESS
) else (
    echo.
    echo BUILD FAILED - Errors:
    type compile_errors.txt
)
endlocal
