/*
 * $Id: ZoomDemo.java,v 1.23 2001/08/27 22:07:26 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.demo;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasPane;
import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;
import diva.canvas.PaneWrapper;

import diva.canvas.event.MouseFilter;
import diva.canvas.event.EventLayer;
import diva.canvas.event.LayerMouseAdapter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.DragInteractor;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.BasicRectangle;
import diva.canvas.toolbox.BasicController;

import diva.gui.BasicFrame;

import java.awt.AlphaComposite;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import java.util.Iterator;

import diva.sketch.SketchPane;
import diva.sketch.toolbox.PanZoomController;
import diva.sketch.SketchController;

/** A class for experimenting with zooming on a pane.
 *
 * @author John Reekie
 * @version $Revision: 1.23 $
 */
public class ZoomDemo {
    /**
     *The name of the training file that contains "P" and "Z"
     *gestures.
     */
    public static final String PZ_TRAINING_DATA = "pz.tc";

    // The JCanvas
    private JCanvas _canvas;

    // The GraphicsPane
    private GraphicsPane _graphicsPane;

    // The transform it contains
    private AffineTransform _transform;
 
    // The controller
    private PanZoomController _controller;

    // The default figure interactor
    private Interactor _defaultInteractor;

    /** Create a JCanvas and put it into a window.
     */
    public ZoomDemo () {
        _canvas = new JCanvas();
        _graphicsPane = (GraphicsPane)_canvas.getCanvasPane();
        _transform = _graphicsPane.getTransformContext().getTransform();

        BasicFrame frame = new BasicFrame("Figure tutorial", _canvas);
        frame.setSize(600,400);
        frame.setVisible(true);
    }

    /** Create a couple of figures. Make them draggable.
     */
    public void createFigures () {
        FigureLayer layer = _graphicsPane.getForegroundLayer();

        // Create the pan-zoom controller
        _controller = new PanZoomController();
        _controller.setSketchPane(_graphicsPane);
        _defaultInteractor = _controller.getSelectionInteractor();

        // A rectangle
        Figure blue = new BasicRectangle(10.0,10.0,50.0,50.0, Color.blue);
        layer.add(blue);
        blue.setInteractor(_defaultInteractor);
      
        // An ellipse
        Figure red = new BasicEllipse(100.0,100.0,100.0,50.0, Color.red);
        layer.add(red);
        red.setInteractor(_defaultInteractor);

        // A warping figure
        Image img = Toolkit.getDefaultToolkit().getImage("surfing.gif");
        MediaTracker tracker = new MediaTracker(_canvas);
        tracker.addImage(img,0);
        try {
            tracker.waitForID(0);
        }
        catch (InterruptedException e) {
            System.err.println(e + "... in LayerImageFigure");
        }
        WarpImageFigure wif = new WarpImageFigure(img);
        layer.add(wif);
        wif.setInteractor(_defaultInteractor);

        SketchController sc = new SketchController();
        GraphicsPane ssp = new GraphicsPane();
        sc.setSketchPane(ssp);
        PaneWrapper pw = new PaneWrapper(ssp);
        Figure pink = new BasicRectangle(30.0,30.0,200.0,200.0, Color.pink);
        pw.setBackground(pink);
        pw.setInteractor(null);//_defaultInteractor);
        layer.add(pw);
    }

    /** Main function
     */
    public static void main (String argv[]) {
        ZoomDemo ex = new ZoomDemo();
        ex.createFigures();
    }
}



