#
# $Id: makefile,v 1.5 2001/11/27 09:12:25 johnr Exp $
#
# Makefile for diva APIs
#
DIVA  = ../..
include $(DIVA)/diva.mk

## Add the tools jar file to the classpath
#CP = "$(CLASSPATH)$(CLASSPATHSEPARATOR)$(JAVAHOME)/lib/tools.jar"
CP = $(CLASSPATH)$

# Build the API docs. Javadoc flags are defined in diva/diva.mk
install:
	$(JAVADOC) -version -classpath $(CP) $(JAVADOC_FLAGS) \
	 	$(PACKAGES) \
		$(UNRELEASED_PACKAGES)

# Clean very viciously
clean:

#clean:
#	@echo "Removing all javadocs"
#	@if [ -r diva ]; then rm -rf diva *.html; fi
#	@cp index.default.html index.html

