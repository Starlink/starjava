/*
 * $Id: SpaceDemo.java,v 1.8 2001/07/22 22:00:34 johnr Exp $
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
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*; 
import javax.swing.event.*;
import diva.graph.*;

/** A mock-up illustrating the concepts of navigation
 * through an information space. The view of the space consists of
 * hierarchical panes which come into being when they become a certain
 * size in the user's field of view.
 *
 * @author John Reekie
 * @version $Revision: 1.8 $
 */
public class SpaceDemo {

    // The main pane, which contains reshapeable figures
    private RootPane rootPane;

    // A simple embedded pane containing figure that can be
    // dragged around
    //private SimplePane simplePane;
    //private SketchSchematicPane simplePane;

    // An embedded pane containing a graph editor.
    private GraphPane graphPane;

    // The demo name
    private String name;

    // The top-level window
    private SpaceWindow window;

    // The canvas
    private JCanvas canvas;

    /** 
     * Constructor
     */
    public SpaceDemo (String name) {

        // The main pane
        rootPane = new RootPane();
        
        // The simple pane
        //createSimplePane();

        // The graph pane
        //createGraphPane();

        // The main frame
        window = new SpaceWindow(name);
        window.setLocation(20,20);
        window.setSize(800,600);

        // Create the canvas with the main pane and add it to the window
        canvas = new JCanvas(rootPane);
        canvas.setPreferredSize(new Dimension(800,400));
        // canvas.setSize(800,400);
        window.setCanvas(canvas);

        // Show the window
        window.show();
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
        new SpaceDemo("Infospace Demo, 0.1");
    }
}




