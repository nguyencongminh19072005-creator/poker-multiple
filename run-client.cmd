@echo off
setlocal
cd /d "%~dp0"
if not defined POKER_JAVA_HOME (
    for /d %%J in ("%ProgramFiles%\Java\jdk-26*") do (
        if exist "%%~fJ\bin\java.exe" set "POKER_JAVA_HOME=%%~fJ"
    )
)
if defined POKER_JAVA_HOME (
    set "JAVA_HOME=%POKER_JAVA_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo [STARTING] Poker JavaFX Client...
call "%~dp0mvnw.cmd" javafx:run
pause
