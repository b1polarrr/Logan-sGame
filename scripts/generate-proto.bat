@echo off
chcp 65001 >nul
REM 一键生成 Protobuf Java 代码并编译
REM 用法：双击运行，或在项目根目录 scripts\generate-proto.bat

set "PROJECT_DIR=%~dp0.."
cd /d "%PROJECT_DIR%"

set "MAVEN_CMD="
for %%I in (
    "D:\APPS\IntelliJ IDEA 2022.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%ProgramFiles%\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
) do (
    if exist %%~I set "MAVEN_CMD=%%~I"
)

if not defined MAVEN_CMD (
    where mvn >nul 2>&1
    if %errorlevel%==0 (
        set "MAVEN_CMD=mvn"
    )
)

if not defined MAVEN_CMD (
    echo [错误] 未找到 mvn。请安装 Maven 或修改本脚本中的 MAVEN_CMD 路径。
    pause
    exit /b 1
)

echo [信息] 使用 Maven: %MAVEN_CMD%
echo [信息] 项目目录: %PROJECT_DIR%
echo.

call "%MAVEN_CMD%" clean generate-sources compile -DskipTests
if errorlevel 1 (
    echo.
    echo [失败] 编译未通过，请查看上方错误。
    pause
    exit /b 1
)

echo.
echo [成功] Protobuf 已生成到:
echo   target\generated-sources\protobuf\java\com\mercury\poker\network\protocol\
echo.
echo 请在 IDEA 中: Maven - Reload Project，必要时 File - Invalidate Caches
pause
