/*
 * $Id: FigureDemo.java,v 1.7 2002/01/04 04:14:18 johnr Exp $
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

import diva.graph.*;
import diva.graph.basic.*;
import diva.graph.layout.*;

import diva.util.java2d.*;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*; 
import javax.swing.event.*;

/** A demonstration of the Canvas class. It creates a
 * pane with some figures, and then creates a couple more
 * panes and plonks them inside the first one. See the
 * Demo page of the Diva documentation for more info.
 *
 * @author John Reekie
 * @version $Revision: 1.7 $
 */
public class FigureDemo {

    // The main pane, which contains reshapeable figures
    private SelectionPane mainPane;

    // A simple embedded pane containing figure that can be
    // dragged around
    private SimplePane simplePane;

    // An embedded pane containing a graph editor.
    private GraphPane graphPane;

    // The demo name
    private String name;

    // The top-level window
    private MainWindow window;

    // The canvas
    private JCanvas canvas;

    /** 
     * Constructor
     */
    public FigureDemo (String name) {

        // The main pane
        mainPane = new SelectionPane();
        
        // The simple pane
        createSimplePane();

        // The graph pane
        createGraphPane();

        // The main frame
        window = new MainWindow(name);
        window.setLocation(20,20);
        window.setSize(800,600);

        // Create the canvas with the main pane and add it to the window
        canvas = new JCanvas(mainPane);
        canvas.setPreferredSize(new Dimension(800,400));
        // canvas.setSize(800,400);
        window.setCanvas(canvas);

        // Show the window
        window.show();
    }

   /** Create the embedded graph editor pane and add it to the foreground
    * layer.
    */
    public final void createGraphPane () {
        // Do it all again for the graph pane.
        final BasicGraphModel graphmodel = new BasicGraphModel();
	final BasicGraphController controller = new BasicGraphController();
	graphPane = new GraphPane(controller, graphmodel);
	graphPane.setSize(300.0,300.0);
	PaneWrapper graphWrapper = new PaneWrapper(graphPane);

        // Display a cloud, Claude
        Shape area = ShapeUtilities.createCloudShape();

        AffineTransform cat = new AffineTransform();
        cat.translate(-50,-50);
        cat.scale(4.0,4.0);
        Shape bigarea = cat.createTransformedShape(area);

        BasicFigure cloud = new BasicFigure(bigarea, Color.blue);
        cloud.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.3f));
	graphWrapper.setBackground(cloud);
        
        // Scale the inner graph by 0.5
	AffineTransform graphTransform = new AffineTransform();
	graphTransform.translate(320.0,50.0);
	graphTransform.scale(0.75,0.75);
	graphWrapper.transform(graphTransform);

        // Add the wrapped graph to the layer
	FigureLayer layer = mainPane.getForegroundLayer();
	layer.add(graphWrapper);
        
        // Make it selectable.
        graphWrapper.setInteractor(mainPane.getSelectionInteractor());

	// Lay it out and create a graph
        // graphPane.setIncrLayout(new RandomIncrLayout(graphPane));
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    initDAG(graphmodel);
                }
            });
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

   /** Create the simple embedded pane and add it to the foreground
    * layer.
    */
    public final void createSimplePane () {
	// Create a pane and a wrapper figure. For now, let's
	// just guess at the pane size
	simplePane = new SimplePane();
	simplePane.setSize(300,300);
	PaneWrapper paneWrapper = new PaneWrapper(simplePane);

       // Display a swatch, sweetie
        Shape area = ShapeUtilities.createSwatchShape();

        AffineTransform sat = new AffineTransform();
        sat.translate(-50,-50);
        sat.scale(5.0,5.0);
        Shape bigarea = sat.createTransformedShape(area);

        BasicFigure bg = new BasicFigure(bigarea, Color.magenta);
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.2f));

        paneWrapper.setBackground(bg);

        // Scale the inner pane by 0.5
	AffineTransform paneTransform = new AffineTransform();
	paneTransform.translate(50.0,300.0);
	paneTransform.scale(0.5,0.5);
	paneWrapper.transform(paneTransform);

        // Add the wrapped pane to the layer
	FigureLayer layer = mainPane.getForegroundLayer();
	layer.add(paneWrapper);
 
        // Make it selectable.
        paneWrapper.setInteractor(mainPane.getSelectionInteractor());
    }

    /** Get the canvas.
     */
    public JCanvas getJCanvas() {
        return canvas;
    }

    /**
     * Run it
     */
    public static void main (String argv[]) {
        // Set the system look-and-feel
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't use the cross-platform "
                    + "look and feel: " + e);
        }

        // Create myself and display
        new FigureDemo("Canvas Demo, 0.1");
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
        m.addNode(this, cn, root);
        m.addNode(this, dn, root);
        m.addNode(this, en, root);
        m.addNode(this, fn, root);

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
}




