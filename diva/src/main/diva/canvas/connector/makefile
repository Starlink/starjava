#
# $Id: makefile,v 1.35 2002/09/26 08:18:10 johnr Exp $
#
# Makefile for diva.canvas
#
DIVA  = ../..
include $(DIVA)/diva.mk

# Java files in this directory
JSRCS = \
	AbstractConnector.java \
	AbstractConnectorTarget.java \
	ArcConnector.java \
	ArcManipulator.java \
	ArcMidpointSite.java \
	ArcInteractor.java \
	Arrowhead.java \
	AutonomousSite.java \
	Blob.java \
	BoundsSite.java \
	CenterSite.java \
	CenterTarget.java \
	Connector.java \
	ConnectorAdapter.java \
	ConnectorEnd.java \
	ConnectorEvent.java \
	ConnectorInteractor.java \
	ConnectorListener.java \
	ConnectorManipulator.java \
	ConnectorTarget.java \
	FixedNormalSite.java \
	ManhattanConnector.java \
	ManhattanRouter.java \
	NullSite.java \
	ParametricSite.java \
	PerimeterSite.java \
	PerimeterTarget.java \
	StraightConnector.java \
	StraightTerminal.java \
	Terminal.java \
	TerminalFigure.java

#	Router.java \
#	ManhattanRouter.java \
#	IncrManhattanRouter.java \
#	BasicManhattanRouter.java

# Include the compilation rules
include $(DIVA)/compile.mk

test: ManhattanRoute.class
	$(JAVA) -classpath $(CLASSPATH) diva.canvas.connector.ManhattanRoute

### DEPENDENCIES: DO NOT EDIT BELOW THIS LINE
