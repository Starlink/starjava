package uk.ac.starlink.frog.plot;

import diva.canvas.AbstractFigure;
import diva.canvas.CanvasUtilities;
import diva.util.java2d.Polygon2D;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.event.EventListenerList;

/**
 * Pointer is a Diva figure that can point up or down to identify
 * features. 
 *
 * @since $Date$
 * @since 05-DEC-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see Figure, AbstractFigure
 */
public class Pointer extends AbstractFigure 
{
    // General path that describes the pointer figure.
    private GeneralPath shape;
    
    // Default pointer characteristics.
    private static final double DEFAULT_X = 0.0;
    private static final double DEFAULT_Y = 0.0;
    private static final boolean DEFAULT_DOWN = true;
    private static final double DEFAULT_LENGTH = 25.0;
    private static final double DEFAULT_ARROWSIZE = 12.0;
    private static final float DEFAULT_WIDTH = 1.0F;

    // Cached stroke.
    private Stroke widthStroke;

    /** 
     * Create a default pointer figure. This points down.
     */
    public Pointer()
    {
        this( DEFAULT_DOWN, DEFAULT_X, DEFAULT_Y );
    }

    /**
     * Create a pointer with a direction.
     *
     * @param down true if the pointer should be directed downwards.
     */
    public Pointer( boolean down ) 
    {
        this( down, DEFAULT_X, DEFAULT_Y );
    }

    /**
     *  Create a pointer at a given position.
     *
     * @param down true if the pointer should be directed downwards.
     * @param x starting position of the pointer
     * @param y starting position of the pointer
     */
    public Pointer( boolean down, double x, double y )
    {
        this( down, x, y, DEFAULT_LENGTH, DEFAULT_ARROWSIZE, DEFAULT_WIDTH );
    }

    /**
     *  Create a pointer at a given position with a given length.
     *
     * @param down true if the pointer should be directed downwards.
     * @param x starting position of the pointer
     * @param y starting position of the pointer
     * @param length length of the pointer.
     */
    public Pointer( boolean down, double x, double y, double length )
    {
        this( down, x, y, length, DEFAULT_ARROWSIZE, DEFAULT_WIDTH );
    }

    /**
     *  Create a pointer at a given position with a new arrow head
     *  size and length.
     *
     * @param down true if the pointer should be directed downwards.
     * @param x starting position of the pointer
     * @param y starting position of the pointer
     * @param length length of the pointer.
     * @param size size of the arrow head.
     */
    public Pointer( boolean down, double x, double y, double length, 
                    double arrowsize )
    {
        this( down, x, y, length, arrowsize, DEFAULT_WIDTH );
    }

    /**
     *  Create a pointer at a given position with a new arrow head
     *  size and length and line width.
     *
     * @param down true if the pointer should be directed downwards.
     * @param x starting position of the pointer
     * @param y starting position of the pointer
     * @param length length of the pointer.
     * @param size size of the arrow head.
     */
    public Pointer( boolean down, double x, double y, double length, 
                    double arrowsize, float width )
    {
        createShape( down, length, arrowsize );
        setWidth( width );
        setStart( x, y );
    }

    /**
     * Set the starting point of the pointer.
     */
    public void setStart( double x, double y ) 
    {
        shape.transform( AffineTransform.getTranslateInstance( x, y ) );
    }

    /**
     * Modify the length of the pointer.
     */
    public void scaleLength( double scale ) 
    {
        shape.transform( AffineTransform.getScaleInstance( 1.0, scale ) );
    }

    /**
     * Modify the width of the pointer.
     */
    public void scaleWidth( double scale ) 
    {
        shape.transform( AffineTransform.getScaleInstance( scale, 1.0 ) );
    }

