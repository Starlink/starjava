#
# $Id: makefile,v 1.3 1999/11/26 22:57:35 neuendor Exp $
#
# Makefile for diva.resource
#
DIVA  = ..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	DefaultBundle.java \
	RelativeBundle.java

MISC_FILES = Defaults.properties \
	dtd_graph.dtd \
	dtd_sketch.dtd \
	dtd_test.dtd \
	img_copy.gif \
	img_cut.gif \
	img_graphIcon.gif \
	img_new.gif \
	img_newPage.gif \
	img_next.gif \
	img_open.gif \
	img_paste.gif \
	img_previous.gif \
	img_save.gif

# Include the shared compilation rules
include $(DIVA)/compile.mk

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
DefaultBundle.class: \
	DefaultBundle.java \
	RelativeBundle.java

RelativeBundle.class: \
	RelativeBundle.java


