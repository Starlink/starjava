package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.StarTable;

/**
 * A Set containing {@link RowLink} objects.  
 * This usually represents a number of
 * matches between different rows of a number of tables.
 * As well as the normal Set operations
 * which provide typesafe equivalents, some methods are provided which 
 * perform operations rationalising the set members.
 * <p>
 * This class is not thread safe.
 *
 * @author   Mark Taylor (Starlink)
 * @see      RowMatcher
 */
public class MatchSet implements Set {

    private Set base = new LinkedHashSet();

    /**
     * Rationalises this set of RowLinks so that it contains no entries
     * corresponding to a match between multiple rows in the same table.
     * Any member RowLink which contains only intra-table matches plus 
     * zero or one inter-table match is removed; any member RowLink which 
     * contains internal matches as well as two or more inter-table matches
     * is replaced by one with only the inter-table ones.  Thus lines
     * within a table which match are effectively ignored for the purposes
     * of the match record.
     */
    public void removeInternalMatches() {

        Set foundTables = new HashSet();
        Set duplicateTables = new HashSet();
        MatchSet replacementLinks = new MatchSet();

        /* Iterate over each member RowLink. */
        for ( Iterator linkIt = iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            Set refs = link.getRowRefs();

            /* Record the identity of any tables which crop up more than 
             * once in the RowRefs that comprise the current RowLink. */
            foundTables.clear();
            duplicateTables.clear();
            for ( Iterator refIt = refs.iterator(); refIt.hasNext(); ) {
                RowRef rref = (RowRef) refIt.next();
                StarTable table = rref.getTable();
                if ( ! foundTables.add( table ) ) {
                    duplicateTables.add( table );
                }
            }

            /* If so, then construct a new set of the RowRefs not implicated. */
            if ( ! duplicateTables.isEmpty() ) {
                Set externalRefs = new HashSet();
                for ( Iterator refIt = refs.iterator(); refIt.hasNext(); ) {
                    RowRef rref = (RowRef) refIt.next();
                    StarTable table = rref.getTable();
                    if ( ! duplicateTables.contains( table ) ) {
                        externalRefs.add( rref );
                    }
                }

                /* If this is a non-degenerate link (>1 element), schedule
                 * it for addition to this set (don't add it yet for fear
                 * of stuffing up the active iterator). */
                if ( externalRefs.size() > 1 ) {
                    replacementLinks.add( externalRefs );
                }

                /* In any case, remove the link which had intra-table parts. */
                linkIt.remove();
            }
        }

        /* Add any of the links which we prepared. */
        addAll( replacementLinks );
    }

    /**
     * Modifies all the RowRefs in this match set by replacing any reference
     * to <tt>srcTable</tt> by a reference to <tt>destTable</tt>.
     * Table references are as defined by the == operator (not <tt>equals</tt>).
     * This is only a sensible thing to do if there is a one-to-one 
     * mapping of rows in <tt>srcTable</tt> and <tt>destTable</tt>.
     *
     * @param   srcTable  table whose references in RowRefs here are to be
     *          replaced
     * @param   destTable  table to replace references to <tt>srcTable</tt>
     */
    public void mapTable( StarTable srcTable, StarTable destTable ) {

        /* Iterate through the list of links copying them to a new set
         * and removing them from the original.  This is necessary since
         * they are immutable so it's not easy to change them in place. */
        Set replacement = new LinkedHashSet();
        for ( Iterator linkIt = base.iterator(); linkIt.hasNext(); ) {
            RowLink srcLink = (RowLink) linkIt.next();
            linkIt.remove();
            List refs = new ArrayList( srcLink.getRowRefs() );
            boolean changed = false;
            for ( ListIterator refIt = refs.listIterator(); refIt.hasNext(); ) {
                RowRef srcRef = (RowRef) refIt.next();
                if ( srcRef.getTable() == srcTable ) {
                    refIt.set( new RowRef( destTable, srcRef.getRowIndex() ) );
                    changed = true;
                }
            }
            replacement.add( changed ? new RowLink( refs ) : srcLink );
        }
        base = replacement;
    }

