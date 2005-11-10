package uk.ac.starlink.topcat.plot;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;

/**
 * MouseMotionListener which acts on mouse movement events to provide
 * the position of the pointer in {@link PlotSurface} data coordinates.
 * When a mouse movement event is heard, the {@link #reportPosition}
 * method is called with suitable arguments.
 *
 * @author   Mark Taylor
 * @since    10 Nov 2005
 */
public abstract class PositionReporter implements MouseMotionListener {

    private static final double LOG10 = Math.log( 1e1 );
    private final DecimalFormat sciFormat_ = new DecimalFormat( "0.#E0" );
    private final DecimalFormat dpFormat_ = new DecimalFormat( ".0" );
    private final PlotSurface surface_;

    /**
     * Constructs a new position reporter for a given plot surface.
     *
     * @param   surface  plotting surface
     */
    public PositionReporter( PlotSurface surface ) {
        surface_ = surface;
    }

    /**
     * Invoked when the mouse has moved.
     * The <code>coords</code> array is either a two-element array giving
     * formatted values for the X and Y coordinates respectively, 
     * or <code>null</code> indicating that the pointer does not currently
     * correspond to a sensible position in data space.
     * Some effort is made to format the coordinate values in a compact
     * but consistent fashion.
     *
     * @param   coords  formatted (x,y) coordinate values, or null
     */
    protected abstract void reportPosition( String[] coords );

    public void mouseMoved( MouseEvent evt ) {
        reportPosition( formatPosition( evt.getX(), evt.getY() ) );
    }

    /**
     * No action.
     */
    public void mouseDragged( MouseEvent evt ) {
    }

    /**
     * Turns the numeric values of graphics space coordinates into
     * strings giving the positions in data space.
     *
     * @param   graphics space X coordinate
     * @param   graphics space Y coordinate
     * @return  2-element (x,y) array giving formatted data space coords,
     *          or null
     */
    private String[] formatPosition( int px, int py ) {

        /* Convert to numeric data space coordinates. */
        double[] dpos = surface_.graphicsToData( px, py, true );

        /* If the point was outside the plotting surface, return null. */
        if ( dpos == null ) {
            return null;
        }

        /* If the coordinates look weird, return null. */
        double dx = dpos[ 0 ];
        double dy = dpos[ 1 ];
        if ( Double.isNaN( dx ) || Double.isInfinite( dx ) ||
             Double.isNaN( dy ) || Double.isInfinite( dy ) ) {
            return null;
        }

        /* Try to get adjacent coordinates in X and Y; these are the
         * data space coordinates of an adjacent pixel in graphics space.
         * The point of this is so that we can see how much precision 
         * we need for coordinate formatting by getting rough X and Y
         * partial derivatives of graphics space with respect to graphics
         * space. */
        double ex = Double.NaN;
        double ey = Double.NaN;
        double[] epos = surface_.graphicsToData( px + 1, py + 1, true );
        if ( epos != null ) {
            ex = epos[ 0 ];
            ey = epos[ 1 ]; 
            if ( Double.isInfinite( ex ) ) {
                ex = Double.NaN;
            }
            if ( Double.isInfinite( ey ) ) {
                ey = Double.NaN;
            }
        }
        if ( Double.isNaN( ex ) || Double.isNaN( ey ) ) {
            epos = surface_.graphicsToData( px - 1, py - 1, true );
            if ( epos != null ) {
                if ( Double.isNaN( ex ) ) {
                    ex = epos[ 0 ];
                }
                if ( Double.isNaN( ey ) ) {
                    ey = epos[ 1 ];
                }
            }
        }

        /* Return null if we couldn't get adjacent X and Y coordinates. */
        if ( Double.isNaN( ex ) || Double.isNaN( ey ) ) {
            return null;
        }

        /* Calculate and return formatted values of the submitted values.  */
        return new String[] { formatValue( dx, Math.abs( ex - dx ) ),
                              formatValue( dy, Math.abs( ey - dy ) ) };
    }

    /**
     * Formats a value given the value itself and an indication of the
     * precision to which it should be displayed.
     * The idea is that calling this routine with arguments 
     * (v1,e), (v1+e,e) and (v1-e,e) should all give results which are
     * different but as similar as possible (typically just differeing
     * by one character = one significant figure).
     *
     * @param   value  value to format
     * @param   precision  precision of <code>value</code>
     */
    private String formatValue( double value, double precision ) {

        /* Determine the number of significant figures which are required. */
        precision = Math.abs( precision );
        double aval = Math.abs( value );
        int nsf = Math.max( 0, (int) Math.round( - Math.log( precision / aval )
                                                   / LOG10 ) );

        /* Large values: use scientific notation with the right number of
         * significant figures. */
        if ( aval >= 1e6 ) {
            sciFormat_.setMaximumFractionDigits( nsf );
            sciFormat_.setMinimumFractionDigits( nsf );
            return sciFormat_.format( value );
        }

        /* Small values: use scientific notation with the right number of
         * significant figures. */
        else if ( aval < 1e-4 ) {
            sciFormat_.setMaximumFractionDigits( nsf );
            sciFormat_.setMinimumFractionDigits( nsf );
            return sciFormat_.format( value );
        }

        /* Medium-sized values for which the integer parts will be different:
         * format as integers. */
        else if ( precision >= 0.9 ) {
            return Integer.toString( (int) Math.round( value ) );
        }

        /* Medium-sized values which may have the same integer values:
         * use fixed format with a suitable number of decimal places. */
        else {
            int ndp =
                (int) Math.round( Math.max( 0, -Math.log( precision ) )
                                               / LOG10 );
            if ( ndp == 0 ) {
                return Integer.toString( (int) Math.round( value ) );
            }
            else {
                dpFormat_.setMaximumFractionDigits( ndp );
                dpFormat_.setMinimumFractionDigits( ndp );
                return dpFormat_.format( value );
            }
        }
    }
}
