package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Joins a number of tables to produce a single combined table.
 * The result consists of all the constituent tables side by side,
 * so has a number of columns equal to the sum of the numbers of columns
 * in all the constituent tables.  
 * The n<sup>th</sup> row of this table is composed by appending the 
 * n<sup>th</sup> rows of all the constituent tables together in sequence.
 * The number of rows is equal to the
 * smallest of all the number of rows in the constituent tables 
 * (typically they will all have the same number of rows).
 * Random access is only available if it is available in all the 
 * constituent tables.
 * <p>
 * While this table is active, the columns that the constituent tables 
 * had at the time of its construction shouldn't change their characteristics
 * in incompatible ways or disappear.  It's OK to add new columns though.
 *
 * @author   Mark Taylor (Starlink)
 */
public class JoinStarTable extends AbstractStarTable {

    private final StarTable[] tables;
    private final StarTable[] tablesByColumn;
    private final int[] indicesByColumn;
    private final int[] nCols;
    private final int nTab;
    private final int nCol;
    private final boolean isRandom;
    private final ColumnInfo[] colInfos;
    private final List auxData;

    /**
     * Constructs a new JoinStarTable from a list of constituent tables,
     * optionally renaming duplicated column names.
     *
     * @param  tables  array of constituent table objects providing the 
     *         data and metadata for a new joined table
     * @param  fixCols actions to be taken in modifying column names from
     *         the originals (may be null for no action)
     */
    public JoinStarTable( StarTable[] tables, JoinFixAction[] fixCols ) {
        nTab = tables.length;
        this.tables = (StarTable[]) tables.clone();
        if ( fixCols == null ) {
            fixCols = new JoinFixAction[ nTab ];
            Arrays.fill( fixCols, JoinFixAction.NO_ACTION );
        }
        if ( fixCols.length != nTab ) {
            throw new IllegalArgumentException( 
                "Incompatible length of array arguments" );
        }

        /* Work out the total number of columns in this table (sum of all
         * constituent tables). */
        int nc = 0;
        nCols = new int[ nTab ];
        for ( int itab = 0; itab < nTab; itab++ ) {
            nCols[ itab ] = tables[ itab ].getColumnCount();
            nc += nCols[ itab ];
        }
        nCol = nc;

        /* Set up lookup tables to find the base table and index within it
         * of each column in this table. */
        tablesByColumn = new StarTable[ nCol ];
        indicesByColumn = new int[ nCol ];
        int icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                tablesByColumn[ icol ] = tables[ itab ];
                indicesByColumn[ icol ] = ic;
                icol++;
            }
        }
        assert icol == nCol;

        /* Set up the column infos as copies of those from the base tables. */
        colInfos = new ColumnInfo[ nCol ];
        List nameList = new ArrayList();
        icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                colInfos[ icol ] =
                    new ColumnInfo( tables[ itab ].getColumnInfo( ic ) );
                nameList.add( colInfos[ icol ].getName() );
                icol++;
            }
        }
        assert icol == nCol;

        /* Deduplicate column names as required. */
        icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                String origName = (String) nameList.remove( icol );
                assert origName.equals( colInfos[ icol ].getName() );
                String name = fixCols[ itab ]
                             .getFixedName( origName, nameList );
                nameList.add( icol, origName );
                nameList.add( name );
                colInfos[ icol ].setName( name );
                icol++;
            }
        }
        assert icol == nCol;

        /* Store auxiliary metadata. */
        Set auxInfos = new LinkedHashSet();
        for ( int itab = 0; itab < nTab; itab++ ) {
            auxInfos.addAll( tables[ itab ].getColumnAuxDataInfos() );
        }
        auxData = new ArrayList( auxInfos );

        /* Store the parameters as the ordered union of all the parameters
         * of the constituent tables. */
        Set params = new LinkedHashSet();
        for ( int itab = 0; itab < nTab; itab++ ) {
            params.addAll( tables[ itab ].getParameters() );
        }
        setParameters( new ArrayList( params ) );

        /* Check random access. */
        boolean rand = true;
        for ( int itab = 0; itab < nTab; itab++ ) {
            rand = rand && tables[ itab ].isRandom();
        }
        isRandom = rand;
    }

    /**
     * Constructs a new JoinStarTable from a list of constituent tables.
     * No column renaming is done.
     *
     * @param  tables  array of constituent table objects providing the 
     *         data and metadata for a new joined table
     */
    public JoinStarTable( StarTable[] tables ) {
        this( tables, null );
    }

    /**
     * Returns an unmodifiable list of the constituent tables providing the
     * base data for this join table.
     *
     * @return   list of tables
     */
    public List getTables() {
        return Collections.unmodifiableList( Arrays.asList( tables ) );
    }

    public int getColumnCount() {
        return nCol;
    }

    public long getRowCount() {
        if ( nTab == 0 ) {
            return 0L;
        }
        else {
            long nrow = Long.MAX_VALUE;
            for ( int itab = 0; itab < nTab; itab++ ) {
                nrow = Math.min( nrow, tables[ itab ].getRowCount() );
            }
            return nrow;
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos[ icol ];
    }

    public boolean isRandom() {
        return isRandom;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        return tablesByColumn[ icol ].getCell( irow, indicesByColumn[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        Object[] row = new Object[ nCol ];
        int icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            Object[] subrow = tables[ itab ].getRow( irow );
            System.arraycopy( subrow, 0, row, icol, nCols[ itab ] );
            icol += nCols[ itab ];
        }
        assert icol == nCol;
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        return new JoinRowSequence();
    }


    /**
     * Helper class providing the row sequence implementation used by
     * a JoinStarTable.
     */
    private class JoinRowSequence implements RowSequence {

        RowSequence[] rseqs;
        RowSequence[] rseqsByColumn;

        JoinRowSequence() throws IOException {
            rseqs = new RowSequence[ nTab ];
            rseqsByColumn = new RowSequence[ nCol ];
            int icol = 0;
            for ( int itab = 0; itab < nTab; itab++ ) {
                rseqs[ itab ] = tables[ itab ].getRowSequence();
                for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                    rseqsByColumn[ icol ] = rseqs[ itab ];
                    assert indicesByColumn[ icol ] == ic;
                    icol++;
                }
            }
            assert icol == nCol;
        }

        public boolean next() throws IOException {
            for ( int itab = 0; itab < nTab; itab++ ) {
                if ( ! rseqs[ itab ].next() ) {
                    return false;
                }
            }
            return true;
        }

        public Object getCell( int icol ) throws IOException {
            return rseqsByColumn[ icol ].getCell( indicesByColumn[ icol ] );
        }

        public Object[] getRow() throws IOException {
            Object[] row = new Object[ nCol ];
            int icol = 0;
            for ( int itab = 0; itab < nTab; itab++ ) {
                Object[] subrow = rseqs[ itab ].getRow();
                System.arraycopy( subrow, 0, row, icol, nCols[ itab ] );
                icol += nCols[ itab ];
            }
            assert icol == nCol;
            return row;
        }

        public void close() throws IOException {
            for ( int itab = 0; itab < nTab; itab++ ) {
                rseqs[ itab ].close();
            }
        }
    }
}
