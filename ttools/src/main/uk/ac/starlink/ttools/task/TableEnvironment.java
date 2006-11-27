package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.task.Environment;

/**
 * Environment subinterface which provides additional functionality 
 * required for table-aware tasks.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public interface TableEnvironment extends Environment {

    /**
     * Returns a table factory suitable for use in this environment.
     *
     * @return  table factory
     */
    StarTableFactory getTableFactory();

    /**
     * Returns a table output marshaller suitable for use in this environment.
     *
     * @return  table output
     */
    StarTableOutput getTableOutput();

    /**
     * Returns a JDBC authenticator suitable for use in this environment.
     *
     * @return   JDBC authenticator
     */
    JDBCAuthenticator getJdbcAuthenticator();

    /**
     * Indicates whether we are running in debug mode.
     *
     * @return  true  iff debugging output is required
     */
    boolean isDebug();

    /**
     * Sets whether we are running in debug mode.
     *
     * @param   debug  set true if you want debugging messages
     */
    void setDebug( boolean debug );

    /**
     * Determines whether votables are to be parsed in strict mode.
     *
     * @return  true if VOTables will be interpreted strictly in accordance
     *          with the standard
     */
    boolean isStrictVotable();

    /**
     * Sets whether votables should be parsed in strict mode.
     *
     * @param  strict  true if VOTables should be interpreted 
     *         strictly in accordance with the standard
     */
    void setStrictVotable( boolean strict );
}
