package uk.ac.starlink.splat.iface;

import diva.canvas.CanvasUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.plot.PlotControl;

/*
 * $Id$
 *
 * Copyright (c) 1998-2000 The Regents of the University of
 * California.  All rights reserved. See the file COPYRIGHT in Diva
 * distribution for details.
 *
 * Some parts copyright (C) 2001 Central Laboratory of the Research Councils
 */
/**
 * A panner is a window that provides a mechanism to visualize and
 * manipulate a JViewport object without using scrollbars.  Unlike the
 * viewport, which contains a partial, full size rendition of the
 * contained component, this class contains a complete, scaled down
 * rendition of the component. The bounds of the component are represented
 * by a blue rectangle and the bounds of the viewport on the component
 * are visible as a red rectangle. Clicking or dragging within the
 * PlotPanner centers the viewport at that point on the component.
 * <p>
 * Additional mouse interactions provide a increment and decrement of
 * the current scale in the X dimension, mouse button 2 increases the
 * zoom, mouse button 3 decreases it.
 *
 * <p>
 * This class is a copy of the Diva JPanner class with some
 * constraints on the viewport bounds that disallow moment outside of
 * the viewport surface (this behaviour is better for spectra). As
 * time has progressed real SPLAT functionality has crept in too.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @since $Date$
 * @since 13-JUN-2001
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotPanner extends JPanel
{
    /**
     * The target window that is being wrapped.
     */
    private JViewport _target = null;

    /**
     * Plot that we're controlling.
     */
    private PlotControl plot = null;

    /**
     * The scrolling listener;
     */
    private ScrollListener _listener = new ScrollListener();

    /**
     * Construct a new panner that is initially viewing
     * nothing.  Use setViewport() to assign it to something.
     */
    public PlotPanner()
    {
        this( null, null );
    }

    /**
     * Construct a new wrapper that wraps the given target.
     */
    public PlotPanner( JViewport target, PlotControl plot )
    {
        setViewport( target );
        setPlotControl( plot );
	addMouseListener( new PanMouseListener() );
        addMouseMotionListener( new PanMouseListener() );
        setToolTipText( "Panner window. Drag red rectangle, or just"+
                        " click on the feature to view" );
    }

    /**
     * Set the target component that is being
     * wrapped.
     */
    public void setViewport( JViewport target )
    {
        if ( _target != null ) {
            _target.removeChangeListener( _listener );
        }
        _target = target;
        if ( _target != null ) {
            _target.addChangeListener( _listener );
        }
	repaint();
    }

    /**
     * Return the target component that is being
     * wrapped.
     */
    public JViewport getViewport()
    {
        return _target;
    }

    /**
     * Set the PlotControl object that is controlling viewport we're
     * wrapping. We need this to modify such things as the
     * magnification.
     */
    public void setPlotControl( PlotControl plot )
    {
        this.plot = plot;
    }

    /**
     * Get the PlotControl object.
     */
    public PlotControl getPlotControl()
    {
        return plot;
    }

    //  Paint the components that we want on each refresh.
    public void paintComponent( Graphics g )
    {
	if ( _target != null ) {

            //  Get a transformation that maps the viewport onto our display
            //  surface.
            Dimension viewSize =_target.getView().getSize();
	    Rectangle viewRect =
		new Rectangle( 0, 0, viewSize.width, viewSize.height );
	    Rectangle myRect = _getInsetBounds();

            AffineTransform forward =
		CanvasUtilities.computeFitTransform( viewRect, myRect );

            //  When the plot is just displaying axes in the visible area and
            //  clipping is switched on that doesn't give a very useful
            //  display, so always switch off the visible only option.
            boolean state = plot.getPlot().isVisibleOnly();
            if ( state ) {
                plot.getPlot().setVisibleOnly( false );
                try {
                    plot.getPlot().staticUpdate();
                }
                catch (Exception e) {
                    // Do nothing.
                }
            }

            //  Do the clever bit. Get the viewport to draw into our
            //  Graphics2D object, using the transform to fit everything on.
            Graphics2D g2d = (Graphics2D)g;
            g2d.transform( forward );
            _target.getView().paint( g );

            //  Restore visibleOnly state.
            if ( state ) {
                plot.getPlot().setVisibleOnly( true );
            }

            //  Now add the viewport position as a rectangle.
            g.setColor(Color.red);
            Rectangle r = _target.getViewRect();
            g.drawRect( r.x, r.y, r.width, r.height );
        }
        else {
	    Rectangle r = _getInsetBounds();
	    g.clearRect( r.x, r.y, r.width, r.height );
	}
    }

    /**
     *  Return a rectangle that fits inside the border.
     */
    private Rectangle _getInsetBounds()
    {
	Dimension mySize = getSize();
	Insets insets = getInsets();
	Rectangle myRect =
	    new Rectangle( insets.left, insets.top,
			   mySize.width - insets.top - insets.bottom,
			   mySize.height - insets.left - insets.right );
	return myRect;
    }


    /**
     * Respond to scroll events in the viewport and display the
     * graphics to match.
     */
    private class ScrollListener implements ChangeListener
    {
        public void stateChanged( ChangeEvent e ) {
            repaint();
        }
    }

    /**
     * A class for responding to mouse events in the component. The
     * main bindings are to a press of button 1, which centers the
     * viewport, and a drag of button 1, which does the same.
     */
    private class PanMouseListener
        extends MouseAdapter
        implements MouseMotionListener
    {
        /**
         * Set the position of the viewport to correspond to a
         * position in this component.
         *
         * @param x component coordinate to center around.
         * @param y component coordinate to center around.
         */
	public void setPosition( int x, int y )
        {
            //  Determine the transformation between viewport and
            //  component coordinates.
	    Dimension viewSize =_target.getView().getSize();
	    Rectangle viewRect =
		new Rectangle(0, 0, viewSize.width, viewSize.height);
	    Rectangle myRect = _getInsetBounds();

	    AffineTransform forward =
		CanvasUtilities.computeFitTransform(viewRect, myRect);

            //  We need the inverse to get scales.
	    AffineTransform inverse;
	    try {
		inverse = forward.createInverse();
	    }
	    catch ( NoninvertibleTransformException e ) {
		throw new RuntimeException( e );
	    }

            //  The position needs to accomodate for the rectangle
            //  that we'll eventually draw (its used as a
            //  center). Also transform to viewport coordinates.
	    Dimension extentSize = _target.getExtentSize();
	    x = (int)(x * inverse.getScaleX()) - extentSize.width/2;
	    y = (int)(y * inverse.getScaleY()) - extentSize.height/2;

            //  Keep the rectangle within the viewport bounds and
            //  help it abutt cleanly to the edges when dragged (can
            //  be left behind, hence additional tests and assignments
            //  to extremes).
            Point p = new Point();
            Dimension wholeRect = _target.getView().getSize();
            if ( x > 0 && x < ( wholeRect.width - extentSize.width ) ) {
                p.x = x;
            }
            else if ( x < 0 ) {
                p.x = 0;
            }
            else if ( x > ( wholeRect.width - extentSize.width ) ) {
                p.x = wholeRect.width - extentSize.width;
            }

            if ( y > 0 && y < ( wholeRect.height - extentSize.height ) ) {
                p.y = y;
            }
            else if ( y < 0 ) {
                p.y = 0;
            }
            else if ( y > ( wholeRect.height - extentSize.height ) ) {
                p.y = wholeRect.height - extentSize.height;
            }

            //  Move the viewport to position.
	    _target.setViewPosition( p );
	}

        /**
         *  Increment the X zoom factor by one.
         */
        protected void incrementXZoom()
        {
            if ( plot != null ) {
                plot.zoomAboutTheCentre( 1, 0 );
            }
        }

        /**
         *  Decrement the X zoom factor by one.
         */
        protected void decrementXZoom()
        {
            if ( plot != null ) {
                plot.zoomAboutTheCentre( -1 , 0 );
            }
        }


        /**
         * Mouse press centers on that position.
         */
        public void mousePressed(MouseEvent evt)
        {
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
		setPosition(evt.getX(), evt.getY());
            }
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON2_MASK) != 0) {
		incrementXZoom();
            }
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
		decrementXZoom();
            }
        }

        /**
         * Mouse moved does nothing.
         */
        public void mouseMoved(MouseEvent evt)
        {
            //  Do nothing.
        }

        /**
         * Mouse drag centers on position (and hence seems to follow).
         */
        public void mouseDragged(MouseEvent evt)
        {
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
		setPosition(evt.getX(), evt.getY());
	    }
        }
    }

    /**
     * Simple test routine.
     */
    public static void main( String argv[] )
    {
        JFrame f = new JFrame();
        JList l = new JList();
        String[] data = {"oneeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                         "twoooooooooooooooooooooooooooooooooooooooo",
                         "threeeeeeeeeeeeeeeee",
                         "fourrrrrrrrrrrrrrrrrrrrrrrrr"};
        JList dataList = new JList(data);
        JScrollPane p = new JScrollPane(dataList);
        p.setSize(200, 200);
        PlotPanner pan = new PlotPanner( p.getViewport(), null );
        pan.setSize(200, 100);
        f.getContentPane().setLayout(new GridLayout(2, 1));
        f.getContentPane().add(p);
        f.getContentPane().add(pan);
        f.setSize(200, 400);
        f.setVisible(true);
    }
}

