package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;

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
     * Integer column, False for a Double column, and null if the type is
     * to be determined from the input values (integer if all are integers).
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
                ? start == (int) start && end == (int) end && step == (int) step
                : isInteger.booleanValue();
        String descrip = "Loop variable";
        final ColumnData colData;
        final long nrow;
        if ( isInt ) {
            nrow = ( (long) end - (long) start ) / (long) step;
            colData = new ColumnData( new ColumnInfo( colName, Integer.class,
                                                      descrip ) ) {
                public Object readValue( long irow ) {
                    return new Integer( (int) ( start + irow * step ) );
                }
            };
        }
        else {
            nrow = (long) Math.floor( ( end - start ) / step );
            colData = new ColumnData( new ColumnInfo( colName, Double.class,
                                                      descrip ) ) {
                public Object readValue( long irow ) {
                    return new Double( start + irow * step );
                }
            };
        }
        nrow_ = Math.max( 0, nrow );
        addColumn( colData );
    }

    public long getRowCount() {
        return nrow_;
    }
}
