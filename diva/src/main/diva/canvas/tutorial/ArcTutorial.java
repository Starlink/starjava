/*
 * $Id: ArcTutorial.java,v 1.6 2000/05/22 17:07:24 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.tutorial;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasPane;
import diva.canvas.GraphicsPane;
import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.JCanvas;
import diva.canvas.Site;

import diva.canvas.connector.ArcConnector;
import diva.canvas.connector.ArcManipulator;
import diva.canvas.connector.Arrowhead;
import diva.canvas.connector.Blob;
import diva.canvas.connector.Connector;
import diva.canvas.connector.ConnectorEvent;
import diva.canvas.connector.ConnectorListener;
import diva.canvas.connector.ConnectorManipulator;
import diva.canvas.connector.ConnectorTarget;
import diva.canvas.connector.StraightConnector;
import diva.canvas.connector.PerimeterSite;
import diva.canvas.connector.PerimeterTarget;
import diva.canvas.connector.ManhattanConnector;

import diva.canvas.event.LayerAdapter;
import diva.canvas.event.LayerEvent;
import diva.canvas.event.LayerListener;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.interactor.BasicSelectionRenderer;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionRenderer;

import diva.canvas.toolbox.BasicRectangle;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.BasicController;
import diva.canvas.toolbox.TypedDecorator;

import diva.gui.BasicFrame;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;


/** Another example showing how to use Connectors. In this example,
 * the connectors are atached to "perimeter sites" -- that is,
 * sites that can relocated themselves to maintain themselves
 * on the perimeter of an object. Unlike the first connector
 * example, this one does not need to create a special kind of
 * figure, as perimeter sites can attach to any figure that
 * has a rectangle or circle shape.
 *
 * <p> To make this example a little more interesting, selected
 * figures have resize handles attached to them. As the figure
 * is resived, attached connectors change accordingly. This
 * tutorial also illustrates the use of the TypedDecorator
 * class to attach different kinds of manipulators to different
 * kinds of figures (in this case, different kinds of connectors).
 *
 * @author John Reekie
 * @version $Revision: 1.6 $
 */
public class ArcTutorial {

    // The JCanvas
    private JCanvas canvas;

    // The GraphicsPane
    private GraphicsPane graphicsPane;

    /** The controller
     */
    private BasicController controller;

    /** The two figures
     */
    private Figure figureA;
    private Figure figureB;
    private Figure figureC;

    /** The connectors
     */
    private StraightConnector connectorA;
    private ArcConnector connectorB;
    private ArcConnector connectorC;

    /** The target that finds sites on the figures
     */
    private ConnectorTarget target;

    /** Create a JCanvas and put it into a window.
     */
    public ArcTutorial () {
        canvas = new JCanvas();
        graphicsPane = (GraphicsPane)canvas.getCanvasPane();

	BasicFrame frame = new BasicFrame("Connector tutorial", canvas);
        frame.setSize(600,400);
        frame.setVisible(true);

        controller = new BasicController(graphicsPane);
   }

    /** Create the figures that we will draw connectors between.
     * This is fairly uninteresting.
     */
    public void createFigures () {
        FigureLayer layer = graphicsPane.getForegroundLayer();

        figureA = new BasicRectangle(10.0,10.0,100.0,50.0,Color.red);
        figureB = new BasicEllipse(100.0,100.0,100.0,100.0,Color.green);
        figureC = new BasicEllipse(300.0,100.0,100.0,100.0,Color.blue);

        layer.add(figureA);
        layer.add(figureB);
        layer.add(figureC);
    }

