#
# $Id: makefile,v 1.3 1999/10/23 17:35:28 johnr Exp $
#
# Makefile for diva APIs
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Set the classpath to include the tools jar file
CLASSPATH = "$(DIVA)/..$(CLASSPATHSEPARATOR)$(DIVA)/lib/aelfred.jar$(CLASSPATHSEPARATOR)$(JAVAHOME)/lib/tools.jar"

# Build the API docs. Javadoc flags are defined in diva/diva.mk
install:
	$(JAVADOC) -version -classpath $(CLASSPATH) $(JAVADOC_FLAGS) \
	 	$(PACKAGES) \
		$(UNRELEASED_PACKAGES)

# Clean very viciously
clean:

#clean:
#	@echo "Removing all javadocs"
#	@if [ -r diva ]; then rm -rf diva *.html; fi
#	@cp index.default.html index.html

