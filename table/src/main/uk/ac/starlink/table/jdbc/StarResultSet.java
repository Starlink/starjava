package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Wraps the {@link java.sql.ResultSet} class to provide the functions which
 * are required to provide {@link uk.ac.starlink.table.StarTable} 
 * functionality.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2007
 */
public class StarResultSet {

    private final ResultSet rset_;
    private final TypeMapper typeMapper_;
    private final ValueHandler[] valueHandlers_;
    private final ColumnInfo[] colInfos_;
    private final boolean isRandom_;
    private long nrow_ = -1L;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );


    /**
     * Constructs a StarResultSet with default type mapping behaviour.
     *
     * @param  rset  result set
     */
    public StarResultSet( ResultSet rset ) throws SQLException {
        this( rset, TypeMappers.STANDARD );
    }

    /**
     * Constructs a StarResultSet with given type mapping behaviour.
     *
     * @param   rset   result set
     * @param   typeMapper  handles conversion of JDBC types to cell types
     */
    public StarResultSet( ResultSet rset, TypeMapper typeMapper )
            throws SQLException {
        rset_ = rset;
        typeMapper_ = typeMapper;

        /* Assemble metadata for each column. */
        ResultSetMetaData meta = rset.getMetaData();
        int ncol = meta.getColumnCount();
        valueHandlers_ = new ValueHandler[ ncol ];
        colInfos_ = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            valueHandlers_[ icol ] =
                typeMapper.createValueHandler( meta, icol + 1 );
            colInfos_[ icol ] = valueHandlers_[ icol ].getColumnInfo();
        }

        /* Work out whether we have random access. */
        switch ( rset_.getType() ) {
            case ResultSet.TYPE_FORWARD_ONLY:
                isRandom_ = false;
                break;
            case ResultSet.TYPE_SCROLL_INSENSITIVE:
            case ResultSet.TYPE_SCROLL_SENSITIVE:
                isRandom_ = true;
                break;
            default:
                assert false : "Unknown ResultSet type";
                isRandom_ = false;
        }
    }

    /**
     * Returns the result set on which this table is based.
     *
     * @return  result set
     */
    public ResultSet getResultSet() {
        return rset_;
    }

    /**
     * Returns the array of column metadata objects corresponding to the
     * columns in this result set.
     *
     * @return   column info array (not a copy)
     */
    public ColumnInfo[] getColumnInfos() {
        return colInfos_;
    }

    /**
     * Indicates whether this result set can be used for random access.
     *
     * @return   true  iff random access is possible
     */
    public boolean isRandom() {
        return isRandom_;
    }

    /**
     * Lazily counts the number of rows in this result set, if it has random
     * access.  Otherwise, returns -1 (unknown), since a count may be 
     * very expensive.  If the count cannot be calculated for a random access
     * table for some reason, zero is returned and a warning is logged.
     *
     * @return   row count
     */
    public long getRowCount() {

        /* If we have random access, get the number of rows by going to the
         * end and finding what row number it is.  This may not be cheap,
         * but random access tables are obliged to know how many rows
         * they have.  Unfortunately this is capable of throwing an
         * SQLException, but this method is not declared to throw anything,
         * (since the methods it is called from will not be)
         * so deal with it best we can. */
        if ( isRandom_ ) {
            if ( nrow_ < 0 ) {
                try {
                    synchronized ( this ) {
                        rset_.afterLast();
                        rset_.previous();
                        nrow_ = rset_.getRow();
                    }
                }
                catch ( SQLException e ) {
                    logger_.warning( "Failed to get table length: " + e );
                    nrow_ = 0L;
                }
            }
            return nrow_;
        }
        else {
            return -1L;
        }
    }

    /**
     * Returns an ordered list of {@link uk.ac.starlink.table.ValueInfo}
     * objects representing the auxilliary metadata returned by
     * this object's ColumnInfo objects.
     *
     * @see   uk.ac.starlink.table.StarTable#getColumnAuxDataInfos
     * @return  an unmodifiable ordered set of known metadata keys
     */
    public List getColumnAuxDataInfos() {
        return typeMapper_.getColumnAuxDataInfos();
    }

    /**
     * Sets the row index from which subsequent {@link #getCell}
     * and {@link #getRow} calls will read.  
     * Callers may need to worry about synchronization.
     *
     * @param  lrow  row index (0-based)
     * @throws  UnsupportedOperationExcepion  for non-random result sets
     */
    public void setRowIndex( long lrow ) throws IOException {
        if ( isRandom_ ) {
            try {
                rset_.absolute( Tables.checkedLongToInt( lrow ) + 1 );
            }
            catch ( SQLException e ) {
                throw (IOException)
                      new IOException( "Error setting to row " + lrow )
                     .initCause( e );
            }
        }
        else {
            throw new UnsupportedOperationException( "No random access" );
        }
    }

    /**
     * Returns the object at a given column in the current row of this 
     * result set in a form suitable for use as the content of a 
     * StarTable cell.
     * Callers may need to worry about synchronization.
     *
     * @param  icol  the column to use (first column is 0)
     * @return   the cell value
     */
    public Object getCell( int icol ) throws IOException {
        Object base;
        try {
            base = rset_.getObject( icol + 1 );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( "SQL read error" + e )
                               .initCause( e );
        }
        return valueHandlers_[ icol ].getValue( base );
    }

    /**
     * Returns the current row of this result set in a form suitable for use
     * as the content of a StarTable.
     * Callers may need to worry about synchronization.
     *
     * @return   array of cell values in current row
     */
    public Object[] getRow() throws IOException {
        int ncol = valueHandlers_.length;
        Object[] row = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            row[ icol ] = getCell( icol );
        }
        return row;
    }

    /**
     * Returns a sequential RowSequence based on this object.
     * This assumes that the cursor is currently at the beginning of the
     * result set - no checking is performed here.
     *
     * @return   row sequence
     */
    public RowSequence createRowSequence() throws IOException {
        return new ResultSetRowSequence();
    }

    /**
     * Row sequence based on this result set.  Assumes that the cursor starts
     * off positioned at the top of the results, and that no other access
     * is being done concurrently on the data.
     */
    private class ResultSetRowSequence implements RowSequence {

        public boolean next() throws IOException {
            try {
                return rset_.next();
            }
            catch ( SQLException e  ) {
                throw (IOException) new IOException( e.getMessage() )
                     .initCause( e );
            }
        }

        public Object getCell( int icol ) throws IOException {
            checkHasCurrentRow();
            return StarResultSet.this.getCell( icol );
        }

        public Object[] getRow() throws IOException {
            checkHasCurrentRow();
            return StarResultSet.this.getRow();
        }

        public void close() throws IOException {
            try {
                rset_.close();
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        /**
         * Ensure that there is a current row.
         *
         * @throws NoSuchElementException  if there is no current row
         */
        private void checkHasCurrentRow() {
            try {
                if ( rset_.isBeforeFirst() ) {
                    throw new NoSuchElementException( "No current row" );
                }
            }

            /* SQL Server driver is known to fail here reporting an unsupported
             * operation when isBeforeFirst is called.  Just assume it's OK
             * in that case. */
            catch ( SQLException e ) {
            }
        }
    }
}
