/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import java.awt.Paint;
import uk.ac.starlink.splat.util.Interpolator;

/**
 * A figure that displays a InterpolatedCurve that can have its
 * vertices moved.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see BasicPlotFigure
 */
public class InterpolatedCurveFigure 
    extends PathPlotFigure
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
    }

    /**
     * Copy constructor, but using the given shape, colour and linewidth.
     */
    public InterpolatedCurveFigure( InterpolatedCurve2D curve, 
                                    Paint fill, float lineWidth )
    {
        super( curve, fill, lineWidth );
    }

    /**
     * Create a new instance with the given origin and colour.
     */
    public InterpolatedCurveFigure( Interpolator interpolator, 
                                    double x, double y, Paint fill )
    {
        super( null );
        curve = createInterpolatedCurve( interpolator, x, y );
        setShape( curve );
    }

    /**
     * Create a new instance with the given origin, colour and linewidth.
     */
    public InterpolatedCurveFigure( Interpolator interpolator,
                                    double x, double y, Paint fill, 
                                    float lineWidth )
    {
        super( null );
        curve = createInterpolatedCurve( interpolator, x, y );
        setShape( curve );
        setStrokePaint( fill );
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
}
