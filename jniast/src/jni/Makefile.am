## Process this file with automake to produce Makefile.in

## List libraries which are to be built.
lib_LTLIBRARIES = libjniast.la

## Declare the sources for the library.
libjniast_la_SOURCES = $(PRIVATE_INCLUDES) $(C_ROUTINES)

## If using Fortran version of SLALIB we need @FCLIBS@. LIBADD is defined by
## the caller (build.xml file). The jniast_libraries script should return the
## full names of any static libraries that are to be included in the final
## shareable library. This forces libtool to include them (and issue some
## warnings that this isn't a good idea).
libjniast_la_LIBADD = $(LIBADD) `$(srcdir)/jniast_libraries` @FCLIBS@

## Stop `$(srcdir)/jniast_libraries` being used as dependency.
libjniast_la_DEPENDENCIES =

## Extra includes defined by the caller (build.xml file).
INCLUDES = $(INCADD)

## We want to dlopen this library, so use -module. It's difficult to deal 
## with versioning from Java, so don't have any -avoid-version (gets the 
## file libjniast.so under UNIX). 
libjniast_la_LDFLAGS = -module -avoid-version

PRIVATE_INCLUDES = err_jniast.h  jniast.h

C_ROUTINES = \
   jniast.c \
   err_jniast.c \
   AstException.c \
   AstObject.c \
   Channel.c \
   CmpFrame.c \
   CmpMap.c \
   FitsChan.c \
   Frame.c \
   FrameSet.c \
   GrismMap.c \
   IntraMap.c \
   LutMap.c \
   Mapping.c \
   MathMap.c \
   MatrixMap.c \
   PcdMap.c \
   PermMap.c \
   Plot.c \
   PolyMap.c \
   SkyFrame.c \
   SlaMap.c \
   ShiftMap.c \
   SpecFrame.c \
   SpecMap.c \
   SphMap.c \
   TranMap.c \
   UnitMap.c \
   WcsMap.c \
   WinMap.c \
   ZoomMap.c \
   GrfEscape.c

## If we switched to versioning the shareable library then the actual name 
## could be determined by the following code. Would need a method to get this
## out to Java.
##getdlname = grep 'dlname=' libjniast.la | sed -e 's:dlname=::' -e s:\'::g
##
##edit = sed \
##        -e "s,@DLNAME\@,$$DLNAME,g"