    /**
     * Create the connectors between the two figures. We will firstly
     * create one StraightConnector with a circle and an arrowhead
     * on it, and then an ArcConnector.
     */
    public void createConnectors () {
        FigureLayer layer = graphicsPane.getForegroundLayer();

        // Create the target that finds sites on the figures
        target = new PerimeterTarget();

        // Create the first connector. We don't care about the actual
        // location at this stage
        Site a = target.getTailSite(figureA, 0.0, 0.0);
        Site b = target.getHeadSite(figureB, 0.0, 0.0);
        connectorA = new StraightConnector(a, b);
        layer.add(connectorA);

        // Add an arrowhead to it
        Arrowhead arrow = new Arrowhead(b.getX(), b.getY(), b.getNormal());
        connectorA.setHeadEnd(arrow);

        // Create the second connector with an arrowhead
        a = target.getTailSite(figureB, 0.0, 0.0);
        b = target.getHeadSite(figureC, 0.0, 0.0);
        connectorB = new ArcConnector(a, b);
        layer.add(connectorB);
        arrow = new Arrowhead(b.getX(), b.getY(), b.getNormal());
        connectorB.setHeadEnd(arrow);

        // Create the third connector with an arrowhead
        a = target.getTailSite(figureB, 0.0, 0.0);
        b = target.getHeadSite(figureC, 0.0, 0.0);
        connectorC = new ArcConnector(a, b);
        // Swap the direction
        connectorC.setAngle(-connectorC.getAngle());
        layer.add(connectorC);
        arrow = new Arrowhead(b.getX(), b.getY(), b.getNormal());
        connectorC.setHeadEnd(arrow);
    }

    /**
     * Set up the interaction so that the connectors stay glued to
     * the two figures. Since BasicController has already set up
     * an interactor for us, we will attach a listener to the drag
     * interactor and just call the connectors to re-route whenever
     * the nmouse moves.
     */
    public void setupInteraction () {
	// Because this pane has connectors on it, we make the pick
	// halo larger than the default so we can click-select connectors
	FigureLayer layer = graphicsPane.getForegroundLayer();
	layer.setPickHalo(2.0);
    
        // Add the default interactor to both figures
        SelectionInteractor si = controller.getSelectionInteractor();
        figureA.setInteractor(si);
        figureB.setInteractor(si);
        figureC.setInteractor(si);

        // Add a layer listener to the interactor attached to that role.
        // The listener just tells both connectors to reroute themselves.
        DragInteractor i = controller.getDragInteractor();
        i.addLayerListener(new LayerAdapter () {
            public void mouseDragged (LayerEvent e) {
                connectorA.reroute();
                connectorB.reroute();
                connectorC.reroute();
            }
        });

        // The connector selection interactor uses the same selection model
        SelectionInteractor ci = new SelectionInteractor(si.getSelectionModel());
        connectorA.setInteractor(ci);
        connectorB.setInteractor(ci);
        connectorC.setInteractor(ci);

        // Tell the selection dragger to select connectors too
        controller.getSelectionDragger().addSelectionInteractor(ci);

        // Create a manipulator to give resize handles on figures
        BoundsManipulator figureManipulator = new BoundsManipulator();
        controller.setSelectionManipulator(figureManipulator);

        // Make resizing reroute the connectors too
        DragInteractor j = figureManipulator.getHandleInteractor();
        j.addLayerListener(new LayerAdapter () {
            public void mouseDragged (LayerEvent e) {
                connectorA.reroute();
                connectorB.reroute();
                connectorC.reroute();
            }
        });

        // Create and set up the manipulators for connectors. Straight
        // connectors will have an instance of ConnectorManipulator
        // attached to them, while arc connectors will have an instance
        // of ArcManipulator attached to them.
        ConnectorManipulator cManipulator = new ConnectorManipulator();
        cManipulator.setSnapHalo(4.0);
        cManipulator.setConnectorTarget(target);

        ArcManipulator aManipulator = new ArcManipulator();
        aManipulator.setSnapHalo(4.0);
        aManipulator.setConnectorTarget(target);

        TypedDecorator typedDecorator = new TypedDecorator();
        typedDecorator.addDecorator(StraightConnector.class, cManipulator);
        typedDecorator.addDecorator(ArcConnector.class, aManipulator);

        ci.setPrototypeDecorator(typedDecorator);

        // In ConnectorTutorial, we used connector listeners to
	// illustrate notification call-backs from the manipulator.
	// If we were to do that here, we would need a different listener
	// for each manipulator. We won't do it, once is enough.
    }

    /** Main function
     */
    public static void main (String argv[]) {
        ArcTutorial ex = new ArcTutorial();
        ex.createFigures();
        ex.createConnectors();
        ex.setupInteraction();
    }
}



