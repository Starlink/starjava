package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;

/**
 * <tt>StarTable</tt> implementation based on a JDBC <tt>ResultSet</tt>.
 * <p>
 * Most of the <tt>ResultSet</tt> access methods are declared to 
 * throw an SQL exception even when they are doing something 
 * unobjectionable like returning the index of the current row.
 * This presents a problem in implementing the corresponding 
 * <tt>StarTable</tt> methods which are not declared to throw 
 * exceptions.  The approach taken here is to hang on to any 
 * SQL exceptions thrown in such methods and throw them the next
 * time a method declared by StarTable to throw an exception 
 * is called.  In this case a spurious (made-up) return value 
 * may be got from the method which wanted to throw but couldn't.
 * Such spurious returns are generally values intended to provoke
 * the user to invoke a method which can throw the exception at a
 * later date.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ResultSetStarTable extends AbstractStarTable {

    private ResultSet rset;
    private ResultSetMetaData rsmeta;
    private int ncol;
    private ColumnInfo[] colinfo;
    private boolean isRandom;
    private IOException pendingException;

    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    /* Auxiliary metadata. */
    private final static ValueInfo labelInfo = 
        new DefaultValueInfo( "Label", String.class );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        labelInfo,
    } );

    /**
     * Constructs a <tt>StarTable</tt> based on a given <tt>ResultSet</tt>.
     *
     * @param  rset  the ResultSet
     * @throws SQLException  if there is an SQL error
     */
    public ResultSetStarTable( ResultSet rset ) throws SQLException {

        /* Store the result set and its metadata. */
        this.rset = rset;
        rsmeta = rset.getMetaData();
        ncol = rsmeta.getColumnCount();

        /* See if we have random access. */
        switch ( rset.getType() ) {
            case ResultSet.TYPE_SCROLL_INSENSITIVE:
            case ResultSet.TYPE_SCROLL_SENSITIVE:
                isRandom = true;
                break;
            case ResultSet.TYPE_FORWARD_ONLY:
                isRandom = false;
                break;
            default:
                logger.warning( "Unknown ResultSet type " + rset.getType() );
        }

        /* Set up the column metadata. */
        colinfo = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {

            /* SQL columns are based at 1 not 0. */
            int jcol = icol + 1;

            /* Set up the name and metadata for this column. */
            String name = rsmeta.getColumnName( jcol );
            colinfo[ icol ] = new ColumnInfo( name );
            ColumnInfo col = colinfo[ icol ];

            /* Find out what class objects will have.  If the class hasn't
             * been loaded yet, just call it an Object (could try obtaining
             * an object from that column and using its class, but then it
             * might be null...). */
            try {
                Class ccls = Class.forName( rsmeta.getColumnClassName( jcol ) );
                col.setContentClass( ccls );
            }
            catch ( ClassNotFoundException e ) {
                col.setContentClass( Object.class );
            }
            if ( rsmeta.isNullable( jcol ) == 
                 ResultSetMetaData.columnNoNulls ) {
                col.setNullable( false );
            }
            List auxdata = col.getAuxData();
            String label = rsmeta.getColumnLabel( jcol );
            if ( label != null && 
                 label.trim().length() > 0 &&
                 ! label.equalsIgnoreCase( name ) ) {
                auxdata.add( new DescribedValue( labelInfo, label.trim() ) );
            }
        }
    }

    public boolean isRandom() {
        return isRandom;
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colinfo[ icol ];
    }

    public int getColumnCount() {
        return ncol;
    }

    /**
     * Returns the current row by moving the cursor to the end of the 
     * result set and seeing what row number it has.  
     * Returns the number of rows currently in this table.
     * Because of limitations in the <tt>ResultSet</tt> interface it is
     * impossible to do this without moving the cursor around; if the
     * table is undergoing concurrent modification by another process it
     * is possible that invoking this method might lose track of 
     * the position in the table.
     *
     * @return  the number of rows currently in the result set
     */
    public long getRowCount() {

        /* Without random access, we have no idea. */
        if ( ! isRandom ) {
            return -1L;
        }

        /* Don't even try if we are in an error state. */
        if ( hasPending() ) {
            return 1L;
        }

        /* Look for the end of the table. */
        try {
            int cur = rset.getRow();
            rset.afterLast();
            int nrow = rset.getRow();
            rset.absolute( cur );
            return (long) nrow;
        }

        /* On failure, save the exception and return a dummy value. */
        catch ( SQLException e ) {
            setPending( "Error locating table size", e );
            return 1L;
        }
    }

    public boolean hasNext() {

        /* Don't even try if we are in an error state. */
        if ( hasPending() ) {
            return true;
        }

        /* Find out if we are at the end. */
        try {
            return rset.isLast();
        }

        /* On failure, save the exception and return a dummy value. */
        catch ( SQLException e ) {
            setPending( "Error moving to next row", e );
            return true;
        }
    }

    public void setCurrent( long lrow ) throws IOException {
        flushPending();
        try {
            rset.absolute( checkedLongToInt( lrow ) );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( "Error setting current row" )
                               .initCause( e );
        }
    }

    public long getCurrent() {

        /* Don't even try if we are in error state. */
        if ( hasPending() ) {
            return 0L;
        }

        /* Find the row. */
        try {
            return (long) rset.getRow();
        }

        /* On failure, save the exception and return a dummy value. */
        catch ( SQLException e ) {
            setPending( "Error locating row", e );
            return 0L;
        }
    }

    public void next() throws IOException {
        flushPending();
        try {
            rset.next();
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( "Error moving to next row" )
                               .initCause( e );
        }
    }

    public void advanceCurrent( long offset ) throws IOException {
        flushPending();
        try {
            rset.relative( checkedLongToInt( offset ) );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( "Error advancing row" )
                               .initCause( e );
        }
    }

    public Object getCell( int icol ) throws IOException {
        flushPending();
        try {
            return rset.getObject( icol + 1 );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException().initCause( e );
        }
    }

    public Object getCell( long irow, int icol ) throws IOException {
        setCurrent( irow );
        return getCell( icol );
    }

    public Object[] getRow() throws IOException {
        Object[] rowData = new Object[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            rowData[ icol ] = getCell( icol );
        }
        return rowData;
    }

    public Object[] getRow( long lrow ) throws IOException {
        setCurrent( lrow );
        return getRow();
    }

    /**
     * Stashes an exception so that it can (hopefully) be thrown by a 
     * suqsequent call of {@link #flushPending}.
     *
     * @param  message  a message to go with the exception
     * @param  e     the SQLException to stash
     */
    private void setPending( String message, SQLException e ) {
        logger.warning( "Deferred SQLException in ResultSetStarTable " + this 
                      + ": " + e );
        pendingException = (IOException) 
                           new IOException( message ).initCause( e );
    }

    /**
     * Indicates whether this object is in an error state.
     * It is in an error state if no call to {@link #flushPending} has
     * occurred since the last call to {@link #setPending}.
     *
     * @return   <tt>true</tt> iff there is a pending exception waiting
     *           to be thrown
     */
    private boolean hasPending() {
        return pendingException != null;
    }

    /**
     * Throws any pending exception.
     * The first time this method is called following a call to 
     * {@link #setPending} it will throw an <tt>IOException</tt> based
     * on the stashed exception.  It will also reset the error status.
     * At other times it has no effect.
     * 
     * @throws   IOException  if this object is in an error state
     */
    private void flushPending() throws IOException {
        if ( pendingException != null ) {
            IOException pender = pendingException;
            pendingException = null;
            throw pender;
        }
    }

}
