#
#  Makefile to build the FROG JNI shared library.
#  Based on the standard FROG package makefile.

#  Defaults.
#  ========

#  Pathname of the root directory beneath which other Starlink software
#  is currently installed. Used to locate include files and static
#  libraries.

STARLINK = /star

#  Starlink include directory.

STAR_INC=$(STARLINK)/include

#  Starlink libraries directory.

STAR_LIB=$(STARLINK)/lib

#  Directory to put the shareable library. This is usually it's place
#  in the local distribution system.

TARGET_LIB = ../../lib

#  List of local shareable libraries that we need to resolve against.
#  Use a list that of search directories and library names like:
#    -L/standardplace -L/localplace1 -ljniast -L/localplace2 -ljnihds

JAVA_SHAREABLES = 

#  Default macros for compiling C source code.

CC = cc
CFLAGS = -O

#  Default file type extension for a shareable library and command for
#  building a shareable library (the default acts as a null command).

SHARE = .so
BLD_SHR = :

#  Java specific platform name (used for locating JNI include files).

JAVA_SYSTEM = unknown

#  Home for the Java development system needed to compile any JNI
#  code. Note this rule assumes this is installed in the Starlink
#  tree, this is unlikely as JDKs may not be distributed and this
#  place normally only contains a JRE. 

JAVA_HOME = $(STARLINK)/java/jdk

#  Include directories for the Java development system. Notes from
#  above also apply here (i.e. these directories will not exist on
#  most systems).

JAVA_INCLUDE = $(JAVA_HOME)/include

JAVA_SYSTEM_INCLUDE = $(JAVA_INCLUDE)/$(JAVA_SYSTEM)

.SUFFIXES:
.SUFFIXES: .o .c

.c.o:
	$(CC) $(CFLAGS) -c $<

#  Define source files.
#  ====================

#  Prefix for the package in lower-case as used in filenames etc.

PKG_NAME = frog

#  List of files comprising the required source.

SOURCE_FILES = $(LINK_SCRIPT) $(PRIVATE_INCLUDES) $(C_ROUTINES)

#  Link script.  This script does not form part of the package but is
#  required for building it.  It will not be installed.

LINK_SCRIPT = frog_libraries

#  List of private include files.  These are include files
#  which form part of the package and are required in order to build
#  it, but which are not required by users of it.

PRIVATE_INCLUDES = 

#  C routines required for building the shared library.

C_ROUTINES = Dummy.c

#  List of files which must be built from the source files before the
#  package can be installed for use.  This should comprise all the files
#  that are required to use the package (but excluding the date stamp
#  file).

BUILT_FILES = $(SHAREABLE_LIBRARY)

#  List of object files produced by compiling the source code.

OBJECT_FILES = $(C_ROUTINES:.c=.o)

#  The shareable library file to be built and rules for building it.
#  The third argument to $(BLD_SHR) should provide the information
#  necessary to link any libraries called by this package. For FROG
#  we need to include all of NDF and its AST dependencies. These are
#  all expected to be in $(STAR_LIB) or $(ADDLIBS). See the
#  "frog_libraries" script for details.

SHAREABLE_LIBRARY = $(TARGET_LIB)/lib$(PKG_NAME)$(SHARE)

$(SHAREABLE_LIBRARY): $(C_ROUTINES) $(OBJECT_FILES) $(LINK_SCRIPT)
	- mkdir temp
	(cd temp; \
	   for f in `../$(LINK_SCRIPT)` ""; do \
	      echo "Processing $$f"; \
	      if test -n "$$f"; then \
	         ar x $$f; \
	      fi;\
	done)
	$(BLD_SHR) $@ '$(OBJECT_FILES) temp/*.o $(JAVA_SHAREABLES)' '';\
	rm -r -f temp

#  build: build the shareable library.

build: $(BUILT_FILES)

#  clean: clean up the source directory.

clean:
	@- rm -f $(OBJECT_FILES)

#  unbuild: perform a clean and also remove the shareable library

unbuild: clean
	@- rm -f $(SHAREABLE_LIBRARY)

#  Include file dependencies.

Dummy.o: Dummy.h
