echo off
REM Batch file for invoking Treeview in Windows.
REM Not sure exactly what operating systems (if any?) this will work for...
REM ...looks like NT may be OK, but not Win9X (the %~dp0 business is no good)

rem Get the path of the directory in which this file is sitting.
rem ------------------------------------------------------------
set BINDIR=%~dp0

rem Set the name of the script which invokes java.
rem ----------------------------------------------
set JAVACMD="%BINDIR%starjava"

rem Set the name of some resources.
rem -------------------------------
set APPJAR="%BINDIR%..\lib\treeview\treeview.jar"
set DEMODIR="%BINDIR%..\etc\treeview\demo"


rem Get the command line arguments.
rem -------------------------------
rem Need to check if we are using the 4NT shell...
if "%@eval[2+2]" == "4" goto setup4NT

rem On NT/2K grab all arguments at once
set CMD_LINE_ARGS=%*
goto gotArgs

:setup4NT
set CMD_LINE_ARGS=%$

:gotArgs

rem Invoke Treeview with the requested args.
rem ----------------------------------------
"%JAVACMD%" -Duk.ac.starlink.treeview.demodir="%DEMODIR%" -jar "%APPJAR%" %CMD_LINE_ARGS%

