package uk.ac.starlink.table;

import java.io.IOException;

/**
 * Implements a StarTable based on random access.
 * The <tt>isRandom</tt> method always returns true, and the 
 * <tt>getRowSequence</tt> method is implemented using the table's
 * (abstract) <tt>getCell</tt> and <tt>getRow</tt> methods.
 * <p>
 * Implementations of this object must supply a non-negative return value
 * for <tt>getColumnCount</tt> method, because the <tt>RowSequence</tt>
 * method requires this knowledge.
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class RandomStarTable extends AbstractStarTable {

    /**
     * Returns true.
     *
     * @return  true
     */
    public boolean isRandom() {
        return true;
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
     * The number of rows in this table.  Implementations must supply
     * a non-negative return value.
     *
     * @return  the number of rows in the table
     */
    abstract public long getRowCount();

}
