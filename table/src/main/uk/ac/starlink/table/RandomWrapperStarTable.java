package uk.ac.starlink.table;

import java.io.IOException;
import java.net.URL;

/**
 * Abstract wrapper table which can provide a random-access view of a
 * non-random base table.  The general strategy is to read through the
 * table only once (one RowSequence is used), storing each row when it
 * is read.  A row is not read until it is requested by a client of
 * this class, so if the latter rows of the resulting table are never
 * requested, they never need to be read from the base table.
 * The exception to this is if the base table does not know how many
 * rows it contains (getRowCount()&lt;0), in which case the whole table
 * must be read straight away to find out this information (a random-access
 * table must always know how many rows it has).
 * <p>
 * Changes in the number of columns of the base table will not be
 * reflected in this table.  Changes in the data elements may or
 * may not be reflected.
 * <p>
 * Concrete subclasses have to provide implementations of the 
 * {@link #storeNextRow} and {@link #retrieveStoredRow} methods.
 *
 * @author   Mark Taylor (Starlnk)
 */
public abstract class RandomWrapperStarTable extends WrapperStarTable {

    private long rowCount = -1L;
    private long rowsStored = 0L;
    private ColumnInfo[] colinfos;
    private RowSequence baseSeq;

    /**
     * Constructs a new random access table from a base table.
     *
     * @param  baseTable  the base StarTable
     */
    public RandomWrapperStarTable( StarTable baseTable ) throws IOException {

        /* Superclass constructor. */
        super( baseTable );

        /* Store the column infos - additions will not be reflected here. */
        colinfos = Tables.getColumnInfos( baseTable );

        /* Take out the sole RowSequence on the base which will supply all
         * the data sequentially. */
        baseSeq = baseTable.getRowSequence();
    }

    /**
     * Stores the next row encountered in the base table's row sequence.
     * This will be called up to <tt>getRowCount</tt> times with the
     * contents of each row of the base table in sequence.
     * Implementations should store it in some way that it can be 
     * retrieved by {@link #retrieveStoredRow}.
     *
     * @param  row   the row to store
     */
    protected abstract void storeNextRow( Object[] row );

    /**
     * Retrieves the row stored by the <tt>lrow</tt>'th invocation of
     * {@link #storeNextRow}.  This method will not be called with a 
     * value of <tt>lrow</tt> greater than or equal to the number of times 
     * <tt>storeNextRow</tt> has been called already.
     *
     * @param  lrow  the index of the row to retrieve
     * @return  the <tt>lrow</tt>'th row to be stored by {@link #storeNextRow}
     */
    protected abstract Object[] retrieveStoredRow( long lrow );

    /**
     * Returns true.
     *
     * @return  true
     */
    public boolean isRandom() {
        return true;
    }

    /**
     * Returns the number of columns.  This is fixed for the life of this table.
     *
     * @return  number of columns
     */
    public int getColumnCount() {
        return colinfos.length;
    }

    /**
     * Returns the URL of the base table.  Unlike most WrapperStarTables,
     * this is a reasonable thing to do, since although this isn't identical
     * to the base table, its data and metadata are identical, it's only
     * the mode of access which is different.
     *
     * @return  URL of the base table
     */
    public URL getURL() {
        return baseTable.getURL();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colinfos[ icol ];
    }

    public synchronized Object[] getRow( long lrow ) throws IOException {

        /* If we haven't got this far in the base table yet, read rows
         * from it until we have. */
        while ( lrow >= rowsStored ) {
            baseSeq.next();
            storeNextRow( baseSeq.getRow() );
            rowsStored++;
        }

        /* Return the row that we have not definitely read from our internal
         * row store. */
        assert lrow < rowsStored;
        return retrieveStoredRow( lrow );
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        return getRow( lrow )[ icol ];
    }

    /**
     * Returns a <tt>RowSequence</tt> object based on the random data
     * access methods of this table.
     *
     * @return  a row iterator
     */
    public RowSequence getRowSequence() {
        return new RandomRowSequence( this );
    }

    /**
     * Utility function for determining the number of rows in the base
     * table.  If there are more than Integer.MAX_VALUE then an
     * IllegalArgumentException will be thrown.  This is intended to
     * be invoked in the constructor by those subclasses which can
     * only cope with a feasible number of rows.
     * <p>
     * If the base table does not know how many rows it contains, 
     * this method will be forced to read the whole table (calling
     * {@link #storeNextRow} appropriately) to find out.
     *
     * @param  the number of rows that the base table has.  This will be
     *         non-negative
     */
    protected synchronized int getCheckedRowCount() throws IOException {
        long nrow = rowCount >= 0 ? rowCount : baseTable.getRowCount();
        if ( nrow > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Table " + baseTable + " has too many rows (" +
                nrow + " > Integer.MAX_VALUE" );
        }

        /* If we don't know the number of rows, we have to load all the
         * data in now so we do. */
        else if ( nrow < 0 ) {
            long irow;
            for ( irow = 0; baseSeq.hasNext(); irow++ ) {
                baseSeq.next();
                storeNextRow( baseSeq.getRow() );
                rowsStored++;
            }
            if ( irow > Integer.MAX_VALUE ) {
                throw new IllegalArgumentException(
                    "Table " + baseTable + " has too many rows (" +
                    nrow + " > Integer.MAX_VALUE" );
            }
            nrow = irow;
        }

        assert nrow == (int) nrow;
        return (int) nrow;
    }
}
