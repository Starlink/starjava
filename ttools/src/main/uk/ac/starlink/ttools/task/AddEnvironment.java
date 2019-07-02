package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * Environment implementation which wraps a base environment instance and
 * adds some extra entries as specified by a given map.
 *
 * <p>Note this does not work perfectly, because of bad design of the
 * Environment class.  The <code>acquireValue</code> method can end up
 * passing an instance of the wrapped environment to a parameter,
 * which means that subequent environment accesses made under the control
 * of that parameter will not pick up entries added to this environment.
 * A redesign of the Environment class is the only good way out of this.
 * Until then you have to hack round it by explicitly calling acquireValue
 * from this class on dependent variables before it gets done under control
 * of the variable they depend on.
 *
 * @author   Mark Taylor
 * @since    2013
 */
public class AddEnvironment implements Environment {

    private final Environment baseEnv_;
    private final Map<String,String> addMap_;

    /**
     * Constructor.
     *
     * @param  baseEnv  base environment
     * @param  addMap   addional key-value pairs to add to this environment
     */
    public AddEnvironment( Environment baseEnv, Map<String,String> addMap ) {
        baseEnv_ = baseEnv;
        addMap_ = addMap;
    }

    public void acquireValue( Parameter<?> par ) throws TaskException {
        String name = par.getName();
        if ( addMap_.containsKey( name ) ) {
            par.setValueFromString( this, addMap_.get( name ) );
        }
        else {
            baseEnv_.acquireValue( par );
        }
    }

    public void clearValue( Parameter<?> par ) {
        String name = par.getName();
        if ( addMap_.containsKey( name ) ) {
            addMap_.remove( name );
        }
        else {
            baseEnv_.clearValue( par );
        }
    }

    public String[] getNames() {

        /* Use the keyset of a LinkedHashMap here rather than just a HashSet
         * to preserve the order as much as possible. */
        Map<String,Void> names = new LinkedHashMap<String,Void>();
        String[] baseNames = baseEnv_.getNames();
        for ( int i = 0; i < baseNames.length; i++ ) {
            names.put( baseNames[ i ], null );
        }
        for ( String addName : addMap_.keySet() ) {
            names.put( addName, null );
        }
        return names.keySet().toArray( new String[ 0 ] );
    }

    public PrintStream getOutputStream() {
        return baseEnv_.getOutputStream();
    }

    public PrintStream getErrorStream() {
        return baseEnv_.getErrorStream();
    }

    /**
     * Returns an AddEnvironment based on a supplied base environment and
     * a map of key-value pairs.  If the supplied base environment is
     * a TableEnvironment instance, the returned value will be as well.
     *
     * @param  baseEnv   base environment
     * @param  addMap   addional key-value pairs to add to this environment
     * @return  Environment or TableEnvironment instance with additional entries
     */
    public static AddEnvironment
            createAddEnvironment( Environment baseEnv,
                                  Map<String,String> addMap ) {
        if ( baseEnv instanceof TableEnvironment ) {
            final TableEnvironment tEnv = (TableEnvironment) baseEnv;
            return new AddEnvironment( baseEnv, addMap ) {
                public JDBCAuthenticator getJdbcAuthenticator() {
                    return tEnv.getJdbcAuthenticator();
                }
                public StarTableFactory getTableFactory() {
                    return tEnv.getTableFactory();
                }
                public StarTableOutput getTableOutput() {
                    return tEnv.getTableOutput();
                }
                public boolean isDebug() {
                    return tEnv.isDebug();
                }
                public boolean isStrictVotable() {
                    return tEnv.isStrictVotable();
                }
                public void setDebug( boolean debug ) {
                    tEnv.setDebug( debug );
                }
                public void setStrictVotable( boolean strict ) {
                    tEnv.setStrictVotable( strict );
                }
            };
        }
        else {
            return new AddEnvironment( baseEnv, addMap );
        }
    }
}
