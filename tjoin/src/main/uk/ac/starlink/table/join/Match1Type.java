package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Defines how an output table is created from the results of an internal
 * (single-table) match operation.
 * This class contains several factory methods for generating sensible 
 * output tables from an internal match.  Others are possible.
 *
 * @author   Mark Taylor
 * @since    15 Nov 2007
 */
public abstract class Match1Type {

    /**
     * Generates an output table given an input table and the LinkSet object
     * which defines how its rows are related to each other by matching.
     *
     * @param  inTable  input table
     * @param  rowLinks  link set object giving the result of a 
     *                   single-table match
     */
    public abstract StarTable createMatchTable( StarTable inTable,
                                                LinkSet rowLinks );

    /**
     * Factory method returning a type object which identifies matched rows
     * by adding some additional columns to the input.  These flag which 
     * rows match which other ones and give a group size count.
     *
     * @return   new identification type
     */
    public static Match1Type createIdentifyType() {
        final JoinFixAction inFix = JoinFixAction
                                   .makeRenameDuplicatesAction( "_old" );
        final JoinFixAction grpFix = JoinFixAction.NO_ACTION;
        return new Match1Type() {
            public StarTable createMatchTable( StarTable inTable,
                                               LinkSet rowLinks ) {
                return createIdentifyMatch( inTable, rowLinks, inFix, grpFix );
            }
        };
    }

    /**
     * Factory method returning a type object which eliminates rows forming
     * part of the same match group.  All rows from a match group starting
     * at index <code>retainCount</code> are removed from the output table.
     * Thus <code>retainCount=0</code> removes any rows which participate 
     * in matches with other ones, and <code>retainCount=1</code> leaves just
     * one from any such group.
     *
     * @param  retainCount  number of items to retain from each match group
     */
    public static Match1Type
            createEliminateMatchesType( final int retainCount ) {
        return new Match1Type() {
            public StarTable createMatchTable( StarTable inTable,
                                               LinkSet rowLinks ) {
                return createDeduplicateMatch( inTable, rowLinks, retainCount );
            }
        };
    }

    /**
     * Factory method returning a type object which aligns match groups with
     * each other in the rows of a new wide table.
     * The output table has columns like <code>grpSize</code> versions of
     * the input table side by side, and where there are exactly 
     * <code>grpSize</code> matches in a group they form a row.
     * Rows which are not part of a <code>grpSize</code>-element match
     * do not appear in the output.
     *
     * @param  grpSize  size of group we are interested in
     */
    public static Match1Type createWideType( final int grpSize ) {
        final JoinFixAction[] fixActs = new JoinFixAction[ grpSize ];
        for ( int i = 0; i < grpSize; i++ ) {
            fixActs[ i ] = JoinFixAction
                          .makeRenameDuplicatesAction( "_" + ( i + 1 ) );
        }
        return new Match1Type() {
            public StarTable createMatchTable( StarTable inTable,
                                               LinkSet rowLinks ) {
                return makeParallelMatchTable( inTable, 0, rowLinks,
                                               grpSize, grpSize, grpSize,
                                               fixActs );
            }
        };
    }

    /**
     * Returns an output match table which identifies matched rows by values
     * in added columns.
     *
     * @param  inTable  input table
     * @param  rowLinks  link set object giving the result of a 
     *                   single-table match
     * @param  inFix     fix action for columns in input table
     * @param  grpFix    fix action for the added columns
     */
    private static StarTable createIdentifyMatch( StarTable inTable, 
                                                  LinkSet rowLinks,
                                                  JoinFixAction inFix,
                                                  JoinFixAction grpFix ) {
        long nrow = inTable.getRowCount();
        StarTable grpTable =
            MatchStarTables.makeInternalMatchTable( 0, rowLinks, nrow );
        return new JoinStarTable( new StarTable[] { inTable, grpTable, },
                                  new JoinFixAction[] { inFix, grpFix, } );
    }

    /**
     * Returns an output match table which eliminates some or all members
     * of each match group.
     *
     * @param  inTable  input table
     * @param  rowLinks  link set object giving the result of a 
     *                   single-table match
     * @param  retainCount  number of items to retain from each match group
     */
    private static StarTable createDeduplicateMatch( StarTable inTable,
                                                     LinkSet rowLinks,
                                                     int retainCount ) {
        int nrow = Tables.checkedLongToInt( inTable.getRowCount() );
        BitSet bits = new BitSet( nrow );
        bits.set( 0, nrow );
        for ( RowLink link : rowLinks ) {
            int nref = link.size();
            for ( int i = retainCount; i < nref; i++ ) {
                RowRef ref = link.getRef( i );
                if ( ref.getTableIndex() != 0 ) {
                    throw new IllegalArgumentException(
                        "Intra-table LinkSet has links from multiple tables" );
                }
                bits.clear( Tables.checkedLongToInt( ref.getRowIndex() ) );
            }
        }
        int nbit = bits.cardinality();
        long[] rowMap = new long[ nbit ];
        int i = 0;
        for ( int ipos = bits.nextSetBit( 0 ); ipos >= 0;
              ipos = bits.nextSetBit( ipos + 1 ) ) {
             rowMap[ i++ ] = (long) ipos;
        }
        assert i == nbit;
        return new RowPermutedStarTable( inTable, rowMap );
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
    private static StarTable makeParallelMatchTable( StarTable table,
                                                     int iTable,
                                                     LinkSet links, int width,
                                                     int minSize, int maxSize,
                                                     JoinFixAction[] fixActs ) {

        /* Get rid of any links which we won't be using. */
        for ( Iterator<RowLink> it = links.iterator(); it.hasNext(); ) {
            RowLink link = it.next();
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
        for ( RowLink link : links ) {
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

        /* For each sub table, work out if it has any blank rows
         * (missing entries from row link elements). */
        boolean[] hasBlankRows = new boolean[ width ];
        for ( int iw = 0; iw < width; iw++ ) {
            for ( int ir = 0; ir < nrow; ir++ ) {
                 hasBlankRows[ iw ] = hasBlankRows[ iw ]
                                  || rowIndices[ iw ][ ir ] < 0;
            }
        }

        /* Perform some additional adjustment of the columns in the
         * constituent tables: doctor the column names to reduce confusion,
         * and if there are blank rows ensure that the relevant columns
         * are marked nullable. */
        int ncol = table.getColumnCount();
        int xNcol = ncol * width;
        final ColumnInfo[] colinfos = new ColumnInfo[ xNcol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo cinfo = table.getColumnInfo( ic );
            for ( int iw = 0; iw < width; iw++ ) {
                ColumnInfo ci = new ColumnInfo( cinfo );
                ci.setName( ci.getName() + "_" + ( iw + 1 ) );
                if ( hasBlankRows[ iw ] ) {
                    ci.setNullable( true );
                }
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
