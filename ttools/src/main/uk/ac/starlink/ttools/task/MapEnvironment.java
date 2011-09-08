package uk.ac.starlink.ttools.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Environment which allows use of ttools tasks from an in-memory context.
 * Input StarTables can be set as values of parameters and output ones
 * can be extracted.
 *
 * @author   Mark Taylor
 */
public class MapEnvironment implements TableEnvironment {

    private final Map paramMap_;
    private final Map outputTables_ = new HashMap();
    private final ByteArrayOutputStream out_ = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err_ = new ByteArrayOutputStream();
    private final PrintStream pout_ = new PrintStream( out_ );
    private final PrintStream perr_ = new PrintStream( err_ );
    private Class resourceBase_ = MapEnvironment.class;
    private boolean strictVot_;
    private boolean debug_;

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

    /**
     * Clone constructor.
     *
     * @param  env  environment to copy
     */
    public MapEnvironment( MapEnvironment env ) {
        this( new HashMap( env.paramMap_ ) );
        this.resourceBase_ = env.resourceBase_;
        this.debug_ = env.debug_;
        this.strictVot_ = env.strictVot_;
    }

    public PrintStream getOutputStream() {
        return pout_;
    }

    public PrintStream getErrorStream() {
        return perr_;
    }

    public void clearValue( Parameter param ) {
        throw new UnsupportedOperationException();
    }

    public String[] getNames() {
        return (String[]) paramMap_.keySet().toArray( new String[ 0 ] );
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
     * Returns the output written by the task so far, in an array one line
     * per element.
     *
     * @return  output text
     */
    public String[] getOutputLines() {
        String text = getOutputText();
        return text.length() == 0 ? new String[ 0 ]
                                  : text.split( "\\n" );
    }

    /**
     * Returns a string which contains all the error output written by the task
     * so far.
     *
     * @return  error text
     */
    public String getErrorText() {
        perr_.flush();
        return new String( err_.toByteArray() );
    }

    /**
     * Returns the error output written by the task so far, in an array one line
     * per element.
     *
     * @return  error text
     */
    public String[] getErrorLines() {
        String text = getErrorText();
        return text.length() == 0 ? new String[ 0 ]
                                  : text.split( "\\n" );
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
     * Sets the class which defines the context for resource discovery.
     *
     * @param  clazz  resource base class
     * @return  this
     * @see   java.lang.Class#getResource
     */
    public MapEnvironment setResourceBase( Class clazz ) {
        resourceBase_ = clazz;
        return this;
    }

    /**
     * If the task which has been executed in this environment has created
     * an output table which has not been otherwise disposed of, you
     * can get it from here.
     *
     * @param  paramName   name of a TableConsumerParameter
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
            throw new ParameterValueException( param,
                "No value supplied for non-nullable parameter" );
        }
        else if ( param instanceof InputTableParameter &&
                  value instanceof StarTable ) {
            ((InputTableParameter) param)
                                  .setValueFromTable( (StarTable) value );
        }
        else if ( param instanceof ConnectionParameter &&
                  value instanceof Connection ) {
            try {
                ((ConnectionParameter) param)
               .setValueFromConnection( (Connection) value );
            }
            catch ( SQLException e ) {
                throw new TaskException( e.getMessage(), e );
            }
        }
        else if ( param instanceof InputTablesParameter &&
                  value instanceof StarTable[] ) {
            StarTable[] tables = (StarTable[]) value;
            int nTable = tables.length;
            TableProducer[] tablePs = new TableProducer[ nTable ];
            for ( int i = 0; i < nTable; i++ ) {
                final StarTable table = tables[ i ];
                final String name = "table_" + ( i + 1 );
                tablePs[ i ] = new TableProducer() {
                    public StarTable getTable() {
                        return table;
                    }
                    public String toString() {
                        return name;
                    }
                };
            }
            ((InputTablesParameter) param).setValueFromTables( tablePs );
        }
        else if ( param instanceof InputTableParameter &&
                  value instanceof String &&
                  ((String) value).indexOf( '/' ) < 0 ) {
            String sval = (String) value;
            String frag = "";
            int ihash = sval.indexOf( '#' );
            if ( ihash > 0 ) {
                frag = sval.substring( ihash );
                sval = sval.substring( 0, ihash );
            }
            param.setValueFromString( this, resourceBase_
                                           .getResource( sval )
                                           .toString() + frag );
        }
        else if ( value instanceof String ) {
            param.setValueFromString( this, (String) value );
        }
        else {
            throw new ParameterValueException( param,
                "Unexpected type " + value.getClass() );
        }
    }

    public StarTableFactory getTableFactory() {
        return new StarTableFactory();
    }

    public StarTableOutput getTableOutput() {
        return new StarTableOutput();
    }

    public JDBCAuthenticator getJdbcAuthenticator() {
        return null;
    }

    public boolean isDebug() {
        return debug_;
    }

    public void setDebug( boolean debug ) {
        debug_ = debug;
    }

    public boolean isStrictVotable() {
        return strictVot_;
    }

    public void setStrictVotable( boolean strict ) {
        strictVot_ = strict;
    }
}
