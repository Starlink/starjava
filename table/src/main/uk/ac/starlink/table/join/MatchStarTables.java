package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPermutedStarTable;
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
        new DefaultValueInfo( "MatchID", Integer.class,
                              "ID for internal match group" );

    /**
     * Defines the characteristics of a table column which represents the
     * number of matched row objects in a given group (with the same group ID).
     */
    public static final ValueInfo GRP_SIZE_INFO =
        new DefaultValueInfo( "MatchCount", Integer.class,
                              "Number of rows in internal match group" );


    /**
     * Constructs a table made out of a set of constituent tables
     * joined together.
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
     *
     * @param   tables  array of constituent tables
     * @param   rowLinks   set of RowLink objects which define which rows
     *          in one table are associated with which rows in the others
     * @param   fixActs  actions to take for deduplicating column names
     *          (array of the same length as <tt>tables</tt>)
     */
    public static StarTable makeJoinTable( StarTable[] tables,
                                           Collection rowLinks,
                                           JoinStarTable.FixAction[] fixActs ) {

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

        /* Populate the index maps from the RowLink list. */
        int iLink = 0;
        for ( Iterator it = rowLinks.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            int nref = link.size();
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                int iTable = ref.getTableIndex();
                if ( tables[ iTable ] != null ) {
                    rowIndices[ iTable ][ iLink ] = ref.getRowIndex();
                }
            }
            iLink++;
        }
        assert iLink == nRow;

        /* Construct a new table from all the joined up ones. */
        List subTableList = new ArrayList();
        for ( int iTable = 0; iTable < nTable; iTable++ ) {
            StarTable table = tables[ iTable ];
            if ( table != null ) {
                StarTable subTable =
                    new RowPermutedStarTable( table, rowIndices[ iTable ] );
                subTableList.add( subTable );
            }
        }
        StarTable[] subTables = 
            (StarTable[]) subTableList.toArray( new StarTable[ 0 ] );
        JoinStarTable joined = new JoinStarTable( subTables, fixActs );
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
                                                    Collection rowLinks, 
                                                    long rowCount ) {
        final int nrow = Tables.checkedLongToInt( rowCount );

        /* Construct and populate arrays containing per-row information
         * about internal matches. */
        final int[] grpIds = new int[ nrow ];
        final int[] grpSizes = new int[ nrow ];
        int grpId = 0;
        for ( Iterator it = rowLinks.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
                    return grpId > 0 ? new Integer( grpSizes[ grpId ] ) : null;
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
     * Constructs a new wide table from a single given base table and a set of
     * RowLinks.  The resulting table consists of a number of sections
     * of the original table placed side by side, so it has 
     * <tt>width</tt> times the number of columns that <tt>table</tt> does.
     * Each row is constructed from one or more rows of the original table; 
     * each output row corresponds to a single RowLink.
     * Only row links which have at least <tt>minSize</tt> entries and
     * no more than <tt>maxSize</tt> entries are converted into output rows;
     * if there are more entries than the width of the table the extras
     * are just discarded.
     * Any row references in a RowLink not corresponding to table index
     * <tt>iTable</tt> are ignored.
     *
     * @param  table  input table
     * @param  iTable  index corresponding to this table in the 
     *                 <tt>rowLinks</tt> set
     * @param  links     collection of {@link RowLink} objects describing the
     *                   matches.  This collection is modified on exit
     * @param  width     width of the output table as a multiple of the
     *                   width of the input table
     * @param  minSize   minimum number of entries in a RowLink to count as
     *                   an output row
     * @param  maxSize   maximum number of entries in a RowLink to count as
     *                   an output row; also the width of the output table
     *                   (as a multiple of the width of the input table)
     * @param  fixActs   actions to take for deduplicating column names
     *                   (<tt>width</tt>-element array, or <tt>null</tt>)
     */
    public static StarTable makeParallelMatchTable( StarTable table, int iTable,
                                                    Collection links, int width,
                                                    int minSize, int maxSize,
                                           JoinStarTable.FixAction[] fixActs ) {

        /* Get rid of any links which we won't be using. */
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            int nref = link.size();
            int n0ref = 0;
            for ( int i = 0; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() == iTable ) {
                    n0ref++;
                }
            }
            if ( n0ref < minSize || n0ref > maxSize ) {
                it.remove();
            }
        }

        /* Get the number of rows. */
        int nrow = links.size();

        /* Construct the constituent tables which will sit side by side 
         * in the returned table. */

        /* Prepare a set of indices which describe where rows in the new
         * constituent subtables will come from in the original table. */
        long[][] rowIndices = new long[ width ][];
        for ( int i = 0; i < width; i++ ) {
            rowIndices[ i ] = new long[ nrow ];
            Arrays.fill( rowIndices[ i ], -1L );
        }

        /* Populate these indices from the link set. */
        int iLink = 0;
        for ( Iterator it = links.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
            int nref = link.size();
            int refPos = 0;
            for ( int i = 0; i < nref && refPos < width; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() == iTable ) {
                    rowIndices[ refPos++ ][ iLink ] = ref.getRowIndex();
                }
            }
            assert refPos >= minSize && refPos <= maxSize;
            iLink++;
        }
        assert iLink == nrow;

        /* Construct a set of new tables, one for each set of columns
         * in the output table. */
        StarTable[] subTables = new StarTable[ width ];
        for ( int i = 0; i < width; i++ ) {
            subTables[ i ] = new RowPermutedStarTable( table, rowIndices[ i ] );
        }

        /* Doctor the column names in the constituent tables to reduce
         * confusion. */
        int ncol = table.getColumnCount();
        int xNcol = ncol * width;
        final ColumnInfo[] colinfos = new ColumnInfo[ xNcol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo cinfo = table.getColumnInfo( ic );
            for ( int iw = 0; iw < width; iw++ ) {
                ColumnInfo ci = new ColumnInfo( cinfo );
                ci.setName( ci.getName() + "_" + ( iw + 1 ) );
                colinfos[ ic + iw * ncol ] = ci;
            }
        }

        /* Construct a new table from all the parallel ones. */
        JoinStarTable joined = new JoinStarTable( subTables, fixActs ) {
            public ColumnInfo getColumnInfo( int icol ) {
                return colinfos[ icol ];
            }
        };
        String name;
        switch ( width ) {
            case 2: name = "pairs"; break;
            case 3: name = "triples"; break;
            case 4: name = "quads"; break;
            default: name = "setsOf" + width;
        }
        joined.setName( name );
        return joined;
    }
}
