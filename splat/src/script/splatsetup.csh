#!/bin/csh
#+
#  Name:
#     splatsetup.csh

#  Purpose:
#     Add SPLAT_DIR to the current PATH.

#  Description:
#     This script is intended to perform the task of adding the
#     main SPLAT directory to PATH, it also makes sure that
#     SPLAT_DIR is set. It should be used in the following
#     manner, assuming this command is already on the PATH:
#
#         source `which splatsetup.csh`
#
#     Or if it isn't on the PATH:
#
#         source /some/where/bin/splatsetup.csh
#
#     (don't use a relative position). The following should work 
#     for simplicity:
#
#         alias splatsetup 'source `which splatsetup.csh`'
#         alias splatsetup 'source /some/where/splatsetup.csh'
#
#     depending on your circumstance.

#  Type of Module:
#     C-shell script.

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
if ( $?SPLAT_DIR ) then
   if ( "$SPLAT_DIR" == "/star/bin/splat" || \
     "$SPLAT_DIR" == "/stardev/bin/splat" ) then
      unsetenv SPLAT_DIR
   endif
endif

#  Locate this script or SPLAT_DIR to find our jar files etc. If the
#  script location is being used it is assumed that SPLAT lives in a
#  sub-directory.
if ( ! $?SPLAT_DIR ) then
    setenv SPLAT_DIR "`starlocation`/splat"
endif

#  Check if SPLAT_DIR is on PATH already, if so do not add it again.
set foundit = "no"
if (`echo ${PATH} | grep -c ":${SPLAT_DIR}"` != 0) set foundit = "yes"
if (`echo ${PATH} | grep -c "^${SPLAT_DIR}"` != 0) set foundit = "yes"

if ( $foundit == "no" ) then
   if ( $?PATH ) then
      setenv PATH ${SPLAT_DIR}:${PATH}
   else
      setenv PATH $SPLAT_DIR
   endif
   echo ""
   echo "Added SPLAT directory to your PATH."
   echo "   ($SPLAT_DIR)"
   echo ""
else
   echo ""
   echo "SPLAT directory already on PATH, not added again."
   echo "   ($SPLAT_DIR)"
   echo ""
endif
unset foundit

#  Remove the SPLAT aliases for the classic version, these get in the
#  way of commands on the PATH.
unalias splat
unalias splatdisp
unalias splatdispmany
