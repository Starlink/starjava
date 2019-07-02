package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.List;

/**
 * Partial Ticker implementation for major tick labels with prefixes.
 * Prefixes are typically common to several adjacent ticks,
 * but to save space only actually included for one of each class,
 * for instance something like:
 * <pre>
 *    |---------|---------|---------|---------|---------|---------|
 *  10:00       20        30        40        50      11:00       10 
 * </pre>
 * <p>This partial implementation is based on a tick generation rule
 * in the same way as <code>BasicTicker</code>.
 * 
 * @author   Mark Taylor
 * @since    18 Oct 2013
 * @see     BasicTicker
 */
public abstract class PrefixTicker implements Ticker {

    private final boolean logFlag_;
    private final BasicTicker basicTicker_;

    /**
     * Constructor.
     *
     * @param   logFlag   true for logarithmic axis, false for linear
     */
    public PrefixTicker( boolean logFlag ) {
        logFlag_ = logFlag;
        basicTicker_ = new BasicTicker( logFlag ) {
            public BasicTicker.Rule createRule( double dlo, double dhi,
                                                double approxMajorCount,
                                                int adjust ) {
                return PrefixTicker.this
                      .createRule( dlo, dhi, approxMajorCount, adjust );
            }
        };
    }

    /**
     * Returns a new rule for labelling an axis in a given range.
     * The tick density is determined by two parameters,
     * <code>approxMajorCount</code>, which gives a baseline value for
     * the number of ticks required over the given range, and
     * <code>adjust</code>.
     * Increasing <code>adjust</code> will give more major ticks, and
     * decreasing it will give fewer ticks.
     * Each value of adjust should result in a different tick count.
     *
     * @param   dlo     minimum axis data value
     * @param   dhi     maximum axis data value
     * @param   approxMajorCount  guide value for number of major ticks
     *                            in range
     * @param   adjust  adjusts density of major ticks, zero is normal
     */
    public abstract Rule createRule( double dlo, double dhi,
                                     double approxMajorCount,
                                     int adjust );

    public Tick[] getTicks( double dlo, double dhi, boolean withMinor,
                            Captioner captioner, Orientation orient,
                            int npix, double crowding ) {

        Rule rule = getRule( dlo, dhi, captioner, orient, npix, crowding );
        Tick[] majors = getMajorTicks( rule, dlo, dhi );
        return withMinor
             ? PlotUtil
              .arrayConcat( majors,
                            BasicTicker.getMinorTicks( rule, dlo, dhi ) )
             : majors;
    }

    /**
     * Returns a Rule suitable for a given axis labelling job.
     * This starts off by generating ticks at roughly a standard separation,
     * guided by the crowding parameter.  However, if the resulting ticks
     * are so close as to overlap, it backs off until it finds a set of
     * ticks that can be displayed in a tidy fashion.
     *
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @param   captioner  caption painter
     * @param   orient     label orientation
     * @param   npix       number of pixels along the axis
     * @param   crowding   1 for normal tick density on the axis,
     *                     lower for fewer labels, higher for more
     * @return   tick generation rule
     */
    private Rule getRule( double dlo, double dhi,
                          Captioner captioner, Orientation orient,
                          int npix, double crowding ) {

        /* This implementation is copied from BasicTicker.
         * However, I don't want to inherit it, since it might be
         * advantageous to change the behaviour, for instance to knock
         * out un-prefixed labels next to the prefixed ones to get more
         * space. */
        if ( dhi <= dlo  ) {
            throw new IllegalArgumentException( "Bad range: "
                                              + dlo + " .. " + dhi );
        }

        /* Work out approximately how many major ticks are requested. */
        double approxMajorCount = Math.max( 1, npix / 80 ) * crowding;

        /* Acquire a suitable rule and use it to generate the major ticks.
         * When we have the ticks, check that they are not so crowded as
         * to generate overlapping tick labels.  If they are, back off
         * to lower tick crowding levels until we have something suitable. */
        Axis axis = Axis.createAxis( 0, npix, dlo, dhi, logFlag_, false );
        int adjust = 0;
        Rule rule;
        Tick[] majors;
        do {
            rule = createRule( dlo, dhi, approxMajorCount, adjust );
            majors = getMajorTicks( rule, dlo, dhi );
       } while ( BasicTicker.overlaps( majors, axis, captioner, orient )
                  && adjust-- > -5 );
        return rule;
    }

    /**
     * Use a given rule to generate major ticks in a given range of
     * coordinates.
     *
     * @param   rule    tick generation rule
     * @param   dlo     minimum axis data value
     * @param   dhi     maximum axis data value
     * @return  array of major ticks
     */
    private Tick[] getMajorTicks( Rule rule, double dlo, double dhi ) {
        List<Tick> list = new ArrayList<Tick>();
        String lastPrefix = "";
        boolean usedPrefix = false;

        /* Go through each major tick, labelling it with at least the suffix.
         * If the prefix has changed from the previous one, include the
         * prefix. */
        for ( long index = rule.floorIndex( dlo ) - 1;
              rule.indexToValue( index ) <= dhi; index++ ) {
            double major = rule.indexToValue( index );
            String prefix = rule.indexToPrefix( index );
            prefix = prefix == null ? "" : prefix;
            if ( major >= dlo && major <= dhi ) {
                String suffix = rule.indexToLabel( index );
                boolean pre = ! prefix.equals( lastPrefix );
                usedPrefix = usedPrefix || pre;
                list.add( new Tick( major, pre ? prefix + suffix : suffix ) );
            }
            lastPrefix = prefix;
        }
        Tick[] ticks = list.toArray( new Tick[ 0 ] );

        /* If none of the labels included the prefix, pick one and add it on. 
         * Otherwise there is no absolute context. */
        if ( lastPrefix.length() > 0 && ! usedPrefix && ticks.length > 0 ) {
            int imid = 0;
            Tick tick = ticks[ imid ];
            ticks[ imid ] =
                new Tick( tick.getValue(), lastPrefix + tick.getLabel() );
        }
        return ticks;
    }

    /**
     * Defines a specific rule for generating tick marks with prefixes.
     * It just extends BasicTicker.Rule with a method for generating the
     * prefix.  The inherited {@link #indexToLabel} method should provide
     * the suffix part.
     */
    public interface Rule extends BasicTicker.Rule {
 
        /**
         * Returns the prefix part only for labelling the major tick
         * identified by a given index.
         *
         * @param  index   major tick index
         * @return   prefix part of label
         */
        String indexToPrefix( long index );

        /**
         * Returns the suffix part only for labelling the major tick
         * identified by a given index.
         *
         * @param  index   major tick index
         * @return   suffix part of label
         */
        String indexToLabel( long index );
    }
}
