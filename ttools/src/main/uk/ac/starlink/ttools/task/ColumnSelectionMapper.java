package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Mapper which operates on a user-supplied selection of the columns of
 * the input table.
 *
 * @author   Mark Taylor
 * @since    9 May 2006
 */
public class ColumnSelectionMapper implements TableMapper {

    private final WordsParameter<String> colsParam_;

    /**
     * Constructor.
     */
    public ColumnSelectionMapper() {

        /* Parameter specifying the column IDs for the selection.
         * You might think it would be more appropriate to have JEL
         * expressions here, but in general we will want to know the
         * value metadata as well as the data, so don't do that.
         * Users will have to create new columns upstream if they want to
         * do algebraic manipulation. */
        colsParam_ = WordsParameter.createStringWordsParameter( "cols" );
        colsParam_.setUsage( "<col-id> ..." );
        colsParam_.setDescription( new String[] {
            "<p>Columns to use for this task.",
            "One or more <code>&lt;col-id&gt;</code> elements, ",
            "separated by spaces, should be given.",
            "Each one represents a column in the table, using either its",
            "name or index.",
            "</p>",
        } );
        colsParam_.setPrompt( "Space-separated list of input columns" );
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            colsParam_,
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {
        final String[] colids = colsParam_.wordsValue( env );
        return new TableMapping() {
            public StarTable mapTables( InputTableSpec[] inSpecs )
                    throws IOException, TaskException {
                StarTable in = inSpecs[ 0 ].getWrappedTable();
                ColumnIdentifier colIdent = new ColumnIdentifier( in );   
                int ncol = colids.length;
                int[] colMap = new int[ ncol ];
                for ( int icol = 0; icol < ncol; icol++ ) {
                    colMap[ icol ] = colIdent.getColumnIndex( colids[ icol ] );
                }
                return new ColumnPermutedStarTable( in, colMap );
            }
        };
    }

    /**
     * Returns the parameter which specifies the column IDs for use.
     *
     * @return  column list parameter
     */
    public WordsParameter<String> getColumnsParameter() {
        return colsParam_;
    }
}
