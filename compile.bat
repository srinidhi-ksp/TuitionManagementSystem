@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal enabledelayedexpansion

set "CLASSPATH=lib\*"
set "SOURCE_17=-source 17"
set "TARGET_17=-target 17"

javac -encoding UTF-8 -d bin -cp "%CLASSPATH%" %SOURCE_17% %TARGET_17% ^
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
  echo Compilation failed!
  exit /b 1
) else (
  echo Compilation successful!
)

echo.
echo Running the application...
java -cp "bin;lib\*" LaunchAdminDashboard
