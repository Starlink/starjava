echo off
REM Batch file for invoking java in Windows.
REM Not sure exactly which operating systems (if any?) this will work for.
REM This assumes that the correct "java" command (1.4) is on the path.

rem Get the path of the directory in which this file is sitting.
rem ------------------------------------------------------------
set BINDIR=%~dp0

rem Augment the PATH with the expected location of DLLs.
rem ----------------------------------------------------
path "%BINDIR%..\lib;%PATH%"

rem Set up some properties.
rem -----------------------
set LOGCONFIG="%BINDIR%..\etc\logging.properties"

rem Set the java command to use (just get one off the path).
rem --------------------------------------------------------
set JAVACMD="java"

rem Get the command line arguments.
rem -------------------------------
rem Need to check if we are using the 4NT shell...
if "%@eval[2+2]" == "4" goto setup4NT

rem On NT/2K grab all arguments at once
set CMD_LINE_ARGS=%*
goto doneStart

:setup4NT
set CMD_LINE_ARGS=%$
goto doneStart

:doneStart

rem Invoke java with the requested args.
rem ------------------------------------
"%JAVACMD%" -Djava.util.logging.config.file="%LOGCONFIG%" %CMD_LINE_ARGS%
