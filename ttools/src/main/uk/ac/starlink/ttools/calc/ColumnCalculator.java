package uk.ac.starlink.ttools.calc;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;

/**
 * Defines an object which can calculate new columns for a table given
 * a fixed set of input columns and some additional configuration.
 *
 * <p>This interface is suitable for algorithms which do not simply operate
 * row-by-row, that is they may need some or all rows of an input table
 * in order to generate the output table.
 * In typical usage it may take a significant amount of time to complete;
 * it will not normally be appropriate to invoke the same calculation on
 * the same table more than once (for instance, to generate virtual data
 * for table cells).
 *
 * @author   Mark Taylor
 * @since    14 Oct 2011
 */
public interface ColumnCalculator<S> {

    /**
     * Returns an array describing the columns of the input table.
     *
     * @return  one info for each column in the tuple table
     */
    ValueInfo[] getTupleInfos();

    /**
     * Performs the calculation.
     * Rows are written to the output sink based on the input table and
     * configuration contained in the <code>spec</code> object.
     * The output table must have the same number of rows as the input table,
     * and will not normally contain any of the same columns.
     *
     * @param  spec  specification object providing additional instructions
     *               about the calculation to be performed
     * @param  tupleTable  input table, with one column for each tuple element
     * @param  sink  sink to which the output table is written
     */
    void calculateColumns( S spec, StarTable tupleTable, TableSink sink )
            throws IOException;
}
