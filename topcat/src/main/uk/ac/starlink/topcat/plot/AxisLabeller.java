package uk.ac.starlink.topcat.plot;

/**
 * Comes up with labels for an axis.
 * You give it the limits of the axis, and approximately how many 
 * labels you want, and it comes up with a number of regularly spaced
 * tickmarks at sensible intervals with associated text labels.
 *
 * @author   Mark Taylor
 */
public class AxisLabeller {

    private final int nTick_;
    private final double[] ticks_;
    private final String[] labels_;

    /**
     * Constructs a new labeller.
     *
     * @param  lo  lower bound of the axis
     * @param  hi  upper bound of the axis
     * @param   approxTicks  the approximate number of ticks you'd like to see
     */
    public AxisLabeller( double lo, double hi, int approxTicks ) {
        Rounder rounder = Rounder.LINEAR;

        double range = hi - lo;
        double gap = rounder.round( ( hi - lo ) / (double) approxTicks );
        double preGap = lo % gap;
        double loTick = preGap == 0 
                      ? lo 
                      : lo - preGap + gap;
        nTick_ = (int) ( ( hi - loTick ) / gap ) + 1;
        ticks_ = new double[ nTick_ ];
        labels_ = new String[ nTick_ ];

        for ( int i = 0; i < nTick_; i++ ) {
            double value = loTick + i * gap;
            ticks_[ i ] = value;
            // labels_[ i ] = pr.formatValue( value, gap );
            String label = Float.toString( (float) value );
            label = label.replaceFirst( "\\.0$", "" );
            label = label.replaceFirst( "\\.0E", "E" );
            labels_[ i ] = label;
        }
    }

    /**
     * Returns the number of ticks that are in use.
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
     * is made to make the representation compact and tidy.
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

    public static void main( String[] args ) {
        System.out.println( new AxisLabeller( Double.parseDouble( args[ 0 ] ),
                                              Double.parseDouble( args[ 1 ] ),
                                              Integer.parseInt( args[ 2 ] ) ) );
    }
}
