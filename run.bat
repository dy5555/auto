@echo off
setlocal

if not exist auto.jar (
    echo auto.jar not found. Run build.bat first.
    pause
    exit /b 1
)

if not exist lib\ojdbc8.jar (
    echo lib\ojdbc8.jar not found.
    echo Put the Oracle JDBC driver in the lib folder.
    pause
    exit /b 1
)

java -cp "auto.jar;lib\ojdbc8.jar" com.dy5555.auto.App
