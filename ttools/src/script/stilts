#!/bin/sh

#+
#  Name:
#     stilts

#  Purpose:
#     Invokes the STILTS application on unix.

#  Description:
#     This shell script invokes the STILTS application. 
#     It's not very complicated, but performs some argument manipulation
#     prior to invoking java with the right classpath and classname.
#
#     1. if a class path is specified using either the CLASSPATH
#        environment variable or the -classpath flag to this script,
#        it will be added to the application classpath
#
#     2. any initial command-line arguments which look like they are destined
#        for java itself (starting with -D or -X, or prefixed with -J) will
#        be sent to java, and the others will be sent to the application

#  Requisites:
#     - java should be on the path.
#
#     - relative to the directory in which this script is installed,
#       one of the following jar files should exist and contain the
#       STILTS classes:
#          ./stilts.jar
#          ../../lib/ttools/stilts-app.jar
#          ./stilts-app.jar
#          ./topcat-full.jar
#          ./topcat-lite.jar
#       (on a Mac it looks in the resource bundle expected from a dmg
#       installation as well)

#  Authors:
#     MBT: Mark Taylor (Starlink)
#-

#  Set locations of acceptable jar files (relative to this script).
stilts_jars="\
 stilts.jar\
 ../lib/ttools/stilts-app.jar\
 stilts-app.jar\
 topcat-full.jar\
 topcat-lite.jar\
"

# If we're on a Mac, look for the jar in the resource bundle.
if test -x /usr/bin/sw_vers && /usr/bin/sw_vers | grep -q 'OS.X'; then
   stilts_jars="$stilts_jars ../Contents/Resources/Java/topcat-full.jar ../TOPCAT.app/Contents/Resources/Java/topcat-full.jar"
fi

#  Find where this script is located.
bindir="`dirname $0`"

#  Locate the application jar file.
for j in $stilts_jars; do
   if test -z "$appjar" -a -f "$bindir/$j"; then
      appjar="$bindir/$j"
   fi
done
if test ! -f "$appjar"
then
   echo 1>&2 "Can't find stilts classes in ${bindir} - looked for:$stilts_jars"
   exit 1
fi

#  Pull out any arguments which look to be destined for the java binary.
javaArgs=""
while test "$1"
do
   if echo $1 | grep -- '^-[XD]' >/dev/null; then
      javaArgs="$javaArgs $1"
      shift
   elif echo $1 | grep -- '^-J' >/dev/null; then
      javaArgs="$javaArgs `echo $1 | sed s/^-J//`"
      shift
   elif [ "$1" = "-classpath" -a -n "$2" ]; then
      shift
      export CLASSPATH="$1"
      shift
   else
      break
   fi
done

#  Check for Cygwin and transform paths.
case "`uname`" in
  CYGWIN*)
    if test -n "$CLASSPATH"; then
       CLASSPATH=`cygpath --path --windows "${appjar}:$CLASSPATH"`
    else
       CLASSPATH=`cygpath --windows "${appjar}"`
    fi
  ;;
  *)
    CLASSPATH="${appjar}:${CLASSPATH}"
  ;;
esac

# Execute the command.
java \
   $javaArgs \
   -classpath $CLASSPATH \
   uk.ac.starlink.ttools.Stilts \
   "$@"
