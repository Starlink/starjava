/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-JAN-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.ast.gui;

import java.awt.geom.Rectangle2D;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.FrameSet;      // For javadocs
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;          // For javadocs
import uk.ac.starlink.diva.FigureProps;
import uk.ac.starlink.diva.interp.Interpolator;

/**
 * Subclass of {@link FigureProps} that can convert a Figure between
 * coordinates systems using a {@link Mapping} when restoring from
 * an XML serialization.

 * <p>
 * This class is intended to be used as a replacement for {@link FigureProps}
 * is intended for use when restoring figures that are stored in some
 * coordinate system that needs modifying to align with the system now in use.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstFigureProps
    extends FigureProps
{
    /**
     * Create an instance ready to be configured using the decode method.
     */
    public AstFigureProps()
    {
        super();
    }

    /**
     * Create an instance which is a copy of an existing FigureProps.
     */
    public AstFigureProps( FigureProps props )
    {
        super( props );
    }

    /** 
     * Decode using a given mapping to transforming stored coordinates to 
     * current graphics coordinates. Usually this will be a {@link FrameSet} 
     * that is produced by combining the existing {@link Plot} with the
     * {@link Plot} that was used when graphics where initially saved.
     */
    public void decode( Element rootElement, Mapping mapping )
    {
        super.decode( rootElement );
        if ( mapping != null ) { 
            Rectangle2D.Double r = new Rectangle2D.Double( getX1(), getY1(), 
                                                           getWidth(),
                                                           getHeight() );
            transform( r, mapping );
            setX1( r.getX() );
            setY1( r.getY() );
            setWidth( r.getWidth() );
            setHeight( r.getHeight() );

            r.setFrame( getX2(), getY2(), getWidth(), getHeight() );
            transform( r, mapping );
            setX2( r.getX() );
            setY2( r.getY() );

            Interpolator i = getInterpolator();
            if ( i != null ) {
                transform( i.getXCoords(), i.getYCoords(), mapping );
                i.setCoords( i.getXCoords(), i.getYCoords(), true );
            }

            if ( getXArray() != null ) {
                transform( getXArray(), getYArray(), mapping );
            }
        }
    }

    //  Local re-useable space.
    private double[] xin = new double[2];
    private double[] yin = new double[2];

    /**
     * Transform a Rectangle using a given mapping.
     */
    public void transform( Rectangle2D.Double r, Mapping mapping )
    {
        xin[0] = r.getX();
        xin[1] = xin[0] + r.getWidth();
        yin[0] = r.getY();
        yin[1] = yin[0] + r.getHeight();
        transform( xin, yin, mapping );
        r.x = Math.min( xin[0], xin[1] );
        r.y = Math.min( yin[0], yin[1] );
        r.width = Math.abs( xin[0] - xin[1] );
        r.height = Math.abs( yin[0] - yin[1] );
    }

    /**
     * Transform arrays of coordinates using a mapping.
     */
    public void transform( double[] x, double[] y, Mapping mapping )
    {
        double[][] res = mapping.tran2( x.length, x, y, true );
        for ( int i = 0; i < x.length; i++ ) {
            x[i] = res[0][i];
            y[i] = res[1][i];
        }
    }

}
