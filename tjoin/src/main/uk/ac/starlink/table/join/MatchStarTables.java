package uk.ac.starlink.table.join;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
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
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.SplitProcessor;

/**
 * Provides methods for producing tables which represent the
 * result of row matching.
 *
 * <p>This class originally contained only static methods.
 * Currently some methods are static and some are instance methods;
 * those which use a {@link ProgressIndicator} or
 * {@link uk.ac.starlink.util.SplitProcessor} are instance methods
 * which use the values set up at construction time.
 *
 * <p>The methods in this class operate on
 * <code>Collection&lt;RowLink&gt;</code>s
 * rather than on {@link LinkSet}s, to emphasise that they do not
 * modify the contents of the collections.
 * Such collections will typically be sorted into their natural sequence,
 * see {@link #orderLinks}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class MatchStarTables {

    private final ProgressIndicator indicator_;
    private final CollectionRunner<RowLink> linkRunner_;

    private static final CollectionRunner<RowLink> SEQ_RUNNER =
        new CollectionRunner<RowLink>( SplitProcessor
                                      .createSequentialProcessor() );

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
     * Constructs a MatchStarTables with default characteristics.
     */
    public MatchStarTables() {
        this( (ProgressIndicator) null, (SplitProcessor) null );
    }

    /**
     * Constructs a MatchStarTables with configuration.
     *
     * <p>The splitProcessor argument allows to configure how potentially
     * parallel processing is done.
     *
     * @param  indicator  progress indicator, or null for no logging
     * @param  splitProcessor  parallel processing implementation,
     *                         or null for default behaviour
     */
    public MatchStarTables( ProgressIndicator indicator,
                            SplitProcessor<?> splitProcessor ) {
        if ( indicator == null ) {
            indicator = new NullProgressIndicator();
        }
        if ( splitProcessor == null ) {
            splitProcessor = RowRunner.DEFAULT.getSplitProcessor();
        }
        indicator_ = indicator;
        linkRunner_ = new CollectionRunner<RowLink>( splitProcessor );
    }

    /**
     * Constructs a table made out of a set of constituent tables
     * joined together according to a set of RowLinks describing
     * row matches.
     * The columns of the resulting table are made by appending the
     * columns of the constituent tables side by side.
     * Each row in the resulting table corresponds to one {@link RowLink}
     * entry in a set <code>rowLinks</code>; if that RowLink
     * contains a row from one of the tables being joined here,
     * the columns corresponding to that table are filled in.
     * If it contains multiple rows from that table, an arbitrary one
     * of them is filled in.
     * <p>
     * The <code>tables</code> array determines which tables columns appear
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
     *          (array of the same length as <code>tables</code>)
     * @param   matchScoreInfo  may supply information about the meaning
     *          of the link scores
     */
    public StarTable makeJoinTable( StarTable[] tables,
                                    Collection<RowLink> rowLinks,
                                    boolean addGroups, JoinFixAction[] fixActs,
                                    ValueInfo matchScoreInfo )
            throws InterruptedException {

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
        Map<Integer,Integer> idMap = new HashMap<>();
        int[] iGrp = new int[ 1 ];
        ProgressTracker imTracker =
            new ProgressTracker( indicator_, rowLinks.size(),
                                 "Populate index maps" );
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

                    /* The integer group IDs we already have could be used
                     * as is, but we perform a one-to-one mapping of them to
                     * new values 1, 2, 3... in the order they are encountered,
                     * for two reasons.  First, it provides deterministic
                     * assignment of group IDs even if the earlier processing
                     * has assigned them in an unpredictable order
                     * (for instance as the result of multi-threading)
                     * with benefits for regression testing etc, and
                     * second this yields more human-friendly groupID values.
                     * We keep track of the mapping using an oldId->newId map,
                     * adding a new entry every time we see a new group ID. */
                    int id =
                        idMap
                       .computeIfAbsent( Integer.valueOf( grp.getID() ),
                                         i -> Integer.valueOf( ++iGrp[ 0 ] ) )
                       .intValue();
                    grpIds[ iLink ] = id;
                    grpSizes[ iLink ] = grp.getSize();
                }
            }
            iLink++;
            imTracker.nextProgress();
        }
        imTracker.close();
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
                            return Integer.valueOf( grpIdData[ irow ] );
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
     * constituent tables joined together according to a RowLink collection.
     * Any input tables which do not have random access must have row
     * ordering consistent with (that is, monotonically increasing for)
     * the ordering of the links.
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
    public static StarTable
            makeSequentialJoinTable( StarTable[] tables,
                                     final Collection<RowLink> rowLinks,
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
                                    return Double.valueOf( score );
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
     * by <code>rowLinks</code> are assigned the same integer value in
     * the new GRP_ID_INFO column, and the GRP_SIZE_INFO column
     * indicates how many rows are linked together in this way.
     * Each group corresponds to a single RowLink; if a row is part of
     * more than one RowLink then only one of them will be recorded
     * in the new columns.
     * Any rows linked in <code>rowLinks</code> which do not refer to 
     * <code>table</code> have null entries in these columns.
     *
     * @param   iTable the index of the table in which internal matches 
     *          are to be sought
     * @param   rowLinks  a collection of {@link RowLink} objects 
     *          linking groups of rows together
     * @param   rowCount  number of rows in the returned table 
     *          (must be large enough
     *          to accommodate the indices in <code>rowLinks</code>)
     * @return  a new two-column table with a one-to-one row correspondance
     *          with the table describing internal row matches
     */
    public static StarTable
            makeInternalMatchTable( int iTable, Collection<RowLink> rowLinks, 
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
                    return grpId > 0 ? Integer.valueOf( grpId ) : null;
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
     * which describes connected groups of links in the input collection.
     * A related group is one in which the RowRefs of its constituent
     * RowLinks form a connected graph in which RowRefs are the nodes
     * and RowLinks are the edges.
     * A LinkGroup with a link count of more than one therefore
     * represents an ambiguous match, that is one in which one or more
     * of its RowRefs is contained in more than one RowLink in the
     * original RowLink collection.
     *
     * <p>The returned map contains entries only for non-trivial LinkGroups,
     * that is ones which contain more than one link.
     * 
     * @param  links  link set representing a set of matches
     * @return  RowLink -&gt; LinkGroup mapping describing connected groups
     *          in <code>links</code> 
     */
    public Map<RowLink,LinkGroup> findGroups( Collection<RowLink> links )
            throws InterruptedException {
 
        /* Populate a map from RowRefs to Tokens for every RowRef in
         * the input link set.  Each token is joined to any other tokens
         * in which its RowRef participates.
         * This is ready for parallelisation, but that seems to slow things
         * down in at least some cases, so keep it single-threaded for now. */
        boolean idrefParallel = false;
        indicator_.startStage( "Identify shared refs" );
        Map<RowRef,Token> refMap =
            ( idrefParallel ? linkRunner_ : SEQ_RUNNER )
           .collect( new RefTokenCollector(), links, indicator_ );
        indicator_.endStage();

        /* Remove any entries for groups which only contain a single link. */
        for ( Iterator<Token> it = refMap.values().iterator(); it.hasNext(); ) {
             Token token = it.next();
             if ( token.getGroupSize() == 1 ) {
                 it.remove();
             }
        }

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
            int id = token.getGroupId();
            Integer groupKey = Integer.valueOf( id );
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
        boolean grpParallel = true;
        indicator_.startStage( "Map links to groups" );
        Map<RowLink,LinkGroup> result =
            ( grpParallel ? linkRunner_ : SEQ_RUNNER )
           .collect( new GroupCollector( refMapGrp ), links, indicator_ );
        indicator_.endStage();
        return result;
    }

    /**
     * Best-efforts Conversion of a LinkSet, which is what RowMatcher outputs,
     * to a Collection of RowLinks, which is what's used by this class.
     * This essentially calls {@link LinkSet#toSorted}, but in case
     * that fails for lack of memory (not that likely, but could happen)
     * it will write a message through the logging system and 
     * return a value giving an unordered result instead.
     *
     * @param   linkSet  unordered LinkSet
     * @return   input links as a collection, but if possible in natural order
     */
    public static Collection<RowLink> orderLinks( final LinkSet linkSet ) {
        try {
            return linkSet.toSorted();
        }
        catch ( OutOfMemoryError e ) {
            logger_.log( Level.WARNING,
                         "Can't sort matches - matched table rows may be "
                       + "in an unhelpful order", e );
            return new AbstractCollection<RowLink>() {
                public int size() {
                    return linkSet.size();
                }
                public Iterator<RowLink> iterator() {
                    return linkSet.iterator();
                }
            };
        }
    }

    /**
     * Creates a MatchStarTables instance based on given optional
     * progress indicator and row runner.
     *
     * @param  indicator  progress indicator, or null for no logging
     * @param  rowRunner  parallel processing implementation,
     *                    or null for default behaviour
     */
    public static MatchStarTables createInstance( ProgressIndicator indicator,
                                                  RowRunner rowRunner ) {
        SplitProcessor<?> splitProcessor =
            rowRunner == null ? null : rowRunner.getSplitProcessor();
        return new MatchStarTables( indicator, splitProcessor );
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

    /**
     * Collector that turns a stream of RowLinks into a RowRef-Token map,
     * which indicates for each RowRef which RowLinks it participates in.
     */
    private static class RefTokenCollector
            implements CollectionRunner
                      .ElementCollector<RowLink,Map<RowRef,Token>> {
        private final AtomicInteger tCounter_;
        RefTokenCollector() {
            tCounter_ = new AtomicInteger();
        }
        public Map<RowRef,Token> createAccumulator() {
            return new HashMap<RowRef,Token>();
        }
        public Map<RowRef,Token> combine( Map<RowRef,Token> map1,
                                          Map<RowRef,Token> map2 ) {
            boolean big1 = map1.size() >= map2.size();
            Map<RowRef,Token> mapA = big1 ? map1 : map2;
            Map<RowRef,Token> mapB = big1 ? map2 : map1;
            for ( Map.Entry<RowRef,Token> entryB : mapB.entrySet() ) {
                RowRef ref = entryB.getKey();
                Token tokenB = entryB.getValue();
                addToken( mapA, ref, tokenB );
            }
            return mapA;
        }
        public void accumulate( RowLink link, Map<RowRef,Token> refMap ) {
            Token linkToken = new Token( tCounter_.getAndIncrement() );
            int nr = link.size();
            for ( int i = 0; i < nr; i++ ) {
                addToken( refMap, link.getRef( i ), linkToken );
            }
        }

        /**
         * Ensures that the map entry for a given RowRef includes
         * association with a given Token.
         *
         * @param  refMap  map
         * @param  ref     row ref
         * @param  linkToken  token that must appear in map entry for ref
         */
        private void addToken( Map<RowRef,Token> refMap, RowRef ref,
                               Token linkToken ) {

            /* If we've already seen this ref, inform it about this
             * link by joining its token with the one for this link. */
            if ( refMap.containsKey( ref ) ) {
                refMap.get( ref ).join( linkToken );
            }

            /* Otherwise, enter a new token into the map for it. */
            else {
                refMap.put( ref, linkToken );
            }
        }
    }

    /**
     * Collector that associates RowLinks with LinkGroups.
     */
    private static class GroupCollector
            implements CollectionRunner
                      .ElementCollector<RowLink,Map<RowLink,LinkGroup>> {
        final Map<RowRef,LinkGroup> refMapGrp_;

        /**
         * Constructor.
         *
         * @param  refMapGrp  map indicating which link group each row ref
         *                    participates in
         */
        GroupCollector( Map<RowRef,LinkGroup> refMapGrp ) {
            refMapGrp_ = refMapGrp;
        }
        public Map<RowLink,LinkGroup> createAccumulator() {
            return new HashMap<RowLink,LinkGroup>();
        }
        public Map<RowLink,LinkGroup> combine( Map<RowLink,LinkGroup> map1,
                                               Map<RowLink,LinkGroup> map2 ) {
            if ( map1.size() > map2.size() ) {
                map1.putAll( map2 );
                return map1;
            }
            else {
                map2.putAll( map1 );
                return map2;
            }
        }
        public void accumulate( RowLink link, Map<RowLink,LinkGroup> result ) {

            /* See if one of the link's RowRefs has an entry in the refMap.
             * If so, it points to a LinkGroup object that the link
             * participates in, so store it in the map. */
            RowRef ref0 = link.getRef( 0 );
            LinkGroup group = refMapGrp_.get( ref0 );
            if ( group != null ) {
                result.put( link, group );
            }

            /* Sanity check: all the refs of any link must point to the
             * same LinkGroup object. */
            assert refsInGroup( link, group );
        }

        /**
         * Tests whether all the refs in a link point to a given LinkGroup.
         *
         * @param  link  link
         * @param  group  group
         * @return true iff all refs in link point to the same group
         */
        private boolean refsInGroup( RowLink link, LinkGroup group ) {
            for ( int i = 0; i < link.size(); i++ ) {
                if ( group != refMapGrp_.get( link.getRef( i ) ) ) {
                    return false;
                }
            }
            return true;
        }
    }
}
