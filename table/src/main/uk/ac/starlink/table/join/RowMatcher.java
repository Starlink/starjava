package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Performs matching on the rows of one or more tables.
 * The specifics of what constitutes a matched row, and some additional
 * intelligence about how to determine this, are supplied by an
 * associated {@link MatchEngine} object, but the generic parts of
 * the matching algorithms are done here.
 *
 * @author   Mark Taylor (Starlink)
 */
public class RowMatcher {

    private MatchEngine engine;

    /**
     * Constructs a new matcher with match characteristics defined by 
     * a given matching engine.
     *
     * @param  engine  matching engine
     */
    public RowMatcher( MatchEngine engine ) {
        this.engine = engine;
    }

    /**
     * Identifies all the pairs of equivalent rows in a given set of tables.
     * The result is a set of {@link RowLink2} objects, one for
     * each pair of rows which is equivalent according to this object's
     * <tt>MatchEngine</tt> implementation.  This will include pairs of
     * rows from the same table as well as pairs of rows from different
     * tables.
     * <p>
     * Note that the columns of the supplied tables must be arranged in
     * such a way that the match engine knows what they mean; 
     * the usual way to arrange this is to prepare a
     * {@link uk.ac.starlink.table.ColumnPermutedStarTable} for each 
     * table of interest which has just the columns required for matching,
     * in a standard order required by the match engine.
     *
     * @param  tables  array of tables to match
     * @return  set of {@link RowLink2} objects indicating pairs of matching
     *          rows
     */
    public MatchSet findPairs( StarTable[] tables ) throws IOException {
        int nTable = tables.length;

        /* Check that all our tables are random access. */
        for ( int itab = 0; itab < nTable; itab++ ) {
            if ( ! tables[ itab ].isRandom() ) {
                throw new IllegalArgumentException( "Table " + tables[ itab ]
                                                  + " is not random access" );
            }
        }

        /* For each table, identify which bin each row falls into, and 
         * place it in a map keyed by that bin. */
        Map rowMap = new HashMap();
        for ( int itab = 0; itab < nTable; itab++ ) {
            StarTable table = tables[ itab ];
            long lrow = 0;
            for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
                rseq.next();
                Object value = new RowRef( table, lrow );
                Object[] keys = engine.getBins( rseq.getRow() );
                int nkey = keys.length;
                for ( int ikey = 0; ikey < nkey; ikey++ ) { 
                    Object key = keys[ ikey ];
                    if ( ! rowMap.containsKey( key ) ) {
                        rowMap.put( key, new LinkedList() );
                    }
                    ((List) rowMap.get( key )).add( value );
                }
                lrow++;
            }
        }
 
        /* Go through all the bins, and identify any with more than one
         * entry - these are the possible matches. */
        MatchSet matchSet = new MatchSet();
        for ( Iterator it = rowMap.keySet().iterator(); it.hasNext(); ) {
            Object binId = it.next();
            List binnedRefs = new ArrayList( (Collection) rowMap.get( binId ) );
            int nref = binnedRefs.size();
            
            if ( nref > 1 ) {

                /* Cache the rows from each ref since it may be expensive to
                 * get them multiple times. */
                Object[][] binnedRows = new Object[ nref ][];
                for ( int i = 0; i < nref; i++ ) {
                    binnedRows[ i ] = ((RowRef) binnedRefs.get( i )).getRow();
                }

                /* Do a pairwise comparison of all the rows in the same bin,
                 * performing a proper test to see if they match. */
                for ( int i = 0; i < nref; i++ ) {
                    for ( int j = 0; j < i; j++ ) {

                        /* If they do match, record this by storing them in a
                         * pair of rows tied to each other. */
                        if ( engine.matches( binnedRows[ i ], 
                                             binnedRows[ j ] ) ) {
                            RowLink2 link = 
                                new RowLink2( (RowRef) binnedRefs.get( i ),
                                              (RowRef) binnedRefs.get( j ) );
                            matchSet.add( link );
                        }
                    }
                }
            }
        }

        /* Return the set of matched rows. */
        return matchSet;
    }
}
