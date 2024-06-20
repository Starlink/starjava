package uk.ac.starlink.topcat.plot;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;
import uk.ac.starlink.ttools.plot.PlotSurface;
import uk.ac.starlink.ttools.convert.ValueConverter;

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
    private final ValueConverter xConv_;
    private final ValueConverter yConv_;
    private final Map<ValueConverter,Integer> convMap_;

    /**
     * Constructs a new position reporter for a given plot surface.
     *
     * @param   surface  plotting surface
     */
    public PositionReporter( PlotSurface surface ) {
        this( surface, null, null );
    }

    /**
     * Constructs a new position reporter for a given plot surface
     * using supplied value converter objects for the X and Y axes.
     * The <code>unconvert</code> methods of said converters should 
     * provide the formatting (number -&gt; formatted string) behaviour
     * for each axis.
     *
     * @param   surface  plotting surface
     * @param   xConv  value converter for X axis (or null)
     * @param   yConv  value converter for Y axis (or null)
     */
    public PositionReporter( PlotSurface surface, ValueConverter xConv,
                             ValueConverter yConv ) {
        surface_ = surface;
        xConv_ = xConv;
        yConv_ = yConv;
        convMap_ = new HashMap<ValueConverter,Integer>();
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
     * @param   px  graphics space X coordinate
     * @param   py  graphics space Y coordinate
     * @return  2-element (x,y) array giving formatted data space coords,
     *          or null
     */
    public String[] formatPosition( int px, int py ) {

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
        return new String[] { formatValue( dx, Math.abs( ex - dx ), xConv_ ),
                              formatValue( dy, Math.abs( ey - dy ), yConv_ ) };
    }

    /**
     * Formats a value given the value itself and an indication of the
     * precision to which it should be displayed.
     * The idea is that calling this routine with arguments 
     * (v1,e), (v1+e,e) and (v1-e,e) should all give results which are
     * different but as similar as possible (typically just differing
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

    /**
     * Formats a string, optionally using a supplied 
     * <code>ValueConverter</code> object.
     * Works as {@link #formatValue(double,double)} but if <code>conv</code>
     * is non-null its <code>unconvert</code> method will be used to
     * perform the formatting.  It is assumed that <code>conv</code>
     * has an output type which is numeric.
     *
     * @param  value  value to format
     * @param  precision of <code>value</code>
     * @param  conv  value converter
     */
    private String formatValue( double value, double precision,
                                ValueConverter conv ) {
        if ( conv == null ) {
            return formatValue( value, precision );
        }

        /* Maintain a record of the conversions done for each converter,
         * and make sure the length of the formatted string never decreases.
         * The point of this is to prevent ugly jiggling of the length of
         * the string in the readout - this should cause the string length
         * to settle down to a fixed value. */
        if ( ! convMap_.containsKey( conv ) ) {
            convMap_.put( conv, Integer.valueOf( 0 ) );
        }
        int itrunc = convMap_.get( conv ).intValue();

        /* Format the given values and ones (precision) either side. */
        Object om = conv.unconvert( Double.valueOf( value - precision ) );
        Object o0 = conv.unconvert( Double.valueOf( value ) );
        Object op = conv.unconvert( Double.valueOf( value + precision ) );
        String fm = om == null ? "" : om.toString();
        String f0 = o0 == null ? "" : o0.toString();
        String fp = op == null ? "" : op.toString();
        int lm = fm.length();
        int l0 = f0.length();
        int lp = fp.length();

        /* Work out how far through the string we have to go to find a 
         * difference between them. */
        boolean diffm = false;
        boolean diffp = false;
        for ( int i = 0; i < l0; i++ ) {
            char c0 = i < l0 ? f0.charAt( i ) : ' ';
            char cm = i < lm ? fm.charAt( i ) : ' ';
            char cp = i < lp ? fm.charAt( i ) : ' ';
            diffm = diffm || cm != c0;
            diffp = diffp || cp != c0;
            if ( diffm && diffp ) {

                /* Found it - try to truncate to near this length. */
                if ( i + 1 > itrunc ) {
                    itrunc = i + 1;
                    convMap_.put( conv, Integer.valueOf( i + 1 ) );
                }
                return truncate( f0, itrunc );
            }
        }

        /* Keep truncation map updated. */
        if ( f0.length() > itrunc ) {
            convMap_.put( conv, Integer.valueOf( f0.length() ) );
        }

        /* No truncation - return the whole string. */
        return f0;
    }

    /**
     * Truncates a formatted value string in a sensitive fashion.
     * Basically it carries on until the next non-numeric character
     * and truncates there, or truncates at the requested position if
     * there is no later non-numeric character.
     * This is a bit ad hoc, but should do the right thing for the
     * currently important formatted types (sexagesimal, ISO-8601).
     *
     * @param  full  full formatted value string
     * @return  iafter   earliest point at which truncation is permitted
     */
    private static String truncate( String full, int iafter ) {

        /* Check for the next non-numeric character after the given point
         * and truncate there. */
        for ( int i = iafter; i < full.length(); i++ ) {
            char c = full.charAt( i );
            if ( c < '0' || c > '9' ) {
                return full.substring( 0, i );
            }
        }

        /* No non-numerics after the suggested point.
         * Work backwards to try to find a decimal point. */
        for ( int i = iafter - 1; i >= 0; i-- ) {
            char c = full.charAt( i );

            /* If we find a decimal point, feturn the string truncated at
             * the requested place. */
            if ( c == '.' ) {
                return full.substring( 0, iafter );
            }

            /* If we find a non-numeric, better return the whole thing to
             * be safe. */
            else if ( c < '0' || c > '9' ) {
                return full;
            }
        }

        /* Numbers all the way back to the start.  Return the whole thing. */
        return full;
    }
}
