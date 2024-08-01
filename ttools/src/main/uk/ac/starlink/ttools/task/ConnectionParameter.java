package uk.ac.starlink.ttools.task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for acquiring a connection to a database using JDBC.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2007
 */
public class ConnectionParameter extends Parameter<Connection> {

    private final StringParameter userParam_;
    private final StringParameter passParam_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public ConnectionParameter( String name ) {
        super( name, Connection.class, true );

        setPrompt( "JDBC-type URL for database connection" );
        setUsage( "<jdbc-url>" );
        setDescription( new String[] {
            "<p>URL which defines a connection to a database.",
            "This has the form ",
            "<code>jdbc:&lt;subprotocol&gt;:&lt;subname&gt;</code>",
            "- the details are database- and driver-dependent.",
            "Consult Sun's JDBC documentation and that for the particular",
            "JDBC driver you are using for details.",
            "Note that the relevant driver class will need to be on your",
            "classpath and referenced in the <code>jdbc.drivers</code>",
            "system property as well for the connection to be made.",
            "</p>",
        } );

        userParam_ = new StringParameter( "user" );
        userParam_.setPrompt( "User name for database connection" );
        try {
            Properties props = System.getProperties();
            if ( props.containsKey( "user.name" ) ) {
                userParam_.setStringDefault( props.getProperty( "user.name" ) );
            }
        }
        catch ( SecurityException e ) {
            // never mind.
        }
        userParam_.setNullPermitted( true );
        userParam_.setDescription( new String[] {
            "<p>User name for logging in to SQL database.",
            "Defaults to the current username.",
            "</p>",
        } );

        passParam_ = new StringParameter( "password" );
        passParam_.setPrompt( "Password for database connection" );
        passParam_.setNullPermitted( true );
        passParam_.setPreferExplicit( true );
        passParam_.setDescription( new String[] {
            "<p>Password for logging in to SQL database.",
            "</p>",
        } );
    }

    /**
     * Returns parameters associated with this.
     *
     * @return   array containing user and password parameters
     */
    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[] {
            userParam_,
            passParam_,
        };
    }

    public Connection stringToObject( Environment env, String stringValue )
            throws TaskException {
        if ( ! stringValue.startsWith( "jdbc:" ) ) {
            String msg = "Must be of form \"jdbc:<subprotocol>:<subname>\"";
            throw new ParameterValueException( this, msg );
        }
        String user = userParam_.stringValue( env );
        String pass = passParam_.stringValue( env );
        try {
            return DriverManager.getConnection( stringValue, user, pass );
        }
        catch ( SQLException e ) {
            throw new ParameterValueException( this, e );
        }
    }

    @Override
    public String objectToString( Environment env, Connection connection ) {
        try {
            return connection.getMetaData().getDatabaseProductName()
                 + ":" + connection.getCatalog();
        }
        catch ( SQLException e ) {
            return "JDBC";
        }
    }
}
