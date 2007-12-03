package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
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

    private final MatchEngine engine;
    private final StarTable[] tables;
    private final int nTable;
    private ProgressIndicator indicator = new NullProgressIndicator();
    private long startTime;

    /**
     * Constructs a new matcher with match characteristics defined by
     * a given matching engine.
     *
     * @param  engine  matching engine
     * @param  tables  the array of tables on which matches are to be done
     */
    public RowMatcher( MatchEngine engine, StarTable[] tables ) {
        this.engine = engine;
        this.tables = tables;
        this.nTable = tables.length;
    }

    /**
     * Sets the progress indicator for this matcher.
     * 
     * @param  indicator  new indicator
     */
    public void setIndicator( ProgressIndicator indicator ) {
        this.indicator = indicator;
    }

    /**
     * Returns the current progress indicator for this matcher.
     *
     * @return   indicator
     */
    public ProgressIndicator getIndicator() {
        return indicator;
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
        return new TreeSetLinkSet();
    }

    /**
     * Returns a set of RowLink objects corresponding to a pairwise match
     * between this matcher's two tables performed with its match engine.
     * Each element in the returned list corresponds to a matched
     * pair with one entry from each of the input tables.
     *
     * @param   bestOnly  whether only the best match between the two tables
     *          is required, or whether you would like to retain every
     *          match which fits the criteria
     * @return  links representing rows to include in the output table
     */
    public LinkSet findPairMatches( boolean bestOnly )
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have two tables. */
        if ( nTable != 2 ) {
            throw new IllegalStateException( "findPairMatches only makes sense"
                                           + " for 2 tables" );
        }
        startMatch();

        /* Get the possible candidates for inter-table links. */
        LinkSet possibleLinks = getPossibleInterLinks( 0, 1 );

        /* Get all the possible inter-table pairs. */
        LinkSet pairs = findInterPairs( possibleLinks, 0, 1 );

        /* If it has been requested, restrict the set of links to the
         * best available matches.  That is, ensure that no row is
         * represented  more than once in the set of matches. */
        if ( bestOnly ) {
            pairs = eliminateMultipleRowEntries( pairs );
        }

        /* Return. */
        endMatch();
        return pairs;
    }

    /**
     * Returns a set of RowLink objects corresponding to a match 
     * between this matcher's two tables performed with its match engine.
     * Each element in the returned list basically corresponds to a matched 
     * pair with one entry from each of the input tables, however using
     * the <tt>req1</tt> and <tt>req2</tt> arguments you can specify whether
     * both input tables need to be represented.
     * Each input table row appears in no more than one RowLink in the
     * returned list.
     *
     * @param  req1  whether an entry from the first table must be present
     *         in each element of the result
     * @param  req2  whether an entry from the second table must be present
     *         in each element of the result
     * @return  link set
     */
    public LinkSet findPairMatches( boolean req1, boolean req2 )
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have two tables. */
        if ( nTable != 2 ) {
            throw new IllegalStateException( "findPairMatches only makes sense"
                                           + " for 2 tables" );
        }
        startMatch();

        /* Get the possible candidates for inter-table links. */
        LinkSet links = getPossibleInterLinks( 0, 1 );

        /* Get all the possible inter-table pairs. */
        links = findInterPairs( links, 0, 1 );

        /* Make sure that no row is represented more than once
         * (this isn't necessary for the result to make sense, but it's 
         * probably what the caller is expecting). */
        links = eliminateMultipleRowEntries( links );

        /* We now have a set of links corresponding to all the matches
         * with one entry for each of two or more of the input tables.
         * In the case that we want to output some links with unmatched
         * parts, add new singleton row links as necessary. 
         * They are added without scores, since they don't represent 
         * actual matches. */
        LinkSet[] missing = new LinkSet[] {
            ( req1 ? null : missingSingles( links, 0 ) ),
            ( req2 ? null : missingSingles( links, 1 ) ),
        };
        for ( int i = 0; i < 2; i++ ) {
            if ( missing[ i ] != null ) {
                for ( Iterator it = missing[ i ].iterator(); it.hasNext(); ) {
                    links.addLink( (RowLink) it.next() );
                    it.remove();
                }
                missing[ i ] = null;
            }
        }

        /* Return. */
        endMatch();
        return links;
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
     * @param  useAlls  array of booleans indicating for each table whether
     *         all rows are to be used (otherwise just matched)
     * @return  set of PairsRowLink objects representing multi-pair matches
     */
    public LinkSet findMultiPairMatches( int index0, boolean bestOnly,
                                         boolean[] useAlls )
            throws IOException, InterruptedException {
        checkRandom();
        if ( useAlls.length != nTable ) {
            throw new IllegalArgumentException(
                "Options length " + useAlls.length +
                " differs from table count " + nTable );
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
        LinkSet[] missing = new LinkSet[ nTable ];
        for ( int i = 0; i < nTable; i++ ) {
            if ( useAlls[ i ] ) {
                missing[ i ] = missingSingles( multiLinks, i );
            }
        }
        for ( int i = 0; i < nTable; i++ ) {
            if ( missing[ i ] != null ) {
                for ( Iterator it = missing[ i ].iterator(); it.hasNext(); ) {
                    multiLinks.addLink( (RowLink) it.next() );
                }
                missing[ i ] = null;
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
     * the returned list.  Whether each returned RowLink must contain
     * an entry from every input table is determined by the
     * <tt>useAll</tt> argument.
     * Any number of tables can be matched.
     * 
     * @param  useAll  array of booleans indicating for each table whether
     *         all rows are to be used (otherwise just matched)
     * @return list of {@link RowLink}s corresponding to the selected rows
     */
    public LinkSet findGroupMatches( boolean[] useAll )
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have multiple tables. */
        if ( nTable < 2 ) {
            throw new IllegalStateException( "Find matches only makes sense "
                                           + "for multiple tables" );
        }

        /* Check that there are the right number of options and that they
         * are not null. */
        if ( useAll.length != nTable ) {
            throw new IllegalArgumentException( 
                "Options length " + useAll.length +
                " differs from table count " + nTable );
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
        LinkSet[] missing = new LinkSet[ nTable ];
        for ( int i = 0; i < nTable; i++ ) {
            if ( useAll[ i ] ) {
                missing[ i ] = missingSingles( links, i );
            }
        }
        for ( int i = 0; i < nTable; i++ ) {
            if ( missing[ i ] != null ) {
                for ( Iterator it = missing[ i ].iterator(); it.hasNext(); ) {
                    links.addLink( (RowLink) it.next() );
                }
                missing[ i ] = null;
            }
        }

        /* Now filter the links to contain only those composite rows we're
         * interested in. */
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            if ( ! acceptRow( link, useAll ) ) {
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
        if ( nTable != 1 ) {
            throw new IllegalStateException( "Internal matches only make sense "
                                           + "with a single table" );
        }
        startMatch();

        /* Locate all the pairs. */
        LinkSet links = findPairs( getAllPossibleLinks() );

        /* Join up pairs into larger groupings. */
        links = agglomerateLinks( links );

        /* Add unmatched rows if required. */
        if ( includeSingles ) {
            for ( Iterator it = missingSingles( links, 0 ).iterator();
                  it.hasNext(); ) {
                links.addLink( (RowLink) it.next() );
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
        indicator.startStage( "Locating pairs" );
        for ( Iterator it = possibleLinks.iterator(); it.hasNext(); ) {

            /* Obtain the link and remove it from the input set for 
             * memory efficiency. */
            RowLink link = (RowLink) it.next();
            it.remove();

            /* Check whether this link is non-trivial. */
            int nref = link.size();
            if ( nref > 1 ) {

                /* Cache the rows from each ref since it may be expensive
                 * to get them multiple times. */
                Object[][] binnedRows = new Object[ nref ][];
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    StarTable table = tables[ ref.getTableIndex() ];
                    binnedRows[ i ] = table.getRow( ref.getRowIndex() );
                }

                /* Do a pairwise comparison of all the rows in the same group.
                 * If they match, add the new pair to the set of pairs. */
                for ( int i = 0; i < nref; i++ ) {
                    for ( int j = 0; j < i; j++ ) {
                        RowLink2 pair = new RowLink2( link.getRef( i ),
                                                      link.getRef( j ) );
                        if ( ! pairs.containsLink( pair ) ) {
                            double score = engine.matchScore( binnedRows[ i ],
                                                              binnedRows[ j ] );
                            if ( score >= 0 ) {
                                pair.setScore( score );
                                pairs.addLink( pair );
                            }
                        }
                    }
                }
            }
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();
        return pairs;
    }

    /**
     * Identifies all the pairs of equivalent rows from a set of RowLinks
     * between rows from two specified tables.  All the elements of the
     * output set will be {@link RowLink2} objects.
     *
     * <p>The input set, <code>possibleLinks</code>, may be affected.
     *
     * <p>Since links are being sought between a pair of tables, a more
     * efficient algorithm can be used than for {@link #findPairs}.
     *
     * @param  possibleLinks  a set of {@link RowLink} objects which 
     *         correspond to groups of possibly matched objects 
     *         according to the match engine's criteria
     * @param  index1  index of the first table
     * @param  index2  index of the second table
     * @return   matched row pairs
     */
    private LinkSet findInterPairs( LinkSet possibleLinks,
                                    int index1, int index2 )
            throws IOException, InterruptedException {
        LinkSet pairs = createLinkSet();
        double nLink = (double) possibleLinks.size();
        int iLink = 0;
        indicator.startStage( "Locating inter-table pairs" );
        for ( Iterator it = possibleLinks.iterator(); it.hasNext(); ) {

            /* Get the next link and delete it from the input list, for
             * memory efficiency. */
            RowLink link = (RowLink) it.next();
            it.remove();

            /* Check if we have a non-trivial link. */
            int nref = link.size();
            if ( nref > 1 ) {

                /* Check if rows from both the required tables are present
                 * in this link - if not, there can't be any suitable pairs. */
                boolean got1 = false;
                boolean got2 = false;
                for ( int i = 0; i < nref && ! ( got1 && got2 ); i++ ) {
                    int tableIndex = link.getRef( i ).getTableIndex();
                    got1 = got1 || tableIndex == index1;
                    got2 = got2 || tableIndex == index2;
                }
                if ( got1 && got2 ) {

                    /* Cache the rows from each ref, since it may be expensive
                     * to get them multiple times. */
                    Object[][] binnedRows = new Object[ nref ][];
                    for ( int i = 0; i < nref; i++ ) {
                        RowRef ref = link.getRef( i );
                        StarTable table = tables[ ref.getTableIndex() ];
                        binnedRows[ i ] = table.getRow( ref.getRowIndex() );
                    }

                    /* Do a pairwise comparison of each eligible pair in the
                     * group.  If they match, add the new pair to the set
                     * of matched pairs. */
                    for ( int i = 0; i < nref; i++ ) {
                        RowRef refI = link.getRef( i );
                        int iTableI = refI.getTableIndex();
                        for ( int j = 0; j < i; j++ ) {
                            RowRef refJ = link.getRef( j );
                            int iTableJ = refJ.getTableIndex();
                            if ( iTableI == index1 && iTableJ == index2 ||
                                 iTableI == index2 && iTableJ == index1 ) {
                                RowLink2 pair = new RowLink2( refI, refJ );
                                if ( ! pairs.containsLink( pair ) ) {
                                    double score = 
                                        engine.matchScore( binnedRows[ i ],
                                                           binnedRows[ j ] );
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
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();
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
        Range range = new Range( tables[ 0 ].getColumnCount() );
        BinContents bins = new BinContents( indicator );
        long totalRows = 0;
        for ( int itab = 0; itab < nTable; itab++ ) {
            binRows( itab, range, bins, true );
            totalRows += tables[ itab ].getRowCount();
        }
        long nBin = bins.getRowCount();
        indicator.logMessage( "Average bin count per row: " +
                              (float) ( nBin / (double) totalRows ) );
        LinkSet links = createLinkSet();
        bins.addRowLinks( links );
        return links;
    }

    /**
     * Gets a list of all the pairs of rows which constitute possible 
     * links between two tables.
     *
     * @param   index1   index of the first table
     * @param   index2   index of the second table
     * @return  set of {@link RowLink} objects which constitute possible
     *          matches
     */
    private LinkSet getPossibleInterLinks( int index1, int index2 )
            throws IOException, InterruptedException {
        int ncol = tables[ index1 ].getColumnCount();
        if ( tables[ index2 ].getColumnCount() != ncol ) {
            throw new IllegalStateException();
        }

        long nIncludedRows1 = tables[ index1 ].getRowCount();
        long nIncludedRows2 = tables[ index2 ].getRowCount();

        /* If we have a match engine which is capable of working out a 
         * restricted region over which matches are possible, find the
         * intersection of those ranges for the two tables being matched.
         * This will enable us to throw out any potential matches outside
         * this region without having to bin them, and can save a lot
         * of time and memory for some cases. */
        Range range;
        if ( engine.canBoundMatch() ) {
            indicator.logMessage( "Attempt to locate " +
                                  "restricted common region" );
            try {
                range = Range.intersection( getRange( index1 ),
                                            getRange( index2 ) );
                if ( range != null ) {
                    indicator.logMessage( "Potential match region: " + range );
                    nIncludedRows1 = countInRange( index1, range );
                    nIncludedRows2 = countInRange( index2, range );
                }
                else {
                    indicator.logMessage( "No region overlap"
                                        + " - matches not possible" );
                    return createLinkSet();
                }
            }

            /* The compare() method used in the above processing could
             * result in ClassCastExceptions if some of the columns 
             * are comparable, but not mutually comparable.
             * This is not very likely, but in that case catch the error,
             * forget about trying to identify bounds, and move on.
             * This will only impact efficiency, not correctness. */ 
            catch ( ClassCastException e ) {
                indicator.logMessage( "Common region location failed " +
                                      "(incompatible value types)" );
                range = new Range( ncol );
            }
        }
        else {
            range = new Range( ncol );
        }

        /* Prepare to do the binning.
         * For efficiency, we want to do the table which will fill the 
         * smallest number of bins first (presumably the one with the
         * smallest number of rows in the match region. */
        final int indexA;
        final int indexB;
        if ( nIncludedRows1 < nIncludedRows2 ) {
            indexA = index1;
            indexB = index2;
        }
        else {
            indexA = index2;
            indexB = index1;
        }

        /* Now do the actual binning.  Bin all the rows in the first table,
         * then add any entries from the second table which are in bins
         * we have already seen.  There is no point adding entries from the
         * second table into bins which do not appear in the first one. */
        BinContents bins = new BinContents( indicator );
        binRows( index1, range, bins, true );
        binRows( index2, range, bins, false );

        /* Return the result. */
        LinkSet links = createLinkSet();
        bins.addRowLinks( links );
        return links;
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
        RowRef[] refs = new RowRef[ nTable ];
        LinkSet replacements = createLinkSet();

        /* Go through every link in the set. */
        indicator.startStage( "Eliminating internal links" );
        double nLink = (double) links.size();
        int iLink = 0;
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
                    List repRefs = new ArrayList();
                    for ( int i = 0; i < nTable; i++ ) {
                        if ( refs[ i ] != null ) {
                            repRefs.add( refs[ i ] );
                        }
                    }
                    RowLink repLink = new RowLink( repRefs );
                    replacements.addLink( repLink );
                }
            }
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();
        indicator.logMessage( "Internal links removed: " 
                            + replacements.size() );
        for ( Iterator it = replacements.iterator(); it.hasNext(); ) {
            RowLink repLink = (RowLink) it.next();
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
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
        int nrow = checkedLongToInt( tables[ iTable ].getRowCount() );
        LinkSet singles = createLinkSet();
        for ( int iRow = 0; iRow < nrow; iRow++ ) {
            if ( ! present.get( iRow ) ) {
                singles.addLink( new RowLink( new RowRef( iTable, iRow ) ) );
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
        int ncol = tables[ index0 ].getColumnCount();

        /* Attempt to restrict ranges for match assessments if we can. */
        Range range;
        if ( engine.canBoundMatch() ) {
            indicator.logMessage( "Attempt to locate "
                                + "restricted common region" );
            try {

                /* Locate the ranges of all the tables. */
                Range[] ranges = new Range[ nTable ];
                for ( int i = 0; i < nTable; i++ ) {
                    ranges[ i ] = getRange( i );
                }

                /* Work out the range of the reference table which we are
                 * interested in.  This is its intersection with the union 
                 * of all the other tables. */
                Range unionOthers = null;
                for ( int i = 0; i < nTable; i++ ) {
                    if ( i != index0 ) {
                        unionOthers = unionOthers == null
                                    ? ranges[ i ]
                                    : Range.union( unionOthers, ranges[ i ] );
                    }
                }
                range = Range.intersection( ranges[ index0 ], unionOthers );
                indicator.logMessage( "Potential match region: " + range );
            }
            catch ( ClassCastException e ) {
                indicator.logMessage( "Region location failed "
                                    + "(incompatible value types)" );
                range = new Range( ncol );
            }
        }
        else {
            range = new Range( ncol );
        }

        /* Bin all the rows in the interesting region of the reference table. */
        BinContents bins = new BinContents( indicator );
        binRows( index0, range, bins, true );

        /* Bin any rows in the other tables which have entries in the bins
         * we have already created for the reference table.  Rows without
         * such entries can be ignored. */
        for ( int itab = 0; itab < nTable; itab++ ) {
            if ( itab != index0 ) {
                binRows( itab, range, bins, false );
            }
        }

        /* Convert the result to a link set and return. */
        LinkSet linkSet = createLinkSet();
        bins.addRowLinks( linkSet );
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
        indicator.startStage( "Locating pair matches between " + index0
                            + " and other tables");
        for ( Iterator it = possibleLinks.iterator(); it.hasNext(); ) {

            /* Get the next link and delete it from the input list, for
             * memory efficiency. */
            RowLink link = (RowLink) it.next();
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
                    StarTable table = tables[ ref.getTableIndex() ];
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
                                        engine.matchScore( binnedRows[ i0 ],
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
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();

        /* Store all the pairs in a map keyed by row reference of the reference
         * table. */
        Map pairMap = new HashMap();
        ListStore store = new ListStore();
        for ( Iterator it = pairs.iterator(); it.hasNext(); ) {
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
            Object key = ref0;
            Object value = new ScoredRef( ref1, pair.getScore() );
            pairMap.put( key, store.addItem( pairMap.get( key ), value ) );
        }

        /* Convert the pairs in pairMap to a LinkSet. */
        LinkSet multiLinks = createLinkSet();
        for ( Iterator it = pairMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            RowRef ref0 = (RowRef) entry.getKey();
            ScoredRef[] sref1s =
                (ScoredRef[]) store.getList( entry.getValue() )
                             .toArray( new ScoredRef[ 0 ] );
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
     * @param  useAll  array of flags indicating for each table whether
     *         all rows should be used or just matching ones
     */
    private boolean acceptRow( RowLink link, boolean[] useAll ) {
        boolean[] present = new boolean[ nTable ];
        int nref = link.size();
        for ( int i = 0; i < nref; i++ ) {
            RowRef ref = link.getRef( i );
            int iTable = ref.getTableIndex();
            present[ iTable ] = true;
        }
        boolean ok = true;
        for ( int i = 0; i < nTable; i++ ) {
            boolean pres = present[ i ];
            if ( useAll[ i ] ) {
                if ( pres ) {
                    return true;
                }
            }
            else {
                if ( ! pres ) {
                    ok = false;
                }
            }
        }
        return ok;
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
    private LinkSet eliminateMultipleRowEntries( LinkSet pairs ) 
            throws InterruptedException {

        /* Set up a map to keep track of the best score so far keyed by
         * RowRef. */
        Map bestRowScores = new HashMap();

        /* We will be copying entries from the input map to an output one,
         * retaining only the best matches for each row. */
        LinkSet inPairs = pairs;
        LinkSet outPairs = createLinkSet();

        /* Iterate over each entry in the input set, deleting it and 
         * selectively copying to the output set as we go. 
         * This means we don't need double the amount of memory. */
        double nPair = inPairs.size();
        int iPair = 0;
        indicator.startStage( "Eliminating multiple row references" );
        for ( Iterator it = inPairs.iterator(); it.hasNext(); ) {
            RowLink2 pair = (RowLink2) it.next();
            it.remove();
            double score = pair.getScore();
            if ( pair.size() != 2 || Double.isNaN( score ) || score < 0.0 ) {
                throw new IllegalArgumentException();
            }
            RowRef ref1 = pair.getRef( 0 );
            RowRef ref2 = pair.getRef( 1 );
            if ( ref1.getTableIndex() != 0 || ref2.getTableIndex() != 1 ) {
                throw new IllegalArgumentException();
            }
            RowLink2 best1 = (RowLink2) bestRowScores.get( ref1 );
            RowLink2 best2 = (RowLink2) bestRowScores.get( ref2 );

            /* If neither row in this pair has been seen before, or we
             * have a better match this time than previous appearances,
             * copy this entry across to the output set. */
            if ( ( best1 == null || score < best1.getScore() ) &&
                 ( best2 == null || score < best2.getScore() ) ) {

                /* If a pair associated with either of these rows has been
                 * encountered before now, remove it from the output set. */
                if ( best1 != null ) {
                    outPairs.removeLink( best1 );
                }
                if ( best2 != null ) {
                    outPairs.removeLink( best2 );
                }

                /* Copy the current pair into the output set. */
                outPairs.addLink( pair );

                /* Record the current pair indexed under both its constituent
                 * rows. */
                bestRowScores.put( ref1, pair );
                bestRowScores.put( ref2, pair );
            }

            /* Report on progress. */
            indicator.setLevel( ++iPair / nPair );
        }
        indicator.endStage();
        assert inPairs.size() == 0;
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
        indicator.logMessage( "Agglomerating links" );

        /* Construct a new hash mapping each RowRef in the given set of
         * links to a list of all the links it appears in. */
        Map refMap = new HashMap();
        for ( Iterator linkIt = links.iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ! refMap.containsKey( ref ) ) {
                    refMap.put( ref, new LinkedList() );
                }
                ((Collection) refMap.get( ref )).add( link );
            }
        }

        /* Prepare a new set to contain the agglomerated links.
         * We will populate this with disjoint links at the same time
         * as removing the corresponding RowRefs from the refMap. 
         * This both keeps track of which ones we've done and keeps
         * memory usage down. */
        LinkSet agglomeratedLinks = createLinkSet();

        /* Check for any isolated links, that is ones none of whose members
         * appear in any other links.  These can be handled more efficiently
         * than ones with more complicated relationships. */
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            int nref = link.size();
            boolean isolated = true;
            for ( int i = 0; isolated && i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                Collection refLinks = (Collection) refMap.get( ref );
                isolated = isolated && refLinks.size() == 1;
            } 

            /* If it is isolated, just copy the link to the agglomerated list,
             * and remove the refs from the map. */
            if ( isolated ) {
                agglomeratedLinks.addLink( link );
                for ( int i = 0; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    Object removed = refMap.remove( ref );
                    assert removed != null;
                }
            }
        }

        /* Take a key from the map we have just constructed, and walk its
         * links recursively to see which nodes we can reach from it.
         * Collect such nodes in a set, and create a new
         * RowLink in the output list from it.  This has the side-effect
         * of removing map entries when they have no more unused links,
         * which means we don't encounter them more than once
         * (and it's also good for memory usage).
         * Repeat until there are no nodes left in the input map. */
        double nRefs = refMap.size();
        indicator.startStage( "Walking links" );
        while ( ! refMap.isEmpty() ) {
            indicator.setLevel( 1.0 - ( refMap.size() / nRefs ) );
            RowRef ref1 = (RowRef) refMap.keySet().iterator().next();
            Set refSet = new HashSet();
            walkLinks( ref1, refMap, refSet );
            agglomeratedLinks.addLink( new RowLink( refSet ) );
        }
        indicator.endStage();

        /* Replace the contents of the used list with the new contents. */
        return agglomeratedLinks;
    }

    /**
     * Recursively pulls out connected nodes (RowRefs) from a map of
     * RowRefs to RowLinks and dumps them in a set of nodes.
     *
     * @param   baseRef  the RowRef at which to start/continue the search
     * @param   refMap   a map of RowRefs to (all so far untraversed) RowLinks
     * @param   outSet   an existing set of RowRefs into which new RowRefs
     *                   connected to baseRef should be inserted
     */
    private static void walkLinks( RowRef baseRef, Map refMap, Set outSet ) {

        /* Do nothing if the output set already contains the requested
         * reference; without this test we would recurse to infinite depth. */
        if ( ! outSet.contains( baseRef ) ) {

            /* Get all the links of which this reference is a member. */
            Collection links = (Collection) refMap.get( baseRef );
            if ( ! links.isEmpty() ) {

                /* Add the current row to the output set. */
                outSet.add( baseRef );

                /* Recurse over all the so-far untraversed rows which are
                 * linked to this one. */
                for ( Iterator linkIt = links.iterator(); linkIt.hasNext(); ) {
                    RowLink link = (RowLink) linkIt.next();
                    for ( int i = 0; i < link.size(); i++ ) {
                        RowRef rref = link.getRef( i );
                        walkLinks( rref, refMap, outSet );
                    }

                    /* Having traversed this link, remove it so it is never
                     * encountered again. */
                    linkIt.remove();
                }
            }

            /* If there are no more links in this list, we can forget
             * about it. */
            if ( links.isEmpty() ) {
                refMap.remove( baseRef );
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
        for ( int itab = 0; itab < tables.length; itab++ ) {
            if ( ! tables[ itab ].isRandom() ) {
                throw new IllegalArgumentException( "Table " + tables[ itab ]
                                                  + " is not random access" );
            }
        }
    }

    /**
     * Calculates the upper and lower bounds of the region in tuple-space
     * for one of this matcher's tables.  The result represents a rectangular
     * region in tuple-space.  Any of the bounds may be null to indicate
     * no limit in that direction.
     *
     * @param   tIndex  index of the table to calculate limits for
     * @return  range   bounds of region inhabited by table
     * @throws  ClassCastException  if objects are not mutually comparable
     */
    private Range getRange( int tIndex )
            throws IOException, InterruptedException {
        StarTable table = tables[ tIndex ];
        int ncol = table.getColumnCount();

        /* We can only do anything useful the match engine knows how to
         * calculate bounds. */
        if ( ! engine.canBoundMatch() ) {
            return new Range( ncol );
        }

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
            return new Range( ncol );
        }

        /* Go through each row finding the minimum and maximum value 
         * for each column (coordinate). */
        Comparable[] mins = new Comparable[ ncol ];
        Comparable[] maxs = new Comparable[ ncol ];
        ProgressRowSequence rseq =
            new ProgressRowSequence( table, indicator,
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
                            Comparable val = (Comparable) cell;
                            mins[ icol ] =
                                Range.min( mins[ icol ], val, false );
                            maxs[ icol ] =
                                Range.max( maxs[ icol ], val, false );
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
        indicator.logMessage( "Limits are: " + new Range( mins, maxs ) );

        /* Get the match engine to convert the min/max values into 
         * a possible match region (presumably by adding a separation
         * region on). */
        Comparable[][] bounds = engine.getMatchBounds( mins, maxs );
        return new Range( bounds[ 0 ], bounds[ 1 ] );
    }

    /**
     * Adds entries for the rows of a table to a given BinContents object.
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
     * @param   bins   bin container object to modify
     * @param   newBins  whether new bins may be added to <code>bins</code>
     */
    private void binRows( int itab, Range range, BinContents bins,
                          boolean newBins )
            throws IOException, InterruptedException {
        StarTable table = tables[ itab ];
        ProgressRowSequence rseq =
            new ProgressRowSequence( table, indicator,
                                     "Binning rows for table " + ( itab + 1 ) );
        long nrow = 0;
        long nexclude = 0;
        try {
            for ( long lrow = 0; rseq.nextProgress(); lrow++ ) {
                Object[] row = rseq.getRow();
                if ( range.isInside( row ) ) {
                    Object[] keys = engine.getBins( row );
                    int nkey = keys.length;
                    if ( nkey > 0 ) {
                        RowRef rref = new RowRef( itab, lrow );
                        for ( int ikey = 0; ikey < nkey; ikey++ ) {
                            Object key = keys[ ikey ];
                            if ( newBins || bins.containsKey( key ) ) {
                                bins.putRowInBin( keys[ ikey ], rref );
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
            indicator.logMessage( nexclude + "/" + nrow + " rows excluded "
                                + "(out of match region)" );
        }
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
    private long countInRange( int tIndex, Range range )
            throws IOException, InterruptedException {
        ProgressRowSequence rseq = 
            new ProgressRowSequence( tables[ tIndex ], indicator, 
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
        indicator.logMessage( nInclude + " rows in match region" );
        return nInclude;
    }

    /**
     * Signals the start of a user-visible matching process.
     */
    private void startMatch() {
        startTime = new Date().getTime();
    }

    /**
     * Signals the end of a user-visible matching process.
     */
    private void endMatch() {
        long millis = new Date().getTime() - startTime;
        indicator.logMessage( "Elapsed time for match: " + ( millis / 1000 ) + 
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
}
