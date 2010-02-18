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
public class InputTableParameter extends AbstractInputTableParameter {

    private StarTable table_;

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public InputTableParameter( String name ) {
        super( name );
        setUsage( "<table>" );
    }

    /**
     * Returns the input table which has been selected by this parameter.
     *
     * @param  env  execution environment
     */
    public StarTable tableValue( Environment env ) throws TaskException {
        checkGotValue( env );
        if ( table_ == null ) {
            table_ = makeTable( env, stringValue( env ) );
        }
        return table_;
    }

    /**
     * Sets the table value of this parameter directly.
     *
     * @param   table
     */
    public void setValueFromTable( StarTable table ) {
        table_ = table;
        String name = table.getName();
        if ( name == null ) {
            name = "unnamed table";
        }
        setStringValue( name );
        setGotValue( true );
    }
}
