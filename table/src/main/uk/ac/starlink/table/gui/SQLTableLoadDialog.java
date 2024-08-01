package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.Action;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.jdbc.JDBCAuthenticator;

/**
 * Load dialogue for loading data from a database via JDBC.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2010
 */
public class SQLTableLoadDialog extends AbstractTableLoadDialog {

    private SQLPanel sqlPanel_;
    private JDBCAuthenticator authenticator_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.gui" );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public SQLTableLoadDialog() {
        super( "SQL Query",
               "Get table as result of an SQL query on a relational database" );
        setIconUrl( StarTable.class.getResource( "gui/sqlread.png" ) );
    }

    public boolean isAvailable() {
        return SQLPanel.isSqlAvailable();
    }

    public void configure( StarTableFactory tfact, Action submitAct ) {
        super.configure( tfact, submitAct );
        authenticator_ = tfact.getJDBCHandler().getAuthenticator();
    }

    protected Component createQueryComponent() {
        sqlPanel_ = new SQLPanel( "SQL Query", true );
        sqlPanel_.useAuthenticator( authenticator_ );
        return sqlPanel_;
    }

    public TableLoader createTableLoader() {
        final String url = sqlPanel_.getFullURL();
        final String qtext = sqlPanel_.getRef();
        return new TableLoader() {
            public String getLabel() {
                return qtext;
            }
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                logger_.info( url );
                return Tables.singleTableSequence( tfact.makeStarTable( url ) );
            }
        };
    }
}
