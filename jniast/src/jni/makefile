
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

#  Pathname of AST installation tree.

STARLINK_AST = $(STARLINK)

#  Pathnames of Starlink sub-directories that may be referenced when
#  building this package.

STAR_BIN = $(STARLINK)/bin
STAR_INC = $(STARLINK)/include
STAR_LIB = $(STARLINK)/lib
STAR_SHARE = $(STARLINK)/share

#  Default macros for compiling C source code.

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

PKG_NAME = jniast

#  Script detailing library dependencies of this package.

LINK_SCRIPT = $(PKG_NAME)_libraries

#  Name of the target shared library.

SHARED_OBJECT = $(TARGET_LIB)/lib$(PKG_NAME)$(SHARE)



#  C source files.

C_ROUTINES = \
   jniast.c \
   err_jniast.c \
   AstException.c \
   AstObject.c \
   Box.c \
   Channel.c \
   Circle.c \
   CmpFrame.c \
   CmpMap.c \
   CmpRegion.c \
   DSBSpecFrame.c \
   Ellipse.c \
   FitsChan.c \
   FluxFrame.c \
   Frame.c \
   FrameSet.c \
   GrismMap.c \
   Interval.c \
   IntraMap.c \
   KeyMap.c \
   LutMap.c \
   Mapping.c \
   MathMap.c \
   MatrixMap.c \
   NullRegion.c \
   PcdMap.c \
   PermMap.c \
   Plot.c \
   PointList.c \
   Polygon.c \
   PolyMap.c \
   Prism.c \
   RateMap.c \
   Region.c \
   SelectorMap.c \
   SkyFrame.c \
   SlaMap.c \
   ShiftMap.c \
   SpecFluxFrame.c \
   SpecFrame.c \
   SpecMap.c \
   SphMap.c \
   Stc.c \
   StcCatalogEntryLocation.c \
   StcObsDataLocation.c \
   StcResourceProfile.c \
   StcSearchLocation.c \
   SwitchMap.c \
   TimeFrame.c \
   TimeMap.c \
   TranMap.c \
   UnitMap.c \
   WcsMap.c \
   WinMap.c \
   ZoomMap.c \
   GrfEscape.c \
   bdpthread.c


#  List of object files produced by compiling the source code and rules
#  for performing the compilations.

OBJECT_FILES = $(C_ROUTINES:.c=.o)

.c.o:
	$(CC) $(CFLAGS) -c $<


#  List of shareable library files to be built and rules for building
#  them. The third argument to $(BLD_SHR) should provide the information
#  necessary to link any libraries called by this package.
#  We need to include all of NDF and AST and there dependencies
#  (it may be possible to these from the JNI layer, but this isn't done
#  at present). These are all expected to be in $(STAR_LIB) or
#  $(ADDLIBS). See the "jniast_libraries" script for details.

shared: $(SHARED_OBJECT)

$(SHARED_OBJECT): $(OBJECT_FILES) $(LINK_SCRIPT)
	- mkdir temp
	(files=`./$(LINK_SCRIPT)`; \
           cd temp; \
           for f in $$files ""; do \
           echo "Processing $$f"; \
           if test -n "$$f"; then \
              ar x $$f; \
           fi;\
        done)
	$(BLD_SHR) $@ '$(OBJECT_FILES) temp/*.o' '';\
	rm -r -f temp


