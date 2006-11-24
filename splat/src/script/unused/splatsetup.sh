#!/bin/sh
#+
#  Name:
#     splatsetup.sh

#  Purpose:
#     Add SPLAT_DIR to the current PATH.

#  Description:
#     This script is intended to perform the task of adding the
#     main SPLAT directory to PATH, it also makes sure that
#     SPLAT_DIR is set. It should be used in the following
#     manner, assuming this command is already on the PATH:
#
#         . `which splatsetup.csh`
#
#     Or if it isn't on the PATH:
#
#         . /some/where/bin/splatsetup.csh
#
#     (don't use a relative position).

#  Type of Module:
#     Bourne shell script.

#  Copyright:
#     Copyright (C) 2003 Central Laboratory of the Research Councils

#  Authors:
#     PWD: P. W. Draper (Starlink, Durham University)
#     {enter_new_authors_here}

#  History:
#     03-JUN-2003 (PWD):
#        Original version.
#     {enter_further_changes_here}

#  Bugs:
#     {note_any_bugs_here}

#-

#  Skip the definition of SPLAT_DIR if it's in the standard place for
#  old-SPLAT. Remove this when new SPLAT is the official release.
if test "$SPLAT_DIR" != ""; then
   if test "$SPLAT_DIR" = "/star/bin/splat" \
      -o "$SPLAT_DIR" = "/stardev/bin/splat"; then
      SPLAT_DIR=""
   fi
fi

#  Locate this script or SPLAT_DIR to find our jar files etc. If the
#  script location is being used it is assumed that SPLAT lives in a
#  sub-directory.
if test "$SPLAT_DIR" = ""; then
    SPLAT_DIR="`dirname $0`/splat"
    export SPLAT_DIR
fi

#  Check if SPLAT_DIR is on PATH already, if so do not add it again.
foundit="no"
if test "`echo $PATH | grep -c \":${SPLAT_DIR}\"`" != "0"; then
   foundit="yes"
fi
if test "`echo $PATH | grep -c \"^${SPLAT_DIR}\"`" != "0"; then
   foundit="yes"
fi

if test "$foundit" = "no"; then
   if test "$PATH" != ""; then
      PATH="${SPLAT_DIR}:${PATH}"
   else
      PATH="${SPLAT_DIR}"
   fi
   export PATH
   echo ""
   echo "Added SPLAT directory to your PATH."
   echo "   ($SPLAT_DIR)"
   echo ""
else
   echo ""
   echo "SPLAT directory already on PATH, not added again."
   echo "   ($SPLAT_DIR)"
   echo ""
fi
foundit=""
