/*
 * $Id: FigureTutorial.java,v 1.15 2000/11/13 08:40:14 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */

package diva.canvas.tutorial;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasPane;
import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.JCanvas;

import diva.canvas.event.MouseFilter;

import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.DragInteractor;
import diva.canvas.interactor.BoundsManipulator;

import diva.canvas.toolbox.BasicFigure;
import diva.canvas.toolbox.BasicEllipse;
import diva.canvas.toolbox.BasicRectangle;
import diva.canvas.toolbox.BasicController;
import diva.canvas.toolbox.ImageFigure;

import diva.util.java2d.ShapeUtilities;

import diva.gui.BasicFrame;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;

/** An example showing how to make custom figures. Although the
 * Diva Canvas provides a number of pre-built concrete figures
 * in <b>diva.canvas.toolbox</b> (BasicFigure is used in many
 * of the examples), you will in general want to define your
 * own Figure classes. The Diva Canvas deliberately does not
 * attempt to hide the power of the Java2D API, but instead to
 * augment it. This means that anything but the simplest types of
 * Figure require some knowledge of Java2D.
 *
 * <p> In general, defining a completely new type of Figure
 * means implementing the Figure interface. However, it is usually
 * simpler just to subclass the AbstractFigure class and override
 * at least the methods getShape(), transform(), and paint(). The
 * example in this file does that to create a new leaf figure. See
 * the documentation for FigureTutorial.CustomRectangle for
 * more details.
 *
 * <p> A simpler, although somewhat slower executing,
 * way of defining a new figure is to create
 * a GIF file that looks like the figure.  The GIF file can be loaded 
 * into an Image and the Image embedded into a figure.   
 * An ImageFigure does exactly this.
 *
 * <p> There are also other ways of creating new Figure classes.
 * You can subclass AbstractFigureContainer to produce a new figure
 * class that contains other figures. You can also subclass FigureWrapper
 * to add application-specific behavior to an existing figure class
 * by "wrapping" it.
 * 
 * @author John Reekie
 * @version $Revision: 1.15 $
 */
public class FigureTutorial {
    // The file name for the image that is displayed
    public static final String IMAGE_FILE_NAME = "demo.gif";
    
    // The JCanvas
    private JCanvas canvas;

    // The GraphicsPane
    private GraphicsPane graphicsPane;

    /** Create a JCanvas and put it into a window.
     */
    public FigureTutorial () {
        canvas = new JCanvas();
        graphicsPane = (GraphicsPane)canvas.getCanvasPane();
        BasicFrame frame = new BasicFrame("Figure tutorial", canvas);
    }

    /** Create instances of the class defined
     * in this file. To make the demo a little more interesting,
     * make them draggable.
     */
    public void createFigures () {
        FigureLayer layer = graphicsPane.getForegroundLayer();

        // Create the interaction role and an interactor to do the work.
        BasicController controller = new BasicController(graphicsPane);
        SelectionInteractor defaultInteractor = controller.getSelectionInteractor();
        BoundsManipulator manip = new BoundsManipulator();
        defaultInteractor.setPrototypeDecorator(manip);
        
        // Create a custom rectangle and make it draggable
        AbstractFigure blue = new CustomRectangle(10.0,10.0,50.0,50.0);
        layer.add(blue);
        blue.setInteractor(defaultInteractor);
	blue.setToolTipText("Blue figure 1");

        // Create a custom rectangle and make it draggable
        AbstractFigure blue2 = new CustomRectangle(100.0,100.0,100.0,50.0);
        layer.add(blue2);
        blue2.setInteractor(defaultInteractor);
	blue2.setToolTipText("Blue figure 2");

        // Create an image figure and make it draggable
        Image img = Toolkit.getDefaultToolkit().getImage(IMAGE_FILE_NAME);
        MediaTracker tracker = new MediaTracker(canvas);
        tracker.addImage(img,0);
        try {
            tracker.waitForID(0);
        }
        catch (InterruptedException e) {
            System.err.println(e + "... in FigureTutorial");
        }
        ImageFigure imgFig = new ImageFigure(img);
        imgFig.translate(300,100);
        layer.add(imgFig);
        imgFig.setInteractor(defaultInteractor);
	imgFig.setToolTipText("Image figure");

    }

    /** Main function
     */
    public static void main (String argv[]) {
        FigureTutorial ex = new FigureTutorial();
        ex.createFigures();
    }

    //////////////////////////////////////////////////////////////////////
    //// CustomRectangle

    /** CustomRectangle is a class that paints itself as a
     * rectangle and draw a red plus sign over the top of itself.
     * This example figure class illustrates the use of different
     * paints and strokes to create the required image. It overrides
     * only the absolute minimum number of methods that must be
     * overridden to create a new figure class.
     */
    public class CustomRectangle extends AbstractFigure {
        // The bounds of the figure
        private Rectangle2D _bounds;

        /** Create a new instance of this figure. All we do here
         * is take the coordinates that we have been given and
         * remember them as a rectangle. In general, we may want
         * several constructors, and methods to set and get fields
         * that will control the visual properties of the figure.
         */
        public CustomRectangle (
                double x, double y,
                double width, double height) {
            _bounds = new Rectangle2D.Double(x,y,width,height);
        }

        /** Get the bounds of this figure. Because, in this example,
         * we have stroked the outline of the rectangle, we have
         * to create a new rectangle that is the bounds of the outside
         * of that stroke. In this method the stroke object is
         * being created each time, but it would normally be created
         * only once.
         */
        public Rectangle2D getBounds () {
            Stroke s = new BasicStroke(1.0f);
            return s.createStrokedShape(_bounds).getBounds2D();
        }

        /** Get the shape of this figure. In this example, it's
         * just the bounding rectangle that we stored in the
         * constructor. Note that in general, figures assume
         * that clients will not modify the object returned by
         * this method.
         */
        public Shape getShape () {
            return _bounds;
        }

        /**
         * Paint this figure onto the given graphics context.
         * We first paint the rectangle blue with a stroke
         * width of 1 unit, and then the plus sign with a stroke
         * width of 4 units. The implementation is fairly inefficient:
         * in general, we would want to cache the Stroke objects.
         * Note that we have to set the stroke and paint in the
         * graphics context before we can do anything useful. 
         */
        public void paint (Graphics2D g) {
            // Create a stroke and fill then outline the rectangle
            Stroke s = new BasicStroke(1.0f);
            g.setStroke(s);
            g.setPaint(Color.blue);
            g.fill(_bounds);
            g.setPaint(Color.black);
            g.draw(_bounds);

            // Create a new stroke and draw the plus
            double x = _bounds.getX();
            double y = _bounds.getY();
            double w = _bounds.getWidth();
            double h = _bounds.getHeight();

            s = new BasicStroke(4.0f,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER);
            g.setStroke(s);
            g.setPaint(Color.red);
            g.draw(new Line2D.Double(x,y+h/2,x+w,y+h/2));
            g.draw(new Line2D.Double(x+w/2,y,x+w/2,y+h));
        }

        /** Transform the object. There are various ways of doing this,
         * some more complicated and some even morer complicated...
         * In this example, we use a utility function in the
         * class diva.canvas.CanvasUtils to transform the bounding
         * box. Both before and after we do the transformation,
         * we have to call the repaint() method so that the region
         * of the canvas that changed is properly repainted.
         */
        public void transform (AffineTransform at) {
            repaint();
            _bounds = (Rectangle2D)
		ShapeUtilities.transformBounds(_bounds, at);
            repaint();
        }
    }
}


