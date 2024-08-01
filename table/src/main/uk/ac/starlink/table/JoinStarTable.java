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

    private final StarTable[] tables_;
    private final StarTable[] tablesByColumn_;
    private final int[] indicesByColumn_;
    private final int[] nCols_;
    private final int nTab_;
    private final int nCol_;
    private final boolean isRandom_;
    private final ColumnInfo[] colInfos_;
    private final List<ValueInfo> auxData_;

    /**
     * Constructs a new JoinStarTable from a list of constituent tables,
     * optionally renaming duplicated column names.
     *
     * @param  tables  array of constituent table objects providing the 
     *         data and metadata for a new joined table
     * @param  fixCols actions to be taken in modifying column names from
     *         the originals (may be null for no action)
     */
    @SuppressWarnings("this-escape")
    public JoinStarTable( StarTable[] tables, JoinFixAction[] fixCols ) {
        nTab_ = tables.length;
        tables_ = tables.clone();
        if ( fixCols == null ) {
            fixCols = new JoinFixAction[ nTab_ ];
            Arrays.fill( fixCols, JoinFixAction.NO_ACTION );
        }
        if ( fixCols.length != nTab_ ) {
            throw new IllegalArgumentException( 
                "Incompatible length of array arguments" );
        }

        /* Work out the total number of columns in this table (sum of all
         * constituent tables). */
        int nc = 0;
        nCols_ = new int[ nTab_ ];
        for ( int itab = 0; itab < nTab_; itab++ ) {
            nCols_[ itab ] = tables[ itab ].getColumnCount();
            nc += nCols_[ itab ];
        }
        nCol_ = nc;

        /* Set up lookup tables to find the base table and index within it
         * of each column in this table. */
        tablesByColumn_ = new StarTable[ nCol_ ];
        indicesByColumn_ = new int[ nCol_ ];
        int icol = 0;
        for ( int itab = 0; itab < nTab_; itab++ ) {
            for ( int ic = 0; ic < nCols_[ itab ]; ic++ ) {
                tablesByColumn_[ icol ] = tables[ itab ];
                indicesByColumn_[ icol ] = ic;
                icol++;
            }
        }
        assert icol == nCol_;

        /* Set up the column infos as copies of those from the base tables. */
        colInfos_ = new ColumnInfo[ nCol_ ];
        List<String> nameList = new ArrayList<String>();
        icol = 0;
        for ( int itab = 0; itab < nTab_; itab++ ) {
            for ( int ic = 0; ic < nCols_[ itab ]; ic++ ) {
                colInfos_[ icol ] =
                    new ColumnInfo( tables[ itab ].getColumnInfo( ic ) );
                nameList.add( colInfos_[ icol ].getName() );
                icol++;
            }
        }
        assert icol == nCol_;

        /* Deduplicate column names as required. */
        icol = 0;
        for ( int itab = 0; itab < nTab_; itab++ ) {
            for ( int ic = 0; ic < nCols_[ itab ]; ic++ ) {
                String origName = nameList.remove( icol );
                assert origName.equals( colInfos_[ icol ].getName() );
                String name = fixCols[ itab ]
                             .getFixedName( origName, nameList );
                nameList.add( icol, origName );
                nameList.add( name );
                colInfos_[ icol ].setName( name );
                icol++;
            }
        }
        assert icol == nCol_;

        /* Store auxiliary metadata. */
        Set<ValueInfo> auxInfos = new LinkedHashSet<ValueInfo>();
        for ( StarTable table : tables ) {
            auxInfos.addAll( table.getColumnAuxDataInfos() );
        }
        auxData_ = new ArrayList<ValueInfo>( auxInfos );

        /* Store the parameters as the ordered union of all the parameters
         * of the constituent tables. */
        Set<DescribedValue> params = new LinkedHashSet<DescribedValue>();
        for ( StarTable table : tables ) {
            params.addAll( table.getParameters() );
        }
        setParameters( new ArrayList<DescribedValue>( params ) );

        /* Check random access. */
        boolean rand = true;
        for ( StarTable table : tables ) {
            rand = rand && table.isRandom();
        }
        isRandom_ = rand;
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
    public List<StarTable> getTables() {
        return Collections.unmodifiableList( Arrays.asList( tables_ ) );
    }

    public int getColumnCount() {
        return nCol_;
    }

    public long getRowCount() {
        if ( nTab_ == 0 ) {
            return 0L;
        }
        else {
            long nrow = Long.MAX_VALUE;
            for ( StarTable table : tables_ ) {
                nrow = Math.min( nrow, table.getRowCount() );
            }
            return nrow;
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public boolean isRandom() {
        return isRandom_;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        return tablesByColumn_[ icol ]
              .getCell( irow, indicesByColumn_[ icol ] );
    }

    public Object[] getRow( long irow ) throws IOException {
        if ( ! isRandom() ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        Object[] row = new Object[ nCol_ ];
        int icol = 0;
        for ( int itab = 0; itab < nTab_; itab++ ) {
            Object[] subrow = tables_[ itab ].getRow( irow );
            System.arraycopy( subrow, 0, row, icol, nCols_[ itab ] );
            icol += nCols_[ itab ];
        }
        assert icol == nCol_;
        return row;
    }

    public RowSequence getRowSequence() throws IOException {
        return new JoinRowSequence();
    }

    public RowAccess getRowAccess() throws IOException {
        return new JoinRowAccess();
    }

    /**
     * Closes all the constituent tables.
     */
    @Override
    public void close() throws IOException {
        for ( StarTable table : tables_ ) {
            table.close();
        }
    }

    /**
     * Helper class providing the row sequence implementation used by
     * a JoinStarTable.
     */
    private class JoinRowSequence implements RowSequence {

        final RowSequence[] rseqs_;
        final RowSequence[] rseqsByColumn_;

        JoinRowSequence() throws IOException {
            rseqs_ = new RowSequence[ nTab_ ];
            rseqsByColumn_ = new RowSequence[ nCol_ ];
            int icol = 0;
            for ( int itab = 0; itab < nTab_; itab++ ) {
                rseqs_[ itab ] = tables_[ itab ].getRowSequence();
                for ( int ic = 0; ic < nCols_[ itab ]; ic++ ) {
                    rseqsByColumn_[ icol ] = rseqs_[ itab ];
                    assert indicesByColumn_[ icol ] == ic;
                    icol++;
                }
            }
            assert icol == nCol_;
        }

        public boolean next() throws IOException {
            for ( RowSequence rseq : rseqs_ ) {
                if ( ! rseq.next() ) {
                    return false;
                }
            }
            return true;
        }

        public Object getCell( int icol ) throws IOException {
            return rseqsByColumn_[ icol ].getCell( indicesByColumn_[ icol ] );
        }

        public Object[] getRow() throws IOException {
            Object[] row = new Object[ nCol_ ];
            int icol = 0;
            for ( int itab = 0; itab < nTab_; itab++ ) {
                Object[] subrow = rseqs_[ itab ].getRow();
                System.arraycopy( subrow, 0, row, icol, nCols_[ itab ] );
                icol += nCols_[ itab ];
            }
            assert icol == nCol_;
            return row;
        }

        public void close() throws IOException {
            for ( RowSequence rseq : rseqs_ ) {
                rseq.close();
            }
        }
    }

    /**
     * RowAccess implementation for use with JoinStarTable.
     */
    private class JoinRowAccess implements RowAccess {

        final RowAccess[] raccs_;
        final RowAccess[] raccsByColumn_;
        final Object[] row_;
        long irow_;

        JoinRowAccess() throws IOException {
            row_ = new Object[ nCol_ ];
            raccs_ = new RowAccess[ nTab_ ];
            raccsByColumn_ = new RowAccess[ nCol_ ];
            int icol = 0;
            for ( int itab = 0; itab < nTab_; itab++ ) {
                raccs_[ itab ] = tables_[ itab ].getRowAccess();
                for ( int ic = 0; ic < nCols_[ itab ]; ic++ ) {
                    raccsByColumn_[ icol ] = raccs_[ itab ];
                    assert indicesByColumn_[ icol ] == ic;
                    icol++;
                }
            }
            assert icol == nCol_;
        }

        public void setRowIndex( long irow ) throws IOException {
            for ( int itab = 0; itab < nTab_; itab++ ) {
                raccs_[ itab ].setRowIndex( irow );
            }
        }

        public Object getCell( int icol ) throws IOException {
            return raccsByColumn_[ icol ].getCell( indicesByColumn_[ icol ] );
        }

        public Object[] getRow() throws IOException {
            int icol = 0;
            for ( int itab = 0; itab < nTab_; itab++ ) {
                Object[] subrow = raccs_[ itab ].getRow();
                System.arraycopy( subrow, 0, row_, icol, nCols_[ itab ] );
                icol += nCols_[ itab ];
            }
            assert icol == nCol_;
            return row_;
        }

        public void close() throws IOException {
            for ( RowAccess racc : raccs_ ) {
                try {
                    racc.close();
                }
                catch ( IOException e ) {
                }
            }
        }
    }
}
