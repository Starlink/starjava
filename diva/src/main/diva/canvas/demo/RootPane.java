/*
 * $Id: RootPane.java,v 1.9 2001/07/22 22:00:33 johnr Exp $
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

import diva.graph.*;
import diva.graph.basic.*;
import diva.graph.layout.*;
import diva.graph.toolbox.*;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.geom.*;

import diva.sketch.toolbox.PanZoomController;

/** A pane that illustrates selection.
 *
 * @author John Reekie
 * @version $Revision: 1.9 $
 */
public class RootPane extends GraphicsPane {

    /** The controller
     */
    PanZoomController controller;

    /** The interactor to give to all figures
     */
    SelectionInteractor selectionInteractor;

    /** The layer to draw all figures in
     */
    FigureLayer figureLayer;

    // An embedded pane containing a graph editor.
    private GraphPane graphPane;

    // A pane containing a bubble-and-arc editor.
    private BubblePane bubblePane;

    public RootPane() {
        super();

        // Get the figure layer
        figureLayer = getForegroundLayer();

        // Construct a simple controller and get the default interactor
        controller = new PanZoomController();
        controller.setSketchPane(this);
        selectionInteractor = controller.getSelectionInteractor();

	// Use bounds resizers
        Manipulator manipulator = new BoundsManipulator();

        // Tell the controller to wrap copies of this manipulator
        controller.setSelectionManipulator(manipulator);

        // Draw the graph pane
        createGraphPane();

        // Draw the bubble pane
        createBubblePane();
    }

   /** Create the embedded graph editor pane and add it to the foreground
    * layer.
    */
    public final void createGraphPane () {
        // Do it all again for the graph pane.
        final BasicGraphModel graphModel = new BasicGraphModel();
	final BasicGraphController graphController = 
	    new BasicGraphController();
	graphPane = new GraphPane(graphController, graphModel);
	graphPane.setSize(300.0, 300.0);
	PaneWrapper graphWrapper = new PaneWrapper(graphPane);

	new LevelLayout(new BasicLayoutTarget(graphController)).layout(
				 graphModel.getRoot());

        // Does this work???
        PanZoomController controller = new PanZoomController();
        controller.setSketchPane(graphPane);
       
        // Display a cloud, Claude
        Shape area = ShapeUtilities.createCloudShape();
	AffineTransform cat = new AffineTransform();
        cat.translate(-50,-50);
        cat.scale(4.0,4.0);
        Shape bigarea = cat.createTransformedShape(area);

        BasicFigure cloud = new BasicFigure(bigarea, new Color(255, 200, 200));
        
	graphWrapper.setBackground(cloud);
        
        // Scale the inner graph by 0.5
	AffineTransform graphTransform = new AffineTransform();
	graphTransform.translate(320.0,50.0);
	graphTransform.scale(0.75,0.75);
	graphWrapper.transform(graphTransform);

        // Add the wrapped graph to the layer
	FigureLayer layer = getForegroundLayer();
	layer.add(graphWrapper);
        
        // Make it selectable.
        graphWrapper.setInteractor(selectionInteractor);

	// Lay it out and create a graph
        // graphPane.setIncrLayout(new RandomIncrLayout(graphPane));
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initDAG(graphModel);
                }
            });
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

   /** Create the embedded bubble editor pane and add it to the foreground
    * layer.
    */
    public final void createBubblePane () {
 	// Create a pane and a wrapper figure. For now, let's
	// just guess at the pane size
	bubblePane = new BubblePane();

        PanZoomController controller = new PanZoomController();
        controller.setSketchPane(bubblePane);

	bubblePane.setSize(300,300);
	PaneWrapper paneWrapper = new PaneWrapper(bubblePane);

        // Display a swatch, sweetie
        Shape area = ShapeUtilities.createSwatchShape();

        AffineTransform sat = new AffineTransform();
        sat.translate(-50,-50);
        sat.scale(5.0,5.0);
        Shape bigarea = sat.createTransformedShape(area);

        BasicFigure bg = new BasicFigure(bigarea, new Color(255,200,255));

        paneWrapper.setBackground(bg);

        // Scale the inner pane by 0.5
	AffineTransform paneTransform = new AffineTransform();
	paneTransform.translate(50.0,300.0);
	paneTransform.scale(0.5,0.5);
	paneWrapper.transform(paneTransform);

        // Add the wrapped pane to the layer
	FigureLayer layer = getForegroundLayer();
	layer.add(paneWrapper);
 
        // Make it selectable.
        paneWrapper.setInteractor(getSelectionInteractor());

        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    populateBubble();
                }
            });
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Build a graph that looks like this:
     *
     * <pre>
     *       a
     *      /|\
     *     b | c
     *    / \|/ \
     *   d   e   f
     * </pre>
     */
    private void initDAG(BasicGraphModel m) {
        // Construct the graph
        Object an = m.createNode("a");
        Object bn = m.createNode("b");
        Object cn = m.createNode("c");
        Object dn = m.createNode("d");
        Object en = m.createNode("e");
        Object fn = m.createNode("f");
	Object root = m.getRoot();
        m.addNode(this, an, root);
        m.addNode(this, bn, root);
        m.addNode(this,cn, root);
        m.addNode(this,dn, root);
        m.addNode(this,en, root);
        m.addNode(this,fn, root);

        Object e;
	e = m.createEdge("ab");
	m.connectEdge(this, e, an, bn);
        e = m.createEdge("ac");
	m.connectEdge(this, e, an, cn);
        e = m.createEdge("bd");
	m.connectEdge(this, e, bn, dn);
        e = m.createEdge("be");
	m.connectEdge(this, e, bn, en);
        e = m.createEdge("ae");
	m.connectEdge(this, e, an, en);
        e = m.createEdge("ce");
	m.connectEdge(this, e, cn, en);
        e = m.createEdge("cf");
	m.connectEdge(this, e, cn, fn);
    }

    /**
     * Populate the bubble pane with some simple stuff.
     */
    public void populateBubble () {
        System.out.println(bubblePane);
        BasicGraphModel model = 
	    (BasicGraphModel) bubblePane.getGraphController().getGraphModel();

        Object a = model.createNode("a");
        model.setProperty(a, "label", "Bubble me over");
        model.setProperty(a, "stateType", 
			  Integer.valueOf(StateBubble.FINAL_STATE));

        Object b = model.createNode("b");
        model.setProperty(b, "label", "Rub a dub bubble");
        model.setProperty(b, "stateType", 
			  Integer.valueOf(StateBubble.INITIAL_STATE));

	Object root = model.getRoot();
        model.addNode(this,a, root);
        model.addNode(this,b, root);

        Object x = model.createEdge("x");
        model.setProperty(x, "label", "Wherefore arc thou Romeo?");
        model.connectEdge(this, x, a, b);

        Object y = model.createEdge("y");
        model.setProperty(y, "label", "Don't arcs me!");
        model.connectEdge(this, y, b, a);
    }

    /** Get the selection interactor
     */
    public SelectionInteractor getSelectionInteractor() {
        return selectionInteractor;
    }
}


