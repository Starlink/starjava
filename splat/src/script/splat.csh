#!/bin/csh
#+          
#  Name:
#     splat.csh

#  Purpose:
#     Set up aliases for the SPLAT package.

#  Type of Module:
#     C shell script.

#  Invocation:
#     source splat.csh

#  Description:
#     This procedure defines an alias for each SPLAT command. The 
#     string install_bin (upper-case) is replaced by the path of the 
#     directory containing the package executable files when the package
#     is installed.  The string help_dir (upper-case) is likewise replaced
#     by the path to the directory containing the help files.

#  Authors:
#     BLY: M.J. Bly (Starlink, RAL)
#     PWD: P.W. Draper (Stalink, Durham University)
#     {enter_new_authors_here}

#  History:
#     23-JUN-1995 (BLY):
#       Original Version.
#     12-DEC-1996 (BLY):
#       Cosmetic mods.
#     23-OCT-2001 (PWD):
#       For SPLAT.
#     {enter_changes_here}

#-

#  Locate the installed binaries, scripts etc.

setenv SPLAT_DIR INSTALL_BIN

#
#  Define symbols for the applications and scripts.
#  ===============================================

alias splat ${SPLAT_DIR}/splat
alias splatdisp ${SPLAT_DIR}/splatdisp
alias splatdispmany ${SPLAT_DIR}/splatdispmany

#
#  Now do the same with alternative names.
#  ======================================

alias splat_splat ${SPLAT_DIR}/splat
alias splat_splatdisp ${SPLAT_DIR}/splatdisp
alias splat_splatdispmany ${SPLAT_DIR}/splatdispmany

#
#  Tell the user that SPLAT commands are now available.
#  =======================================================

echo ""
echo "   SPLAT commands are now available -- (Version PKG_VERS)"
echo " "
echo "   See the on-line help system for more information"
echo " "

#
# end
