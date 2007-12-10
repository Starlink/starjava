package uk.ac.starlink.ttools.jel;

import gnu.jel.CompiledExpression;
import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Implements JELRowReader for a random access table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Feb 2005
 */
public class RandomJELRowReader extends StarTableJELRowReader {

    private long lrow_ = -1L;
    private final StarTable table_;

    /**
     * Constructs a new row reader for a random-access table.
     *
     * @param   table  table object
     */
    public RandomJELRowReader( StarTable table ) {
        super( table );
        table_ = table;
    }

    /**
     * Returns the current row for evaluations.
     *
     * @return  current row
     */
    public long getCurrentRow() {
        return lrow_;
    }

    /**
     * Sets the current row for evaluations.
     *
     * @param  lrow  current row
     */
    public void setCurrentRow( long lrow ) {
        lrow_ = lrow;
    }

    /**
     * Evaluates a given compiled expression at a given row.
     * The returned value is wrapped up as an object if the result of
     * the expression is a primitive.
     *
     * @param  compEx  compiled expression
     */
    public synchronized Object evaluateAtRow( CompiledExpression compEx,
                                              long lrow ) throws Throwable {
        setCurrentRow( lrow );
        return evaluate( compEx );
    }

    /**
     * Returns the cell at a given column in the current row.
     *
     * @param  icol  column index
     * @return  cell at <tt>(getCurrentRow(),icol)</tt>
     */
    public Object getCell( int icol ) throws IOException {
        return table_.getCell( lrow_, icol );
    }
}
