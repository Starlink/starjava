/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

import java.awt.AlphaComposite;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import uk.ac.starlink.diva.geom.InterpolatedCurve2D;
import uk.ac.starlink.diva.interp.Interpolator;;

/**
 * A figure that displays a InterpolatedCurve that can have its
 * vertices moved.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DrawPathFigure
 */
public class InterpolatedCurveFigure 
    extends DrawPathFigure
{
    /**
     * The InterpolatedCurve that we're using.
     */
    protected InterpolatedCurve2D curve = null;

    /**
     * Copy constructor.
     * This has unit-width continuous stroke and no paint pattern.
     */
    public InterpolatedCurveFigure( InterpolatedCurve2D curve )
    {
        super( curve );
        this.curve = curve;
    }

    /**
     * Copy constructor, but using the given shape, colour, linewidth and
     * composite.
     */
    public InterpolatedCurveFigure( InterpolatedCurve2D curve, 
                                    Paint outline, float lineWidth,
                                    AlphaComposite composite )
    {
        super( curve, outline, lineWidth );
        this.curve = curve;
        setComposite( composite );
    }

    /**
     * Create a new instance with the given origin and colour.
     */
    public InterpolatedCurveFigure( Interpolator interpolator, 
                                    double x, double y, Paint outline )
    {
        super( null );
        curve = createInterpolatedCurve( interpolator, x, y );
        setShape( curve );
    }

    /**
     * Create a new instance with the given origin, colour and linewidth.
     */
    public InterpolatedCurveFigure( Interpolator interpolator,
                                    double x, double y, Paint outline, 
                                    float lineWidth, AlphaComposite composite )
    {
        super( null );
        curve = createInterpolatedCurve( interpolator, x, y );
        setShape( curve );
        setLineWidth( lineWidth );
        setStrokePaint( outline );
        setComposite( composite );
    }

    /**
     * Create an instance and position it at the origin.
     */
    public InterpolatedCurve2D 
        createInterpolatedCurve( Interpolator interpolator, 
                                 double x, double y )
    {
        curve = new InterpolatedCurve2D( interpolator );
        curve.moveTo( x, y );
        return curve;
    }

    public void transform( AffineTransform at )
    {
        repaint();
        curve.transform( at );
        repaint();
    }

    public void translate( double x, double y )
    {
        repaint();
        curve.translate( x, y );
        repaint();
    }

    // Do not use default implementation, this is more efficient
    // (avoids trip to GeneralPath).
    public boolean hit( Rectangle2D r ) 
    {
        if ( !isVisible() ) {
            return false;
        }
        return intersects( r );
    }


    // Keep the shape and curve in sync.
    public void setShape( Shape shape )
    {
        if ( shape instanceof InterpolatedCurve2D ) {
            this.curve = (InterpolatedCurve2D) shape;
        }
        super.setShape( shape );
    }
}
