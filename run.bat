@echo off
setlocal
set CP=out;lib\bson-4.11.1.jar;lib\bson-record-codec-4.11.1.jar;lib\itextpdf-5.5.13.3.jar;lib\mongodb-driver-core-4.11.1.jar;lib\mongodb-driver-sync-4.11.1.jar
java -cp "%CP%" LaunchLoginUI
endlocal
