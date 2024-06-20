package uk.ac.starlink.table;

import java.util.function.DoubleFunction;

/**
 * Single-column table whose column values are the values of a loop iterator
 * variable.
 * 
 * @author   Mark Taylor
 * @since    6 Nov 2013
 */
public class LoopStarTable extends ColumnStarTable {

    private final long nrow_;

    /**
     * Constructs a loop table from values like the initialisers of a for loop.
     * The <code>isInteger</code> parameter may be set True for an
     * integer column, False for a Double column, and null if the type is
     * to be determined from the input values (integer if all are integers).
     * Integer columns are 32-bit if the values permit, otherwise 64-bit.
     * 
     * @param  colName  name of the single column name in the table
     * @param  start    initial (row 0) value of variable
     * @param  end      value which variable will not exceed
     * @param  step     per-row increment of variable
     */
    public LoopStarTable( final String colName, final double start,
                          final double end, final double step,
                          Boolean isInteger ) {
        final boolean isInt = 
            isInteger == null
                ? start == (long) start && end == (long) end
                                        && step == (long) step
                : isInteger.booleanValue();
        String descrip = "Loop variable";
        final ColumnData colData;
        final long nrow;
        final DoubleFunction<?> typedValue;
        final Class<?> clazz;
        if ( isInt ) {
            nrow = ( (long) end - (long) start ) / (long) step;
            boolean is32bit =
                start == (int) start && end == (int) end && step == (int) step;
            if ( is32bit ) {
                clazz = Integer.class;
                typedValue = dval -> Integer.valueOf( (int) dval );
            }
            else {
                clazz = Long.class;
                typedValue = dval -> Long.valueOf( (long) dval );
            }
        }
        else {
            nrow = (long) Math.floor( ( end - start ) / step );
            clazz = Double.class;
            typedValue = dval -> Double.valueOf( dval );
        }
        nrow_ = Math.max( 0, nrow );
        addColumn( new ColumnData( new ColumnInfo( colName, clazz, descrip ) ) {
            public Object readValue( long irow ) {
                return typedValue.apply( start + irow * step );
            }
        } );
    }

    public long getRowCount() {
        return nrow_;
    }
}
