/*
 * $Id: SimpleTutorial.java,v 1.9 2000/05/22 17:07:25 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.tutorial;

import diva.canvas.CanvasPane;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicRectangle;

import diva.gui.BasicFrame;

import diva.util.java2d.Polyline2D;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * A simple example showing how to construct a canvas and place
 * some simple figures on it. The JCanvas class contains a GraphicsPane,
 * which in turn contains several layers. In this example, we draw
 * on the <i>foreground layer</i> of the pane.
 *
 * @author John Reekie
 * @version $Revision: 1.9 $
 */
public class SimpleTutorial {

    // The JCanvas
    private JCanvas canvas;

    // The GraphicsPane
    private GraphicsPane graphicsPane;

    /** Create a JCanvas and put it into a window
     */
    public SimpleTutorial () {
        canvas = new JCanvas();
        graphicsPane = (GraphicsPane)canvas.getCanvasPane();

	BasicFrame frame = new BasicFrame("Simple canvas tutorial", canvas);
        frame.setSize(600,400);
        frame.setVisible(true);
    }

    /** Create a rectangle figure. The rectangle is an instance of
     * the BasicRectangle class. This class, together with a number
     * other useful predefined figure classes, is contained in the
     * package <b>diva.canvas.toolbox</b>.
     */
    public void createBasicRectangle () {
        FigureLayer layer = graphicsPane.getForegroundLayer();
        Figure rectangle = new BasicRectangle(50,50,80,80,Color.blue);
        layer.add(rectangle);
    }

    /** Create an odd-shaped figure. The rectangle is an instance of
     * the BasicShape class, which draws itself using any instance
     * of the Java2D interface, <b>java.awt.Shape</b>. In this example,
     * we use an instance of GeneralPath.
     */
    public void createBasicFigure () {
        FigureLayer layer = graphicsPane.getForegroundLayer();
        GeneralPath path = new GeneralPath();
        path.moveTo(120,240);
        path.lineTo(240,240);
        path.quadTo(180,120,120,240);
        path.closePath();
        Figure semi = new BasicFigure(path, Color.green);
        layer.add(semi);
    }

    /** Create a polyline. Again, this uses the BasicFigure class,
     * but this time the shape is an instance of <b>diva.util.Polyline2D</b>.
     */
    public void createPolyline () {
        FigureLayer layer = graphicsPane.getForegroundLayer();
        Polyline2D path = new Polyline2D.Double();
        path.moveTo(240,120);
        path.lineTo(280,140);
        path.lineTo(240,160);
        path.lineTo(280,180);
        path.lineTo(240,200);
        path.lineTo(280,220);
        path.lineTo(240,240);
        Figure line = new BasicFigure(path);
        layer.add(line);
    }

    /** Main function
     */
    public static void main (String argv[]) {
        SimpleTutorial ex = new SimpleTutorial();
        ex.createBasicRectangle();
        ex.createBasicFigure();
        ex.createPolyline();
    }
}

