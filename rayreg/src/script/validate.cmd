echo off
if "%1" == "-h" goto :usage

goto :run

:usage
echo verify an XML instance document using Apache Xalan
echo
echo Synopsis:
echo    verify [options] uri ...
echo
echo Options:
echo  -p name     Select parser by name.
echo  -x number   Select number of repetitions.
echo  -n  | -N    Turn on/off namespace processing.
echo  -np | -NP   Turn on/off namespace prefixes.
echo              NOTE: Requires use of -n.
echo  -v  | -V    Turn on/off validation.
echo  -s  | -S    Turn on/off Schema validation support.
echo              NOTE: Not supported by all parsers.
echo  -f  | -F    Turn on/off Schema full checking.
echo              NOTE: Requires use of -s and not supported by all parsers.
echo  -dv | -DV   Turn on/off dynamic validation.
echo              NOTE: Requires use of -v and not supported by all parsers.
echo  -m  | -M    Turn on/off memory usage report
echo  -t  | -T    Turn on/off "tagginess" report.
echo  --rem text  Output user defined comment before next parse.
echo  -h          display help info and exit
echo

goto :EOF

:run
set ERR=

set BIN=
if not "%JAVA_HOME%"=="" set BIN="%JAVA_HOME%\bin\"

set ADQL_HOME=@ADQL_HOME@
set APACHE_LIB=@APACHE_LIB@

set defaultSchemaFiles=http://www.ivoa.net/xml/ADQL/v0.7.4 ADQL-0.7.4.xsd urn:nvo-region nvo-regions.xsd urn:nvo-coords nvo-coords.xsd http://www.ivoa.net/xml/ADQL/v1.0 ADQL-v1.0.xsd http://www.ivoa.net/xml/STC/STCcoords/v1.10 coords-v1.10.xsd http://www.ivoa.net/xml/STC/STCregion/v1.10 region-v1.10.xsd
set schemaPath=%ADQL_HOME%\etc\v0.7.4\schemas:%ADQL_HOME%\etc\v1.0\schemas

set cp=%APACHE_LIB%\lib\xercesImpl.jar;%APACHE_LIB%\lib\xml-apis.jar;%CLASSPATH%;%ADQL_HOME%\lib\@adql_jar@

if "%ERR%"=="" %BIN%java -Dadql.schemaPath=%schemaPath% -Dadql.defaultSchemaFiles="%defaultSchemaFiles%" -cp %cp% net.ivoa.adql.app.Validate %*%




