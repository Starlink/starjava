echo off
REM
REM Usage: xalan -in input_xml -xsl stylesheet [-out outputfile] [other_options]
REM
REM This is a shell wrapper around the Xalan XSLT processing application.  
REM For a full explanation of all supported command line options, consult
REM http://xml.apache.org/xalan-j/commandline.html
REM
set ERR=

set BIN=
if not "%JAVA_HOME%"=="" set BIN="%JAVA_HOME%\bin\"

set ADQL_HOME=@ADQL_HOME@
set APACHE_LIB=@APACHE_LIB@

set cp=%APACHE_LIB%\lib\xercesImpl.jar;%APACHE_LIB%\lib\xml-apis.jar;%APACHE_LIB%\lib\xalan.jar;%CLASSPATH%
set PARSER=org.apache.xalan.xslt.Process

if "%ERR%"=="" %bin%java -cp %cp% %PARSER% %*%
