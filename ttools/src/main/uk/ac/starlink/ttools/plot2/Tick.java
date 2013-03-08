package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines a tick on an axis.
 * A tick has a numerical value, used for positioning, and optionally
 * a text label.  Ticks with no label are considered minor.
 * The {@link #getTicks} static method generates tickmark arrays suitable
 * for axis labelling.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
@Equality
public class Tick {

    private final double value_;
    private final String label_;

    /**
     * Constructs a minor tick.
     * This has no text label.
     *
     * @param  value  numeric value
     */
    public Tick( double value ) {
        this( value, null );
    }

    /**
     * Constructs a tick.
     * As long as the label is non-null, this is considered a major tick.
     * 
     * @param  value  numeric value
     * @param  label  text label
     */
    public Tick( double value, String label ) {
        value_ = value;
        label_ = label;
    }

    /**
     * Returns this tick's numeric value.
     *
     * @return  value
     */
    public double getValue() {
        return value_;
    }

    /**
     * Returns this tick's text label.
     *
     * @return   text label
     */
    public String getLabel() {
        return label_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Tick ) {
            Tick other = (Tick) o;
            return other.value_ == this.value_
                && PlotUtil.equals( other.label_, this.label_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits( (float) value_ )
             + PlotUtil.hashCode( label_ );
    }

    /**
     * Generates a tick array suitable for labelling a plot axis.
     *
     * @param   dlo  minimum axis data value
     * @param   dhi  maximum axis data value
     * @param   npix  number of pixels along the axis
     * @param   log   true for logarithmic scaling, false for linear
     * @param   withMinor  if true minor axes are included,
     *                     if false only major (labelled) ones are
     * @param crowding   1 for normal tick density on the axis,
     *                   lower for fewer labels, higher for more
     * @return  tick array
     */
    public static Tick[] getTicks( double dlo, double dhi, int npix,
                                   boolean log, boolean withMinor,
                                   double crowding ) {
        int nt0 = 1 + (int) Math.round( crowding * npix / 100 );
        return log ? labelLogAxis( dlo, dhi, nt0, withMinor )
                   : labelLinearAxis( dlo, dhi, nt0, withMinor );
    }

    /**
     * Generates a tick array suitable for labelling a logarithmic axis.
     *
     * @param   dlo  minimum axis data value
     * @param   dhi  maximum axis data value
     * @param   approxTicks  approximate number of major ticks to return
     * @param   withMinor  true to include minor ticks as well
     * @return  tick array
     */
    private static Tick[] labelLogAxis( double lo, double hi, int approxTicks,
                                        boolean withMinor ) {

        /* Reject negative values for a logarithmic axis. */
        if ( hi <= lo || lo <= 0 || hi <= 0 ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + lo + " .. " + hi );
        }

        /* If the range is less than a factor of ten, fall back to linear
         * axis labelling. */
        double nDecade = Math.log10( hi ) - Math.log10( lo );
        if ( nDecade <= 1 ) {
            return labelLinearAxis( lo, hi, approxTicks, withMinor );
        }
        List<Tick> major = new ArrayList<Tick>();
        List<Tick> minor = withMinor ? new ArrayList<Tick>() : null;

        /* If the number of factors of ten is at least comparable to the
         * number of ticks, the step will be a power of ten, and all ticks
         * will be pow10(int).  Work out what the integer step is and
         * generate the ticks accordingly. */
        if ( nDecade > approxTicks * 1.5 ) {
            int logStep =
                Math.max( 1, (int) Math.round( nDecade / approxTicks ) );
            int logMin =
                (int) ( Math.ceil( Math.log10( lo ) / logStep ) * logStep );
            int logMax =
                (int) ( Math.floor( Math.log10( hi ) / logStep ) * logStep );
            for ( int log = logMin; log <= logMax; log += logStep ) {
                major.add( createLogTick( 1, log ) );
            }
            if ( withMinor ) {
                if ( logStep > 1 ) {
                    for ( int log = logMin - 1; log <= logMax + 1;
                          log += logStep - 1 ) {
                        double value = exp10( log );
                        if ( value >= lo && value <= hi ) {
                            minor.add( new Tick( value ) );
                        }
                    }
                }
                else {
                    assert logStep == 1;
                    for ( int log = logMin - 1; log <= logMax + 1; log++ ) {
                        double exponent = exp10( log );
                        for ( int mantissa = 1; mantissa < 10; mantissa++ ) {
                            double value = mantissa * exponent;
                            if ( value >= lo && value <= hi ) {
                                minor.add( new Tick( value ) );
                            }
                        }
                    }
                }
            }
        }

        /* Otherwise we have multiple ticks per factor of ten.
         * Get a suitable set of round numbers and generate ticks
         * accordingly. */
        else {
            int[] mantissas = getLogMantissas( approxTicks / nDecade );
            int nmant = mantissas.length;
            int log0 = (int) Math.floor( Math.log10( lo ) );
            int nlog = (int) Math.ceil( Math.log10( hi ) ) - log0 + 1;
            for ( int il = 0; il < nlog; il++ ) {
                for ( int im = 0; im < nmant; im++ ) {
                    Tick tick = createLogTick( mantissas[ im ], log0 + il );
                    double value = tick.getValue();
                    if ( value >= lo && value <= hi ) {
                        major.add( tick );
                    }
                }
            }
            if ( withMinor ) {
                int[] mantissas1 = getLogMantissas( 5 * approxTicks / nDecade );
                int nmant1 = mantissas1.length;
                for ( int il = 0; il < nlog; il++ ) {
                    for ( int im = 0; im < nmant1; im++ ) {
                        double value = mantissas1[ im ] * exp10( log0 + il );
                        if ( value >= lo && value <= hi ) {
                            minor.add( new Tick( value ) );
                        }
                    }
                }
            }
        }

        /* Prepare ticks in an array and return them. */
        Tick[] ticks = major.toArray( new Tick[ 0 ] );
        if ( minor != null ) {
            ticks = combineTicks( ticks, minor.toArray( new Tick[ 0 ] ) );
        }
        return ticks;
    }

    /**
     * Returns a list of round numbers between 1 and 9 (inclusive)
     * to use as multipliers for axis ticks.
     *
     * @param   approxCount  approximate number of values required,
     *                       in the region of 1-10
     * @return   array of values
     */
    private static int[] getLogMantissas( double approxCount ) {
        if ( approxCount < 1.5 ) {
            return new int[] { 1 };
        }
        else if ( approxCount < 4.5 ) {
            return new int[] { 1, 2, 5 };
        }
        else if ( approxCount < 7.5 ) {
            return new int[] { 1, 2, 3, 4, 5 };
        }
        else {
            return new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        }
    }

    /**
     * Creates a tick for use on a logarithmic axis.
     *
     * @param  mantissa  multiplier in range 1-9 (inclusive)
     * @param  exponent  power of 10
     * @return  labelled tick
     */
    private static Tick createLogTick( int mantissa, int exponent ) {
        assert mantissa > 0 && mantissa < 10;
        double value = mantissa * exp10( exponent );

        /* Some care is required assembling the label, to make sure we
         * avoid rounding issues (like 0.999999999999).
         * Double.toString() is not good enough. */
        final String label;
        String smantissa = Integer.toString( mantissa );
        if ( exponent == 0 ) {
            label = smantissa;
        }
        else if ( exponent > -4 && exponent < 0 ) {
            label = "0." + zeros( - exponent - 1 ) + smantissa;
        }
        else if ( exponent < 4 && exponent > 0 ) {
            label = smantissa + zeros( exponent );
        }
        else {
            label = smantissa + "E" + exponent;
        }
        assert Math.abs( ( Double.parseDouble( label ) / value ) - 1 ) < 1e-10
             : '"' + label + '"' + " != " + value;
        return new Tick( value, label );
    }

    /**
     * Generates a tick array suitable for labelling a linear axis.
     *
     * @param   dlo  minimum axis data value
     * @param   dhi  maximum axis data value
     * @param   approxTicks  approximate number of major ticks to return
     * @param   withMinor  true to include minor ticks as well
     * @return  tick array
     */
    private static Tick[] labelLinearAxis( double lo, double hi,
                                           int approxTicks,
                                           boolean minorTick ) {
        if ( hi <= lo ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + lo + " .. " + hi );
        }

        /* Get major ticks. */
        Tick[] ticks = getLinearTicks( lo, hi, approxTicks, true );

        /* Add minor ticks if required. */
        if ( minorTick ) {
            ticks = combineTicks( ticks,
                                  getLinearTicks( lo, hi, approxTicks * 5,
                                                  false ) );
        }
        return ticks;
    }

    /**
     * Generates an array of all-major or all-minor ticks
     * suitable for labelling a linear axis.
     *
     * @param   dlo  minimum axis data value
     * @param   dhi  maximum axis data value
     * @param   approxTicks  approximate number of major ticks to return
     * @param   withLabels  whether the output ticks should have labels or not
     * @return  tick array
     */
    private static Tick[] getLinearTicks( double lo, double hi, int approxTicks,
                                          boolean withLabels ) {

        /* We need to come up with an exponent and a multiplier representing
         * the inter-tick gap.  Represent these both as integers to avoid
         * rounding errors.  Start off with an underestimate. */
        double range = hi - lo;
        double approxGap = range / approxTicks;
        int exp = (int) Math.floor( Math.log10( approxGap ) ) - 1;

        /* Iterate until we have a suitable number of ticks. */
        int mult = 1;
        for ( boolean done = false; ! done; ) {
            double oversize = approxGap / ( mult * exp10( exp ) );
            if ( oversize < 1.5 ) {
                done = true;
            }
            else if ( oversize < 3.5 ) {
                mult = 2;
                done = true;
            }
            else if ( oversize < 7.5 ) {
                mult = 5;
                done = true;
            }
            else {
                exp++;
            }
        }

        /* Turn these into an array of optionally labelled tick marks. */
        double unit = mult * exp10( exp );
        long ilo = (long) Math.ceil( lo / unit );
        long ihi = (long) Math.floor( hi / unit );
        int nt = (int) ( ihi - ilo + 1 );
        Tick[] ticks = new Tick[ nt ];
        for ( int it = 0; it < nt; it++ ) {
            long imult = ilo + it;
            double value = imult * unit;
            final Tick tick;
            if ( withLabels ) {
                String label = linearLabel( imult * mult, exp );
                assert Math.abs( Double.parseDouble( label ) - value ) < 1e-10
                     : '"' + label + '"' + " != " + value;
                tick = new Tick( value, label );
            }
            else {
                tick = new Tick( value );
            }
            ticks[ it ] = tick;
        }
        return ticks;
    }

    /**
     * Generates a tick label for a tick position on a linear axis.
     * Some care is required assembling the label, to make sure we
     * avoid rounding issues (like 0.999999999999).
     * Double.toString() is not good enough.
     *
     * @param  mantissa  multiplier
     * @param  exponent  power of 10
     * @return  tick label
     */
    private static String linearLabel( long mantissa, int exp ) {
        boolean minus = mantissa < 0;
        String sign = minus ? "-" : "";
        String digits = Long.toString( minus ? -mantissa : mantissa );
        int ndigit = digits.length();
        int sciLimit = 3;
        if ( mantissa == 0 ) {
            return "0";
        }
        else if ( exp >= 0 && exp <= sciLimit ) {
            return new StringBuffer()
                  .append( sign )
                  .append( digits )
                  .append( zeros( exp ) )
                  .toString();
        }
        else if ( exp < 0 && exp >= -sciLimit ) {
            int pointPos = ndigit + exp;
            if ( pointPos <= 0 ) {
                return new StringBuffer()
                      .append( sign )
                      .append( "0." )
                      .append( zeros( -pointPos ) )
                      .append( digits )
                      .toString();
            }
            else {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( sign )
                    .append( digits.substring( 0, pointPos ) );
                if ( pointPos < ndigit ) {
                    sbuf.append( "." )
                        .append( digits.substring( pointPos ) );
                }
                return sbuf.toString();
            }
        }
        else if ( exp > sciLimit ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( sign )
                .append( digits.charAt( 0 ) );
            int postDigit = ndigit - 1;
            if ( postDigit > 0 ) {
                sbuf.append( "." )
                    .append( digits.substring( 1 ) );
            }
            int pexp = exp + postDigit;
            if ( pexp > sciLimit ) {
                sbuf.append( "e" )
                    .append( Integer.toString( pexp ) );
            }
            else {
                sbuf.append( zeros( pexp ) );
            }
            return sbuf.toString();
        }
        else if ( exp < -sciLimit ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( sign );
            int pexp = exp + ndigit;
            if ( pexp == 0 ) {
                sbuf.append( digits );
            }
            else if ( pexp > 0 ) {
                sbuf.append( digits.substring( 0, pexp ) )
                    .append( "." )
                    .append( digits.substring( pexp ) );
            }
            else if ( pexp < 0 && pexp <= -sciLimit ) {
                sbuf.append( "0." )
                    .append( zeros( -pexp ) )
                    .append( digits );
            }
            else if ( pexp < -sciLimit ) {
                sbuf.append( digits.charAt( 0 ) )
                    .append( "." )
                    .append( digits.substring( 1 ) )
                    .append( "e" )
                    .append( Integer.toString( pexp ) );
            }
            else {
                assert false;
                sbuf.append( "??" );
            }
            return sbuf.toString();
        }
        else {
            assert false;
            return "??";
        }
    }

    /**
     * Combines arrays of major and minor ticks to produce a single list.
     * Any minor ticks whose values also appear in the major list are
     * removed.
     *
     * @param   major  major ticks
     * @param   minor  minor ticks
     * @return   combined tick list
     */
    private static Tick[] combineTicks( Tick[] major, Tick[] minor ) {
        Set<Double> values = new HashSet<Double>();
        List<Tick> ticks = new ArrayList<Tick>();
        for ( int i = 0; i < major.length; i++ ) {
            Tick tick = major[ i ];
            ticks.add( tick );
            assert ! values.contains( tick.getValue() );
            values.add( tick.getValue() );
        }
        for ( int i = 0; i < minor.length; i++ ) {
            Tick tick = minor[ i ];
            if ( ! values.contains( tick.getValue() ) ) {
                ticks.add( tick );
            }
        }
        return ticks.toArray( new Tick[ 0 ] );
    }

    /**
     * Power of ten.
     *
     * @param  exp  exponent
     * @return   pow(10,exp)
     */
    private static double exp10( int exp ) {
       return Math.pow( 10, exp );
    }

    /**
     * Returns a string which is a given number of zeros.
     *
     * @param  n  number of zeros
     * @return  zero-filled string of length <code>n</code>
     */
    private static String zeros( int n ) {
        StringBuffer sbuf = new StringBuffer( n );
        for ( int i = 0; i < n; i++ ) {
            sbuf.append( '0' );
        }
        return sbuf.toString();
    }
}