    /**
     * Rationalises this set's elements so that it contains guaranteed 
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
     * resulting from an invocation of {@link RowMatcher#findPairs}.
     * Note that in this case
     * it is not guaranteed that all the rows it links are mutually equal
     * in the sense defined by the match engine's
     * {@link MatchEngine#matches} method, but the members of each 
     * <tt>RowLink</tt> will form a connected graph with the nodes 
     * connected by matches.
     */
    public void agglomerateLinks() {

        /* Construct a new hash mapping each RowRef in the given set of
         * links to a Set of all the links it appears in. */
        Map refMap = new HashMap();
        for ( Iterator linkIt = iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            for ( Iterator refIt = link.getRowRefs().iterator();
                  refIt.hasNext(); ) {
                RowRef rref = (RowRef) refIt.next();
                if ( ! refMap.containsKey( rref ) ) {
                    refMap.put( rref, new HashSet() );
                }
                ((Set) refMap.get( rref )).add( link );
            }
        }

        /* Take a key from the map we have just constructed, and walk its
         * links recursively to see which nodes we can reach from it.
         * Collect such nodes in a set, and create a new
         * RowLink in the output list from it.  This has the side-effect
         * of removing map entries when they have no more unused links,
         * which means we don't encounter them more than once.
         * Repeat until there are no nodes left in the input map. */
        Set agglomeratedLinks = new HashSet();
        while ( ! refMap.isEmpty() ) {
            RowRef rref1 = (RowRef) refMap.keySet().iterator().next();
            Set refSet = new HashSet();
            walkLinks( rref1, refMap, refSet );
            agglomeratedLinks.add( new RowLink( refSet ) );
        }

        /* Return the list of disjoint links. */
        this.base = agglomeratedLinks;
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
            Set links = (Set) refMap.get( baseRef );
            if ( ! links.isEmpty() ) {

                /* Add the current row to the output set. */
                outSet.add( baseRef );

                /* Recurse over all the so-far untraversed rows which are
                 * linked to this one. */
                for ( Iterator linkIt = links.iterator(); linkIt.hasNext(); ) {
                    RowLink link = (RowLink) linkIt.next();
                    for ( Iterator refIt = link.getRowRefs().iterator();
                          refIt.hasNext(); ) {
                        RowRef rref = (RowRef) refIt.next();
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

    //
    // Set implementation (all on top of the base set).
    //

    /**
     * Adds a new element to this set.
     *
     * @param   link  RowLink  object to add
     * @throws  ClassCastException   if <tt>link</tt> is not a {@link RowLink}
     * @throws  NullPointerException  if <tt>link</tt> is null
     */
    public boolean add( Object link ) {
        link.toString();
        return base.add( (RowLink) link );
    }

    public int size() {
        return base.size();
    }

    public boolean isEmpty() {
        return base.isEmpty();
    }

    public boolean contains( Object o ) {
        return base.contains( o );
    }

    public Iterator iterator() {
        return base.iterator();
    }

    public Object[] toArray() {
        return base.toArray();
    }

    public Object[] toArray( Object[] a ) {
        return base.toArray( a );
    }

    public boolean remove( Object o ) {
        return base.remove( o );
    }

    public boolean containsAll( Collection c ) {
        return base.containsAll( c );
    }

    public boolean addAll( Collection c ) {
        return base.addAll( c );
    }

    public boolean retainAll( Collection c ) {
        return base.retainAll( c );
    }

    public boolean removeAll( Collection c ) {
        return base.removeAll( c );
    }

    public void clear() {
        base.clear();
    }

    public boolean equals( Object o ) {
        return base.equals( o );
    }

    public int hashCode() {
        return base.hashCode();
    }
}