    /**
     * Set the shape of the figure to match the given properties.
     */
    public void createShape( boolean down, double length, double arrowsize ) 
    {   
        //  The shape is made from a line and a arrow head made from a
        //  polygon. The origin of the line is at 0,0 and the arrow
        //  head lies at the other end.

        //  Draw the line.
        Line2D line = new Line2D.Double( 0.0, 0.0, 0.0, length );
        
        //  Draw the arrow head.
        Polygon2D polygon = new Polygon2D.Double();
        double l1 = arrowsize * 1.0;
        double l2 = arrowsize * 0.25;
        double w = arrowsize * 0.25;
        polygon.moveTo( 0.0, length      );
        polygon.lineTo( w  , length - l2 );
        polygon.lineTo( 0.0, length + l1 );
        polygon.lineTo( -w , length - l2 );
        polygon.closePath();
        
        //  The complete shape is stored in a GeneralPath.
        shape = new GeneralPath();
        shape.append( line, false );
        shape.append( polygon, false );

        //  Flip if necessary.
        if ( ! down ) {
            shape.transform( AffineTransform.getScaleInstance( 1.0, -1.0 ) );
        }
    }

    /**
     * Set the width of the lines used to draw the figure (start from 1.0).
     */
    public void setWidth( float width ) 
    {
        //  Create the stroke to draw this once and cache it.
        widthStroke = new BasicStroke( width );
    }
    
    /** 
     * Get the bounds of this figure.
     */
    public Rectangle2D getBounds () 
    {
        return widthStroke.createStrokedShape( shape ).getBounds2D();
    }
    
    /** 
     * Get the shape of this figure.
     */
    public Shape getShape () 
    {
        return shape;
    }
    
    /**
     * Paint this figure onto the given graphics context. 
     */
    public void paint (Graphics2D g) 
    {
        g.setStroke( widthStroke );
        g.setPaint( Color.black );
        g.draw( shape );
        g.fill( shape );
    }
    
    /** 
     * Transform the object.
     */
    public void transform ( AffineTransform at ) 
    {
        repaint();
        shape = (GeneralPath) CanvasUtilities.transform( shape, at );
        repaint();
    }

    //  Implement the PlotFigure extensions, so that we are compatible.
    //
    //  Transform freely interface.
    //
    /**
     * Hint that figures should ignore any transformation constraints
     * (to match a resize of Plot).
     */
    protected static boolean transformFreely = false;

    /**
     * Enable the hint that a figure should allow itself to transform
     * freely, rather than obey any constraints (this is meant for
     * figures that could not otherwise redraw themselves to fit a
     * resized Plot, given their normal constraints, e.g. XRangeFigure).
     */
    public static void setTransformFreely( boolean state )
    {
        transformFreely = state;
    }

    /**
     * Find out if this is an occasion when a figure should give up
     * any constraints and traneform freely.
     */
    public static boolean isTransformFreely() 
    {
        return transformFreely;
    }

    //
    //  Events interface.
    //
    protected EventListenerList listeners = new EventListenerList();

    /**
     *  Registers a listener for to be informed when figure changes
     *  occur. 
     *
     *  @param l the FigureListener
     */
    public void addListener( FigureListener l ) 
    {
        listeners.add( FigureListener.class, l );
    }

    /**
     * Remove a listener.
     *
     * @param l the FigureListener
     */
    public void removeListener( FigureListener l ) 
    {
        listeners.remove( FigureListener.class, l );
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has created to all listeners.
     */
    protected void fireCreated() 
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.CREATED );
                }
                ((FigureListener)list[i+1]).figureCreated( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has been removed.
     */
    protected void fireRemoved() 
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.REMOVED );
                }
                ((FigureListener)list[i+1]).figureRemoved( e );
            }
        }
    }

    /**
     * Send a FigureChangedEvent object specifying that this figure
     * has changed.
     */
    protected void fireChanged() 
    {
        Object[] list = listeners.getListenerList();
        FigureChangedEvent e = null;
        for ( int i = list.length - 2; i >= 0; i -= 2 ) {
            if ( list[i] == FigureListener.class ) {
                if ( e == null ) {
                    e = new FigureChangedEvent( this, 
                                                FigureChangedEvent.CHANGED );
                }
                ((FigureListener)list[i+1]).figureChanged( e );
            }
        }
    }
}
