package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.LineEnvironment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.votable.VOElementFactory;

/**
 * Execution environment suitable for use with the TTOOLS package.
 * This inherits most of its behaviour from 
 * {@link uk.ac.starlink.task.LineEnvironment} but also impelements
 * the additional methods of the {@link TableEnvironment} interface.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2006
 */
public class LineTableEnvironment extends LineEnvironment
                                  implements TableEnvironment {

    private StarTableFactory tfact_;
    private StarTableOutput tout_;
    private JDBCAuthenticator jdbcAuth_;
    private boolean debug_;
    private Boolean isStrict_;

    public boolean isHidden( Parameter<?> param ) {
        return param.getName().equals( "password" );
    }

    public String getParamHelp( Parameter<?> param ) {
        return LineInvoker.getParamHelp( this, null, param );
    }

    /**
     * Returns a table factory suitable for use in this environment.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() {
        if ( tfact_ == null ) {
            tfact_ = new StarTableFactory();
            Stilts.addStandardSchemes( tfact_ );
        }
        return tfact_;
    }

    /**
     * Sets the table factory that should be used with this environment.
     *
     * @param  tfact  table factory
     */
    public void setTableFactory( StarTableFactory tfact ) {
        tfact_ = tfact;
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
     * Returns a JDBC authenticator suitable for use in this environment.
     *
     * @return   JDBC authenticator
     */
    public JDBCAuthenticator getJdbcAuthenticator() {
        if ( jdbcAuth_ == null ) {
            jdbcAuth_ = new TerminalAuthenticator( getErrorStream() );
        }
        return jdbcAuth_;
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
     * @param  strict  true if VOTables should be interpreted
     *         strictly in accordance with the standard
     */
    public void setStrictVotable( boolean strict ) {
        isStrict_ = strict ? Boolean.TRUE
                           : Boolean.FALSE;
    }

    /**
     * Uses {@link #normaliseName}.
     */
    public boolean paramNameMatches( String envName, Parameter<?> param ) {
        boolean matches = normaliseName( param.getName() )
                         .equals( normaliseName( envName ) );

        /* This ought to match anything matched by the superclass 
         * implementation and then some. */
        assert matches || ! super.paramNameMatches( envName, param );
        return matches;
    }

    /**
     * Normalises a given name.
     * This folds to lower case, and may modify spelling.
     *
     * @param   name   input name
     * @return  normalised name
     */
    public static String normaliseName( String name ) {
        if ( name != null ) {
            name = name.toLowerCase();
            name = name.replaceFirst( "color", "colour" );
        }
        return name;
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
     * Returns a suitable JDBC authenticator for a given environment.
     * If <code>env</code> is a TableEnvironment then <code>env</code>'s
     * authenticator is returned, otherwise a new one is returned.
     *
     * @param   env  execution environment
     * @return  JDBC authenticator
     */
    public static JDBCAuthenticator getJdbcAuthenticator( Environment env ) {
        return env instanceof TableEnvironment
             ? ((TableEnvironment) env).getJdbcAuthenticator()
             : new TerminalAuthenticator();
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
