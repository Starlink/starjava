# ESO Archive
# 
# $Id: Makefile,v 1.12 2002/08/08 22:19:19 gillies Exp $
# 
# who             when        what
# --------------  ----------  ----------------------------------------
# Allan Brighton  1999/12/08  Created

# Source files to compile
SOURCES = \
	JSkyCatVersion.java \
	JSkyCatMenuBar.java \
	JSkyCatRemoteControl.java \
	JSkyCat.java

# Java package name
PACKAGE_DIR = jsky/app/jskycat

# Resources to be installed in the package directory
PACKAGE_RESOURCES = \
	i18n/gui.properties \
	i18n/gui_gb.properties

# Path to top level dir
TOP = ../../../..

# include master makefile
include $(TOP)/conf/master.mk

JSkyCatVersion.java: $(TOP)/VERSION
	v=`cat $(TOP)/VERSION`; \
	echo "package jsky.app.jskycat;" > JSkyCatVersion.java ;\
	echo "public class JSkyCatVersion {public static final String JSKYCAT_VERSION = \"$$v\";}" >> JSkyCatVersion.java

# image file to test with
IMAGE = ../../image/gui/test/ngc1275.fits

# tests
test: test1

test1 JSkyCat: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat $(IMAGE)

test1a: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat -nointernalframes $(IMAGE)

test1b: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat $(IMAGE) -shownavigator 

test1c: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat -internalframes $(IMAGE) -shownavigator

test1d: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat

test1e: all
	java $(JVMFLAGS) -native -Xnoclassgc -Xrunoii intuitive.audit.Audit jsky.app.jskycat.JSkyCat bigdss.fits

test1f: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat -port 9624


test1g: all
	java $(JVMFLAGS) -Duser.language=de jsky.app.jskycat.JSkyCat $(IMAGE)

test1h: all
	java $(JVMFLAGS) -Duser.language=gb jsky.app.jskycat.JSkyCat $(IMAGE)

test1i: all
	java $(JVMFLAGS) -Duser.language=fr jsky.app.jskycat.JSkyCat $(IMAGE)

test1j: all
	java $(JVMFLAGS) -Djsky.catalog.directory="jsky.catalog.skycat.SkycatConfigFile" jsky.app.jskycat.JSkyCat $(IMAGE)

test1k: all
	java $(JVMFLAGS) -Djsky.catalog.irsa.config="http://irsa.ipac.caltech.edu/cgi-bin/Oasis/CatList/nph-catlist" jsky.app.jskycat.JSkyCat $(IMAGE)

test1l: all
	java $(JVMFLAGS) jsky.app.jskycat.JSkyCat -tilecache 128

