
#  Defaults.
#  ========
#
#  This section defines default macros and should rarely need changing.
#  The values given here should be overridden externally to adapt to
#  the local system setup (either use the mk script or use environment
#  variables and invoke "make" with the "-e" option).

#  Name of computer hardware/OS combination.

SYSTEM = unknown

#  Name used to distinguish platform-specific source files.

SOURCE_VARIANT = $(SYSTEM)

#  Pathname of the root directory beneath which other Starlink software
#  is currently installed.

STARLINK = /star

#  Pathnames of Starlink sub-directories that may be referenced when
#  building this package.

STAR_BIN = $(STARLINK)/bin
STAR_INC = $(STARLINK)/include
STAR_LIB = $(STARLINK)/lib
STAR_SHARE = $(STARLINK)/share

#  Default macros for compiling C and Fortran source code.

CC = c89
CFLAGS = -O

#  Default file type extension for a shareable library and command for
#  building a shareable library (the default acts as a null command).

SHARE = .so
BLD_SHR = :

#  Java specific platform name (used for locating JNI include files).

JAVA_SYSTEM = unknown

#  Directory into which the target library is built

TARGET_LIB = .

#  Include directories for the Java development system. Notes from
#  above also apply here (i.e. these directories will not exist on
#  most systems).

#  Locations of include files.

JAVA_INCLUDE = $(JDK)/include
JAVA_SYSTEM_INCLUDE = $(JAVA_INCLUDE)/$(JAVA_SYSTEM)

#  Name of the package

PKG_NAME = KAPPA

#  Name of the target shared library.

SHARED_OBJECT = $(TARGET_LIB)/lib$(PKG_NAME)$(SHARE)

#  C source files.

C_ROUTINES = KAPPA.c 

#  INCLUDE and header files

PRIVATE_INCLUDES = uk_ac_starlink_kappa_KAPPA.h

#  List of object files produced by compiling the source code and rules
#  for performing the compilations.

OBJECT_FILES = $(C_ROUTINES:.c=.o) 

$(OBJECT_FILES): $(PRIVATE_INCLUDES) $(EXTERNAL_INCLUDES)

.f.o:
	$(FC) $(FFLAGS) -c $<
.c.o:
	$(CC) $(CFLAGS) -c $<


#  List of shareable library files to be built and rules for building
#  them. The third argument to $(BLD_SHR) should provide the information
#  necessary to link any libraries called by this package.
#  These are all expected to be in $(STAR_LIB) or $(ADDLIBS). 
#  See the "PKG_libraries" script for details.

shared: $(SHARED_OBJECT)

$(SHARED_OBJECT): $(OBJECT_FILES) 
	$(BLD_SHR) $@ \
	"$(ADDLIBS) -L$(STAR_SHARE) -L$(STAR_LIB) $(OBJECT_FILES) \
	-lkappa -lkapview -lkapsub -lard_adam -lkapgen -lira -lirq \
        -llpg -lctg -lfits -laif -lcat -lcfitsio -lndg_adam -lndg \
        -lndf_adam -lndf -lary_adam -lary -last_grf -last -last_wcslib \
	-last_slalib -lsla -last_pass2 -last_ems -lagp_adam -lagp -lgns \
	-lref -lfio_adam -lfio -lgrp_adam -lgrp -ltrn -lerr -lerr_adam \
	-lprm -lprm_a -lmag_adam -lmag -lhds -lpda -lkapgrf -lpgppar_adam \
	-lpgplot -lgwm -L/usr/X11R6/lib -lX11 -lpar_adam -lsubpar_adam \
	-lstring_adam -lemsf -lems -lcnf -lchr -lpsx -ljniast -ljnihds"
