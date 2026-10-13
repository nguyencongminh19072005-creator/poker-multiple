@echo off
setlocal
cd /d "%~dp0"
if not defined POKER_JAVA_HOME (
    for /d %%J in ("%ProgramFiles%\Java\jdk-26*") do (
        if exist "%%~fJ\bin\java.exe" set "POKER_JAVA_HOME=%%~fJ"
    )
)
if not defined POKER_JAVA_HOME (
    echo [ERROR] JDK 26 not found. Set POKER_JAVA_HOME to your JDK 26 directory.
    exit /b 1
)
set "JAVA_HOME=%POKER_JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0mvnw.cmd" "-Dmaven.repo.local=%~dp0.m2\repository" -Panimation-preview javafx:run
exit /b %ERRORLEVEL%
