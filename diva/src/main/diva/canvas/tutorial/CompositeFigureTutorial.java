/*
 * $Id: CompositeFigureTutorial.java,v 1.13 2000/05/22 17:07:24 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.canvas.tutorial;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasPane;
import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.FigureWrapper;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.Site;
import diva.canvas.TransformContext;
import diva.canvas.CompositeFigure;

import diva.canvas.connector.CenterSite;
import diva.canvas.connector.Connector;
import diva.canvas.connector.StraightConnector;

import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.*;
import diva.canvas.toolbox.*;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.BasicRectangle;

import diva.gui.BasicFrame;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;


/**
 * This tutorial demonstrates how to use composite figures.
 * It instantiates a composite figure which contains a
 * square (which represents the body of a component) and
 * four circles (which represents "ports" on the component).
 * The ports can be moved and scaled independently of the body, and
 * when the body is scaled, the ports are scaled proportionally.
 *
 * @author John Reekie      (johnr@eecs.berkeley.edu)
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.13 $
 * @rating Red
 */
public class CompositeFigureTutorial {
    // The JCanvas
    private JCanvas canvas;

    // The GraphicsPane
    private GraphicsPane graphicsPane;

    // More objects
    FigureLayer layer;
    BasicController controller;
    Interactor defaultInteractor;

    /** Create a JCanvas and put it into a window.
     */
    public CompositeFigureTutorial () {
        canvas = new JCanvas();
        graphicsPane = (GraphicsPane)canvas.getCanvasPane();
        layer = graphicsPane.getForegroundLayer();
        controller = new BasicController(graphicsPane);
        defaultInteractor = controller.getSelectionInteractor();

        BasicFrame frame = new BasicFrame("Composite figure tutorial", canvas);
        frame.setSize(600,400);
        frame.setVisible(true);
    }

    /** Main function
     */
    public static void main (String argv[]) {
        CompositeFigureTutorial ex = new CompositeFigureTutorial();
        ex.createCompositeFigure();
        ex.createBackgroundedCompositeFigure();
        ex.graphicsPane.repaint();
    }
    
    /** Create a composite figure that does not have a background
     */
    public void createCompositeFigure () {
	CompositeFigure tc = new CompositeFigure();
	Figure bg = new BasicRectangle(100.0, 100.0, 100.0, 100.0,
                Color.green);
	tc.add(bg);
        layer.add(tc);   
	tc.setInteractor(defaultInteractor);
        addPorts(tc);
    }

    /** Create a composite figure that uses the background facility.
     * Generally, for figures of this nature, this is a better thing to do.
     */
    public void createBackgroundedCompositeFigure () {
	CompositeFigure tc = new CompositeFigure();
	Figure bg = new BasicRectangle(100.0, 100.0, 100.0, 100.0,
                Color.blue);
        tc.setBackgroundFigure(bg);
        layer.add(tc);   
	tc.setInteractor(defaultInteractor);
        addPorts(tc);
        tc.translate(200.0,0);
    }

    /** Utility function to add the "ports" to the composite figure.
     */
    public void addPorts (CompositeFigure tc) {
	Figure p1 = new BasicEllipse(150.0, 100.0, 20.0, 20.0, Color.red);
	p1.translate(-10,-10);
	Figure p2 = new BasicEllipse(200.0, 150.0, 20.0, 20.0, Color.blue);
	p2.translate(-10,-10);
	Figure p3 = new BasicEllipse(150.0, 200.0, 20.0, 20.0, Color.yellow);
	p3.translate(-10,-10);
	Figure p4 = new BasicEllipse(100.0, 150.0, 20.0, 20.0, Color.magenta);
	p4.translate(-10,-10);

	tc.add(p1);
	tc.add(p2);
	tc.add(p3);
	tc.add(p4);

	p1.setInteractor(defaultInteractor);
	p2.setInteractor(defaultInteractor);
	p3.setInteractor(defaultInteractor);
	p4.setInteractor(defaultInteractor);
    }
}

