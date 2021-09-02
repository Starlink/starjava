package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Provides factory methods for producing tables which represent the
 * result of row matching.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MatchStarTables {

    /**
     * Defines the characteristics of a table column which represents the
     * ID of a group of matched row objects.
     */
    public static final ValueInfo GRP_ID_INFO =
        new DefaultValueInfo( "GroupID", Integer.class,
                              "ID for match group" );

    /**
     * Defines the characteristics of a table column which represents the
     * number of matched row objects in a given group (with the same group ID).
     */
    public static final ValueInfo GRP_SIZE_INFO =
        new DefaultValueInfo( "GroupSize", Integer.class,
                              "Number of rows in match group" );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.join" );

    /**
     * Constructs a table made out of two constituent tables joined together
     * according to a {@link LinkSet} describing row matches and 
     * a flag determining what conditions on a {@link RowLink} 
     * give you an output row.
     * The columns of the resulting table are made by appending the
     * columns of the constituent tables side by side.
     * <p>
     * The <tt>tables</tt> array determines which tables columns appear
     * in the output table.  It must have (at least) as many elements
     * as the highest table index in the RowLink set.  Table data
     * will be picked from the <i>n</i>'th table in this array for RowRef
     * elements with a tableIndex of <i>n</i>.  If the <i>n</i>th 
     * element is null, the corresponding columns will not appear in
     * the output table.
     * <p>
     * The <code>matchScoreInfo</code> parameter is optional. 
     * If it is non-null, then an additional column, described by 
     * <code>matchScoreInfo</code>, will be added to the table containing 
     * the <code>score</code> values from any <code>RowLink2</code>s in
     * <code>links</code>.  The content class of <code>matchScoreInfo</code>
     * should be <code>Number</code> or one of its subclasses.
     * <p>
     * This is a convenience method which calls the other
     * <code>makeJoinTable</code> method.
     * 
     * @param  table1  first input table
     * @param  table2  second input table
     * @param  pairs  set of links each representing a matched pair of rows
     *         between <code>table1</code> and <code>table2</code>.
     *         Contents of this set may be modified by this routine
     * @param  joinType  describes how the input list of matched pairs
     *         is used to generate an output sequence of rows
     * @param  addGroups  flag which indicates whether the output table
     *         should, if appropriate, include {@link #GRP_ID_INFO} and
     *         {@link #GRP_SIZE_INFO} columns
     * @param  fixActs  actions to take for deduplicating column names
     *         (array of the same length as <tt>tables</tt>)
     * @param  matchScoreInfo  may supply information about the meaning
     *         of the match scores
     * @return   table representing the join
     */
    public static StarTable makeJoinTable( StarTable table1, StarTable table2,
                                           LinkSet pairs, JoinType joinType,
                                           boolean addGroups,
                                           JoinFixAction[] fixActs,
                                           ValueInfo matchScoreInfo ) {

         /* Process the row link lists according to the chosen join type.
          * This will give a set of rows to be retained in
          * the output table based in some way on the actual pair matches
          * which were found. */
         int nrows1 = Tables.checkedLongToInt( table1.getRowCount() );
         int nrows2 = Tables.checkedLongToInt( table2.getRowCount() );
         LinkSet links = 
             joinType.processLinks( pairs, new int[] { nrows1, nrows2 } );

         /* Work out which of the input tables will actually make an
          * appearance in the output table (i.e. which of their columns
          * will be required). */
         boolean[] useFlags = joinType.getUsedTableFlags();
         StarTable[] tables = new StarTable[ 2 ];
         if ( useFlags[ 0 ] ) {
             tables[ 0 ] = table1;
         }
         if ( useFlags[ 1 ] ) {
             tables[ 1 ] = table2;
         }

         /* If a match score column is going to be empty, make sure it's
          * not entered. */
         if ( ! joinType.getUsedMatchFlag() ) {
             matchScoreInfo = null;
         }

         /* Finally construct and return the new table. */
         return makeJoinTable( tables, links, addGroups, fixActs,
                               matchScoreInfo );
    }

    /**
     * Constructs a table made out of a set of constituent tables
     * joined together according to a {@link LinkSet} describing
     * row matches.
     * The columns of the resulting table are made by appending the
     * columns of the constituent tables side by side.
     * Each row in the resulting table corresponds to one {@link RowLink}
     * entry in a set <tt>rowLinks</tt>; if that RowLink
     * contains a row from one of the tables being joined here,
     * the columns corresponding to that table are filled in.
     * If it contains multiple rows from that table, an arbitrary one
     * of them is filled in.
     * <p>
     * The <tt>tables</tt> array determines which tables columns appear
     * in the output table.  It must have (at least) as many elements
     * as the highest table index in the RowLink set.  Table data
     * will be picked from the <i>n</i>'th table in this array for RowRef
     * elements with a tableIndex of <i>n</i>.  If the <i>n</i>th 
     * element is null, the corresponding columns will not appear in
     * the output table.
     * <p>
     * The <code>matchScoreInfo</code> parameter is optional. 
     * If it is non-null, then an additional column, described by 
     * <code>matchScoreInfo</code>, will be added to the table containing 
     * the <code>score</code> values from the <code>RowLink</code>s in
     * <code>links</code>.  The content class of <code>matchScoreInfo</code>
     * should be <code>Number</code> or one of its subclasses.
     *
     * @param   tables  array of constituent tables
     * @param   rowLinks   set of RowLink objects which define which rows
     *          in one table are associated with which rows in the others
     * @param   addGroups  flag which indicates whether the output table
     *          should, if appropriate, include {@link #GRP_ID_INFO} and
     *          {@link #GRP_SIZE_INFO} columns
     * @param   fixActs  actions to take for deduplicating column names
     *          (array of the same length as <tt>tables</tt>)
     * @param   matchScoreInfo  may supply information about the meaning
     *          of the link scores
     */
    public static StarTable makeJoinTable( StarTable[] tables,
                                           LinkSet rowLinks, boolean addGroups,
                                           JoinFixAction[] fixActs,
                                           ValueInfo matchScoreInfo ) {

        /* Set up index map arrays for each of the constituent tables. */
        int nTable = tables.length;
        int nRow = rowLinks.size();
        long[][] rowIndices = new long[ nTable ][];
        for ( int iTable = 0; iTable < nTable; iTable++ ) {
            if ( tables[ iTable ] != null ) {
                rowIndices[ iTable ] = new long[ nRow ];
                Arrays.fill( rowIndices[ iTable ], -1L );
            }
        }

        /* Initialise an array of score values if required. */
        double[] scores;
        if ( matchScoreInfo != null ) {
            try {
                scores = new double[ nRow ];
                Arrays.fill( scores, Double.NaN );
            }
            catch ( OutOfMemoryError e ) {
                scores = null;
                logger_.warning( "Out of memory calculating match scores - no "
                               + matchScoreInfo.getName()
                               + " column in output" );
            }
        }
        else {
            scores = null;
        }
        int nScore = 0;

        /* Get ready to calculate the link groups if requested. */
        Map<RowLink,LinkGroup> grpMap = null;
        int[] grpSizes = null;
        int[] grpIds = null;
        if ( addGroups ) {
            try {
                grpMap = findGroups( rowLinks );
                if ( grpMap.size() > 0 ) {
                    grpSizes = new int[ nRow ];
                    grpIds = new int[ nRow ];
                }
                else {
                    grpMap = null;
                }
            }
            catch ( OutOfMemoryError e ) {
                grpMap = null;
                grpSizes = null;
                grpIds = null;
                logger_.warning( "Out of memory calculating match groups - no "
                               + GRP_ID_INFO.getName() + " or "
                               + GRP_SIZE_INFO.getName()
                               + " columns in output" );
            }
        }

        /* Populate the index maps from the RowLink list. */
        int iLink = 0;
        for ( RowLink link : rowLinks ) {
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                int iTable = ref.getTableIndex();
                if ( tables[ iTable ] != null ) {
                    rowIndices[ iTable ][ iLink ] = ref.getRowIndex();
                }
            }

            /* If we're scoring and there is a score associated with
             * this row, store it. */
            if ( scores != null && link instanceof RowLink2 ) {
                double score = ((RowLink2) link).getScore();
                if ( ! Double.isNaN( score ) ) {
                    scores[ iLink ] = score;
                    nScore++;
                }
            }

            /* If we're adding groups, store the values. */
            if ( grpMap != null ) {
                LinkGroup grp = grpMap.get( link );
                if ( grp != null ) {
                    grpIds[ iLink ] = grp.getID();
                    grpSizes[ iLink ] = grp.getSize();
                }
            }

            iLink++;
        }
        assert iLink == nRow;

        /* Construct a new table with reordered rows for each of the 
         * input tables; the N'th row of each one corresponds to the
         * N'th RowLink. */
        List<StarTable> subTableList = new ArrayList<StarTable>();
        List<JoinFixAction> fixActList = new ArrayList<JoinFixAction>();
        for ( int iTable = 0; iTable < nTable; iTable++ ) {
            StarTable table = tables[ iTable ];
            if ( table != null ) {
                long[] rowIxs = rowIndices[ iTable ];

                /* Set up column metadata for the sub table; this is mostly
                 * the same as the metadata for the relevant input table but
                 * may need slight adjustment (so clone rather than copy). */
                int nCol = table.getColumnCount();
                final ColumnInfo[] colInfos = new ColumnInfo[ nCol ];
                for ( int iCol = 0; iCol < nCol; iCol++ ) {
                    colInfos[ iCol ] =
                        new ColumnInfo( table.getColumnInfo( iCol ) );
                }

                /* Work out whether there are any blank rows in this table
                 * (missing entries from row link elements). */
                boolean hasBlankRows = false;
                for ( int iRow = 0; iRow < nRow; iRow++ ) {
                    hasBlankRows = hasBlankRows || rowIxs[ iRow ] < 0;
                }

                /* If there are, ensure that all the table's columns are
                 * marked as nullable. */
                if ( hasBlankRows ) {
                    for ( int iCol = 0; iCol < nCol; iCol++ ) {
                        colInfos[ iCol ].setNullable( true );
                    }
                }

                /* Set up a row permuted table based on the row indices 
                 * we have calculated and using the correct column metadata. */
                StarTable subTable = new RowPermutedStarTable( table, rowIxs ) {
                    public ColumnInfo getColumnInfo( int icol ) {
                        return colInfos[ icol ];
                    }
                };

                /* Add the new subtable to the list. */
                subTableList.add( subTable );
                fixActList.add( fixActs[ iTable ] );
            }
        }

        /* We may want to add some additional columns. */
        List<ColumnData> extraCols = new ArrayList<ColumnData>();

        /* If we're collecting group sizes and IDs, add columns containing
         * these values. */
        if ( grpMap != null ) {
            ColumnInfo grpIdInfo = new ColumnInfo( GRP_ID_INFO );
            ColumnInfo grpSizeInfo = new ColumnInfo( GRP_SIZE_INFO );
            final int[] grpIdData = grpIds;
            final int[] grpSizeData = grpSizes;
            assert grpIdData != null;
            assert grpSizeData != null;
            extraCols.add( new ColumnData( GRP_ID_INFO ) {
                public Object readValue( long lrow ) {
                    if ( lrow < Integer.MAX_VALUE ) {
                        int irow = (int) lrow;
                        if ( grpSizeData[ irow ] > 1 ) {
                            return new Integer( grpIdData[ irow ] );
                        }
                    }
                    return null;
                }
            } );
            extraCols.add( new ColumnData( GRP_SIZE_INFO ) {
                public Object readValue( long lrow ) {
                    if ( lrow < Integer.MAX_VALUE ) {
                        int irow = (int) lrow;
                        if ( grpSizeData[ irow ] > 1 ) {
                            return Integer.valueOf( grpSizeData[ irow ] );
                        }
                    }
                    return null;
                }
            } );
        }

        /* If we have match scores, add a column containing them. */
        if ( matchScoreInfo != null && scores != null ) {
            extraCols.add( ArrayColumn
                          .makeColumn( new ColumnInfo( matchScoreInfo ),
                                       scores ) );
        }

        /* If we have come up with any additional columns, package these into
         * a new subtable ready to join with the others. */
        if ( extraCols.size() > 0 ) {
            ColumnStarTable extraTable =
                ColumnStarTable.makeTableWithRows( nRow );
            for ( ColumnData cdata : extraCols ) {
                extraTable.addColumn( cdata );
            }
            subTableList.add( extraTable );
            fixActList.add( JoinFixAction.NO_ACTION );
        }

        /* Join all the subtables up to make one big one. */
        StarTable[] subTables = subTableList.toArray( new StarTable[ 0 ] );
        JoinFixAction[] subFixes = fixActList.toArray( new JoinFixAction[ 0 ] );
        JoinStarTable joined = new JoinStarTable( subTables, subFixes );
        joined.setName( "Joined" );
        return joined;
    }

    /**
     * Constructs a non-random table made out of a set of possibly non-random
     * constituent tables joined together according to a LinkSet.
     * Any input tables which do not have random access must have row
     * ordering consistent with (that is, monotonically increasing for)
     * the ordering of the links in the LinkSet.
     * In practice, this is only likely to be the case if all the input tables
     * are random access except for (at most) one, and the links are
     * ordered with reference to that one.
     * If this requirement is not met, sequential access to the resulting
     * table is likely to fail at some point.
     *
     * @param  tables  array of constituent tables
     * @param  rowLinks  link set defining the match
     * @param  fixActs  actions to take for deduplicating column names
     *                  (array of the same size as <code>tables</code>)
     * @param  matchScoreInfo  may suply information about the meaning of
     *                         the match scores, if present
     */
    public static StarTable makeSequentialJoinTable( StarTable[] tables,
                                                     final LinkSet rowLinks,
                                                     JoinFixAction[] fixActs,
                                                     ValueInfo matchScoreInfo ){

        /* Prepare subtables, one for each input table but based on the
         * given row links (these control row ordering). */
        List<StarTable> subTableList = new ArrayList<StarTable>();
        List<JoinFixAction> fixActList = new ArrayList<JoinFixAction>();
        for ( int iTable = 0; iTable < tables.length; iTable++ ) {
            StarTable table = tables[ iTable ];
            subTableList.add( new RowLinkTable( table, iTable ) {
                public Iterator<RowLink> getLinkIterator() {
                    return rowLinks.iterator();
                }
            } );
            fixActList.add( fixActs[ iTable ] );
        }

        /* Add a subtable for additional information (score column)
         * if required. */
        if ( matchScoreInfo != null ) {
            final ColumnInfo matchInfo = new ColumnInfo( matchScoreInfo );
            StarTable matchTable = new AbstractStarTable() {
                public ColumnInfo getColumnInfo( int icol ) {
                    return icol == 0 ? matchInfo : null;
                }
                public int getColumnCount() {
                    return 1;
                }
                public long getRowCount() {
                    return -1L;
                }
                public RowSequence getRowSequence() {
                    final Iterator<RowLink> linkIt = rowLinks.iterator();
                    return new RowSequence() {
                        RowLink link_;
                        public boolean next() {
                            if ( linkIt.hasNext() ) {
                                link_ = linkIt.next();
                                return true;
                            }
                            else {
                                link_ = null;
                                return false;
                            }
                        }
                        public Object getCell( int icol ) {
                            return getScore();
                        }
                        public Object[] getRow() {
                            return new Object[] { getScore() };
                        }
                        public void close() {
                        }
                        private Double getScore() {
                            if ( link_ instanceof RowLink2 ) {
                                double score = ((RowLink2) link_).getScore();
                                if ( ! Double.isNaN( score ) ) {
                                    return new Double( score );
                                }
                            }
                            return null;
                        }
                    };
                }
            };
            subTableList.add( matchTable );
            fixActList.add( JoinFixAction.NO_ACTION );
        }

        /* Amalgamate tables and return. */
        RowLinkTable[] subTables = subTableList.toArray( new RowLinkTable[ 0 ]);
        JoinFixAction[] subFixes = fixActList.toArray( new JoinFixAction[ 0 ] );
        StarTable joined = new JoinStarTable( subTables, subFixes );
        joined.setName( "Joined" );
        return joined;
    }

    /**
     * Analyses a set of RowLinks to mark as linked rows of a given table.
     * The result of this method is a two-column table whose rows 
     * correspond one-to-one with the rows of the table referenced in
     * the link set.
     * The output columns are defined by the constants 
     * {@link #GRP_ID_INFO} and {@link #GRP_SIZE_INFO}.
     * Rows of the table linked together
     * by <tt>rowLinks</tt> are assigned the same integer value in
     * the new GRP_ID_INFO column, and the GRP_SIZE_INFO column
     * indicates how many rows are linked together in this way.
     * Each group corresponds to a single RowLink; if a row is part of
     * more than one RowLink then only one of them will be recorded
     * in the new columns.
     * Any rows linked in <tt>rowLinks</tt> which do not refer to 
     * <tt>table</tt> have null entries in these columns.
     *
     * @param   iTable the index of the table in which internal matches 
     *          are to be sought
     * @param   rowLinks  a collection of {@link RowLink} objects 
     *          linking groups of rows together
     * @param   rowCount  number of rows in the returned table 
     *          (must be large enough
     *          to accommodate the indices in <tt>rowLinks</tt>)
     * @return  a new two-column table with a one-to-one row correspondance
     *          with the table describing internal row matches
     */
    public static StarTable makeInternalMatchTable( int iTable, 
                                                    LinkSet rowLinks, 
                                                    long rowCount ) {
        final int nrow = Tables.checkedLongToInt( rowCount );

        /* Construct and populate arrays containing per-row information
         * about internal matches. */
        final int[] grpIds = new int[ nrow ];
        final int[] grpSizes = new int[ nrow ];
        int grpId = 0;
        for ( RowLink link : rowLinks ) {
            grpId++;
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() == iTable ) {
                    long lrow = ref.getRowIndex();
                    int irow = Tables.checkedLongToInt( lrow );
                    grpIds[ irow ] = grpId;
                    grpSizes[ grpId ]++;
                }
            } 
        }

        /* Create column objects for the per-row internal match information. */
        ColumnData grpIdColumn = new ColumnData( GRP_ID_INFO ) {
            public Object readValue( long lrow ) {
                if ( lrow >= nrow ) {
                    return null;
                }
                else {
                    int grpId = grpIds[ (int) lrow ];
                    return grpId > 0 ? new Integer( grpId ) : null;
                }
            }
        };
        ColumnData grpSizeColumn = new ColumnData( GRP_SIZE_INFO ) {
            public Object readValue( long lrow ) {
                if ( lrow >= nrow ) {
                    return null;
                }
                else {
                    int grpId = grpIds[ (int) lrow ];
                    return grpId > 0 ? Integer.valueOf( grpSizes[ grpId ] )
                                     : null;
                }
            }
        };

        /* Construct and return a new table which incorporates the 
         * per-row internal match information. */
        ColumnStarTable grpTable = ColumnStarTable.makeTableWithRows( nrow );
        grpTable.addColumn( grpIdColumn );
        grpTable.addColumn( grpSizeColumn );
        return grpTable;
    }

    /**
     * Returns a mapping from {@link RowLink}s to {@link LinkGroup}s
     * which describes connected groups of links in the input LinkSet.
     * A related group is one in which the RowRefs of its constituent
     * RowLinks form a connected graph in which RowRefs are the nodes
     * and RowLinks are the edges.
     * A LinkGroup with a link count of more than one therefore
     * represents an ambiguous match, that is one in which one or more
     * of its RowRefs is contained in more than one RowLink in the
     * original LinkSet.
     *
     * <p>The returned map contains entries only for non-trivial LinkGroups,
     * that is ones which contain more than one link.
     * 
     * @param  links  link set representing a set of matches
     * @return  RowLink -&gt; LinkGroup mapping describing connected groups
     *          in <code>links</code> 
     */
    public static Map<RowLink,LinkGroup> findGroups( LinkSet links ) {
 
        /* Populate a map from RowRefs to Tokens for every RowRef in
         * the input link set.  Each token is joined to any other tokens
         * in which its RowRef participates. */
        Map<RowRef,Token> refMap = new HashMap<RowRef,Token>();
        int iLink = 0;
        for ( RowLink link : links ) {
            iLink++;
            Token linkToken = new Token( iLink );
            for ( int i = 0; i < link.size(); i++ ) {
                RowRef ref = link.getRef( i );

                /* If we've already seen this ref, inform it about this
                 * link by joining its token with the one for this link. */
                if ( refMap.containsKey( ref ) ) {
                    Token refToken = refMap.get( ref );
                    refToken.join( linkToken );
                }

                /* Otherwise, enter a new token into the map for it. */
                else {
                    refMap.put( ref, linkToken );
                }
            }
        }

        /* Remove any entries for groups which only contain a single link. */
        for ( Iterator<Token> it = refMap.values().iterator(); it.hasNext(); ) {
             Token token = it.next();
             if ( token.getGroupSize() == 1 ) {
                 it.remove();
             }
        }

        /* Arrange to renumber the tokens so that we can use smaller 
         * (more human-friendly) LinkGroup IDs to deal with. */
        int[] idMap = getSortedGroupIds( refMap.values() );

        /* Now replace every Token in the map with a LinkGroup that contains
         * the same information.  LinkGroups can be smaller and simpler,
         * since they are immutable. */
        Map<RowRef,LinkGroup> refMapGrp = new HashMap<RowRef,LinkGroup>();
        Map<Integer,LinkGroup> knownGroups = new HashMap<Integer,LinkGroup>();
        for ( Iterator<Map.Entry<RowRef,Token>> it =
                  refMap.entrySet().iterator();
              it.hasNext(); ) {
            Map.Entry<RowRef,Token> entry = it.next();
            RowRef ref = entry.getKey();
            Token token = entry.getValue();
            int grpSize = token.getGroupSize();
            assert grpSize > 1;

            /* If we've constructed an equivalent LinkGroup object before,
             * use that. */ 
            int id = Arrays.binarySearch( idMap, token.getGroupId() ) + 1;
            Integer groupKey = new Integer( id );
            if ( ! knownGroups.containsKey( groupKey ) ) { 
                knownGroups.put( groupKey, new LinkGroup( id, grpSize ) );
            }
            LinkGroup group = knownGroups.get( groupKey );
            refMapGrp.put( ref, group );
            it.remove();
        }
        assert refMap.size() == 0;
        refMap = null;
        knownGroups = null;

        /* Prepare a RowLink to LinkGroup mapping which can be the result. */
        Map<RowLink,LinkGroup> result = new HashMap<RowLink,LinkGroup>();
        for ( RowLink link : links ) {

            /* See if one of the link's RowRefs has an entry in the refMap.
             * If so, it points to a LinkGroup object that the link
             * participates in, so store it in the map. */
            RowRef ref0 = link.getRef( 0 );
            LinkGroup group = refMapGrp.get( ref0 );
            if ( group != null ) {
                result.put( link, group );
            }

            /* Sanity check: all the refs of any link must point to the
             * same LinkGroup object. */
            for ( int i = 0; i < link.size(); i++ ) {
                assert group == refMapGrp.get( link.getRef( i ) );
            }
        }
        return result;
    }

    /**
     * Returns a sorted array of the distinct group ID values in a 
     * collection of Tokens.
     *
     * @param   tokens  collection of tokens
     * @return  sorted array of unique group ID values which were represented
     *          in <code>tokens</code>
     */
    private static int[] getSortedGroupIds( Collection<Token> tokens ) {
        Set<Integer> idSet = new HashSet<Integer>();
        for ( Token token : tokens ) {
            idSet.add( new Integer( token.getGroupId() ) );
        }
        int[] ids = new int[ idSet.size() ];
        int index = 0;
        for ( Integer id : idSet ) {
            ids[ index++ ] = id.intValue();
        }
        assert index == idSet.size();
        Arrays.sort( ids );
        return ids;
    }

    /**
     * Helper class used by the {@link #findGroups} method.
     * A Token represents a single RowLink, but can keep track of
     * other associated Tokens, and thereby work out how many
     * refs constitute a group.
     */
    private static class Token implements Comparable<Token> {

        private final int id_;
        private TokenGroup group_;

        /**
         * Constructs a token with an ID value.
         *
         * @param  id  identifier - should be unique
         */
        public Token( int id ) {
            id_ = id;
        }

        /**
         * Joins this token to another one.
         * This amalgamates the two groups.
         * The behaviour is exactly the same as doing it the other way
         * around (<code>other.join(this)</code>).
         *
         * @param  other  other token whose group is to be amalgamated
         *         with this one's
         */
        public void join( Token other ) {
            if ( this.group_ == null && other.group_ == null ) {
                this.group_ = new TokenGroup();
                this.group_.add( this );
                this.group_.add( other );
                other.group_ = this.group_;
            }
            else if ( this.group_ == null && other.group_ != null ) {
                other.group_.add( this );
                this.group_ = other.group_;
            }
            else if ( this.group_ != null && other.group_ == null ) {
                this.group_.add( other );
                other.group_ = this.group_;
            }
            else if ( this.group_ != null && other.group_ != null ) {
                assert this.group_.contains( this );
                assert other.group_.contains( other );
                if ( this.group_ != other.group_ ) {
                    TokenGroup otherGroup = other.group_;
                    for ( Token tok : other.group_ ) {
                        assert tok.group_ == otherGroup;
                        assert tok.group_ != this.group_;
                        this.group_.add( tok );
                        tok.group_ = this.group_;
                    }
                }
            }
            else {
                assert false;
            }
            assert this.group_ == other.group_;
        }

        /**
         * Returns an ID value which is the same for all members of this
         * token's group.
         *
         * @return  group id
         */
        public int getGroupId() {
            return group_ == null ? id_ 
                                  : group_.getId();
        }

        /**
         * Returns the number of tokens in this token's group.
         *
         * @return  group size (at least 1)
         */
        public int getGroupSize() {
            return group_ == null ? 1
                                  : group_.size();
        }

        public String toString() {
            return "token" + getGroupId() + "[" + getGroupSize() + "]"
                 + "-" + id_;
        }

        public int compareTo( Token other ) {
            if ( this.id_ == other.id_ ) {
                return 0;
            }
            else {
                return this.id_ > other.id_ ? +1 : -1;
            }
        }
    }

    /**
     * Container for Token objects.  Like a Set, each token (by equals())
     * can only be contained once.
     */
    private static class TokenGroup implements Iterable<Token> {
        private final Set<Token> set_ = new HashSet<Token>();
        private int minTokenId_ = Integer.MAX_VALUE;

        /**
         * Adds a token to the set.
         *
         * @param  token  token to add
         */
        void add( Token token ) {
            set_.add( token );
            minTokenId_ = Math.min( minTokenId_, token.id_ );
        }

        /**
         * Indicates whether a given token has been added to this group.
         *
         * @param   token  token to test
         * @return   true iff token is in this group
         */
        boolean contains( Token token ) {
            return set_.contains( token );
        }

        /**
         * Returns the number of distince tokens in this group.
         *
         * @return  size
         */
        int size() {
            return set_.size();
        }

        /**
         * Returns a unique identifier for this group.
         * In the current implementation, it's the ID of the lowest-numbered
         * token.
         *
         * @return  group id
         */
        int getId() {
            return minTokenId_;
        }

        /**
         * Returns an interator over the unique tokens in this group.
         *
         * @return  iterator over Token objects
         */
        public Iterator<Token> iterator() {
            return set_.iterator();
        }
    }
}
