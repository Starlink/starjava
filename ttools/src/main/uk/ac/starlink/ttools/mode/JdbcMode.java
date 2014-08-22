package uk.ac.starlink.ttools.mode;

import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.WriteMode;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Mode for writing a table as a new table in a JDBC-connected database.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class JdbcMode implements ProcessingMode {

    private final StringParameter protoParam_;
    private final StringParameter hostParam_;
    private final StringParameter dbParam_;
    private final StringParameter tableParam_;
    private final ChoiceParameter<WriteMode> writeParam_;
    private final StringParameter userParam_;
    private final StringParameter passwdParam_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    public JdbcMode() {

        protoParam_ = new StringParameter( "protocol" );
        protoParam_.setPrompt( "Subprotocol for JDBC connection (e.g. mysql)" );
        protoParam_.setUsage( "<jdbc-protocol>" );
        protoParam_.setDescription( new String[] {
            "<p>The driver-specific sub-protocol specifier for the JDBC",
            "connection.",
            "For MySQL's Connector/J driver, this is <code>mysql</code>,",
            "and for PostgreSQL's driver it is <code>postgresql</code>.",
            "For other drivers, you may have to consult the driver",
            "documentation.",
            "</p>",
        } );

        hostParam_ = new StringParameter( "host" );
        hostParam_.setPrompt( "SQL database host" );
        hostParam_.setPrompt( "<hostname>" );
        hostParam_.setStringDefault( "localhost" );
        hostParam_.setDescription( new String[] {
            "<p>The host which is acting as a database server.",
            "</p>",
        } );

        dbParam_ = new StringParameter( "db" );
        dbParam_.setPrompt( "Name of database on database server" );
        dbParam_.setUsage( "<db-name>" );
        dbParam_.setDescription( new String[] {
            "<p>The name of the database on the server into which the",
            "new table will be written.",
            "</p>",
        } );

        tableParam_ = new StringParameter( "dbtable" );
        tableParam_.setPrompt( "Name of table to write to database" );
        tableParam_.setUsage( "<table-name>" );
        tableParam_.setDescription( new String[] {
            "<p>The name of the table which will be written to the",
            "database.",
            "</p>",
        } );

        WriteMode[] modes = WriteMode.getAllModes();
        writeParam_ = new ChoiceParameter<WriteMode>( "write", modes );
        writeParam_.setPrompt( "Mode for writing to the database table" );
        writeParam_.setDefaultOption( WriteMode.CREATE );
        StringBuffer descrip = new StringBuffer()
            .append( "<p>Controls how the values are written to a table " )
            .append( "in the database. " )
            .append( "The options are:\n" )
            .append( "<ul>\n" );
        for ( int i = 0; i < modes.length; i++ ) {
            descrip.append( "<li><code>" )
                   .append( modes[ i ].toString() )
                   .append( "</code>: " )
                   .append( modes[ i ].getDescription() )
                   .append( "</li>" )
                   .append( "\n" );
        }
        descrip.append( "</ul>\n" )
               .append( "</p>" );
        writeParam_.setDescription( descrip.toString() );

        userParam_ = new StringParameter( "user" ); 
        userParam_.setPrompt( "Username for SQL connection" );
        userParam_.setUsage( "<username>" );
        userParam_.setNullPermitted( true );
        userParam_.setPreferExplicit( true );
        try {
            userParam_.setStringDefault( System.getProperty( "user.name" ) );
        }
        catch ( SecurityException e ) {
            // no default - OK
        }
        userParam_.setDescription( new String[] {
            "<p>User name for the SQL connection to the database.",
            "</p>",
        } );

        passwdParam_ = new StringParameter( "password" );
        passwdParam_.setPrompt( "Password for SQL connection" );
        passwdParam_.setUsage( "<passwd>" );
        passwdParam_.setNullPermitted( true );
        passwdParam_.setPreferExplicit( true );
        passwdParam_.setDescription( new String[] { 
            "<p>Password for the SQL connection to the database.",
            "</p>",
        } );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            protoParam_,
            hostParam_,
            dbParam_,
            tableParam_,
            writeParam_,
            userParam_,
            passwdParam_,
        };
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Writes a new table to an SQL database.",
            "You need the appropriate JDBC drivers and",
            "<code>-Djdbc.drivers</code> set as usual",
            "(see <ref id=\"jdbcConfig\"/>).",
            "</p>",
        } );
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
        logger_.info( "JDBC URL: " + url );
        final String user = userParam_.stringValue( env );
        final String passwd = passwdParam_.stringValue( env );
        final WriteMode mode = writeParam_.objectValue( env );
        JDBCAuthenticator auth = new JDBCAuthenticator() {
            public String[] authenticate() {
                return new String[] { user, passwd };
            }
        };
        return new JdbcConsumer( url, new JDBCHandler( auth ), mode );
    }
}
