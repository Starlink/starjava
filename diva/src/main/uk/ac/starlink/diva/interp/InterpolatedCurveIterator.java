/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-NOV-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva.interp;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

import uk.ac.starlink.diva.geom.InterpolatedCurve2D;

/**
 * An iterator over an InterpolatedCurve2D.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class InterpolatedCurveIterator
    implements PathIterator
{
    /**
     * X coordinates of curve interpolation points.
     */
    private double[] xCoords;

    /**
     * Interpolated Y coordinates.
     */
    private double[] yCoords;

    /**
     * Index of the current coordinate (next to be used).
     */
    private int index = 0;

    /**
     * Create an iterator for the given InterpolatedCurve2D.
     *
     * @param curve the InterpolatedCurve2D instance to iterate over.
     * @param at a transform to apply to the InterpolatedCurve2D vertices.
     *           If null then an identity transform will be used.
     */
    public InterpolatedCurveIterator( InterpolatedCurve2D curve,
                                      AffineTransform at )
    {
        int count = curve.getCoordCount();
        if ( count > 0 ) {
            xCoords = new double[count];
            yCoords = new double[count];

            if ( at == null || at.isIdentity() ) {
                System.arraycopy( curve.getXCoords(), 0, xCoords, 0, count );
                System.arraycopy( curve.getYCoords(), 0, yCoords, 0, count );
            }
            else {
                double[] x = curve.getXCoords();
                double[] y = curve.getYCoords();
                double[] xy = new double[x.length*2];
                int j = 0;
                for ( int i = 0; i < x.length; i++ ) {
                    xy[j++] = x[i];
                    xy[j++] = y[i];
                }
                at.transform( xy, 0, xy, 0, x.length );
                j = 0;
                for ( int i = 0; i < x.length; i++ ) {
                    xCoords[i] = xy[j++];
                    yCoords[i] = xy[j++];
                }
            }
        }
        else {
            xCoords = new double[1];
            yCoords = new double[1];
        }
    }

    // Returns the coordinates and type of the current path segment in
    // the iteration. This is just a line segment as all interpolated
    // curves are really made of polylines.
    public int currentSegment( double coords[] )
    {
        coords[0] = xCoords[index];
        coords[1] = yCoords[index];
        if ( index == 0 ) {
            return PathIterator.SEG_MOVETO;
        }
        return PathIterator.SEG_LINETO;
    }

    public int currentSegment( float coords[] )
    {
        coords[0] = (float) xCoords[index];
        coords[1] = (float) yCoords[index];
        if ( index == 0 ) {
            return PathIterator.SEG_MOVETO;
        }
        return PathIterator.SEG_LINETO;
     }


    // Return the winding rule. This is WIND_NON_ZERO for all
    // InterpolatedCurve2D instances.
    public int getWindingRule()
    {
        return PathIterator.WIND_NON_ZERO;
    }

    /**
     * Test if the iterator is done.
     */
    public boolean isDone()
    {
        return ( index >= xCoords.length );
    }

    /**
     * Move the iterator along by one point.
     */
    public void next()
    {
        index++;
    }
}
