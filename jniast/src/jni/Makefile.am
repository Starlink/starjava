## Process this file with automake to produce Makefile.in

## List libraries which are to be built, these are not installed by this 
## package, convert does that.
lib_LTLIBRARIES = libjniast.la

## Declare the sources for the library.
libjniast_la_SOURCES = \
 $(PRIVATE_INCLUDES) \
 $(C_ROUTINES)

libjniast_la_LIBADD = $(LIBADD) `$(srcdir)/jniast_libraries`

`$(srcdir)/jniast_libraries`:
	:

INCLUDES = $(INCADD)

## We want to dlopen this library, how to get all libraries resolved into 
## this one?
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

## In order to get the correct name for the dlopened library we need to let
## libtool do it's work and then query the convert_idl.la file for the dlname
## variable. XXX need to get this back out to Java?

getdlname = grep 'dlname=' convert_idl.la | sed -e 's:dlname=::' -e s:\'::g

edit = sed \
        -e "s,@DLNAME\@,$$DLNAME,g"
