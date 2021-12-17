package uk.ac.starlink.ttools.task;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;

/**
 * Environment which allows use of ttools tasks from an in-memory context.
 * Input StarTables can be set as values of parameters and output ones
 * can be extracted.
 *
 * @author   Mark Taylor
 */
public class MapEnvironment implements TableEnvironment {

    private final Map<String,Object> paramMap_;
    private final Map<String,StarTable> outputTables_ =
        new LinkedHashMap<String,StarTable>();
    private final ByteArrayOutputStream out_ = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err_ = new ByteArrayOutputStream();
    private final PrintStream pout_ = new PrintStream( out_ );
    private final PrintStream perr_ = new PrintStream( err_ );
    private final Set<String> usedNames_ = new HashSet<String>();
    private Class<?> resourceBase_ = MapEnvironment.class;
    private StarTableFactory tfact_;
    private StarTableOutput tout_;
    private boolean strictVot_;
    private boolean debug_;

    /**
     * Constructs a new environment with no values.
     */
    public MapEnvironment() {
        this( new LinkedHashMap<String,Object>() );
    }

    /**
     * Constructs a new environment with a map of parameter name-&gt;value
     * pairs.
     *
     * @param  map   parameter map
     */
    public MapEnvironment( Map<String,Object> map ) {
        paramMap_ = map;
    }

    /**
     * Clone constructor.
     *
     * @param  env  environment to copy
     */
    public MapEnvironment( MapEnvironment env ) {
        this( new LinkedHashMap<String,Object>( env.paramMap_ ) );
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

    public void clearValue( Parameter<?> param ) {
        throw new UnsupportedOperationException();
    }

    public String[] getNames() {
        return paramMap_.keySet().toArray( new String[ 0 ] );
    }

    /**
     * Returns the map object that contains the data for this environment.
     * Modify it at your own risk.
     *
     * @return   content map
     */
    public Map<String,Object> getMap() {
        return paramMap_;
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
    public MapEnvironment setResourceBase( Class<?> clazz ) {
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
        return outputTables_.get( paramName );
    }

    public void acquireValue( Parameter<?> param ) throws TaskException {
        final String pname = param.getName();
        final Class<?> pclazz = param.getValueClass();
        final Object value;

        /* Parameters not explicitly specified. */
        if ( ! paramMap_.containsKey( pname ) ) {

            /* Special treatment if output table is not specified;
             * just store the result for later retrieval. */
            if ( param instanceof TableConsumerParameter ) {
                value = new TableConsumer() {
                    public void consume( StarTable table ) {
                        outputTables_.put( pname, table );
                    }
                };
            }

            /* For any other unspecified value, use the parameter default. */
            else {
                value = param.getStringDefault();
            }
        }

        /* Explicitly specified parameters. */
        else {
            Object mapVal = paramMap_.get( pname );

            /* Map arrays of StarTables to arrays of TableProducers for
             * convenience. */
            if ( pclazz.equals( TableProducer[].class ) &&
                 mapVal instanceof StarTable[] ) {
                StarTable[] tables = (StarTable[]) mapVal;
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
                value = tablePs;
            }

            /* If the table corresponds to a resource in the current context,
             * use that.  Otherwise use the specified value. */
            else if ( pclazz.equals( StarTable.class ) &&
                      mapVal instanceof String &&
                      ((String) mapVal).indexOf( '/' ) < 0 ) {
                String sval = (String) mapVal;
                String frag = "";
                int ihash = sval.indexOf( '#' );
                if ( ihash > 0 ) {
                    frag = sval.substring( ihash );
                    sval = sval.substring( 0, ihash );
                }
                URL url = resourceBase_.getResource( sval );
                value = url == null ? mapVal : url.toString() + frag;
            }

            /* Otherwise use the specified value. */
            else {     
                value = mapVal;
            }
        }
        usedNames_.add( pname );

        /* Now we have the value, pass it to the parameter somehow. */
        /* Treat null values specially. */
        if ( value == null ) {
            if ( param.isNullPermitted() ) {
                param.setValueFromString( this, null );
            }
            else {
                throw new ParameterValueException( param,
                    "No value supplied for non-nullable parameter" );
            }
        }

        /* If we have a value of the object type required by the parameter,
         * we can set it directly. */
        else if ( pclazz.isAssignableFrom( value.getClass() ) ) {
            setParamValueFromObject( param, value );
        }

        /* If we have a string value, use the parameter's required
         * set-from-string capability. */
        else if ( value instanceof String ) {
            param.setValueFromString( this, (String) value );
        }

        /* If we have a scalar corresponding to the element type of an
         * array parameter, turn it into a 1-element array. */
        else if ( pclazz.getComponentType() != null &&
                  pclazz.getComponentType().isInstance( value ) ) {
            Object array = Array.newInstance( pclazz.getComponentType(), 1 );
            Array.set( array, 0, value );
            setParamValueFromObject( param, array );
        }

        /* Special treatment for table consumers (see above). */
        else if ( param instanceof TableConsumerParameter &&
                  value instanceof TableConsumer ) {
            ((TableConsumerParameter) param)
                .setValueFromConsumer( this, (TableConsumer) value );
        }

        /* Otherwise, looks like user error. */
        else {
            throw new ParameterValueException(
                param,
                "Unexpected type " + value.getClass()
                + " (expecting String or " + pclazz.getName() + ")" );

        }
    }

    public synchronized StarTableFactory getTableFactory() {
        if ( tfact_ == null ) {
            tfact_ = new StarTableFactory();
            Stilts.addStandardSchemes( tfact_ );
        }
        return tfact_;
    }

    public synchronized StarTableOutput getTableOutput() {
        if ( tout_ == null ) {
            tout_ = new StarTableOutput();
        }
        return tout_;
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

    /**
     * Returns an array containing any words of the input argument list
     * which were never queried by the application to find their value.
     * Such unused words probably merit a warning, since they
     * may for instance be misspelled versions of real parameters.
     *
     * @return   array of unused words
     */
    public String[] getUnused() {
        List<String> pnames = new ArrayList<String>( paramMap_.keySet() );
        pnames.removeAll( usedNames_ );
        return pnames.toArray( new String[ 0 ] );
    }

    /**
     * Sets the value of a typed parameter from an object of the right type.
     *
     * @param  param  parameter
     * @param  obj  object of type X
     * @throws  ClassCastException  iff obj is not of type X
     */
    private <X> void setParamValueFromObject( Parameter<X> param, Object obj )
            throws TaskException {
        param.setValueFromObject( this, param.getValueClass().cast( obj ) );
    }
}
