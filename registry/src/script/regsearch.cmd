echo off
if "%1" == "-h" goto :usage

goto :run

:usage
echo 
echo regsearch:  query an IVOA searchable registry. 
echo
echo    This rudimentary command line interface provides access to a
echo    registry's search functions via six commands.  Various options--some
echo    command specific--allow you to select out the information.
echo
echo    If you do not provide any arguments regsearch will run some internal 
echo    tests.  
echo
echo Usage:  regsearch -e url [ options ] command [ args ] 
echo General Options:
echo   -e url         the registry search service endpoint URL.  If none is 
echo                     provided, regsearch will run in a self-test mode without
echo                     contacting a real registry; search inputs are generally
echo                     ignored.
echo   -v             verbose mode; print extra messages
echo   -x             print to standard out the request and response SOAP messages 
echo Arguments:
echo   command        the (case-insensitive) name of the registry function to 
echo                  access:
echo                     getidentity       return the description of the registry
echo                     getresource       return the description of the resource 
echo                                         specified by an identifier
echo                     searchbyadql      search for resources whose 
echo                                         descriptions match ADQL constraints
echo                     searchbykeywords  search for resources whose descriptions
echo                                         contain given keywords
echo                     idsbyadql         return just IDs of resources whose 
echo                                         descriptions match ADQL constraints
echo                     idsbykeywords     return just IDs of resources whose 
echo                                         descriptions contain given keywords
echo                  see below for command specific options
echo
echo GetIdentity:  return the description of the registry
echo Usage:  regsearch -e url [ -v ] [-s metadatum[,...]] getidentity
echo Options:
echo   -s metadatum,...  a list of metadata values to print.  Each item is a
echo                      simple XPath pointer to a VOResource element.  If not
echo                      provided, the following are returned:
echo                         identifier,title,capability/interface/accessURL
echo
echo GetResource: return the description of the resource specified by an identifier
echo Usage:  regsearch -e url [ -v ] [-s metadatum[,...]] getResource id
echo Options:
echo   -s metadatum,...  a list of metadata to print (same as for getIdentity)
echo Arguments:
echo   id                the IVOA identifier of the resource of interest
echo
echo SearchByADLQ: search for resources whose descriptions match ADQL constraints 
echo Usage:  regsearch -e url [-v -m max] [-s metadatum[,...]] SearchByADLQ adql
echo Options:
echo   -m max           the maximum number of records to print out
echo   -s metadatum,... a list of metadata to print (same as for getIdentity)
echo Arguments:
echo   adql             the ADQL constraints to apply (e.g. "title like '%quasars%'
echo                      and [capability/@xsi:type] like '%ConeSearch'")
echo
echo IDsByADLQ: return just the identifiers of resources whose descriptions 
echo            match ADQL constraints 
echo Usage:  regsearch -e url [-v -m max] [-s metadatum[,...]] SearchByADLQ adql
echo Options:
echo   -m max           the maximum number of identifiers to print out
echo Arguments:
echo   adql             the ADQL constraints to apply (e.g. "title like '%quasars%'
echo                      and [capability/@xsi:type] like '%ConeSearch'")
echo
echo SearchByKeywords: search for resources whose descriptions contain given 
echo                   keywords
echo Usage:  regsearch -e url [-vo -m max] searchbykeywords keyword ...
echo Options:
echo   -m max            the maximum number of records to print out
echo   -o                logical OR: return records if the descriptions contain
echo                        any of the keywords.  If not set, the matched records
echo                        must contain all of the keywords
echo Arguments:
echo   keyword           a word to search for
echo
echo IDsByKeywords: return just the identifiers of resources whose descriptions 
echo                contain given keywords
echo Usage:  regsearch -e url [-vo -m max] searchbykeywords keyword ...
echo Options:
echo   -m max            the maximum number of identifiers to print out
echo   -o                logical OR: return records if the descriptions contain
echo                        any of the keywords.  If not set, the matched records
echo                        must contain all of the keywords
echo Arguments:
echo   keyword           a word to search for
echo 

goto :EOF

:run
set ERR=

if "%NVOSS_HOME%"=="" set ERR=NVOSS_HOME not set.
if not "%ERR%"=="" echo Validate: %ERR%

if "%ANT_HOME%"=="" echo "Validate: Warning: NVOSS software apparently not setup"
set BIN=
if not "%JAVA_HOME%"=="" set BIN=%JAVA_HOME%\bin\

set IVOAREG_HOME=%NVOSS_HOME%\java\src\ivoaregistry

set cp=%CLASSPATH%;%IVOAREG_HOME%/lib/ivoaregistry.jar

if "%ERR%"=="" "%BIN%java" -cp "%cp%" net.ivoa.registry.search.test.TestRegistryClient %*%

