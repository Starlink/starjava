/*
 * $Id: SelectionPane.java,v 1.23 2001/11/27 02:10:19 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.demo;

import diva.canvas.*;
import diva.canvas.event.*;
import diva.canvas.interactor.*;
import diva.canvas.toolbox.*;
import diva.util.java2d.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.geom.*;

import diva.sketch.toolbox.PanZoomController;

/** A pane that illustrates selection.
 *
 * @author John Reekie
 * @version $Revision: 1.23 $
 */
public class SelectionPane extends GraphicsPane {

    /** The controller
     */
    PanZoomController controller;

    /** The interactor to give to all figures
     */
    SelectionInteractor selectionInteractor;

    /** The layer to draw all figures in
     */
    FigureLayer figureLayer;

    public SelectionPane() {
        super();

        // Get the figure layer
        figureLayer = getForegroundLayer();

        // Construct a simple controller and get the default interactor
        controller = new PanZoomController();
        controller.setSketchPane(this);
        selectionInteractor = controller.getSelectionInteractor();

	// This bit of code will get you handles on vertices
        // of non-rectangular figures, and handles on the bounds
        // of everything else.
        //PathManipulatorFactory manipFactory = new PathManipulatorFactory();
        //manipFactory.setBoundsIfRectangular(true);
        //SelectionRenderer selectionRenderer = new ReshapeSelectionRenderer(
        //        manipFactory);

	// This bit of code will get you bounding box resizers
        //SelectionRenderer selectionRenderer = new BasicSelectionRenderer(
        //        new BoundsManipulator());
        Manipulator manipulator = new BoundsManipulator();

        // Tell the controller to wrap copies of this manipulator
        controller.setSelectionManipulator(manipulator);

        // Draw something
        drawFigures();
     }
    
    /** Draw some figures on it
     */
    public void drawFigures() {
        /**
         * Display some rectangles
         */
        Figure r2 = new BasicRectangle(10.0, 100.0, 50.0, 50.0,
					Color.blue, 2.0f);
        figureLayer.add(r2);
        r2.setInteractor(selectionInteractor);

        /**
         * Display some ellipses
         */
        Figure e2 = new BasicEllipse(80.0, 30.0, 30.0, 50.0,
					Color.magenta, 2.0f);
        figureLayer.add(e2);
        e2.setInteractor(selectionInteractor);

        // Here's a label
        LabelFigure label = new LabelFigure("Hello!");
        label.translate(200,200);
        figureLayer.add(label);
        label.setInteractor(selectionInteractor);

	/**
	 * Display a star.
	 */
        Polygon2D p = new Polygon2D.Double();
        p.moveTo(- 50.0f, - 12.0f);
        p.lineTo(+ 50.0f, - 12.0f);
        p.lineTo(- 25.0f, + 50.0f);
        p.lineTo(+ 0.0f, - 50.0f);
        p.lineTo(+ 25.0f, + 50.0f);
        p.closePath();
    
        // translate origin towards center of canvas -- tedious!
        AffineTransform at = new AffineTransform();
        at.translate(250.0f, 20.0f);
        p.transform(at);

        BasicFigure star = new BasicFigure(p);
        star.setLineWidth(2.0f);
        star.setStrokePaint(Color.blue);

        figureLayer.add(star);
	star.setInteractor(selectionInteractor);

        // Display a cloud, Claude
        Shape area = ShapeUtilities.createCloudShape();

	AffineTransform cat = new AffineTransform();
        cat.translate(400,300);
        cat.scale(1.5,1.5);
        Shape bigarea = cat.createTransformedShape(area);

        BasicFigure cloud = new BasicFigure(bigarea, Color.green);
        cloud.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
        
        figureLayer.add(cloud);
	cloud.setInteractor(selectionInteractor);
        
         // Draw a translucent object
        GeneralPath s = new GeneralPath(GeneralPath.WIND_NON_ZERO);
        s.moveTo(10,10);
        s.quadTo(100,0,100,90);
        s.quadTo(40,120,20,80);
        s.quadTo(20,20,10,10);
        s.closePath();
  
        AlphaComposite ac =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.6f);

        BasicFigure blob = new BasicFigure(s, Color.red);
        blob.setComposite(ac);
        blob.translate(300,350);
        figureLayer.add(blob);
        blob.setInteractor(selectionInteractor);

       /**
         * Display a transformed composite figure. The top-level _and_ the
         * contained items respond to the mouse.
	 */
	CompositeFigure tc = new CompositeFigure();
	Figure bg = new BasicRectangle(100.0, 100.0, 100.0, 100.0,
				  Color.green);
	Figure p1 = new BasicEllipse(150.0, 100.0, 20.0, 20.0, Color.red);
	p1.translate(-10,-10);
	Figure p2 = new BasicEllipse(200.0, 150.0, 20.0, 20.0, Color.blue);
	p2.translate(-10,-10);
	Figure p3 = new BasicEllipse(150.0, 200.0, 20.0, 20.0, Color.yellow);
	p3.translate(-10,-10);
	Figure p4 = new BasicEllipse(100.0, 150.0, 20.0, 20.0, Color.magenta);
	p4.translate(-10,-10);

	figureLayer.add(tc);
	tc.add(bg);
	tc.add(p1);
	tc.add(p2);
	tc.add(p3);
	tc.add(p4);

	// The background doesn't respond to events, but the whole thing does
	tc.setInteractor(selectionInteractor);
	p1.setInteractor(selectionInteractor);
	p2.setInteractor(selectionInteractor);
	p3.setInteractor(selectionInteractor);
	p4.setInteractor(selectionInteractor);
    }

    /** Get the selection interactor
     */
    public SelectionInteractor getSelectionInteractor() {
        return selectionInteractor;
    }
}


