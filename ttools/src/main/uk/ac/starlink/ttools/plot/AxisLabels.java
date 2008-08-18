package uk.ac.starlink.ttools.plot;

/**
 * Contains labels for an axis.
 * The instance methods of this class don't do anything clever, but 
 * factory methods are provided which can perform sensible axis labelling.
 *
 * @author   Mark Taylor
 */
public class AxisLabels {

    private final int nTick_;
    private double[] ticks_;
    private String[] labels_;

    /**
     * Sets up a new AxisLabels.
     *
     * @param   ticks  numeric values of the ticks
     * @param   labels  string values for each of the elements of
     *                  <code>ticks</code>
     */
    public AxisLabels( double[] ticks, String[] labels ) {
        ticks_ = (double[]) ticks.clone();
        labels_ = (String[]) labels.clone();
        nTick_ = ticks_.length;
        if ( nTick_ != labels_.length ) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the number of ticks on the axis.
     *
     * @return   tick count
     */
    public int getCount() {
        return nTick_;
    }

    /**
     * Returns the axis position of one of the tick marks.
     *
     * @param  itick  index of the tick
     * @return   tick value
     */
    public double getTick( int itick ) {
        return ticks_[ itick ];
    }

    /**
     * Returns the label for one of the tick marks.  This is essentially
     * a stringification of <code>getTick(itick)</code>, but some attempt
     * may be made to make the representation compact and tidy.
     *
     * @param  itick  index of the tick
     * @return   tick label
     */
    public String getLabel( int itick ) {
        return labels_[ itick ];
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < nTick_; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ", " );
            }
            sbuf.append( ticks_[ i ] )
                .append( '=' )
                .append( labels_[ i ] );
        }
        return sbuf.toString();
    }

    /**
     * Sets up axis labels for a linearly scaled axis.
     *
     * @param  lo  lower bound of the axis
     * @param  hi  upper bound of the axis
     * @param  approxTicks  the approximate number of ticks you'd like to see
     */
    public static AxisLabels labelLinearAxis( double lo, double hi,
                                              int approxTicks ) {
        Rounder rounder = Rounder.LINEAR;
        double gap = rounder.round( ( hi - lo ) / (double) approxTicks );
        double preGap = lo % gap;
        if ( preGap < 0 ) {
            preGap += gap;
        }
        double loTick = preGap == 0
                      ? lo
                      : lo - preGap + gap;
        int nTick = (int) ( ( hi - loTick ) / gap ) + 1;
        double[] ticks = new double[ nTick ];
        String[] labels = new String[ nTick ];
        for ( int i = 0; i < nTick; i++ ) {
            double value = loTick + i * gap;
            ticks[ i ] = value;
            labels[ i ] = getLabel( value );
        }
        return new AxisLabels( ticks, labels );
    }

    /**
     * Sets up axis labels for a logarithmically scaled axis.
     *
     * @param  lo   lower bound of axis
     * @param  hi   upper bound of axis
     * @param  approxTicks  the approximate number of ticks you'd like to see
     */
    public static AxisLabels labelLogAxis( double lo, double hi,
                                           int approxTicks ) {
        if ( hi <= lo || lo <= 0 || hi <= 0 ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + lo + " .. " + hi );
        }
        if ( hi / lo < 4.0 ) {
            return labelLinearAxis( lo, hi, approxTicks );
        }
        double gapFactor = Double.NaN;
        for ( int i = 0; i < 100 && Double.isNaN( gapFactor ); i++ ) {
            double factor = i == 0 ? 2.0 : Math.pow( 10, i );
            if ( Math.pow( factor, approxTicks ) > hi / lo ) {
                gapFactor = factor;
            }
        }
        if ( Double.isNaN( gapFactor ) ) {
            // weird.
            return labelLinearAxis( lo, hi, approxTicks );
        }
        double logPregap = Math.log( lo ) % Math.log( gapFactor );
        if ( logPregap < 0.0 ) {
            logPregap += Math.log( gapFactor );
        }
        double loTick = logPregap == 0.0
                      ? lo
                      : lo / Math.exp( logPregap ) * gapFactor;
        int nTick =
            (int) ( Math.log( hi / loTick ) / Math.log( gapFactor ) ) + 1;
        double[] ticks = new double[ nTick ];
        String[] labels = new String[ nTick ];
        for ( int i = 0; i < nTick; i++ ) {
            double value = loTick * Math.pow( gapFactor, i );
            ticks[ i ] = value;
            String label = getLabel( value );
            if ( label.equals( "1000" ) ) {
                label = "1E3";
            }
            else if ( label.equals( "10000" ) ) {
                label = "1E4";
            }
            else if ( label.equals( "100000" ) ) {
                label = "1E5";
            }
            else if ( label.equals( "1000000" ) ) {
                label = "1E6";
            }
            // the others get done automatically
            labels[ i ] = label;
        }
        return new AxisLabels( ticks, labels );
    }

    /**
     * Returns a somewhat tidied stringification of a numeric value.
     *
     * @param  value
     * @param  string representing <code>value</code>
     */
    private static String getLabel( double value ) {
        float fval = (float) value;
        String label = Float.isInfinite( fval ) || fval == 0f
                     ? Double.toString( value )
                     : Float.toString( fval );
        label = label.replaceFirst( "\\.0$", "" );
        label = label.replaceFirst( "\\.0E", "E" );
        return label;
    }

    public static void main( String[] args ) {
        double lo = Double.parseDouble( args[ 0 ] );
        double hi = Double.parseDouble( args[ 1 ] );
        int ntick = Integer.parseInt( args[ 2 ] );
        System.out.println( AxisLabels.labelLinearAxis( lo, hi, ntick ) );
        if ( lo > 0 && hi > 0 ) {
            System.out.println( AxisLabels.labelLogAxis( lo, hi, ntick ) );
        }
    }
}
