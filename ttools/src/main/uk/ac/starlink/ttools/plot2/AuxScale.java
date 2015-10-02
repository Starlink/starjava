package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

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
     * AuxRange object used for the standard colour scaling axis.
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
     * @param   dataStore  data repository
     * @return   map with a range entry for each of the <code>scales</code>
     */
    public static Map<AuxScale,Range>
             calculateAuxRanges( AuxScale[] scales, PlotLayer[] layers,
                                 Surface surface, DataStore dataStore ) {
        Map<AuxScale,Range> rangeMap = new HashMap<AuxScale,Range>();
        for ( int is = 0; is < scales.length; is++ ) {
            AuxScale scale = scales[ is ];
            rangeMap.put( scale,
                          calculateRange( scale, layers, surface, dataStore ) );
        }
        return rangeMap;
    }

    /**
     * Calculates range information for an AuxScale object from the
     * data in a given list of layers.
     *
     * @param   scale  scale to calculate ranges for
     * @param   layers   plot layers
     * @param   surface  approximate plot surface
     * @param   dataStore   data repository
     * @return   range for <code>scale</code>
     */
    private static Range calculateRange( AuxScale scale, PlotLayer[] layers,
                                         Surface surface,
                                         DataStore dataStore ) {
        Range range = new Range();
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            AuxReader rdr = layer.getAuxRangers().get( scale );
            if ( rdr != null ) {
                TupleSequence tseq =
                    dataStore.getTupleSequence( layer.getDataSpec() );
                rdr.adjustAuxRange( surface, tseq, range );
            }
        }
        return range;
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
     * @param   dataRanges  actual data ranges acquired by scanning the data,
     *                      keyed by scale (optional per scale)
     * @param   fixRanges   single- or double-ended fixed data ranges,
     *                      keyed by scale (optional per scale)
     * @param   subranges   subrange keyed by scale; optional per scale,
     *                      if absent 0-1 is assumed
     * @param   logFlags    flags indicating logarithmic scale; optional
     *                      per scale, absent equivalent to false indicates
     *                      linear scaling
     * @return   map with one entry for each input scale giving definite
     *           ranges to use in the plot
     */
    public static Map<AuxScale,Range>
            getClippedRanges( AuxScale[] scales,
                              Map<AuxScale,Range> dataRanges,
                              Map<AuxScale,Range> fixRanges,
                              Map<AuxScale,Subrange> subranges,
                              Map<AuxScale,Boolean> logFlags ) {
        Map<AuxScale,Range> clipRanges = new HashMap<AuxScale,Range>();
        for ( int i = 0; i < scales.length; i++ ) {
            AuxScale scale = scales[ i ];
            boolean logFlag = logFlags.containsKey( scale )
                            ? logFlags.get( scale )
                            : false;
            clipRanges.put( scale, clipRange( dataRanges.get( scale ),
                                              fixRanges.get( scale ),
                                              subranges.get( scale ),
                                              logFlag ) );
        }
        return clipRanges;
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
    public static Range clipRange( Range dataRange, Range fixRange,
                                   Subrange subrange, boolean isLog ) {
        Range fullRange = dataRange == null ? new Range()
                                            : new Range( dataRange );
        if ( fixRange != null ) {
            fullRange.limit( fixRange );
        }
        if ( subrange == null ) {
            return fullRange;
        }
        else {
            double[] bounds = fullRange.getFiniteBounds( isLog );
            return new Range( PlotUtil.scaleRange( bounds[ 0 ], bounds[ 1 ],
                                                   subrange, isLog ) );
        }
    }

    /**
     * Returns a list of scale objects for which calculations are required.
     * A list of scale objects for which ranges are required is supplied,
     * along with optional associated data and fixed value ranges.
     * A list of those scales for which data scans are necessary,
     * on the assumption that they are to be fed to the
     * {@link #getClippedRanges} method, is returned.
     *
     * @param  reqScales  scales needed
     * @param  dataRanges   ranges acquired by scanning data keyed by scale,
     *                      (optional per scale)
     * @param  fixRanges    single- or double-ended fixed data ranges,
     *                      keyed by scale (optional per scale)
     * @return  list of scales for which data scans are required
     */
    public static AuxScale[] getMissingScales( AuxScale[] reqScales,
                                               Map<AuxScale,Range> dataRanges,
                                               Map<AuxScale,Range> fixRanges ) {
        List<AuxScale> scaleList = new ArrayList<AuxScale>();
        for ( int i = 0; i < reqScales.length; i++ ) {
            AuxScale scale = reqScales[ i ];
            Range dataRange = dataRanges.get( scale );
            Range fixRange = fixRanges.get( scale );
            if ( dataRange != null && dataRange.isFinite() ||
                 fixRange != null && fixRange.isFinite() ) {
                // no action
            }
            else {
                scaleList.add( scale );
            }
        }
        return scaleList.toArray( new AuxScale[ 0 ] );
    }
}
