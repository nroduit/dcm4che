@echo off
rem -------------------------------------------------------------------------
rem dcm2  Launcher
rem -------------------------------------------------------------------------

if not "%ECHO%" == ""  echo %ECHO%
if "%OS%" == "Windows_NT"  setlocal

set MAIN_CLASS=org.dcm4che3.tool.dcmconv.Dcmconv
set MAIN_JAR=dcm4che-tool-dcmconv-${project.version}.jar

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%

rem Read all command line arguments

set ARGS=
:loop
if [%1] == [] goto end
        set ARGS=%ARGS% %1
        shift
        goto loop
:end

if not "%DCM4CHE_HOME%" == "" goto HAVE_DCM4CHE_HOME

set DCM4CHE_HOME=%DIRNAME%..

:HAVE_DCM4CHE_HOME

if not "%JAVA_HOME%" == "" goto HAVE_JAVA_HOME

set JAVA=java

goto SKIP_SET_JAVA_HOME

:HAVE_JAVA_HOME

set JAVA=%JAVA_HOME%\bin\java

:SKIP_SET_JAVA_HOME

set CP=%DCM4CHE_HOME%\etc\dcm2dcm\
set CP=%CP%;%DCM4CHE_HOME%\lib\%MAIN_JAR%
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-core-${project.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-image-${project.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-imageio-${project.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\dcm4che-img-${project.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\weasis-core-img-${weasis.core.img.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\slf4j-api-${slf4j.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\slf4j-log4j12-${slf4j.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\log4j-${log4j.version}.jar
set CP=%CP%;%DCM4CHE_HOME%\lib\picocli-${picocli.version}.jar

rem Setup the native library path
"%JAVA%" -version 2>&1 | findstr 64-Bit >nul && set "OS=windows-x86-64" || set "OS=windows-x86"
set JAVA_LIBRARY_PATH=%DCM4CHE_HOME%\lib\%OS%

set JAVA_OPTS=%JAVA_OPTS% -Djava.library.path="%JAVA_LIBRARY_PATH%"

"%JAVA%" %JAVA_OPTS% -cp "%CP%" %MAIN_CLASS% %ARGS%
