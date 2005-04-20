package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;

/**
 * Mode for writing a table as a new table in a JDBC-connected database.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Mar 2005
 */
public class JdbcMode extends ProcessingMode {

    private String url_;
    private String user_;
    private String password_;

    public String getName() {
        return "tosql";
    }

    public boolean setArgs( List argList ) {
        for ( Iterator it = argList.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-" ) && arg.length() > 1 ) {
                if ( arg.equals( "-user" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        user_ = (String) it.next(); 
                        it.remove();
                    }
                    else {
                        return false;
                    }
                }
                else if ( arg.equals( "-password" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        password_ = (String) it.next();
                        it.remove();
                    }
                    else {
                        return false;
                    }
                }
                else if ( arg.equals( "-url" ) ) {
                    it.remove();
                    if ( it.hasNext() ) {
                        url_ = (String) it.next();
                        it.remove();
                    }
                    else {
                        return false;
                    }
                }
            }
            else if ( url_ == null ) {
                it.remove();
                url_ = arg;
            }
        }
        return url_ != null;
    }

    public String getModeUsage() {
        return "<jdbc-url> [-user <username>] [-password <password>]";
    }

    public void process( StarTable table ) throws IOException {
        JDBCAuthenticator authenticator = new JDBCAuthenticator() {
            public String[] authenticate() throws IOException {
                if ( user_ == null ) {
                    user_ = TerminalAuthenticator.readUser();
                }
                if ( password_ == null ) {
                    password_ = TerminalAuthenticator.readPassword();
                }
                return new String[] { user_, password_ };
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
