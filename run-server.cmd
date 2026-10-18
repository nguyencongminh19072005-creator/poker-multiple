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

set "DB_URL=jdbc:mysql://localhost:3307/poker_java?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh"
set "DB_USERNAME=root"
set "DB_PASSWORD=replace_with_your_local_mysql_password"
set "SERVER_PORT=8080"
set "JWT_SECRET=replace_with_a_random_secret_of_at_least_32_characters"

echo [STARTING] Poker WebSocket Server on port %SERVER_PORT%...
call "%~dp0mvnw.cmd" exec:java "-Dexec.mainClass=com.poker.ServerMain"
pause
