@echo off
chcp 65001 >nul 2>&1
echo ======================================
echo   WebCompressor 编译运行脚本
echo ======================================
echo.

:: 检查 Java
where java >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java，请先安装 JDK 17+
    pause
    exit /b 1
)

java -version 2>&1 | findstr "version" >nul
if errorlevel 1 (
    echo [错误] 需要 JDK 17 或更高版本
    pause
    exit /b 1
)

echo [OK] Java 环境检查通过

:: 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%

:: 切换到项目目录
cd /d "%PROJECT_DIR%"

:: 创建输出目录
if not exist "out" mkdir "out"
if not exist "out\compressor" mkdir "out\compressor\gui"
if not exist "out\compressor\core" mkdir "out\compressor\core"
if not exist "out\compressor\model" mkdir "out\compressor\model"
if not exist "out\compressor\algorithms" mkdir "out\compressor\algorithms"
if not exist "out\compressor\utils" mkdir "out\compressor\utils"

echo.
echo [1/3] 编译中...
echo.

:: 编译所有 Java 文件
set CLASSPATH=
for /r "%PROJECT_DIR%src\main\java" %%f in (*.java) do (
    javac -encoding UTF-8 -d out -sourcepath src\main\java "%%f" 2>>compile.log
)

if errorlevel 1 (
    echo [错误] 编译失败，请查看 compile.log
    type compile.log
    pause
    exit /b 1
)

echo [OK] 编译成功

:: 复制资源文件
echo.
echo [2/3] 复制资源文件...
xcopy /y /e "src\main\resources\*" "out\" >nul 2>&1

echo [OK] 资源文件已复制

:: 运行
echo.
echo [3/3] 启动应用...
echo ======================================
echo.

cd /d "%PROJECT_DIR%\out"
java --module-path "%JAVA_HOME%\lib" --add-modules javafx.controls,javafx.graphics ^
    compressor.gui.App

if errorlevel 1 (
    echo.
    echo [提示] 如果出现模块错误，请确保 JDK 包含 JavaFX
    echo        或使用包含 JavaFX 的 JDK 发行版（如 Azul Zulu JDK FX）
)

pause
