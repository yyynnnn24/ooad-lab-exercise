@echo off
javac -cp ".;sqlite-jdbc-3.51.1.0.jar" *.java
if %errorlevel% neq 0 pause && exit
java -cp ".;sqlite-jdbc-3.51.1.0.jar" LoginScreen
pause