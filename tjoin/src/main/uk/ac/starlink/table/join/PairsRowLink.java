package uk.ac.starlink.table.join;

import java.util.HashMap;
import java.util.Map;

/**
 * RowLink implementation which stores a number of pair matches to a single
 * reference RowRef.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2007
 */
public class PairsRowLink extends RowLink {

    /**
     * Constructor.
     *
     * @param   ref0  reference RowRef, common to all pairs
     * @param   ref1s array of RowRefs which are pair matched to 
     *                <code>ref0</code>
     * @param   scores  array of scores for <code>ref1s</code>, same number of
     *                  elements
     * @param   bestOnly  true iff only the best match for each non-reference
     *                    table should be retained
     */
    public PairsRowLink( RowRef ref0, RowRef[] ref1s, double[] scores,
                         boolean bestOnly ) {
        super( amalgamateRefs( ref0, ref1s, scores, bestOnly ) );
    }

    /**
     * Returns the score associated with a given RowRef.
     * Will be NaN for the reference RowRef.
     *
     * @param   i  ref index
     */
    public double getScore( int i ) {
        RowRef ref = getRef( i );
        return ref instanceof ScoredRowRef ? ((ScoredRowRef) ref).getScore()
                                           : Double.NaN;
    }

    /**
     * Returns an array of RowRef objects comprising the reference one and
     * others.  In addition, some are replaced by corresponding 
     * <code>ScoredRowRef</code> objects.
     *
     * <p>This static method is invoked by the constructor.
     *
     * @param   ref0  reference row ref
     * @param   ref1s  array of other row refs which match ref0
     * @param   scores    array of scores, one for each of ref1s
     * @param   bestOnly  if true, only the best score for each 
     *          non-reference table will be retained and others
     *          will be discarded
     */
    private static RowRef[] amalgamateRefs( RowRef ref0, RowRef[] ref1s,
                                            double[] scores,
                                            boolean bestOnly ) {

        /* Turn ref1s into ScoredRefs. */
        ScoredRowRef[] sref1s = new ScoredRowRef[ ref1s.length ];
        for ( int i = 0; i < ref1s.length; i++ ) {
            sref1s[ i ] = new ScoredRowRef( ref1s[ i ], scores[ i ] );
        }

        /* If only the best matches are required, throw out any 
         * non-best ones. */
        if ( bestOnly ) {
            Map<Integer,ScoredRowRef> refMap =
                new HashMap<Integer,ScoredRowRef>();
            for ( int i = 0; i < sref1s.length; i++ ) {
                ScoredRowRef ref = sref1s[ i ];
                Integer key = Integer.valueOf( ref.getTableIndex() );
                if ( refMap.containsKey( key ) ) {
                    ScoredRowRef bestRef = refMap.get( key );
                    if ( ref.getScore() < bestRef.getScore() ) {
                        refMap.put( key, ref );
                    }
                }
                else {
                    refMap.put( key, ref );
                }
            }
            sref1s = refMap.values().toArray( new ScoredRowRef[ 0 ] );
        }

        /* Stick all of the refs together in a single array and return. */
        RowRef[] refs = new RowRef[ 1 + sref1s.length ];
        refs[ 0 ] = ref0;
        System.arraycopy( sref1s, 0, refs, 1, sref1s.length );
        return refs;
    }

    /**
     * Utility class which decorates RowRef with a score.
     * This score does not, affect the equals, hashCode or compareTo methods.
     */
    private static class ScoredRowRef extends RowRef {
        private final double score_;

        /**
         * Constructor.
         *
         * @param   ref   base row ref
         * @param   score   score value
         */
        ScoredRowRef( RowRef ref, double score ) {
            super( ref.getTableIndex(), ref.getRowIndex() );
            score_ = score;
        }

        /**
         * Returns the score value.
         *
         * @return   score
         */
        public double getScore() {
            return score_;
        }
    }
}
