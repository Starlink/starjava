package uk.ac.starlink.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    public JoinStarTable( StarTable[] tables, FixAction[] fixCols ) {
        nTab = tables.length;
        this.tables = (StarTable[]) tables.clone();
        if ( fixCols == null ) {
            fixCols = new FixAction[ nTab ];
            Arrays.fill( fixCols, FixAction.NO_ACTION );
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

        /* Set up the column as copies of those from the base tables.
         * Keep a record of which columns have duplicate names. */
        colInfos = new ColumnInfo[ nCol ];
        Set colNames = new HashSet();
        Set colDups = new HashSet();
        icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                colInfos[ icol ] =
                    new ColumnInfo( tables[ itab ].getColumnInfo( ic ) ); 
                String name = colInfos[ icol ].getName();
                ( colNames.contains( name ) ? colDups : colNames ).add( name );
                icol++;
            } 
        }
        assert icol == nCol;

        /* Fix any duplicated names if required. */
        icol = 0;
        for ( int itab = 0; itab < nTab; itab++ ) {
            for ( int ic = 0; ic < nCols[ itab ]; ic++ ) {
                String name = colInfos[ icol ].getName();
                boolean isDup = colDups.contains( name );
                colInfos[ icol ].setName( fixCols[ itab ]
                                         .getFixedName( name, isDup ) );
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
        long index = -1L;

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

        public void next() throws IOException {
            for ( int itab = 0; itab < nTab; itab++ ) {
                rseqs[ itab ].next();
            }
            index++;
        }

        public boolean hasNext() {
            for ( int itab = 0; itab < nTab; itab++ ) {
                if ( ! rseqs[ itab ].hasNext() ) {
                    return false;
                }
            }
            return true;
        }

        public void advance( long nrows ) throws IOException {
            for ( int itab = 0; itab < nTab; itab++ ) {
                rseqs[ itab ].advance( nrows );
            }
            index += nrows;
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

        public long getRowIndex() {
            return index;
        }

        public void close() throws IOException {
            for ( int itab = 0; itab < nTab; itab++ ) {
                rseqs[ itab ].close();
            }
        }
    }

    /**
     * Class defining the possible actions for doctoring
     * column names when joining tables.  
     * Joining tables can cause confusion if columns with the same names
     * exist in some of them.  An instance of this class defines 
     * how the join should behave in this case.
     */
    public static class FixAction {

        /** Column names should be left alone. */
        public static final FixAction NO_ACTION = 
            new FixAction( "No Action", null, false, false );

        private final String name;
        private final String appendage;
        private final boolean renameDup;
        private final boolean renameAll;

        /**
         * Private constructor.
         */
        private FixAction( String name, String appendage,
                           boolean renameDup, boolean renameAll ) {
            this.name = name;
            this.appendage = appendage;
            this.renameDup = renameDup;
            this.renameAll = renameAll;
        }

        /**
         * Returns an action indicating that column names which would be 
         * duplicated elsewhere in the result table should be modified
         * by appending a given string.
         *
         * @param  appendage  string to append to duplicate columns
         */
        public static FixAction makeRenameDuplicatesAction( String appendage ) {
            return new FixAction( "Fix Duplicates: " + appendage, appendage, 
                                  true, false );
        }

        /**
         * Returns an action indicating that all column names should be
         * modified by appending a given string.
         *
         * @param  appendage  string to append to columns
         */
        public static FixAction makeRenameAllAction( String appendage ) {
            return new FixAction( "Fix All: " + appendage, appendage,
                                  true, true );
        }

        /**
         * Returns the, possibly modified, name of a column.
         *
         * @param  origName  unmodified column name
         * @param  isDup  whether the column name would be duplicated
         *         in the set of unmodified names
         */
        private String getFixedName( String origName, boolean isDup ) {
            return renameAll || ( renameDup && isDup ) ? origName + appendage 
                                                       : origName;
        }

        public String toString() {
            return name;
        }
    }
}
