package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Represents a ranged value that can differ according to the content
 * of a plot.  To calculate the range of an AuxScale it is not necessary
 * to have detailed information about the geometry of the plot surface
 * (the Surface object required for final plotting).  That means it
 * is not required to generate the actual plot surface geometry.
 * However, an approximate plot surface may be required for range
 * calculations.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public class AuxScale {

    private final String name_;

    /**
     * AuxScale object used for the standard colour scaling axis.
     */
    public static AuxScale COLOR = new AuxScale( "Aux" );

    /**
     * Constructor.
     *
     * @param  name   scale name for human consumption
     */
    public AuxScale( String name ) {
        name_ = name;
    }

    /**
     * Returns the scale name.
     *
     * @return   name
     */
    public String getName() {
        return name_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Utility method to acquire all the scales that are used in an
     * array of layers.  The result is unordered and contains each scale
     * only once (like a Set).
     *
     * @param   layers   layers that may reference scales
     * @return   array of uses scales
     */
    public static AuxScale[] getAuxScales( PlotLayer[] layers ) {
        Set<AuxScale> scaleSet = new HashSet<AuxScale>();
        for ( int il = 0; il < layers.length; il++ ) {
            scaleSet.addAll( layers[ il ].getAuxRangers().keySet() );
        }
        return scaleSet.toArray( new AuxScale[ 0 ] );
    }

    /** 
     * Fills in range information for a submitted list of AuxScale objects
     * from the data in a given list of layers.
     * The supplied plot surface may be used to assess marker shape etc.
     * It should have approximately the same behaviour as the surface on
     * which the plot will take place, but it is usually not critical that
     * it behaves identically.
     *
     * @param   scales  scales to calculate ranges for
     * @param   layers   plot layers
     * @param   surface   approximate plot surface
     * @param   knownPlans  array of available plan objects; may be empty
     * @param   dataStore  data repository
     * @return   map with a range entry for each of the <code>scales</code>
     */
    public static Map<AuxScale,Span>
             calculateAuxSpans( AuxScale[] scales, PlotLayer[] layers,
                                Surface surface, Object[] knownPlans,
                                DataStore dataStore ) {
        Map<AuxScale,Span> spanMap = new HashMap<AuxScale,Span>();
        for ( int is = 0; is < scales.length; is++ ) {
            AuxScale scale = scales[ is ];
            spanMap.put( scale,
                         calculateSpan( scale, layers, surface, knownPlans,
                                        dataStore ) );
        }
        return spanMap;
    }

    /**
     * Calculates range information for an AuxScale object from the
     * data in a given list of layers.
     *
     * @param   scale  scale to calculate ranges for
     * @param   layers   plot layers
     * @param   surface  approximate plot surface
     * @param   knownPlans  array of available plan objects; may be empty
     * @param   dataStore   data repository
     * @return   span for <code>scale</code>
     */
    private static Span calculateSpan( AuxScale scale, PlotLayer[] layers,
                                       Surface surface, Object[] knownPlans,
                                       DataStore dataStore ) {

        /* Work out what kind of ranging is required, and construct a
         * suitable ranger object. */
        Collection<Scaling> scalings = new HashSet<Scaling>();
        for ( PlotLayer layer : layers ) {
            AuxReader rdr = layer.getAuxRangers().get( scale );
            if ( rdr != null ) {
                scalings.add( rdr.getScaling() );
            }
        }
        Ranger ranger =
            Scalings.createRanger( scalings.toArray( new Scaling[ 0 ] ) );

        /* Feed the data from all the relevant layers to the ranger
         * to generate the result. */
        for ( PlotLayer layer : layers ) {
            AuxReader rdr = layer.getAuxRangers().get( scale );
            if ( rdr != null ) {
                rdr.adjustAuxRange( surface, layer.getDataSpec(), dataStore,
                                    knownPlans, ranger );
            }
        }
        return ranger.createSpan();
    }

    /**
     * Amalgamates range requirements for a set of scales to return
     * actual ranges to be used.
     * An array of input scales is supplied, along with several 
     * maps of optional range constraints, each of which may contain entries
     * for zero or more of the input scales.  It is not required to supply
     * entries to any of these maps for each given scale.
     * The returned map has one entry for each scales element.
     *
     * @param   scales  list of scales for which output ranges are required
     * @param   dataSpans   actual data ranges acquired by scanning the data,
     *                      keyed by scale (optional per scale)
     * @param   fixSpans    single- or double-ended fixed data ranges,
     *                      keyed by scale (optional per scale)
     * @param   subranges   subrange keyed by scale; optional per scale,
     *                      if absent 0-1 is assumed
     * @param   logFlags    flags indicating logarithmic scale; optional
     *                      per scale, absent equivalent to false indicates
     *                      linear scaling
     * @return   map with one entry for each input scale giving definite
     *           ranges to use in the plot
     */
    public static Map<AuxScale,Span>
            getClippedSpans( AuxScale[] scales,
                             Map<AuxScale,Span> dataSpans,
                             Map<AuxScale,Span> fixSpans,
                             Map<AuxScale,Subrange> subranges,
                             Map<AuxScale,Boolean> logFlags ) {
        Map<AuxScale,Span> clipSpans = new HashMap<AuxScale,Span>();
        for ( int i = 0; i < scales.length; i++ ) {
            AuxScale scale = scales[ i ];
            boolean logFlag = logFlags.containsKey( scale )
                            ? logFlags.get( scale )
                            : false;
            clipSpans.put( scale, clipSpan( dataSpans.get( scale ),
                                            fixSpans.get( scale ),
                                            subranges.get( scale ),
                                            logFlag ) );
        }
        return clipSpans;
    }

    /**
     * Amalgamates range requirements for a single scale to return the
     * actual range to use.  Any or all of the arguments may be null.
     *
     * @param   dataRange  actual data range acquired by scanning the data,
     *                     or null
     * @param   fixRange   single- or double-ended fixed data range, or null
     * @param   subrange   subrange, if null 0-1 is assumed
     * @param   isLog   true for logarithmic scale, false for linear
     * @return   definite range for plotting
     */
    private static Span clipSpan( Span dataSpan, Span fixSpan,
                                  Subrange subrange, boolean isLog ) {
        Span span = dataSpan == null ? PlotUtil.EMPTY_SPAN : dataSpan;
        if ( fixSpan != null ) {
            span = span.limit( fixSpan.getLow(), fixSpan.getHigh() );
        }
        if ( subrange == null ) {
            return span;
        }
        else {
            double[] bounds = span.getFiniteBounds( isLog );
            bounds = PlotUtil.scaleRange( bounds[ 0 ], bounds[ 1 ],
                                          subrange, isLog );
            return span.limit( bounds[ 0 ], bounds[ 1 ] );
        }
    }

    /**
     * Returns a list of scale objects for which calculations are required.
     * A list of scale objects for which ranges are required is supplied,
     * along with optional associated data and fixed value ranges.
     * A list of those scales for which data scans are necessary,
     * on the assumption that they are to be fed to the
     * {@link #calculateAuxSpans calculateAuxSpans} method, is returned.
     *
     * @param  layers      layers to be plotted
     * @param  dataSpans    ranges acquired by scanning data keyed by scale,
     *                      (optional per scale)
     * @param  fixSpans     single- or double-ended fixed data ranges,
     *                      keyed by scale (optional per scale)
     * @return  list of scales for which data scans are required
     */

    public static AuxScale[] getMissingScales( PlotLayer[] layers,
                                               Map<AuxScale,Span> dataSpans,
                                               Map<AuxScale,Span> fixSpans ) {

        /* Get a set of scales along with associated scalings for all
         * the layers that will be plotted. */
        Map<AuxScale,Collection<Scaling>> reqMap =
            new HashMap<AuxScale,Collection<Scaling>>();
        for ( PlotLayer layer : layers ) {
            Map<AuxScale,AuxReader> auxRangers = layer.getAuxRangers();
            for ( Map.Entry<AuxScale,AuxReader> entry :
                  layer.getAuxRangers().entrySet() ) {
                AuxScale scale = entry.getKey();
                Scaling scaling = entry.getValue().getScaling();
                if ( ! reqMap.containsKey( scale ) ) {
                    reqMap.put( scale, new HashSet<Scaling>() );
                }
                reqMap.get( scale ).add( scaling );
            }
        }

        /* For each of the scales, add it to the missing list if we don't
         * have the relevant scaling information already. */
        List<AuxScale> missingScales = new ArrayList<AuxScale>();
        for ( Map.Entry<AuxScale,Collection<Scaling>> entry :
              reqMap.entrySet() ) {
            AuxScale scale = entry.getKey();
            Scaling[] scalings = entry.getValue().toArray( new Scaling[ 0 ] );
            if ( ! Scalings.canScale( scalings, dataSpans.get( scale ),
                                      fixSpans.get( scale ) ) ) {
                missingScales.add( scale );
            }
        }
        return missingScales.toArray( new AuxScale[ 0 ] );
    }
}
