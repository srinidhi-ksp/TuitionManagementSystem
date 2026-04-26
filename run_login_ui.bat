@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal enabledelayedexpansion

set "CLASSPATH=lib\*"

echo Compiling all Java files with UTF-8 encoding...
javac -encoding UTF-8 -source 17 -target 17 -d bin -cp "%CLASSPATH%" ^
  src\LaunchLoginUI.java ^
  src\LaunchAdminDashboard.java ^
  src\dao\*.java ^
  src\db\*.java ^
  src\model\*.java ^
  src\service\*.java ^
  src\util\*.java ^
  src\ui\*.java ^
  src\ui\admin\*.java ^
  src\ui\parent\*.java ^
  src\ui\student\*.java ^
  src\ui\teacher\*.java

if errorlevel 1 (
  echo.
  echo Compilation FAILED!
  echo.
  pause
  exit /b 1
) else (
  echo.
  echo Compilation SUCCESSFUL!
  echo.
)

echo Running Login UI...
java -cp "bin;lib\*" LaunchLoginUI

pause
