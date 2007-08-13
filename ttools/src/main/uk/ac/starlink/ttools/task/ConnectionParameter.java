package uk.ac.starlink.ttools.task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

public class ConnectionParameter extends Parameter {

    private final Parameter userParam_;
    private final Parameter passParam_;
    private Connection connection_;

    public ConnectionParameter( String name ) {
        super( name );

        userParam_ = new Parameter( "user" );
        userParam_.setPrompt( "User name for database connection" );
        userParam_.setNullPermitted( true );
        userParam_.setDescription( new String[] {
            "<p>User name for logging in to SQL database.",
            "</p>",
        } );

        passParam_ = new Parameter( "password" );
        passParam_.setPrompt( "Password for database connection" );
        passParam_.setNullPermitted( true );
        passParam_.setDescription( new String[] {
            "<p>Password for logging in to SQL database.",
            "</p>",
        } );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            userParam_,
            passParam_,
        };
    }

    public Connection connectionValue( Environment env )
            throws TaskException {
        checkGotValue( env );
        return connection_;
    }

    public void setValueFromConnection( Connection connection )
            throws SQLException {
        connection_ = connection;
        String name = connection.getMetaData().getDatabaseProductName()
                    + ":" + connection.getCatalog();
        setStringValue( name );
        setGotValue( true );
    }

    public void setValueFromString( Environment env, String stringValue )
            throws TaskException {
        if ( ! stringValue.startsWith( "jdbc:" ) ) {
            String msg = "Must be of form \"jdbc:<subprotocol>:<subname>\"";
            throw new ParameterValueException( this, msg );
        }
        try {
            String user = userParam_.stringValue( env );
            String pass = passParam_.stringValue( env );
            connection_ =
                DriverManager.getConnection( stringValue, user, pass );
        }
        catch ( SQLException e ) {
            throw new ParameterValueException( this, e );
        }
        super.setValueFromString( env, stringValue );
    }
}
