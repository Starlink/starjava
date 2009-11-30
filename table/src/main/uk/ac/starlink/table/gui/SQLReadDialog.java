package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.jdbc.JDBCStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * A table load dialogue which interrogates the user about an SQL query on 
 * a JDBC database.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public class SQLReadDialog implements TableLoadDialog {
    private final Icon icon_;
    private SQLDialog sqlDialog_;

    /**
     * Constructs a new <tt>SQLReadDialog</tt>.
     */
    public SQLReadDialog() {
        icon_ = new ImageIcon( getClass().getResource( "sqlread.gif" ) );
    }

    public String getName() {
        return "SQL Query";
    }

    public String getDescription() {
        return "Get table as result of an SQL query on a relational database";
    }

    public Icon getIcon() {
        return icon_;
    }

    public boolean isAvailable() {
        return SQLDialog.isSqlAvailable();
    }

    public boolean showLoadDialog( Component parent, 
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        SQLDialog sqlDialog = getSqlDialog();
        sqlDialog.useAuthenticator( factory.getJDBCHandler()
                                           .getAuthenticator() );
        JDialog dialog = sqlDialog.createDialog( parent, "Open JDBC table" );
        while ( true ) {
            dialog.show();
            if ( sqlDialog.getValue() instanceof Integer &&
                 ((Integer) sqlDialog.getValue()).intValue()
                   == JOptionPane.OK_OPTION ) {
                String qtext = sqlDialog.getRef();
                final String url = sqlDialog.getFullURL();
                new LoadWorker( eater, qtext ) {
                    public StarTable attemptLoad() throws IOException {
                        return factory.makeStarTable( url );
                    }
                }.invoke();
                return true;
            }
            else {
                dialog.dispose();
                return false;
            }
        }
    }

    /**
     * Returns a lazily-constructed SQLDialog.
     *
     * @return  SQLDialog for use with this component
     */
    private SQLDialog getSqlDialog() {
        if ( sqlDialog_ == null ) {
            sqlDialog_ = new SQLDialog( "SQL query" );
        }
        return sqlDialog_;
    }
}
