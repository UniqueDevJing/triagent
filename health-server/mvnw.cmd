@REM Maven Wrapper Startup Script
@echo off
setlocal
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven...
    "%JAVA_HOME%\bin\java" -jar "%~dp0.mvn\wrapper\maven-wrapper.jar" -Dmaven.repo.central=https://repo.maven.apache.org/maven2
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
