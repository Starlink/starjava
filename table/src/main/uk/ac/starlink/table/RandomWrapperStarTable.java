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
 * rows it contains (<tt>getRowCount()&lt;0</tt>), 
 * in which a <tt>getRowCount</tt> on this table will force all the rows
 * to be read straight away to count them (a random-access
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

    private long rowsStored = 0L;
    private ColumnInfo[] colinfos;
    private RowSequence baseSeq;
    private IOException savedError;

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

    public long getRowCount() {

        /* If we've read all the rows, we know how many there are. */
        if ( baseSeq == null ) {
            return rowsStored;
        }

        /* Otherwise, see if the base table knows. */
        long nr = super.getRowCount();
        if ( nr >= 0 ) {
            return nr;
        }

        /* Otherwise, we have no choice but to count all the remaining
         * rows by reading them in.  A random table must supply its row
         * count on demand. 
         * Unfortunately, this method doesn't throw an exception, so we
         * have to store any exception for later.
         * It looks like it would be simpler to do this in the constructor, 
         * but that's no good, since it would get done before subclasses
         * did their construction phases, and we shouldn't call the abstract
         * storeNextRow method before the constructor has completed. */
        try {
            synchronized ( this ) {
                while ( baseSeq.next() ) {
                    storeNextRow( baseSeq.getRow() );
                    rowsStored++;
                }
                baseSeq.close();
                baseSeq = null;
            }
        }
        catch ( IOException e ) {
            savedError = e;
        }
        return rowsStored;
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

    public Object[] getRow( long lrow ) throws IOException {
        checkSavedError();

        /* If we haven't got this far in the base table yet, read rows
         * from it until we have. */
        synchronized ( this ) {
            while ( lrow >= rowsStored ) {
                if ( baseSeq != null ) {
                    if ( baseSeq.next() ) {
                        storeNextRow( baseSeq.getRow() );
                        rowsStored++;
                    }
                    else {
                        baseSeq.close();
                        baseSeq = null;
                    }
                }
                else {
                    throw new IllegalArgumentException( 
                        "Attempted read beyond end of table" );
                }
            }
        }

        /* Return the row that we have now definitely read from our internal
         * row store. */
        assert lrow < rowsStored;
        return retrieveStoredRow( lrow );
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        checkSavedError();
        return getRow( lrow )[ icol ];
    }

    /**
     * Returns a <tt>RowSequence</tt> object based on the random data
     * access methods of this table.
     *
     * @return  a row iterator
     */
    public RowSequence getRowSequence() throws IOException {
        checkSavedError();
        return new RandomRowSequence( this );
    }

    /**
     * Arranges to rethrow an exception which we deferred from an earlier
     * operation.
     */
    private void checkSavedError() throws IOException {
        if ( savedError != null ) {
            IOException e = savedError;
            savedError = null;
            throw e;
        }
    }
}
