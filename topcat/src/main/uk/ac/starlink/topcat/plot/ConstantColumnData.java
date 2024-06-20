package uk.ac.starlink.topcat.plot;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;

/**
 * ColumnData implementation which always returns the same Double value.
 * The <code>equals</code> and <code>hashCode</code> methods are implemented
 * so that instances with the same values are equal to each other.
 *
 * @author   Mark Taylor
 * @since    1 Jun 2007
 */
public class ConstantColumnData extends ColumnData {

    private final Double value_;

    /** Instance with values of 0. */
    public static final ConstantColumnData ZERO =
        new ConstantColumnData( "Zero", 0.0 );

    /** Instance with values of 1. */
    public static final ConstantColumnData ONE =
        new ConstantColumnData( "One", 1.0 );

    /** Instance with values of Double.NaN. */
    public static final ConstantColumnData NAN =
        new ConstantColumnData( "NaN", Double.NaN );

    /**
     * Constructor.
     *
     * @param   name  column name
     * @param   value  constant column value
     */
    public ConstantColumnData( String name, double value ) {
        super( new ColumnInfo( name, Double.class,
                               "Constant value of " + value ) );
        value_ = Double.valueOf( value );
    }

    public Object readValue( long irow ) {
        return value_;
    }

    public boolean equals( Object other ) {
        if ( other instanceof ConstantColumnData ) {
            return this.value_.equals( ((ConstantColumnData) other).value_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return value_.hashCode();
    }
}
