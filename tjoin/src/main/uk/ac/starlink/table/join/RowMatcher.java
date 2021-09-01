package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Performs matching on the rows of one or more tables.
 * The specifics of what constitutes a matched row, and some additional
 * intelligence about how to determine this, are supplied by an
 * associated {@link MatchEngine} object, but the generic parts of
 * the matching algorithms are done here. 
 *
 * <p>Note that since the LinkSets and other objects handled by this
 * class may be very large when large tables are being matched, 
 * the algorithms in this class are coded carefully to use as little
 * memory as possible.  Techniques include removing items from one
 * collection as they are added to another.  This means that in many
 * cases input values may be modified by the methods.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class RowMatcher {

    private final MatchEngine engine_;
    private final StarTable[] tables_;
    private final int nTable_;
    private ProgressIndicator indicator_;
    private long startTime_;

    /**
     * Constructs a new matcher with match characteristics defined by
     * a given matching engine.
     *
     * @param  engine  matching engine
     * @param  tables  the array of tables on which matches are to be done
     */
    public RowMatcher( MatchEngine engine, StarTable[] tables ) {
        engine_ = engine;
        tables_ = tables;
        nTable_ = tables.length;
        indicator_ = new NullProgressIndicator();
    }

    /**
     * Sets the progress indicator for this matcher.
     * 
     * @param  indicator  new indicator
     */
    public void setIndicator( ProgressIndicator indicator ) {
        indicator_ = indicator;
    }

    /**
     * Returns the current progress indicator for this matcher.
     *
     * @return   indicator
     */
    public ProgressIndicator getIndicator() {
        return indicator_;
    }

    /**
     * Constructs a new empty LinkSet for use by this matcher.
     * The current implementation returns one based on a SortedSet,
     * but future implementations may provide the option of LinkSet
     * implementations backed by disk.
     *
     * @return  new LinkSet
     */
    public LinkSet createLinkSet() {
        return new HashSetLinkSet();
    }

    /**
     * Returns a set of RowLink objects corresponding to a pairwise match
     * between this matcher's two tables performed with its match engine.
     * Each element in the returned list corresponds to a matched
     * pair with one entry from each of the input tables.
     *
     * @param  pairMode  matching mode to determine which rows appear
     *         in the result
     * @return  links representing matched rows
     */
    public LinkSet findPairMatches( PairMode pairMode )
            throws IOException, InterruptedException {
        if ( nTable_ != 2 ) {
            throw new IllegalStateException( "findPairMatches only makes sense"
                                           + " for 2 tables" );
        }
        startMatch();
        LinkSet pairs = pairMode.findPairMatches( this );
        endMatch();
        return pairs;
    }

    /**
     * Returns a set of RowLink objects corresponding to a pairwise match
     * between tables with given indices, possibly including unwanted
     * multiple entries.  At least one of the input tables must provide
     * random access.
     *
     * @param  index1  index of first table to match
     * @param  index2  index of second table to match
     * @param  bestOnly  if false, all matches will be included in the result;
     *         if true, the best matches will be included and some non-best
     *         ones may be as well
     * @return  links representing pair matches
     */
    LinkSet findAllPairs( int index1, int index2, boolean bestOnly )
             throws IOException, InterruptedException {
        int ncol = getPairColumnCount( index1, index2 );

        /* Work out which table will have its rows cached in bins 
         * (R for Random) and which will just be scanned (S for Sequential),
         * and possibly calculate a common range within which any matches
         * must fall. */
        final int indexR;
        final int indexS;
        final NdRange range;

        /* If neither table has random access, we can't proceed. */
        if ( ! tables_[ index1 ].isRandom() &&
             ! tables_[ index2 ].isRandom() ) {
            throw new IllegalArgumentException( "Neither table random-access" );
        }

        /* If only one table has random access, use that as table R. */
        else if ( ! tables_[ index1 ].isRandom() ) {
            assert tables_[ index2 ].isRandom();
            indexS = index1;
            indexR = index2;
            range = new NdRange( ncol );
        }
        else if ( ! tables_[ index2 ].isRandom() ) {
            assert tables_[ index1 ].isRandom();
            indexS = index2;
            indexR = index1;
            range = new NdRange( ncol );
        }

        /* If both tables have random access, calculate the possible match
         * ranges.  Then use the one with the smaller number of rows in range
         * as the random access one, since this should be cheaper. */
        else {
            Intersection intersect =
                getIntersection( new int[] { index1, index2 } );
            range = intersect.range_;

            /* No overlap means no matches. */
            if ( range == null ) {
                return createLinkSet();
            }
            else {
                long inRangeCount1 = intersect.inRangeCounts_[ 0 ];
                long inRangeCount2 = intersect.inRangeCounts_[ 1 ];
                if ( inRangeCount1 < inRangeCount2 ) {
                    indexR = index1;
                    indexS = index2;
                }
                else {
                    indexR = index2;
                    indexS = index1;
                }
            }
        }

        /* Perform the actual match given the table ordering and range we
         * have calculated. */
        return scanForPairs( indexR, indexS, range, bestOnly );
    }

    /**
     * Processes one table using random access and another using sequential
     * access within a given range to locate matched inter-table pairs.
     *
     * @param  indexR  index of table which will be accessed randomly
     * @param  indexS  index of table which will be accessed sequentially
     * @param  range   range outside which pairs can be ignored
     * @param  bestOnly  if false, all matches will be included in the result;
     *         if true, for each row in the sequential table, only the best
     *         match in the random table will be included
     * @return  links representing pair matches
     */
    LinkSet scanForPairs( int indexR, int indexS, NdRange range,
                          boolean bestOnly )
            throws IOException, InterruptedException {

        /* Bin the row indices for the random table. */
        ProgressRowSequence rseq =
            new ProgressRowSequence( tables_[ indexR ], indicator_,
                                     "Binning rows for table "
                                     + ( indexR + 1 ) );
        LongBinner binner =
            Binners.createLongBinner( tables_[ indexR ].getRowCount() );
        long nrow = 0;
        long nref = 0;
        long nexclude = 0;
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( range.isInside( row ) ) {
                    Object[] keys = engine_.getBins( row );
                    int nkey = keys.length;
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        binner.addItem( keys[ ikey ], lrow );
                    }
                    nref += nkey;
                }
                else {
                    nexclude++;
                }
                nrow++;
            }
            assert nrow == tables_[ indexR ].getRowCount();
        }
        finally {
            rseq.close();
        }
        if ( nexclude > 0 ) {
            indicator_.logMessage( nexclude + "/" + nrow + " rows excluded "
                                 + "(out of match region)" );
        }
        long nbin = binner.getBinCount();
        indicator_.logMessage( nref + " row refs for " + nrow + " rows in "
                             + nbin + " bins" );
        indicator_.logMessage( "(average bin occupancy " +
                               ( (float) nref / (float) nbin ) + ")" );

        /* Scan the rows for the sequential table. */
        LinkSet linkSet = createLinkSet();
        ProgressRowSequence sseq =
            new ProgressRowSequence( tables_[ indexS ], indicator_,
                                     "Scanning rows for table "
                                   + ( indexS + 1 ) );
        try {
            for ( long isrow = 0; sseq.nextProgress(); isrow++ ) {
                Object[] srowData = sseq.getRow();
                if ( range.isInside( srowData ) ) {

                    /* Identify rows from table R which may match table S. */
                    Object[] keys = engine_.getBins( srowData );
                    int nkey = keys.length;
                    Set<Long> rrowSet = new HashSet<Long>();
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        long[] rrows = binner.getLongs( keys[ ikey ] );
                        if ( rrows != null ) {
                            for ( int ir = 0; ir < rrows.length; ir++ ) {
                                rrowSet.add( new Long( rrows[ ir ] ) );
                            }
                        }
                    }
                    long[] rrows = new long[ rrowSet.size() ];
                    int ir = 0;
                    for ( Long rr : rrowSet ) {
                        rrows[ ir++ ] = rr.longValue();
                    }
                    Arrays.sort( rrows );

                    /* Score and accumulate matched links. */
                    List<RowLink2> linkList = new ArrayList<RowLink2>( 1 );
                    double bestScore = Double.MAX_VALUE;
                    for ( ir = 0; ir < rrows.length; ir++ ) {
                        long irrow = rrows[ ir ];
                        Object[] rrowData = tables_[ indexR ].getRow( irrow );
                        double score = engine_.matchScore( srowData, rrowData );
                        if ( score >= 0 &&
                             ( ! bestOnly || score < bestScore ) ) {
                            RowRef rref = new RowRef( indexR, irrow );
                            RowRef sref = new RowRef( indexS, isrow );
                            RowLink2 pairLink = new RowLink2( rref, sref );
                            pairLink.setScore( score );
                            if ( bestOnly ) {
                                bestScore = score;
                                linkList.clear();
                            }
                            linkList.add( pairLink );
                            assert ( ! bestOnly ) || ( linkList.size() == 1 );
                        }
                    }

                    /* Add matched links to output set. */
                    for ( RowLink2 pairLink : linkList ) {
                        assert ! linkSet.containsLink( pairLink );
                        linkSet.addLink( pairLink );
                    }
                }
            }
        }
        finally {
            sseq.close();
        }
        return linkSet;
    }

    /**
     * Returns a set of RowLink objects each of which represents matches
     * between one of the rows of a reference table and any of the other tables
     * which can provide matches.  Elements of the result set will be
     * instances of {@link PairsRowLink}.
     *
     * @param   index0  index of the reference table in the list of tables
     *          owned by this row matcher
     * @param   bestOnly  true if only the best match between the reference
     *          table and any other table should be retained
     * @param   joinTypes  inclusion criteria for output table rows
     * @return  set of PairsRowLink objects representing multi-pair matches
     */
    public LinkSet findMultiPairMatches( int index0, boolean bestOnly,
                                         MultiJoinType[] joinTypes )
            throws IOException, InterruptedException {
        checkRandom();
        if ( joinTypes.length != nTable_ ) {
            throw new IllegalArgumentException(
                "Options length " + joinTypes.length +
                " differs from table count " + nTable_ );
        }
        startMatch();

        /* Get all the possible candidates for inter-table links containing
         * the reference table. */
        LinkSet possibleLinks = getPossibleMultiPairLinks( index0 );

        /* Get the actual matches based on this set. */
        LinkSet multiLinks =
            findMultiPairMatches( possibleLinks, index0, bestOnly );

        /* We now have a set of links corresponding to all the matches
         * with one entry for each row of the reference table which has
         * one or more matches.  In the case that we want to output 
         * some links with unmatched parts, add new singleton row 
         * links as necessary. */
        LinkSet[] missing = new LinkSet[ nTable_ ];
        for ( int i = 0; i < nTable_; i++ ) {
            if ( joinTypes[ i ] == MultiJoinType.ALWAYS ) {
                missing[ i ] = missingSingles( multiLinks, i );
            }
        }
        for ( int i = 0; i < nTable_; i++ ) {
            if ( missing[ i ] != null ) {
                for ( RowLink link : missing[ i ] ) {
                    multiLinks.addLink( link );
                }
                missing[ i ] = null;
            }
        }

        /* Filter the links to contain only those rows we're interested in. */
        for ( Iterator<RowLink> it = multiLinks.iterator(); it.hasNext(); ) {
            RowLink link = it.next();
            if ( ! acceptRow( link, joinTypes ) ) {
                it.remove();
            }
        }

        /* Return. */
        endMatch();
        return multiLinks;
    }

    /**
     * Returns a list of RowLink objects corresponding to a match
     * performed with this matcher's tables using its match engine.
     * Each element in the returned list corresponds to a matched group of 
     * input rows, with no more than one entry from each table.
     * Each input table row appears in no more than one RowLink in
     * the returned list.
     * Any number of tables can be matched.
     * 
     * @param  joinTypes  inclusion criteria for output table rows
     * @return list of {@link RowLink}s corresponding to the selected rows
     */
    public LinkSet findGroupMatches( MultiJoinType[] joinTypes )
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have multiple tables. */
        if ( nTable_ < 2 ) {
            throw new IllegalStateException( "Find matches only makes sense "
                                           + "for multiple tables" );
        }

        /* Check that there are the right number of options. */
        if ( joinTypes.length != nTable_ ) {
            throw new IllegalArgumentException( 
                "Options length " + joinTypes.length +
                " differs from table count " + nTable_ );
        }
        startMatch();

        /* Get all the possible pairs. */
        LinkSet pairs = findPairs( getAllPossibleLinks() );

        /* Exclude any pairs which represent links between different rows
         * of the same table. */
        eliminateInternalLinks( pairs );

        /* Join up pairs into larger groupings. */
        LinkSet links = agglomerateLinks( pairs );
        pairs = null;

        /* This could introduce more internal links - get rid of them. */
        eliminateInternalLinks( links );

        /* We now have a set of links corresponding to all the matches
         * with one entry for each of two or more of the input tables.
         * In the case that we want to output some links with unmatched
         * parts, add new singleton row links as necessary. */
        LinkSet[] missing = new LinkSet[ nTable_ ];
        for ( int i = 0; i < nTable_; i++ ) {
            if ( joinTypes[ i ] == MultiJoinType.ALWAYS ) {
                missing[ i ] = missingSingles( links, i );
            }
        }
        for ( int i = 0; i < nTable_; i++ ) {
            if ( missing[ i ] != null ) {
                for ( RowLink link : missing[ i ] ) {
                    links.addLink( link );
                }
                missing[ i ] = null;
            }
        }

        /* Now filter the links to contain only those composite rows we're
         * interested in. */
        for ( Iterator<RowLink> it = links.iterator(); it.hasNext(); ) {
            RowLink link = it.next();
            if ( ! acceptRow( link, joinTypes ) ) {
                it.remove();
            }
        }

        /* Return the matched list. */
        endMatch();
        return links;
    }

    /**
     * Returns a list of RowLink objects corresponding to all the internal
     * matches in this matcher's sole table using its match engine.
     *
     * @param  includeSingles  whether to include unmatched (singleton) 
     *         row links in the returned link set
     * @return a set of {@link RowLink} objects giving all the groups of
     *         matched objects in this matcher's sole table
     */
    public LinkSet findInternalMatches( boolean includeSingles ) 
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have a single table. */
        if ( nTable_ != 1 ) {
            throw new IllegalStateException( "Internal matches only make sense "
                                           + "with a single table" );
        }
        startMatch();

        /* Locate all the pairs. */
        LinkSet links = findPairs( getAllPossibleInternalLinks( 0 ) );

        /* Join up pairs into larger groupings. */
        links = agglomerateLinks( links );

        /* Add unmatched rows if required. */
        if ( includeSingles ) {
            for ( Iterator<RowLink> it = missingSingles( links, 0 ).iterator();
                  it.hasNext(); ) {
                links.addLink( it.next() );
                it.remove();
            }
        }

        /* Return the list. */
        endMatch();
        return links;
    }

    /**
     * Identifies all the pairs of equivalent rows in a set of RowLinks.
     * Internal matches (ones corresponding to two rows of the same table)
     * are included as well as external ones.
     * The input set <code>possibleLinks</code> may be affected
     * by this routine.
     * 
     * @param  possibleLinks  a set of {@link RowLink} objects which 
     *         correspond to groups of possibly matched objects according
     *         to the match engine's criteria
     * @return  a set of RowLink objects which represent all the actual
     *          distinct row pairs from <tt>possibleLinks</tt>
     */
    private LinkSet findPairs( LinkSet possibleLinks )
            throws IOException, InterruptedException {
        LinkSet pairs = createLinkSet();
        double nLink = (double) possibleLinks.size();
        int iLink = 0;
        indicator_.startStage( "Locating pairs" );
        for ( Iterator<RowLink> it = possibleLinks.iterator(); it.hasNext(); ) {

            /* Obtain the link and remove it from the input set for 
             * memory efficiency. */
            RowLink link = it.next();
            it.remove();

            /* Check whether this link is non-trivial. */
            int nref = link.size();
            if ( nref > 1 ) {

                /* Cache the rows from each ref since it may be expensive
                 * to get them multiple times. */
                Object[][] binnedRows = new Object[ nref ][];
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    StarTable table = tables_[ ref.getTableIndex() ];
                    binnedRows[ i ] = table.getRow( ref.getRowIndex() );
                }

                /* Do a pairwise comparison of all the rows in the same group.
                 * If they match, add the new pair to the set of pairs. */
                for ( int i = 0; i < nref; i++ ) {
                    for ( int j = 0; j < i; j++ ) {
                        RowLink2 pair = new RowLink2( link.getRef( i ),
                                                      link.getRef( j ) );
                        if ( ! pairs.containsLink( pair ) ) {
                            double score = engine_.matchScore( binnedRows[ i ],
                                                               binnedRows[ j ]);
                            if ( score >= 0 ) {
                                pair.setScore( score );
                                pairs.addLink( pair );
                            }
                        }
                    }
                }
            }
            indicator_.setLevel( ++iLink / nLink );
        }
        indicator_.endStage();
        return pairs;
    }

    /**
     * Goes through all tables and gets a preliminary set of all
     * the groups of rows which are possibly linked by a chain of
     * matches.  This includes all inter- and intra-table matches.
     *
     * @return  set of {@link RowLink} objects which constitute possible
     *          matches
     */
    private LinkSet getAllPossibleLinks()
            throws IOException, InterruptedException {
        NdRange range = new NdRange( tables_[ 0 ].getColumnCount() );
        ObjectBinner<Object,RowRef> binner = Binners.createObjectBinner();
        long totalRows = 0;
        for ( int itab = 0; itab < nTable_; itab++ ) {
            binRows( itab, range, binner, true );
            totalRows += tables_[ itab ].getRowCount();
        }
        long nBin = binner.getBinCount();
        indicator_.logMessage( "Average bin count per row: " +
                               (float) ( nBin / (double) totalRows ) );
        LinkSet links = createLinkSet();
        binsToLinks( binner, links );
        return links;
    }

    /**
     * Goes through the rows of a single table and gets a set of all
     * the groups of rows which are possibly linked by a chain of matches.
     *
     * @param   itable  index of table to examine
     * @return  set of {@link RowLink} objects which constitute possible
     *          matches
     */
    private LinkSet getAllPossibleInternalLinks( int itable )
            throws IOException, InterruptedException {
        StarTable table = tables_[ itable ];
        long nRow = table.getRowCount();
        LongBinner binner = Binners.createLongBinner( nRow );
        ProgressRowSequence rseq =
            new ProgressRowSequence( table, indicator_, "Binning rows" );
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                Object[] keys = engine_.getBins( row );
                int nkey = keys.length;
                for ( int ikey = 0; ikey < nkey; ikey++ ) {
                    binner.addItem( keys[ ikey ], lrow );
                }
            }
        }
        finally {
            rseq.close();
        }
        long nBin = binner.getBinCount();
        indicator_.logMessage( "Average bin count per row: " +
                               (float) ( nBin / (double) nRow ) );

        LinkSet links = createLinkSet();
        binsToInternalLinks( binner, links, itable );
        return links;
    }

    /**
     * Attempts to locate an intersection between multiple tables.
     * If we have a match engine which is capable of working out a restricted
     * region over which matches are possible, an intersection can be 
     * identified.  Subsequent work can be restricted to such a range,
     * which can save CPU time and memory in some cases.
     * If the match engine is not capable of making range calculations,
     * an unrestrictive range will be returned.
     *
     * @param   iTables  indices of tables whose common range is to be found
     * @return  range within which the intersection of all input tables
     *          is guaranteed to be located
     */
    private Intersection getIntersection( int[] iTables )
            throws IOException, InterruptedException {
        int ncol = tables_[ iTables[ 0 ] ].getColumnCount();
        int nt = iTables.length;
        long[] inRangeCounts = new long[ nt ];
        for ( int iTable = 0; iTable < nt; iTable++ ) {
            int index = iTables[ iTable ];
            inRangeCounts[ iTable ] = tables_[ index ].getRowCount();
        }
        NdRange range;
        if ( engine_.canBoundMatch() && iTables.length > 1 ) {
            indicator_.logMessage( "Attempt to locate " +
                                   "restricted common region" );
            try {
                NdRange[] inRanges = new NdRange[ nt ];
                for ( int iTable = 0; iTable < nt; iTable++ ) {
                    int index = iTables[ iTable ];
                    inRanges[ iTable ] = readTupleRange( index );
                }
                NdRange[] extRanges = new NdRange[ nt ];
                for ( int iTable = 0; iTable < nt; iTable++ ) {
                    extRanges[ iTable ] =
                        engine_.getMatchBounds( inRanges, iTable );
                }
                range = extRanges[ 0 ];
                for ( int iTable = 1; iTable < nt; iTable++ ) {
                    range = NdRange.intersection( range, extRanges[ iTable ] );
                }
                if ( range != null ) {
                    indicator_.logMessage( "Potential match region: " + range );
                    for ( int iTable = 0; iTable < nt; iTable++ ) {
                        int index = iTables[ iTable ];
                        inRangeCounts[ iTable ] = countInRange( index, range );
                    }
                }
                else {
                    indicator_.logMessage( "No region overlap"
                                         + " - matches not possible" );
                    return new Intersection( null, new long[ iTables.length ] );
                }
            }

            /* The compare() method used in the above processing could
             * result in ClassCastExceptions if some of the columns 
             * are comparable, but not mutually comparable.
             * This is not very likely, but in that case catch the error,
             * forget about trying to identify bounds, and move on.
             * This will only impact efficiency, not correctness. */ 
            catch ( ClassCastException e ) {
                indicator_.logMessage( "Common region location failed " +
                                       "(incompatible value types)" );
                range = new NdRange( ncol );
            }
        }
        else {
            range = new NdRange( ncol );
        }
        return new Intersection( range, inRangeCounts );
    }

    /**
     * Removes any links in a set between different rows of the same table.
     * More precisely, every RowLink in the given set is checked and 
     * if necessary replaced by one that contains no more than one RowRef
     * from any one table.  If any refs are discarded from a group,
     * the one earliest in the sort order is retained.
     *
     * @param   links  a mutable collection of {@link RowLink} objects 
     *          to operate on
     */
    private void eliminateInternalLinks( LinkSet links )
            throws InterruptedException {
        RowRef[] refs = new RowRef[ nTable_ ];
        LinkSet replacements = createLinkSet();

        /* Go through every link in the set. */
        indicator_.startStage( "Eliminating internal links" );
        double nLink = (double) links.size();
        int iLink = 0;
        int nReplace = 0;
        int nRemove = 0;
        for ( Iterator<RowLink> it = links.iterator(); it.hasNext(); ) {
            RowLink link = it.next();
            int nref = link.size();
            if ( link.size() > 1 ) {

                /* Fill up slots for each table with zero or one RowRef,
                 * noting if we come across any multiple potential entries
                 * from the same table. */
                Arrays.fill( refs, null );
                boolean dup = false;
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    int iTable = ref.getTableIndex();
                    if ( refs[ iTable ] == null ) {
                        refs[ iTable ] = ref;
                    }
                    else {
                        dup = true;
                    }
                }

                /* If we came across duplicates, remove this entry from the
                 * set and schedule a replacement for addition later. */
                if ( dup ) {
                    it.remove();
                    List<RowRef> repRefs = new ArrayList<RowRef>();
                    for ( int i = 0; i < nTable_; i++ ) {
                        if ( refs[ i ] != null ) {
                            repRefs.add( refs[ i ] );
                        }
                    }

                    /* Only schedule a replacement if the link is
                     * non-trivial. */
                    if ( repRefs.size() > 1 ) {
                        replacements.addLink( RowLink.createLink( repRefs ) );
                        nReplace++;
                    }
                    else {
                        nRemove++;
                    }
                }
            }
            indicator_.setLevel( ++iLink / nLink );
        }
        indicator_.endStage();
        if ( nReplace > 0 ) {
            indicator_.logMessage( "Internal links replaced: " + nReplace );
        }
        if ( nRemove > 0 ) {
            indicator_.logMessage( "Internal links removed: " + nRemove );
        }
        for ( Iterator<RowLink> it = replacements.iterator(); it.hasNext(); ) {
            RowLink repLink = it.next();
            links.addLink( repLink );
            it.remove();
        }
    }

    /**
     * Constructs a set of singleton row links representing rows which 
     * are not already represented in a given link set.
     *
     * @param   links  set of {@link RowLink}s
     * @param   iTable  table index for the rows which must be represented
     * @return  new set of RowLinks containing one singleton entry for
     *          each RowRef in table <tt>iTable</tt> which does not appear
     *          in any of the links in <tt>links</tt>
     */
    private LinkSet missingSingles( LinkSet links, int iTable ) {

        /* Find out what rowrefs for this table are present in all the links. */
        BitSet present = new BitSet();
        for ( RowLink link : links ) {
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() == iTable ) {
                    present.set( checkedLongToInt( ref.getRowIndex() ) );
                }
            }
        }

        /* Construct and return a set with one new singleton RowRef for 
         * each row that is not present. */
        int nrow = checkedLongToInt( tables_[ iTable ].getRowCount() );
        LinkSet singles = createLinkSet();
        for ( int iRow = 0; iRow < nrow; iRow++ ) {
            if ( ! present.get( iRow ) ) {
                singles.addLink( new RowLink1( new RowRef( iTable, iRow ) ) );
            }
        }
        return singles;
    }

    /**
     * Gets a list of all the row links which constitute possible matches
     * including a given reference table.  Links of interest are only those 
     * ones which constitute pair matches from the reference table to another.
     * This differs from the semantics of a match group.
     *
     * @param  index0  index of reference table 
     */
    private LinkSet getPossibleMultiPairLinks( int index0 )
            throws IOException, InterruptedException {
        int ncol = tables_[ index0 ].getColumnCount();

        /* Attempt to restrict ranges for match assessments if we can. */
        NdRange range;
        if ( engine_.canBoundMatch() ) {
            indicator_.logMessage( "Attempt to locate "
                                 + "restricted common region" );
            try {

                /* Locate the ranges of all the tables. */
                NdRange[] inRanges = new NdRange[ nTable_ ];
                for ( int i = 0; i < nTable_; i++ ) {
                    inRanges[ i ] = readTupleRange( i );
                }
                NdRange[] extRanges = new NdRange[ nTable_ ];
                for ( int i = 0; i < nTable_; i++ ) {
                    extRanges[ i ] = engine_.getMatchBounds( inRanges, i );
                }

                /* Work out the range of the reference table which we are
                 * interested in.  This is its intersection with the union 
                 * of all the other tables. */
                NdRange unionOthers = null;
                for ( int i = 0; i < nTable_; i++ ) {
                    if ( i != index0 ) {
                        unionOthers = unionOthers == null
                                    ? extRanges[ i ]
                                    : NdRange.union( unionOthers,
                                                     extRanges[ i ] );
                    }
                }
                range = NdRange.intersection( extRanges[ index0 ],
                                              unionOthers );
                indicator_.logMessage( "Potential match region: " + range );
            }
            catch ( ClassCastException e ) {
                indicator_.logMessage( "Region location failed "
                                     + "(incompatible value types)" );
                range = new NdRange( ncol );
            }
        }
        else {
            range = new NdRange( ncol );
        }

        /* Bin all the rows in the interesting region of the reference table. */
        ObjectBinner<Object,RowRef> binner = Binners.createObjectBinner();
        binRows( index0, range, binner, true );

        /* Bin any rows in the other tables which have entries in the bins
         * we have already created for the reference table.  Rows without
         * such entries can be ignored. */
        for ( int itab = 0; itab < nTable_; itab++ ) {
            if ( itab != index0 ) {
                binRows( itab, range, binner, false );
            }
        }

        /* Convert the result to a link set and return. */
        LinkSet linkSet = createLinkSet();
        binsToLinks( binner, linkSet );
        return linkSet;
    }

    /**
     * Takes a set of links representing possible matches and filters it
     * to retain actual desired matches.  The return value is a
     * set of links, one per row of a reference table, including rows
     * from any other tables which match it.  All of the input possibleLinks
     * should contain at least one entry from the reference table.
     *
     * @param  possibleLinks  set of {@link RowLink} objects which correspond
     *         to groups of possibly matched objects; each link must contain
     *         at least one object from the reference table
     *         (<code>index0</code>)
     * @param  index0  index of the reference table in this row matcher's list
     *         of tables
     * @param  bestOnly  true iff only the best match with each other table
     *         is required; if false multiple matches from each non-reference
     *         table may appear in the output RowLinks
     * @return   a set of RowLink objects which represent all the actual
     *           multi-pair matches
     */
    private LinkSet findMultiPairMatches( LinkSet possibleLinks, int index0,
                                          boolean bestOnly ) 
            throws IOException, InterruptedException {

        /* Set up a link set which will be populated with every pair involving
         * the reference table and another table. */
        LinkSet pairs = createLinkSet();
        double nLink = (double) possibleLinks.size();
        int iLink = 0;
        indicator_.startStage( "Locating pair matches between " + index0
                             + " and other tables");
        for ( Iterator<RowLink> it = possibleLinks.iterator(); it.hasNext(); ) {

            /* Get the next link and delete it from the input list, for
             * memory efficiency. */
            RowLink link = it.next();
            it.remove();

            /* Work out if this link contains any rows which are not from the
             * reference table. */
            int nref = link.size();
            boolean hasOthers = false;
            for ( int iref = 0; iref < nref && ! hasOthers; iref++ ) {
                if ( link.getRef( iref ).getTableIndex() != index0 ) {
                    hasOthers = true;
                }
            }

            /* If there are any rows from tables other than the reference
             * table, we need to test them for matches. */
            if ( hasOthers ) {

                /* Cache the rows from each ref, since it may be expensive
                 * to get them multiple times. */
                Object[][] binnedRows = new Object[ nref ][];
                for ( int iref = 0; iref < nref; iref++ ) {
                    RowRef ref = link.getRef( iref );
                    StarTable table = tables_[ ref.getTableIndex() ];
                    binnedRows[ iref ] = table.getRow( ref.getRowIndex() );
                }

                /* Iterate over each of the reference table rows. */
                for ( int i0 = 0; i0 < nref; i0++ ) {
                    RowRef ref0 = link.getRef( i0 );
                    int iTable0 = ref0.getTableIndex();
                    if ( iTable0 == index0 ) {
                        long irow0 = ref0.getRowIndex();

                        /* For each reference table row iterate over all the 
                         * non-reference table rows, looking for matches. */
                        for ( int i1 = 0; i1 < nref; i1++ ) {
                            RowRef ref1 = link.getRef( i1 );
                            int iTable1  = ref1.getTableIndex();
                            if ( iTable1 != index0 ) {
                                RowLink2 pair = new RowLink2( ref0, ref1 );
                                if ( ! pairs.containsLink( pair ) ) {
                                    double score =
                                        engine_.matchScore( binnedRows[ i0 ],
                                                            binnedRows[ i1 ] );
                                    if ( score >= 0 ) {
                                        pair.setScore( score );
                                        pairs.addLink( pair );
                                    }
                                }
                            }
                        }
                    }
                }
            }
            indicator_.setLevel( ++iLink / nLink );
        }
        indicator_.endStage();

        /* Store all the pairs in a map keyed by row reference of the reference
         * table. */
        ObjectBinner<RowRef,ScoredRef> pairBinner =
            Binners.createObjectBinner();
        for ( Iterator<RowLink> it = pairs.iterator(); it.hasNext(); ) {
            RowLink2 pair = (RowLink2) it.next();
            it.remove();
            RowRef refA = pair.getRef( 0 );
            RowRef refB = pair.getRef( 1 );
            final RowRef ref0;
            final RowRef ref1;
            if ( refA.getTableIndex() == index0 ) {
                assert refB.getTableIndex() != index0;
                ref0 = refA;
                ref1 = refB;
            }
            else if ( refB.getTableIndex() == index0 ) {
                assert refA.getTableIndex() != index0;
                ref0 = refB;
                ref1 = refA;
            }
            else {
                throw new IllegalArgumentException( "Pair doesn't contain "
                                                  + "reference table" );
            }
            RowRef key = ref0;
            ScoredRef value = new ScoredRef( ref1, pair.getScore() );
            pairBinner.addItem( key, value );
        }

        /* Convert the pairs in pairMap to a LinkSet. */
        LinkSet multiLinks = createLinkSet();
        for ( Iterator<RowRef> it = pairBinner.getKeyIterator();
              it.hasNext(); ) {
            RowRef ref0 = it.next();
            ScoredRef[] sref1s =
                pairBinner.getList( ref0 ).toArray( new ScoredRef[ 0 ] );
            int nref1 = sref1s.length;
            if ( nref1 > 0 ) {
                RowRef[] ref1s = new RowRef[ nref1 ];
                double[] scores = new double[ nref1 ];
                for ( int ir1 = 0; ir1 < nref1; ir1++ ) {
                    ref1s[ ir1 ] = sref1s[ ir1 ].ref_;
                    scores[ ir1 ] = sref1s[ ir1 ].score_;
                }
                multiLinks.addLink( new PairsRowLink( ref0, ref1s, scores,
                                                      bestOnly ) );
            }
        }
        return multiLinks;
    }

    /**
     * Applies a set of options to a RowLink, eliminating
     * any elements which do not fit the given options.
     *
     * @param  link   RowLink representing the row to be tested
     * @param  joinTypes  array of per-table inclusion criteria
     */
    private boolean acceptRow( RowLink link, MultiJoinType[] joinTypes ) {
        boolean[] present = new boolean[ nTable_ ];
        int nref = link.size();
        for ( int i = 0; i < nref; i++ ) {
            RowRef ref = link.getRef( i );
            int iTable = ref.getTableIndex();
            present[ iTable ] = true;
        }
        return MultiJoinType.accept( joinTypes, present );
    }

    /**
     * Goes through a set of matched pairs and makes sure that no RowRef is
     * contained in more than one pair.  If multiple pairs exist containing
     * the same RowRef, all but one (the one with the lowest score) 
     * are discarded.  
     * <p>The links in the input set should be {@link RowLink2}s 
     * representing a matched pair, with non-blank pair scores.
     * The pairs may only contain RowRefs with a table index of 0 or 1.
     *
     * <p>The input set, <code>pairs</code>, may be affected by this method.
     * 
     * @param  pairs  set of <code>RowLink2</code> objects
     *                representing matched pairs
     * @return  set resembling pairs but with multiple entries discarded
     */
    LinkSet eliminateMultipleRowEntries( LinkSet pairs ) 
            throws InterruptedException {

        /* Sort the input pairs in ascending score order.  In this way,
         * better links will be favoured (inserted into the output set)
         * over worse ones. */
        Collection<RowLink> inPairs =
                toSortedList( pairs, new Comparator<RowLink>() {
            public int compare( RowLink o1, RowLink o2 ) {
                RowLink2 r1 = (RowLink2) o1;
                RowLink2 r2 = (RowLink2) o2;
                double score1 = r1.getScore();
                double score2 = r2.getScore();
                if ( score1 < score2 ) {
                    return -1;
                }
                else if ( score1 > score2 ) {
                    return +1;
                }
                else {
                    return r1.compareTo( r2 );
                }
            }
        } );
        pairs = null;

        /* We will be copying entries from the input map to an output one,
         * retaining only the best matches for each row. */
        LinkSet outPairs = createLinkSet();

        /* Prepare to keep track of which rows we have seen. */
        Set<RowRef> seenRows = new HashSet<RowRef>();

        /* Iterate over each entry in the input set, selectively copying
         * to the output set as we go. */
        double nPair = inPairs.size();
        int iPair = 0;
        indicator_.startStage( "Eliminating multiple row references" );
        for ( Iterator<RowLink> it = inPairs.iterator(); it.hasNext(); ) {
            RowLink2 pair = (RowLink2) it.next();
            double score = pair.getScore();
            if ( pair.size() != 2 || Double.isNaN( score ) || score < 0.0 ) {
                throw new IllegalArgumentException();
            }
            RowRef ref1 = pair.getRef( 0 );
            RowRef ref2 = pair.getRef( 1 );
            if ( ref1.getTableIndex() != 0 || ref2.getTableIndex() != 1 ) {
                throw new IllegalArgumentException();
            }
            boolean seen1 = ! seenRows.add( ref1 );
            boolean seen2 = ! seenRows.add( ref2 );

            /* If neither row in this pair has been seen before,
             * copy it across to the output set. */
            if ( ! seen1 && ! seen2 ) {
                outPairs.addLink( pair );
            }

            /* Report on progress. */
            indicator_.setLevel( ++iPair / nPair );
        }
        indicator_.endStage();
        return outPairs;
    }

    /**
     * Rationalises the elements of a set of <tt>RowLink</tt>s 
     * so that it contains guaranteed
     * mutually exclusive RowLink objects (no member RowLink contains
     * a RowRef contained by any other member).
     * Any set of rows linked up in the initial members are
     * joined with any links which are joined
     * by other links in which any of its members participates.
     * Any row which appears in the initial
     * one or more times (as part of one or more <tt>RowLink</tt>s)
     * therefore appears exactly once in the resulting set
     * as part of a new <tt>RowLink</tt> which may contain more RowRefs.
     *
     * <p>Typically this method will be called on a set
     * resulting from an invocation of {@link #findPairs}.
     * Note that in this case
     * it is not guaranteed that all the rows it links are mutually equal
     * in the sense defined by the match engine's
     * {@link MatchEngine#matches} method, but the members of each
     * <tt>RowLink</tt> will form a connected graph with the nodes
     * connected by matches.
     *
     * @param   links  set of {@link RowLink} objects
     * @return  disjoint set of {@link RowLink} objects
     */
    private LinkSet agglomerateLinks( LinkSet links ) 
            throws InterruptedException {

        /* Construct a new hash mapping each RowRef in the given set of
         * links to a list of all the links it appears in. */
        ObjectBinner<RowRef,RowLink> refBinner =
            Binners.createModifiableObjectBinner();
        indicator_.startStage( "Mapping rows to links" );
        double nlink1 = links.size();
        int ilink1 = 0;
        for ( RowLink link : links ) {
            indicator_.setLevel( ++ilink1 / nlink1 );
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                refBinner.addItem( ref, link );
            }
        }
        indicator_.endStage();

        /* Prepare a new set to contain the agglomerated links.
         * We will populate this with disjoint links at the same time
         * as removing the corresponding RowRefs from the refMap. 
         * This both keeps track of which ones we've done and keeps
         * memory usage down. */
        LinkSet agglomeratedLinks = createLinkSet();

        /* Check for any isolated links, that is ones none of whose members
         * appear in any other links.  These can be handled more efficiently
         * than ones with more complicated relationships. */
        indicator_.startStage( "Identifying isolated links" );
        double nlink2 = links.size();
        int ilink2 = 0;
        for ( RowLink link : links ) {
            indicator_.setLevel( ++ilink2 / nlink2 );
            int nref = link.size();
            boolean isolated = true;
            for ( int i = 0; isolated && i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                Collection<RowLink> refLinks = refBinner.getList( ref );
                assert refLinks.size() > 0;
                isolated = isolated && refLinks.size() == 1;
            } 

            /* If it is isolated, just copy the link to the agglomerated list,
             * and remove the refs from the map. */
            if ( isolated ) {
                assert ! agglomeratedLinks.containsLink( link );
                agglomeratedLinks.addLink( link );
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    refBinner.remove( ref );
                }
            }
        }
        indicator_.endStage();

        /* Take a key from the map we have just constructed, and walk its
         * links recursively to see which nodes we can reach from it.
         * Collect such nodes in a set, and create a new
         * RowLink in the output list from it.  This has the side-effect
         * of removing map entries when they have no more unused links,
         * which means we don't encounter them more than once
         * (and it's also good for memory usage).
         * Repeat until there are no nodes left in the input map. */
        double nRefs = refBinner.getBinCount();
        indicator_.startStage( "Walking links" );
        while ( refBinner.getBinCount() > 0 ) {
            indicator_.setLevel( 1.0 - ( refBinner.getBinCount() / nRefs ) );
            RowRef ref1 = refBinner.getKeyIterator().next();
            Set<RowRef> refSet = new HashSet<RowRef>();
            walkLinks( ref1, refBinner, refSet );
            RowLink link = RowLink.createLink( refSet );
            assert ! agglomeratedLinks.containsLink( link );
            agglomeratedLinks.addLink( link );
        }
        indicator_.endStage();

        /* Replace the contents of the used list with the new contents. */
        return agglomeratedLinks;
    }

    /**
     * Returns the common number of columns for two tables owned by this
     * matcher.  If the number differs between the two, an exception is
     * thrown.
     * 
     * @param  index1  index of one table
     * @param  index2  index of other table
     * @return  number of columns for both tables
     */
    int getPairColumnCount( int index1, int index2 ) {
        int ncol = tables_[ index1 ].getColumnCount();
        if ( tables_[ index2 ].getColumnCount() != ncol ) {
            throw new IllegalArgumentException( "Column count mismatch" );
        }
        return ncol;
    }

    /**
     * Recursively pulls out connected nodes (RowRefs) from a map of
     * RowRefs to RowLinks and dumps them in a set of nodes.
     *
     * @param   baseRef  the RowRef at which to start/continue the search
     * @param   refBinner  a modifiable ObjectBinner mapping RowRefs to lists
     *                   of (all so far untraversed) RowLinks
     * @param   outSet   an existing set of RowRefs into which new RowRefs
     *                   connected to baseRef should be inserted
     */
    private static void walkLinks( RowRef baseRef,
                                   ObjectBinner<RowRef,RowLink> refBinner,
                                   Set<RowRef> outSet ) {

        /* Do nothing if the output set already contains the requested
         * reference; without this test we would recurse to infinite depth. */
        if ( ! outSet.contains( baseRef ) ) {

            /* Get all the links of which this reference is a member. */
            List<RowLink> links = refBinner.getList( baseRef );
            if ( ! links.isEmpty() ) {

                /* Add the current row to the output set. */
                outSet.add( baseRef );

                /* Recurse over all the so-far untraversed rows which are
                 * linked to this one. */
                for ( Iterator<RowLink> linkIt = links.iterator();
                      linkIt.hasNext(); ) {
                    RowLink link = linkIt.next();
                    for ( int i = 0; i < link.size(); i++ ) {
                        RowRef rref = link.getRef( i );
                        walkLinks( rref, refBinner, outSet );
                    }

                    /* Having traversed this link, remove it so it is never
                     * encountered again. */
                    linkIt.remove();
                }
            }

            /* If there are no more links in this list, we can forget
             * about it. */
            if ( links.isEmpty() ) {
                refBinner.remove( baseRef );
            }
        }
    }

    /**
     * Checks that the given tables all provide random access.
     *
     * @throws  IllegalArgumentException if any of <tt>tables</tt> does
     *          not provide random access
     */
    private void checkRandom() {
        for ( StarTable table : tables_ ) {
            if ( ! table.isRandom() ) {
                throw new IllegalArgumentException( "Table " + table
                                                  + " is not random access" );
            }
        }
    }

    /**
     * Calculates the upper and lower bounds of the region in tuple-space
     * for one of this matcher's tables.  The result represents a rectangular
     * region in tuple-space.  Any of the bounds may be null to indicate
     * no limit in that direction.
     * Note that this range does not include any padding to accommodate
     * matches within a search radius (or similar concept) of the edge.
     *
     * @param   tIndex  index of the table to calculate limits for
     * @return  range   bounds of tuple-space region inhabited by table
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    private NdRange readTupleRange( int tIndex )
            throws IOException, InterruptedException {
        StarTable table = tables_[ tIndex ];
        int ncol = table.getColumnCount();

        /* See which columns are comparable. */
        boolean[] isComparable = new boolean[ ncol ];
        int ncomp = 0;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( Comparable.class.isAssignableFrom( table.getColumnInfo( icol )
                                                   .getContentClass() ) ) {
                isComparable[ icol ] = true;
                ncomp++;
            }
        }

        /* If none of the columns is comparable, there's no point. */
        if ( ncomp == 0 ) {
            return new NdRange( ncol );
        }

        /* Go through each row finding the minimum and maximum value 
         * for each column (coordinate). */
        Comparable<?>[] mins = new Comparable<?>[ ncol ];
        Comparable<?>[] maxs = new Comparable<?>[ ncol ];
        ProgressRowSequence rseq =
            new ProgressRowSequence( table, indicator_,
                                     "Assessing range of coordinates " +
                                     "from table " + ( tIndex + 1 ) );
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( isComparable[ icol ] ) {
                        Object cell = row[ icol ];
                        if ( cell instanceof Comparable &&
                             ! Tables.isBlank( cell ) ) {
                            Comparable<?> val = (Comparable<?>) cell;
                            mins[ icol ] =
                                NdRange.min( mins[ icol ], val, false );
                            maxs[ icol ] =
                                NdRange.max( maxs[ icol ], val, false );
                        }
                    }
                }
            }
        }

        /* It's possible, though not particularly likely, that a 
         * compare invocation can result in a ClassCastException 
         * (e.g. comparing an Integer to a Double).  Such ClassCastExceptions
         * should get caught higher up, but we need to make sure the
         * row sequence is closed or the logging will get in a twist. */
        finally {
            rseq.close();
        }

        /* Deal sensibly with funny numbers. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( mins[ icol ] instanceof Number ) {
                double min = ((Number) mins[ icol ]).doubleValue();
                assert ! Double.isNaN( min );
                if ( Double.isInfinite( min ) ) {
                    mins[ icol ] = null;
                }
            }
            if ( maxs[ icol ] instanceof Number ) {
                double max = ((Number) maxs[ icol ]).doubleValue();
                assert ! Double.isNaN( max );
                if ( Double.isInfinite( max ) ) {
                    maxs[ icol ] = null;
                }
            }
        }

        /* Report and return. */
        NdRange range = new NdRange( mins, maxs );
        indicator_.logMessage( "Limits are: " + range );
        return range;
    }

    /**
     * Adds entries for the rows of a table to a given ObjectBinner object.
     * The binner is populated with keys that are match engine bins,
     * and list items that are {@link RowRef}s.
     *
     * <p>The <code>newBins</code> parameter determines whether new bins
     * will be started in the <code>bins</code> object.  If true, then
     * every relevant row in the table will be binned.  If false, then
     * only rows with entries in bins which are already present in 
     * the <code>bins</code> object will be added and others will be ignored.
     *
     * @param   itab   index of table to operate on
     * @param   range  range of row coordinates of interest - any rows outside
     *                 this range are ignored
     * @param   binner   binner object to modify
     * @param   newBins  whether new bins may be added to <code>bins</code>
     */
    private void binRows( int itab, NdRange range,
                          ObjectBinner<Object,RowRef> binner, boolean newBins )
            throws IOException, InterruptedException {
        if ( range == null ) {
            return;
        }
        StarTable table = tables_[ itab ];
        ProgressRowSequence rseq =
            new ProgressRowSequence( table, indicator_,
                                     "Binning rows for table " + ( itab + 1 ) );
        long nrow = 0;
        long nexclude = 0;
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( range.isInside( row ) ) {
                    Object[] keys = engine_.getBins( row );
                    int nkey = keys.length;
                    if ( nkey > 0 ) {
                        RowRef rref = new RowRef( itab, lrow );
                        for ( int ikey = 0; ikey < nkey; ikey++ ) {
                            Object key = keys[ ikey ];
                            if ( newBins || binner.containsKey( key ) ) {
                                binner.addItem( key, rref );
                            }
                        }
                    }
                }
                else {
                    nexclude++;
                }
                nrow++;
            }
            assert nrow == table.getRowCount();
        }
        finally {
            rseq.close();
        }
        if ( nexclude > 0 ) {
            indicator_.logMessage( nexclude + "/" + nrow + " rows excluded "
                                 + "(out of match region)" );
        }
    }

    /**
     * Calculates a set of RowLink objects which represent all the
     * distinct groups of RowRefs associated with any of the bins,
     * and adds them to a given LinkSet.
     * Only RowLinks containing more than one entry are put in the
     * resulting set, since the others aren't interesting.
     *
     * <p><strong>Note</strong> that this method will
     * (for memory efficiency purposes)
     * clear out the binner; following a call to this method the binner
     * is effectively empty of any data.
     *
     * @param   binner  binner with bin item values which are {@link RowRef}s;
     *          contents may be disrupted
     * @param   linkSet  link set into which created RowLinks will be dumped
     */
    private void binsToLinks( ObjectBinner<Object,RowRef> binner,
                              LinkSet linkSet )
            throws InterruptedException {
        long nrow = binner.getItemCount();
        long nbin = binner.getBinCount();
        indicator_.logMessage( nrow + " row refs in " + nbin + " bins" );
        indicator_.logMessage( "(average bin occupancy " +
                               ( (float) nrow / (float) nbin ) + ")" );
        indicator_.startStage( "Consolidating potential match groups" );
        double nl = (double) nbin;
        long il = 0;
        for ( Iterator<?> it = binner.getKeyIterator(); it.hasNext(); ) {
            Object key = it.next();
            List<RowRef> refList = binner.getList( key );

            /* If there is more than one RowRef, create and store the
             * corresponding RowLink.  Items with 0 or 1 entry are not
             * potential matches - take no action. */
            if ( refList.size() > 1 ) {
                linkSet.addLink( RowLink.createLink( refList ) );
            }

            /* Remove the entry from the map as we're going along,
             * to save on memory. */
            it.remove();
            indicator_.setLevel( ++il / nl );
        }
        assert binner.getBinCount() == 0;
        indicator_.endStage();
    }

    /**
     * Accumulates a set of RowLink objects which represent all the
     * distinct groups of RowRefs associated with any of the bins,
     * and adds them to a given LinkSet.
     * Only RowLinks containing more than one entry are put in the the
     * resulting set, since the others aren't interesting.
     *
     * @param  binner  binner with items that represent row indices in a
     *         single table; contents may be disrupted
     * @param  linkSet  set to add links to
     * @param  itable  index of table which <code>binner</code>'s row indices
     *                 refer to
     */
    private void binsToInternalLinks( LongBinner binner, LinkSet linkSet,
                                      int itable )
            throws InterruptedException {
        long nbin = binner.getBinCount();
        indicator_.startStage( "Consolidating potential match groups" );
        double nl = (double) nbin;
        long il = 0;
        for ( Iterator<?> it = binner.getKeyIterator(); it.hasNext(); ) {
            Object key = it.next();
            long[] irs = binner.getLongs( key );
            int nir = irs.length;
            if ( nir > 1 ) {
                final RowLink link;
                if ( nir == 2 ) {
                    link = new RowLink2( new RowRef( itable, irs[ 0 ] ),
                                         new RowRef( itable, irs[ 1 ] ) );
                }
                else {
                    RowRef[] refs = new RowRef[ nir ];
                    for ( int iir = 0; iir < nir; iir++ ) {
                        refs[ iir ] = new RowRef( itable, irs[ iir ] );
                    }
                    link = RowLinkN.fromModifiableArray( refs );
                }
                linkSet.addLink( link );
            }
            it.remove();
            indicator_.setLevel( ++il / nl );
        }
        assert binner.getBinCount() == 0;
        indicator_.endStage();
    }

    /**
     * Returns the number of rows in a table which fall within a given
     * range of min/max values.
     * 
     * @param  tIndex  index of table to assess
     * @param  range   bounds of permissible coordinates
     * @return  number of rows of <tt>table</tt> that fall within supplied
     *          bounds
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    private long countInRange( int tIndex, NdRange range )
            throws IOException, InterruptedException {
        ProgressRowSequence rseq = 
            new ProgressRowSequence( tables_[ tIndex ], indicator_, 
                                     "Counting rows in match region " +
                                     "for table " + ( tIndex + 1 ) );
        long nInclude = 0;
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                if ( range.isInside( rseq.getRow() ) ) {
                    nInclude++;
                }
            }
        }
        finally {
            rseq.close();
        }
        indicator_.logMessage( nInclude + " rows in match region" );
        return nInclude;
    }

    /**
     * Returns a list with the same content of RowLinks as the
     * input LinkSet, but ordered according to the given comparator.
     *
     * @param  linkSet  set of RowLinks
     * @param  comparator  comparator that operates on the members of
     *                     <code>linkSet</code>, or null for natural order
     * @return  sorted collection of {@link RowLink}s
     */
    private Collection<RowLink> toSortedList( LinkSet linkSet,
                                              Comparator<RowLink> comparator ) {
        int nLink = linkSet.size();
        RowLink[] links = new RowLink[ nLink ];
        int il = 0;
        for ( RowLink link : linkSet ) {
            links[ il++ ] = link;
        }

        /* Don't use Collections.sort, it's evil. */
        Arrays.parallelSort( links, comparator );
        return Arrays.asList( links );
    }

    /**
     * Signals the start of a user-visible matching process.
     */
    private void startMatch() {
        startTime_ = new Date().getTime();
        indicator_.logMessage( "Params:"
                             + formatParams( engine_.getMatchParameters() ) );
        indicator_.logMessage( "Tuning:"
                             + formatParams( engine_.getTuningParameters() ) );
    }

    /**
     * Formats a list of DescribedValues for compact display.
     *
     * @param  params  values
     * @return   values line
     */
    private static String formatParams( DescribedValue[] params ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < params.length; i++ ) {
            sbuf.append( i == 0 ? " " : ", " )
                .append( params[ i ] );
        }
        return sbuf.toString();
    }

    /**
     * Signals the end of a user-visible matching process.
     */
    private void endMatch() {
        long millis = new Date().getTime() - startTime_;
        indicator_.logMessage( "Elapsed time for match: " + ( millis / 1000 ) + 
                               " seconds" );
    }

    /**
     * Turns a <tt>long</tt> into an <tt>int</tt>, throwing an unchecked
     * exception if it can't be done.
     */
    private static int checkedLongToInt( long lval ) {
        return Tables.checkedLongToInt( lval );
    }

    /**
     * Helper class which decorates a RowRef with a score value.
     */
    private static class ScoredRef {
        final RowRef ref_;
        final double score_;

        /** 
         * Constructor.
         *
         * @param   ref  row ref
         * @param   score  score
         */
        public ScoredRef( RowRef ref, double score ) {
            ref_ = ref;
            score_ = score;
        }
    }

    /**
     * Encapsulates information about a range intersection of multiple tables.
     */
    private static class Intersection {
        final NdRange range_;
        final long[] inRangeCounts_;

        /**
         * Constructor.
         *
         * @param   range  common range
         * @param   inRangeCounts  per-table count of rows within the range
         */
        public Intersection( NdRange range, long[] inRangeCounts ) {
            range_ = range;
            inRangeCounts_ = inRangeCounts;
        }
    }
}
