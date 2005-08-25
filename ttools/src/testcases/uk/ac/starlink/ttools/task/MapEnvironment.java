package uk.ac.starlink.ttools.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Environment which allows use of ttools tasks from an in-memory context.
 * Input StarTables can be set as values of parameters and output ones
 * can be extracted.
 */
public class MapEnvironment extends TableEnvironment {

    private final Map paramMap_;
    private final Map outputTables_ = new HashMap();
    private final ByteArrayOutputStream out_ = new ByteArrayOutputStream();
    private final PrintStream pout_ = new PrintStream( out_ );

    /**
     * Constructs a new environment with no values.
     */
    public MapEnvironment() {
        this( new HashMap() );
    }

    /**
     * Constructs a new environment with a map of parameter name->value
     * pairs.
     *
     * @param  map   parameter map
     */
    public MapEnvironment( Map map ) {
        paramMap_ = map;
    }

    public PrintStream getPrintStream() {
        return pout_;
    }

    public void clearValue( Parameter param ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a string which contains all the output written by the task
     * so far.
     *
     * @return  output text
     */
    public String getOutputText() {
        pout_.flush();
        return new String( out_.toByteArray() );
    }

    /** 
     * Sets the value of a parameter.  A string value is OK; in some cases
     * other parameter types are catered for.
     * For convenience, this method returns this object (cf. StringBuffer).
     *
     * @param  paramName  name
     * @param  value   string or other value
     * @return  this
     */
    public MapEnvironment setValue( String paramName, Object value ) {
        paramMap_.put( paramName, value );
        return this;
    }

    /**
     * If the task which has been executed in this environment has created
     * an output table which has not been otherwise disposed of, you
     * can get it from here.
     *
     * @param  name of a TableConsumerParameter
     * @return   output table stored under <code>name</code>
     */
    public StarTable getOutputTable( String paramName ) {
        return (StarTable) outputTables_.get( paramName );
    }

    public void acquireValue( Parameter param ) throws TaskException {
        final String pname = param.getName();
        boolean isDefault = ! paramMap_.containsKey( pname );
        Object value = isDefault ? param.getDefault()
                                 : paramMap_.get( pname );
        if ( isDefault &&
             param instanceof TableConsumerParameter ) {
            ((TableConsumerParameter) param).setValueFromConsumer(
                new TableConsumer() {
                    public void consume( StarTable table ) {
                        outputTables_.put( pname, table );
                    }
                } );
        }
        else if ( value == null && param.isNullPermitted() ) {
            param.setValueFromString( this, null );
        }
        else if ( value == null ) {
            throw new IllegalArgumentException(
                "No value supplied for param " + param );
        }
        else if ( value instanceof StarTable &&
                  param instanceof InputTableParameter ) {
            ((InputTableParameter) param)
                                  .setValueFromTable( (StarTable) value );
        }
        else if ( value instanceof String ) {
            param.setValueFromString( this, (String) value );
        }
        else {
            throw new IllegalArgumentException( 
                "Can't fit value of type " + value.getClass() +
                " into parameter " + param );
        }
    }

}
