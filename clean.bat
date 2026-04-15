@echo off
chcp 65001 >nul 2>&1
echo ======================================
echo   WebCompressor 清理脚本
echo ======================================
echo.

cd /d "%~dp0"

if exist "out" (
    rmdir /s /q "out"
    echo [OK] 已删除 out 目录
) else (
    echo [跳过] out 目录不存在
)

if exist "compile.log" (
    del "compile.log"
    echo [OK] 已删除编译日志
)

if exist "*.class" (
    del /s /q "*.class" >nul 2>&1
    echo [OK] 已删除所有 class 文件
)

echo.
echo 清理完成!
pause
