@echo off
setlocal

set CURDIR=%~dp0
set CP="%CURDIR%\*"

if "%JAVA_HOME%"=="" (
  set JAVA=java
) else (
  set JAVA="%JAVA_HOME%\bin\java.exe"
)

%JAVA% -cp "%CP%" com.github.zabbicook.cli.Main %*

EXIT /B %ERRORLEVEL%

