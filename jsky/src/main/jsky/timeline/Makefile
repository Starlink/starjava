# Makefile
#
# Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
# Observatory Control System, Gemini Telescopes Project.
#
# $Id: Makefile,v 1.3 2002/08/08 22:40:23 gillies Exp $
#

# Source files to compile
SOURCES = \
	BlockTimeLineNode.java \
	DefaultTimeLineModel.java \
	DefaultTimeLineNode.java \
	DefaultTimeLineNodeModel.java \
	DefaultVetoableTimeLineNodeModel.java \
	TimeLine.java \
	TimeLineModel.java \
	TimeLineNode.java \
	TimeLineNodeModel.java \
	VetoableTimeLineNodeModel.java \
	DetailedPropertyVetoException.java \
	IllegalNodePositionException.java


# Java package name
PACKAGE_DIR = jsky/timeline

# Path to top level dir
TOP = ../../..

# include master makefile
include $(TOP)/conf/master.mk

# tests
test: test1

test1 TimeLine : all
	java $(JVMFLAGS) jsky.timeline.TimeLine
