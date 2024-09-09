package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.util.Bi;

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

    public TickRun getTicks( double dlo, double dhi, boolean withMinor,
                             Captioner captioner, Orientation[] orients,
                             int npix, double crowding ) {

        Bi<Rule,Orientation> orule =
            getRule( dlo, dhi, captioner, orients, npix, crowding );
        Rule rule = orule.getItem1();
        Orientation orient = orule.getItem2();
        Tick[] majors = getMajorTicks( rule, dlo, dhi );
        Tick[] ticks = withMinor
                     ? PlotUtil.arrayConcat( majors,
                                             BasicTicker
                                            .getMinorTicks( rule, dlo, dhi ) )
                     : majors;
        return new TickRun( ticks, orient );
    }

    /**
     * Returns a Rule suitable for a given axis labelling job.
     * This starts off by generating ticks at roughly a standard separation,
     * guided by the crowding parameter.
     * If none of the orientations can generate ticks without overlap,
     * it backs off until it finds a set of ticks that can be displayed
     * in a tidy fashion.
     *
     * @param   dlo        minimum axis data value
     * @param   dhi        maximum axis data value
     * @param   captioner  caption painter
     * @param   orients    label orientation options in order of preference
     * @param   npix       number of pixels along the axis
     * @param   crowding   1 for normal tick density on the axis,
     *                     lower for fewer labels, higher for more
     * @return   tick generation rule with associated orientation
     */
    private Bi<Rule,Orientation> getRule( double dlo, double dhi,
                                          Captioner captioner,
                                          Orientation[] orients,
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
         * When we have the ticks, try to find an orientation for which
         * they are not so crowded as to overlap.  If that's not possible,
         * back off to lower crowding levels until we have
         * something suitable. */
        Axis axis = Axis.createAxis( 0, npix, dlo, dhi, logFlag_, false );
        int maxAdjust = -5;
        for ( int adjust = 0 ; adjust > maxAdjust; adjust-- ) {
            Rule rule = createRule( dlo, dhi, approxMajorCount, adjust );
            Tick[] majors = getMajorTicks( rule, dlo, dhi );
            for ( Orientation orient : orients ) {
                if ( ! BasicTicker
                      .overlaps( majors, axis, captioner, orient ) ) {
                    return new Bi<Rule,Orientation>( rule, orient );
                }
            }
        }

        /* Adjustment is getting too extreme.  Return rule with overlapping
         * labels, too bad. */
        Rule rule = createRule( dlo, dhi, approxMajorCount, maxAdjust );
        return new Bi<Rule,Orientation>( rule, orients[ 0 ] );
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
        final Caption noPrefix = Caption.createCaption( "" );
        Caption lastPrefix = noPrefix;
        boolean usedPrefix = false;

        /* Go through each major tick, labelling it with at least the suffix.
         * If the prefix has changed from the previous one, include the
         * prefix. */
        for ( long index = rule.floorIndex( dlo ) - 1;
              rule.indexToValue( index ) <= dhi; index++ ) {
            double major = rule.indexToValue( index );
            Caption prefix = rule.indexToPrefix( index );
            prefix = prefix == null ? noPrefix : prefix;
            if ( major >= dlo && major <= dhi ) {
                Caption suffix = rule.indexToSuffix( index );
                boolean pre = ! prefix.equals( lastPrefix );
                usedPrefix = usedPrefix || pre;
                Caption caption = pre ? prefix.append( suffix )
                                      : suffix;
                list.add( new Tick( major, caption ) );
            }
            lastPrefix = prefix;
        }
        Tick[] ticks = list.toArray( new Tick[ 0 ] );

        /* If none of the labels included the prefix, pick one and add it on. 
         * Otherwise there is no absolute context. */
        if ( lastPrefix != noPrefix && ! usedPrefix && ticks.length > 0 ) {
            int imid = 0;
            Tick tick = ticks[ imid ];
            ticks[ imid ] = new Tick( tick.getValue(),
                                      lastPrefix.append( tick.getLabel() ) );
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
        Caption indexToPrefix( long index );

        /**
         * Returns the suffix part only for labelling the major tick
         * identified by a given index.
         *
         * @param  index   major tick index
         * @return   suffix part of label
         */
        Caption indexToSuffix( long index );
    }
}
