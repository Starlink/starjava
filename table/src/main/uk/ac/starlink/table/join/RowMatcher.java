package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.AbstractStarTable;
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
     * performed with this matcher's tables using its match engine.
     * Each element in the returned array corresponds to one row of
     * the resulting table, with entries from one or more of the
     * constituent tables.
     * 
     * @param  useAll  array of booleans indicating for each table whether
     *         all rows are to be used (otherwise just matched)
     * @return list of {@link RowLink}s corresponding to the selected rows
     */
    public List findMatches( boolean[] useAll ) throws IOException, 
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

        /* Get all the possible pairs. */
        Set pairs = findPairs( getBinContents() );

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

        /* Locate all the pairs. */
        Collection links = findPairs( getBinContents() );

        /* Join up pairs into larger groupings. */
        links = agglomerateLinks( links );

        /* Add unmatched rows if required. */
        if ( includeSingles ) {
            links.addAll( missingSingles( links, 0 ) );
        }

        /* Sort and return the list. */
        links = new ArrayList( links );
        Collections.sort( (List) links );
        return (List) links;
    }

    /**
     * Identifies all the pairs of equivalent rows in a set of RowLinks.
     * The original set is not affected.
     * 
     * @param  possibleLinks  a set of {@link RowLink} objects which 
     *         correspond to groups of possibly matched objects according
     *         to the match engine's criteria
     * @return  a set of RowLink2 objects which represent all the actual
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
                            if ( engine.matches( binnedRows[ i ], 
                                                 binnedRows[ j ] ) ) {
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
     * Returns a set of RowLinks representing bin contents populated 
     * from each row of each table in a given list.
     * Each RowLink in the returned set represents a group of rows
     * which might match according to the MatchEngine's criteria.
     * The complete set gives all the possible groupings of rows 
     * which might match (including singletons).
     * <p>
     * For each row, an entry is made in any of the bins to which it 
     * might correspond.  One or more entries may be made for each row.
     * Finally a set is formed with one entry for each of the encountered
     * bin contents.  Note it is possible (in fact very likely) that
     * a RowRef will crop up in an entry on its own as well as in entries 
     * with any others that it may match.  Note also that inclusion
     * of two RowRefs in the same entry in the result set does not mean
     * that those rows do match, only that they might do.
     * <p>
     * The returned set has values which are {@link RowLink}s
     * for sets of rows which can be found in the same bin.
     *
     * @return  bin-&gt;rowlist map
     */
    private Set getBinContents() throws IOException, InterruptedException {

        /* For each table, identify which bin each row falls into, and
         * place it in a map keyed by that bin. */
        Map rowMap = new HashMap();
        for ( int itab = 0; itab < nTable; itab++ ) {
            indicator.startStage( "Binning rows for table " + ( itab + 1 ) );
            StarTable table = tables[ itab ];
            double nrow = (double) table.getRowCount();
            long lrow = 0;
            for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
                rseq.next();
                Object rref = new RowRef( itab, lrow );
                Object[] keys = engine.getBins( rseq.getRow() );
                int nkey = keys.length;
                for ( int ikey = 0; ikey < nkey; ikey++ ) {
                    Object key = keys[ ikey ];
                    if ( ! rowMap.containsKey( key ) ) {
                        rowMap.put( key, new LinkedList() );
                    }
                    ((Collection) rowMap.get( key )).add( rref );
                }
                lrow++;
                indicator.setLevel( lrow / nrow );
            }
            indicator.endStage();
        }

        /* Replace the value at each bin with a RowLink, since they count
         * as equal if they have the same contents, which means they 
         * are suitable keys for unique inclusion in a set. */
        for ( Iterator it = rowMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            entry.setValue( new RowLink( (Collection) entry.getValue() ) );
        }

        /* Obtain a set containing an entry for each unique contents of a
         * bin. */
        Set binContents = new HashSet( rowMap.values() );

        /* Report on bin occupancy. */
        int nref = 0;
        int nlink = 0;
        for ( Iterator it = binContents.iterator(); it.hasNext(); ) {
            nlink++;
            nref += ((RowLink) it.next()).size();
        }
        indicator.logMessage( "Average bin occupancy: " + 
                              (float) ( nref / (double) nlink ) );

        /* Return the result. */
        return binContents;
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
     * Turns a <tt>long</tt> into an <tt>int</tt>, throwing an unchecked
     * exception if it can't be done.
     */
    private int checkedLongToInt( long lval ) {
        return AbstractStarTable.checkedLongToInt( lval );
    }
}
