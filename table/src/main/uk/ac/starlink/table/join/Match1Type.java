package uk.ac.starlink.table.join;

import java.util.BitSet;
import java.util.Iterator;
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
                return MatchStarTables
                      .makeParallelMatchTable( inTable, 0, rowLinks,
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
        for ( Iterator it = rowLinks.iterator(); it.hasNext(); ) {
            RowLink link = (RowLink) it.next();
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
}
