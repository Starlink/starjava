package uk.ac.starlink.ttools.task;

import java.io.PrintStream;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Environment subinterface which provides additional functionality 
 * required for table-aware tasks.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public abstract class TableEnvironment implements Environment {

    private StarTableFactory tfact_;
    private StarTableOutput tout_;
    private boolean debug_;
    private Boolean isStrict_;

    /**
     * Returns a table factory suitable for use in this environment.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() {
        if ( tfact_ == null ) {
            tfact_ = new StarTableFactory();
        }
        return tfact_;
    }

    /**
     * Returns a table output marshaller suitable for use in this environment.
     *
     * @return  table output
     */
    public StarTableOutput getTableOutput() {
        if ( tout_ == null ) {
            tout_ = new StarTableOutput();
        }
        return tout_;
    }

    /**
     * Indicates whether we are running in debug mode.
     *
     * @return  true  iff debugging output is required
     */
    public boolean isDebug() {
        return debug_;
    }

    /**
     * Sets whether we are running in debug mode.
     *
     * @param   debug  set true if you want debugging messages
     */
    public void setDebug( boolean debug ) {
        debug_ = debug;
    }

    /**
     * Determines whether votables are to be parsed in strict mode.
     *
     * @return  true if VOTables will be interpreted strictly in accordance
     *          with the standard
     */
    public boolean isStrictVotable() {
        return isStrict_ == null
             ? VOElementFactory.isStrictByDefault()
             : isStrict_.booleanValue();
    }

    /**
     * Sets whether votables should be parsed in strict mode.
     *
     * @param  true if VOTables should be interpreted strictly in accordance
     *         with the standard
     */
    public void setStrictVotable( boolean strict ) {
        isStrict_ = strict ? Boolean.TRUE
                           : Boolean.FALSE;
    }

    /**
     * Returns a suitable table factory for a given environment.
     * If <code>env</code> is a TableEnvironement then <code>env</code>'s
     * factory is returned, otherwise a default one is returned.
     *
     * @param  env  execution environment
     * @return  table factory
     */
    public static StarTableFactory getTableFactory( Environment env ) {
        return env instanceof TableEnvironment
             ? ((TableEnvironment) env).getTableFactory()
             : new StarTableFactory();
    }

    /**
     * Returns a suitable table output marshaller for a given environment.
     * If <code>env</code> is a TableEnvironment then <code>env</code>'s
     * outputter is returned, otherwise a default one is returned.
     * 
     * @param  env  execution environment
     * @return  table output
     */
    public static StarTableOutput getTableOutput( Environment env ) {
        return env instanceof TableEnvironment
             ? ((TableEnvironment) env).getTableOutput()
             : new StarTableOutput();
    }

    /**
     * Returns a suitable storage policy for a given environment.
     *
     * @param  env  execution environment
     * @return  storage policy
     */
    public static StoragePolicy getStoragePolicy( Environment env ) {
        return getTableFactory( env ).getStoragePolicy();
    }

    /**
     * Determines whether votables are to be parsed in strict mode.
     *
     * @param  env  execution environment
     * @return  true if VOTables will be interpreted strictly in accordance
     *          with the standard
     */
    public static boolean isStrictVotable( Environment env ) {
        return env instanceof TableEnvironment
             ? ((TableEnvironment) env).isStrictVotable()
             : VOElementFactory.isStrictByDefault();
    }
}
