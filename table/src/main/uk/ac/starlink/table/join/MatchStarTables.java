package uk.ac.starlink.table.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
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
     * entry in a MatchSet <tt>rowLinks</tt>; if that RowLink
     * contains a row from one of the tables being joined here, 
     * the columns corresponding to that table are filled in.
     * If it contains multiple rows from that table, an arbitrary one
     * of them is filled in.
     * Whether each RowLink results in a row in the resulting table
     * or not is determined by the <tt>requireEntries</tt> argument:
     * for any element of <tt>requireEntries</tt> which is true, 
     * only RowLinks which have an entry for the corresponding element
     * of <tt>tables</tt> will be represented in the output.
     *
     * @param   tables  array of constituent tables
     * @param   rowLinks   set of RowLink objects which define which rows
     *          in one table are associated with which rows in the others
     * @param   requireEntries   array of flags indicating which constituent
     *          tables have to be represented in a RowLink for it to result
     *          in an output row
     * @throws  IllegalArgumentException  
     *          if <tt>tables.length!=requireEntries.length</tt>
     */
    public static StarTable makeJoinTable( StarTable[] tables, 
                                           MatchSet rowLinks,
                                           boolean[] requireEntries ) {
        int nTab = tables.length;
        if ( requireEntries.length != nTab ) {
            throw new IllegalArgumentException( 
                "tables array is a different size from requireEntries array" +
                " (" + tables.length + " != " + requireEntries.length + ")" );
        }

        /* Construct an array of row indices for each row which will appear
         * in the final table.  This has an entry >= 0 in each slot for
         * which a subrow is taken from the table in that position. */
        List indexSets = new ArrayList();
        for ( Iterator linkIt = rowLinks.iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            long[] rowIndices = new long[ nTab ];
            Arrays.fill( rowIndices, -1L );
            for ( Iterator rowIt = link.getRowRefs().iterator();
                  rowIt.hasNext(); ) {
                RowRef rref = (RowRef) rowIt.next();
                StarTable refTab = rref.getTable();
                for ( int itab = 0; itab < nTab; itab++ ) {
                    if ( refTab == tables[ itab ] ) {
                        rowIndices[ itab ] = rref.getRowIndex();
                    }
                }
            }
            boolean use = true;
            for ( int itab = 0; ( itab < nTab ) && use; itab++ ) {
                if ( requireEntries[ itab ] && rowIndices[ itab ] < 0L ) {
                    use = false;
                }
            }
            if ( use ) {
                indexSets.add( rowIndices );
            }
        }

        /* Transpose this list of arrays to give an array of arrays suitable
         * for creating indexed subtables. */
        int nrow = indexSets.size();
        long[][] rowIndices = new long[ nTab ][ nrow ];
        int irow = 0;
        for ( Iterator rowIt = indexSets.iterator(); rowIt.hasNext(); ) {
            long[] row = (long[]) rowIt.next();
            for ( int itab = 0; itab < nTab; itab++ ) {
                rowIndices[ itab ][ irow ] = row[ itab ];
            }
            irow++;
        }
        assert irow == nrow;

        /* Create the subtables representing the parts got from each of 
         * the base tables. */
        StarTable[] parts = new StarTable[ nTab ];
        for ( int itab = 0; itab < nTab; itab++ ) {
            parts[ itab ] = new RowPermutedStarTable( tables[ itab ],
                                                      rowIndices[ itab ] );
        }

        /* Slap these next to each other to create the final joined table. */
        JoinStarTable joined = new JoinStarTable( parts );
        StringBuffer name = new StringBuffer( "Join of " );
        for ( int itab = 0; itab < nTab; itab++ ) {
            if ( itab > 0 ) {
                name.append( ", " );
            }
            name.append( tables[ itab ] );
        }
        joined.setName( name.toString() );
        return joined;
    }

    /**
     * Analyses a MatchSet to find linked rows of a given table.
     * The result of this method is a two-column table whose rows 
     * correspond one-to-one with the rows of the input <tt>table</tt>.
     * The output columns are defined by the constants 
     * {@link #GRP_ID_INFO} and {@link #GRP_SIZE_INFO}.
     * Rows of <tt>table</tt> linked together
     * by <tt>rowLinks</tt> are assigned the same integer value in
     * the new GRP_ID_INFO column, and the GRP_SIZE_INFO column
     * indicates how many rows are linked together in this way.
     * Each group corresponds to a single RowLink; if a row is part of
     * more than one RowLink then only one of them will be recorded
     * in the new columns.  For this reason is is a good idea to
     * call {@link MatchSet#agglomerateLinks} before this method.
     * Any rows linked in <tt>rowLinks</tt> which do not refer to 
     * <tt>table</tt> are just ignored.
     *
     * @param   table  the table in which internal matches are to be sought
     * @param   rowLinks  a MatchSet linking groups of rows together
     * @return  a new two-column table with a one-to-one row correspondance
     *          with <tt>table</tt> describing internal row matches
     */
    public static StarTable makeInternalMatchTable( final StarTable table,
                                                    MatchSet rowLinks ) {

        /* Find the largest row number represented in the match set. */
        long mrow = 0L;
        for ( Iterator linkIt = rowLinks.iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            for ( Iterator rowIt = link.getRowRefs().iterator();
                  rowIt.hasNext(); ) {
                RowRef rref = (RowRef) rowIt.next();
                if ( rref.getTable() == table ) {
                    mrow = Math.max( mrow, rref.getRowIndex() );
                }
            }
        }
        if ( mrow > Integer.MAX_VALUE ) {
            throw new RuntimeException( "Sorry, too many rows (" + mrow +
                                        " > " + Integer.MAX_VALUE + ")" );
        }
        int maxRow = (int) mrow;

        /* Construct and populate arrays containing per-row information
         * about internal matches. */
        final int[] grpIds = new int[ maxRow + 1 ];
        final int[] grpSizes = new int[ maxRow + 1 ];
        int grpId = 0;
        for ( Iterator linkIt = rowLinks.iterator(); linkIt.hasNext(); ) {
            RowLink link = (RowLink) linkIt.next();
            grpId++;
            for ( Iterator rowIt = link.getRowRefs().iterator();
                  rowIt.hasNext(); ) {
                RowRef rref = (RowRef) rowIt.next();
                if ( rref.getTable() == table ) {
                    int irow = (int) rref.getRowIndex();
                    grpIds[ irow ] = grpId;
                    grpSizes[ grpId ]++;
                }
            }
        }

        /* Create column data objects which can return the per-row
         * internal match information. */
        ColumnData grpIdColumn = new ColumnData( GRP_ID_INFO ) {
            public Object readValue( long lrow ) {
                int irow = (int) lrow;
                if ( irow >= grpIds.length ) {
                    return null;
                }
                else {
                    int grpId = grpIds[ irow ];
                    return grpId > 0 ? new Integer( grpId ) : null;
                }
            }
        };
        ColumnData grpSizeColumn = new ColumnData( GRP_SIZE_INFO ) {
            public Object readValue( long lrow ) {
                int irow = (int) lrow;
                if ( irow >= grpIds.length ) {
                    return null;
                }
                else {
                    int grpId = grpIds[ irow ];
                    return grpId > 0 ? new Integer( grpSizes[ grpId ] ) : null;
                }
            }
        };

        /* Construct and return a new table which incorporates the 
         * per-row internal match information. */
        ColumnStarTable grpTable = ColumnStarTable
                                  .makeTableWithRows( table.getRowCount() );
        grpTable.addColumn( grpIdColumn );
        grpTable.addColumn( grpSizeColumn );
        return grpTable;
    }
}
