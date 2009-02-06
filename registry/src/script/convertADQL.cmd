echo off
if "%1" == "-h" goto :usage

goto :run

:usage
echo 
echo convertADQL:  convert between ADQL/s and ADQL/x
echo convertADQL -X|-S|-x xmlfile|-s sqlfile [-o outfile] [-t transformer]
echo               [-c config] [sql...]
echo Options:
echo   -X              read and convert XML from standard input
echo   -x xmlfile      read and convert XML from xmlfile
echo   -S              read and convert SQL from command line or standard input
echo   -s sqlfile      read and convert SQL from sqlfile
echo   -o outfile      write results to output file; if not given, write to
echo                      standard out
echo   -t transformer  use named transformer (e.g. XSLx2s)
echo   -c config       load customized config file
echo Arguments:
echo   sql             ADQL/s string to convert with -S; if not given, read from
echo                       standard in

goto :EOF

:run
set ERR=

set BIN=
if not "%JAVA_HOME%"=="" set BIN="%JAVA_HOME%\bin\"

set ADQL_HOME=@ADQL_HOME@
set APACHE_LIB=@APACHE_LIB@

set cp=%APACHE_LIB%\lib\xercesImpl.jar;%APACHE_LIB%\lib\xml-apis.jar;%APACHE_LIB%\lib\xalan.jar;%CLASSPATH%;%ADQL_HOME%\lib\@adql_jar@
if "%ERR%"=="" %BIN%java -cp %cp% net.ivoa.adql.app.ConvertADQL %*%
