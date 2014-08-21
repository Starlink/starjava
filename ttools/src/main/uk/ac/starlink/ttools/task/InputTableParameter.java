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
        String name = table.getName();
        return name == null ? "unnamed table" : name;
    }

    public StarTable tableValue( Environment env ) throws TaskException {
        return objectValue( env );
    }
}
