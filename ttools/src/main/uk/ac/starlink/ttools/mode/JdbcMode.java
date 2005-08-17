package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.sql.SQLException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Mode for writing a table as a new table in a JDBC-connected database.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class JdbcMode implements ProcessingMode {

    private final Parameter protoParam_;
    private final Parameter hostParam_;
    private final Parameter dbParam_;
    private final Parameter tableParam_;
    private final Parameter userParam_;
    private final Parameter passwdParam_;

    public JdbcMode() {

        protoParam_ = new Parameter( "protocol" );
        protoParam_.setPrompt( "Subprotocol for JDBC connection (e.g. mysql)" );
        protoParam_.setUsage( "<jdbc-protocol>" );

        hostParam_ = new Parameter( "host" );
        hostParam_.setPrompt( "SQL database host" );
        hostParam_.setPrompt( "<hostname>" );
        hostParam_.setDefault( "localhost" );

        dbParam_ = new Parameter( "database" );
        dbParam_.setPrompt( "Name of database on database server" );
        dbParam_.setUsage( "<db-name>" );

        tableParam_ = new Parameter( "newtable" );
        tableParam_.setPrompt( "Name of new table to write to database" );
        tableParam_.setUsage( "<table-name>" );

        userParam_ = new Parameter( "user" ); 
        userParam_.setPrompt( "Username for SQL connection" );
        userParam_.setUsage( "<username>" );
        userParam_.setNullPermitted( true );

        passwdParam_ = new Parameter( "password" );
        passwdParam_.setPrompt( "Password for SQL connection" );
        passwdParam_.setUsage( "<passwd>" );
        passwdParam_.setNullPermitted( true );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            protoParam_,
            hostParam_,
            dbParam_,
            tableParam_,
            userParam_,
            passwdParam_,
        };
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        String url = "jdbc:" 
                   + protoParam_.stringValue( env )
                   + "://"
                   + hostParam_.stringValue( env )
                   + "/"
                   + dbParam_.stringValue( env )
                   + "#"
                   + tableParam_.stringValue( env );
        return new JdbcConsumer( url, userParam_.stringValue( env ),
                                      passwdParam_.stringValue( env ) );
    }

    /**
     * Implements the table consumer which writes to a JDBC database.
     */
    private static class JdbcConsumer implements TableConsumer {

        final String url_;
        String user_;
        String passwd_;

        JdbcConsumer( String url, String user, String password ) {
            url_ = url;
            user_ = user;
            passwd_ = password;
        }

        public void consume( StarTable table ) throws IOException {
            JDBCAuthenticator authenticator = new JDBCAuthenticator() {
                public String[] authenticate() throws IOException {
                    if ( user_ == null ) {
                        user_ = TerminalAuthenticator.readUser();
                    }
                    if ( passwd_ == null ) {
                        passwd_ = TerminalAuthenticator.readPassword();
                    }
                    return new String[] { user_, passwd_ };
                }
            };
            JDBCHandler handler = new JDBCHandler( authenticator );
            try {
                handler.createJDBCTable( table, url_ );
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }
    }
}
