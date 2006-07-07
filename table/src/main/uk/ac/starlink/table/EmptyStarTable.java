package uk.ac.starlink.table;

/**
 * A wrapper table which has the same metadata as its base table, but no rows.
 *
 * @author   Mark Taylor (Starlink)
 * @since    5 Aug 2004
 */
public class EmptyStarTable extends WrapperStarTable {

    /**
     * Creates a new empty table with metadata taken from an existing one.
     *
     * @param  baseTable  base table
     */
    public EmptyStarTable( StarTable baseTable ) {
        super( baseTable );
    }

    /**
     * Creates a new empty table with no columns, no rows and no parameters.
     */
    public EmptyStarTable() {
        this( ColumnStarTable.makeTableWithRows( 0L ) );
    }

    public boolean isRandom() {
        return true;
    }

    public long getRowCount() {
        return 0L;
    }

    public Object getCell( long lrow, int icol ) {
        throw new IllegalArgumentException();
    }

    public Object[] getRow( long lrow ) {
        throw new IllegalArgumentException();
    }

    public RowSequence getRowSequence() {
        return new RandomRowSequence( this );
    }
}
