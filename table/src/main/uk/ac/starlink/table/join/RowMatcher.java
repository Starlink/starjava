package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
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
     * Returns a list of RowLink objects corresponding to a match 
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
     * @return  list of {@link RowLink}s describing matched groups
     */
    public List findPairMatches( boolean req1, boolean req2 )
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have two tables. */
        if ( nTable != 2 ) {
            throw new IllegalStateException( "findPairMatches only makes sense"
                                           + " for 2 tables" );
        }
        startMatch();

        /* Get the possible candidates for inter-table links. */
        long nr1 = tables[ 0 ].getRowCount();
        long nr2 = tables[ 1 ].getRowCount();
        Set possibleLinks = nr1 < nr2 ? getPossibleInterLinks( 0, 1 )
                                      : getPossibleInterLinks( 1, 0 );

        /* Get all the possible inter-table pairs. */
        Map pairScores = findInterPairs( possibleLinks, 0, 1 );
        possibleLinks = null;

        /* Make sure that no row is represented more than once
         * (this isn't necessary for the result to make sense, but it's 
         * probably what the caller is expecting). */
        eliminateMultipleRowEntries( pairScores );
        Collection pairs = new HashSet( pairScores.keySet() );
        pairScores = null;

        /* We now have a set of links corresponding to all the matches
         * with one entry for each of two or more of the input tables.
         * In the case that we want to output some links with unmatched
         * parts, add new singleton row links as necessary. */
        Set singles1 = req1 ? null : missingSingles( pairs, 0 );
        Set singles2 = req2 ? null : missingSingles( pairs, 1 );
        if ( singles1 != null ) {
            pairs.addAll( singles1 );
        }
        if ( singles2 != null ) {
            pairs.addAll( singles2 );
        }

        /* Sort and return. */
        List pairList = new ArrayList( pairs );
        Collections.sort( pairList );
        endMatch();
        return pairList;
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
    public List findGroupMatches( boolean[] useAll ) throws IOException, 
                                                     InterruptedException {
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
        Set pairs = findPairs( getAllPossibleLinks() );

        /* Exclude any pairs which represent links between different rows
         * of the same table. */
        eliminateInternalLinks( pairs );

        /* Join up pairs into larger groupings. */
        Set links = agglomerateLinks( pairs );
        pairs = null;

        /* This could introduce more internal links - get rid of them. */
        eliminateInternalLinks( links );

        /* We now have a set of links corresponding to all the matches
         * with one entry for each of two or more of the input tables.
         * In the case that we want to output some links with unmatched
         * parts, add new singleton row links as necessary. */
        Set singles = new HashSet();
        for ( int i = 0; i < nTable; i++ ) {
            if ( useAll[ i ] ) {
                singles.addAll( missingSingles( links, i ) );
            }
        }
        links.addAll( singles );
        singles = null;

        /* Now filter the links to contain only those composite rows we're
         * interested in.  Copy or discard and remove rows as we go to
         * save on memory. */
        List selected = new ArrayList();
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            if ( acceptRow( link, useAll ) ) {
                selected.add( link );
            }
            it.remove();
        }

        /* Finally sort the rows into a sensible order. */
        Collections.sort( selected );

        /* Return the matched list. */
        endMatch();
        return selected;
    }

    /**
     * Returns a list of RowLink objects corresponding to all the internal
     * matches in this matcher's sole table using its match engine.
     *
     * @param  includeSingles  whether to include unmatched (singleton) 
     *         row links in the returned link set
     * @return a list of {@link RowLink} objects giving all the groups of
     *         matched objects in this matcher's sole table
     */
    public List findInternalMatches( boolean includeSingles ) 
            throws IOException, InterruptedException {
        checkRandom();

        /* Check we have a single table. */
        if ( nTable != 1 ) {
            throw new IllegalStateException( "Internal matches only make sense "
                                           + "with a single table" );
        }
        startMatch();

        /* Locate all the pairs. */
        Collection links = findPairs( getAllPossibleLinks() );

        /* Join up pairs into larger groupings. */
        links = agglomerateLinks( links );

        /* Add unmatched rows if required. */
        if ( includeSingles ) {
            links.addAll( missingSingles( links, 0 ) );
        }

        /* Sort and return the list. */
        links = new ArrayList( links );
        Collections.sort( (List) links );
        endMatch();
        return (List) links;
    }

    /**
     * Identifies all the pairs of equivalent rows in a set of RowLinks.
     * Internal matches (ones corresponding to two rows of the same table)
     * are included as well as external ones.
     * The original set is not affected.
     * 
     * @param  possibleLinks  a set of {@link RowLink} objects which 
     *         correspond to groups of possibly matched objects according
     *         to the match engine's criteria
     * @return  a set of RowLink objects which represent all the actual
     *          distinct row pairs from <tt>possibleLinks</tt>
     */
    private Set findPairs( Collection possibleLinks )
            throws IOException, InterruptedException {
        Set pairSet = new HashSet();
        double nLink = (double) possibleLinks.size();
        int iLink = 0;
        indicator.startStage( "Locating pairs" );
        for ( Iterator it = possibleLinks.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
                        RowLink pair = new RowLink( link.getRef( i ),
                                                    link.getRef( j ) );
                        if ( ! pairSet.contains( pair ) ) {
                            double score = engine.matchScore( binnedRows[ i ],
                                                              binnedRows[ j ] );
                            if ( score >= 0 ) {
                                pairSet.add( pair );
                            }
                        }
                    }
                }
            }
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();
        return pairSet;
    }

    /**
     * Identifies all the pairs of equivalent rows from a set of RowLinks
     * between rows from two specified tables.
     * The returned object is a {@link RowLink}->{@link java.lang.Number} map,
     * in which the keys represent pairs of matching rows, 
     * and the values are their match scores, as obtained from 
     * {@link MatchEngine#matchScore}.
     *
     * <p>The original set is not affected.
     *
     * <p>Since links are being sought between a pair of tables, a more
     * efficient algorithm can be used than for {@link #findPairs}.
     *
     * @param  possibleLinks  a set of {@link RowLink} objects which 
     *         correspond to groups of possibly matched objects 
     *         according to the match engine's criteria
     * @param  index1  index of the first table
     * @param  index2  index of the second table
     * @return  a RowLink-&gt;Double map
     */
    private Map findInterPairs( Collection possibleLinks,
                                int index1, int index2 )
            throws IOException, InterruptedException {
        Map pairMap = new HashMap();
        double nLink = (double) possibleLinks.size();
        int iLink = 0;
        indicator.startStage( "Locating inter-table pairs" );
        for ( Iterator it = possibleLinks.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
                                RowLink pair = new RowLink( refI, refJ );
                                if ( ! pairMap.containsKey( pair ) ) {
                                    double score = 
                                        engine.matchScore( binnedRows[ i ],
                                                           binnedRows[ j ] );
                                    if ( score >= 0 ) {
                                        pairMap.put( pair, 
                                                     new Double( score ) );
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
        return pairMap;
    }

    /**
     * Goes through all tables and gets a preliminary set of all
     * the groups of rows which are possibly linked by a chain of
     * matches.  This includes all inter- and intra-table matches.
     *
     * @return  set of {@link RowLink} objects which constitute possible
     *          matches
     */
    private Set getAllPossibleLinks() throws IOException, InterruptedException {
        BinContents bins = new BinContents( indicator );
        long totalRows = 0;
        long nBin = 0;
        for ( int itab = 0; itab < nTable; itab++ ) {
            StarTable table = tables[ itab ];
            ProgressRowSequence rseq = 
                new ProgressRowSequence( tables[ itab ], indicator,
                                         "Binning rows for table " + 
                                         (itab + 1 ) );
            for ( long lrow = 0; rseq.hasNext(); lrow++ ) {
                rseq.nextProgress();
                Object[] keys = engine.getBins( rseq.getRow() );
                int nkey = keys.length;
                if ( nkey > 0 ) {
                    RowRef rref = new RowRef( itab, lrow );
                    for ( int ikey = 0; ikey < nkey; ikey++ ) {
                        bins.putRowInBin( keys[ ikey ], rref );
                    }
                }
                nBin += nkey;
                totalRows++;
            }
            rseq.close();
        }
        indicator.logMessage( "Average bin count per row: " +
                              (float) ( nBin / (double) totalRows ) );
        return bins.getRowLinks();
    }

    /**
     * Gets a list of all the pairs of rows which constitute possible 
     * links between two tables.  For reasons of efficiency, it's
     * probably best if <tt>index1</tt> refers to the shorter of the tables.
     *
     * @param   index1   index of the first table
     * @param   index2   index of the second table
     * @return  set of {@link RowLink} objects which constitute possible
     *          matches
     */
    private Set getPossibleInterLinks( int index1, int index2 )
            throws IOException, InterruptedException {
        BinContents bins = new BinContents( indicator );

        /* Bin all the rows in the first (hopefully shorter) table. */
        ProgressRowSequence rseq1 = 
            new ProgressRowSequence( tables[ index1 ], indicator,
                                     "Binning rows for table " + 
                                     ( index1 + 1 ) );
        for ( long lrow1 = 0; rseq1.hasNext(); lrow1++ ) {
            rseq1.nextProgress();
            Object[] keys = engine.getBins( rseq1.getRow() );
            int nkey = keys.length;
            if ( nkey > 0 ) {
                RowRef rref = new RowRef( index1, lrow1 );
                for ( int ikey = 0; ikey < nkey; ikey++ ) {
                    bins.putRowInBin( keys[ ikey ], rref );
                }
            }
        }
        rseq1.close();

        /* Bin any of the rows in the second table which will go in bins
         * we've already added from the first one.  There's no point in
         * doing ones which haven't been filled by the first table, 
         * since they can't result in inter-table matches. */
        ProgressRowSequence rseq2 =
            new ProgressRowSequence( tables[ index2 ], indicator,
                                     "Binning rows for table " + 
                                     ( index2 + 1 ) );
        for ( long lrow2 = 0; rseq2.hasNext(); lrow2++ ) {
            rseq2.nextProgress();
            Object[] keys = engine.getBins( rseq2.getRow() );
            int nkey = keys.length;
            if ( nkey > 0 ) {
                RowRef rref = new RowRef( index2, lrow2 );
                for ( int ikey = 0; ikey < nkey; ikey++ ) {
                    Object key = keys[ ikey ];
                    if ( bins.containsKey( key ) ) {
                        bins.putRowInBin( key, rref );
                    }
                }
            }
        }
        rseq2.close();

        /* Return the result. */
        return bins.getRowLinks();
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
    private void eliminateInternalLinks( Collection links )
            throws InterruptedException {
        RowRef[] refs = new RowRef[ nTable ];
        Collection replacements = new ArrayList();

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
                    replacements.add( repLink );
                }
            }
            indicator.setLevel( ++iLink / nLink );
        }
        indicator.endStage();
        indicator.logMessage( "Internal links removed: " 
                            + replacements.size() );
        links.addAll( replacements );
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
    private Set missingSingles( Collection links, int iTable ) {

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
        Set singles = new HashSet();
        for ( int iRow = 0; iRow < nrow; iRow++ ) {
            if ( ! present.get( iRow ) ) {
                singles.add( new RowLink( new RowRef( iTable, iRow ) ) );
            }
        }
        return singles;
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
     * <p>The input collection must be a 
     * {@link RowLink}-&gt;{@link java.lang.Number} map;
     * the keys are 2-ref <tt>RowLink</tt>s representing a matched pair, and
     * the values give the match score, as per {@link MatchEngine#matchScore}.
     * The pairs may only contain RowRefs with a table index of 0 or 1.
     * 
     * @param   pairScores  map {@link RowLink}-&gt;<tt>Number</tt> objects
     *          representing matched pairs - on exit
     *          each RowRef will only appear in at most one RowLink in
     *          the keys
     */
    private void eliminateMultipleRowEntries( Map pairScores ) 
            throws InterruptedException {

        /* Set up a map to keep track of the best score so far keyed by
         * RowRef. */
        Map bestRowScores = new HashMap();

        /* We will be copying entries from the input map to an output one,
         * retaining only the best matches for each row. */
        Map inPairs = pairScores;
        Map outPairs = new HashMap();

        /* Iterate over each entry in the input set, deleting it and 
         * selectively copying to the output set as we go. 
         * This means we don't need double the amount of memory. */
        double nPair = inPairs.size();
        int iPair = 0;
        indicator.startStage( "Eliminating multiple row references" );
        for ( Iterator it = inPairs.entrySet().iterator(); it.hasNext(); ) {

            /* Get the next pair and its score. */
            Map.Entry entry = (Map.Entry) it.next();
            RowLink pair = (RowLink) entry.getKey();
            Number score = (Number) entry.getValue();
            assert pair.size() == 2;
            assert score != null;
            double scoreVal = score.doubleValue();
            RowRef ref1 = pair.getRef( 0 );
            RowRef ref2 = pair.getRef( 1 );
            assert ref1.getTableIndex() == 0;
            assert ref2.getTableIndex() == 1;
            Number score1 = (Number) bestRowScores.get( ref1 );
            Number score2 = (Number) bestRowScores.get( ref2 );

            /* If neither row in this pair has been seen before, or we 
             * have a better match this time than previous appearances,
             * copy this entry across to the output set. */
            if ( ( score1 == null || scoreVal < score1.doubleValue() ) &&
                 ( score2 == null || scoreVal < score2.doubleValue() ) ) {
                outPairs.put( pair, score );
                bestRowScores.put( ref1, score );
                bestRowScores.put( ref2, score );
            }

            /* In any case, remove it from the input set. */
            it.remove();

            /* Report on progress. */
            indicator.setLevel( ++iPair / nPair );
        }
        indicator.endStage();
        assert inPairs.isEmpty();

        /* Repopulate the supplied input set with the output set. */
        assert pairScores == inPairs;  // so..
        assert pairScores.isEmpty();
        pairScores.putAll( outPairs );
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
    private Set agglomerateLinks( Collection links ) 
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
        Set agglomeratedLinks = new HashSet();

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
                agglomeratedLinks.add( link );
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
            agglomeratedLinks.add( new RowLink( refSet ) );
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
    private int checkedLongToInt( long lval ) {
        return Tables.checkedLongToInt( lval );
    }
}
