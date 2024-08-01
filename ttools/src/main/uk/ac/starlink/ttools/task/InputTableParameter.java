package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.util.DataSource;

/**
 * Parameter for specifying a single input table.
 *
 * @author   Mark Taylor
 */
public class InputTableParameter
             extends AbstractInputTableParameter<StarTable> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    @SuppressWarnings("this-escape")
    public InputTableParameter( String name ) {
        super( name, StarTable.class );
        setUsage( "<table>" );
    }

    /**
     * Returns the input table which has been selected by this parameter.
     *
     * @param  env  execution environment
     */
    public StarTable stringToObject( Environment env, String sval )
            throws TaskException {
        return makeTable( env, sval );
    }

    public String objectToString( Environment env, StarTable table ) {

        /* This return value may be used in cases where the table is
         * specified as a StarTable object rather than a string
         * (e.g. JyStilts or other programmatic use of a MapEnvironment)
         * to distinguish tables with different content.
         * It must return a different value for tables that have 
         * different content.  Ideally it will return the same value for
         * tables with the same content too, but that's not essential.
         * See ConsumerTask.IdentifiedStarTable.
         * The implementation used here, just calling toString, is OK
         * as long as toString is not overridden to return something that
         * might be similar for tables with different content.
         * It's hard to be sure that won't happen, but unfortunately
         * using a more definitely unique value here can cause other
         * problems, e.g. with the loc parameter of CatMapper
         * (try it and run the unit tests). */
        return table.toString();
    }

    public StarTable tableValue( Environment env ) throws TaskException {
        return objectValue( env );
    }
}
