package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.sql.SQLException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.WriteMode;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.LineTableEnvironment;

/**
 * Table consumer which disposes of a table by writing it as a new 
 * table in a SQL database using JDBC.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2005
 */
public class JdbcConsumer implements TableConsumer {

    private final String url_;
    private final JDBCHandler handler_;
    private final WriteMode mode_;

    /**
     * Constructs a new consumer from a location and a JDBC handler.
     *
     * @param   url  destination (jdbc:) URL for table
     * @param   handler  JDBC handler
     * @param   mode   write mode
     */
    public JdbcConsumer( String url, JDBCHandler handler, WriteMode mode ) {
        url_ = url;
        handler_ = handler;
        mode_ = mode;
    }

    /**
     * Constructs a new consumer from a location and an execution environment
     * which will be used for JDBC authentication.
     *
     * @param   url  destination (jdbc:) URL for table
     * @param   env  execution environment
     * @param   mode   write mode
     */
    public JdbcConsumer( String url, Environment env, WriteMode mode ) {
        this( url, new JDBCHandler( LineTableEnvironment
                                   .getJdbcAuthenticator( env ) ),
              mode );
    }

    public void consume( StarTable table ) throws IOException {
        try {
            handler_.createJDBCTable( table, url_, mode_ );
        }
        catch ( SQLException e ) {
            String msg = e.getMessage();
            if ( msg == null ) {
                msg = "SQL error";
            }
            throw (IOException) new IOException( msg ).initCause( e );
        }
    }
}
