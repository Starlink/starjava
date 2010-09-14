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
    private SQLPanel sqlPanel_;

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
        return SQLPanel.isSqlAvailable();
    }

    public boolean showLoadDialog( Component parent, 
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        SQLPanel sqlPanel = getSqlPanel();
        sqlPanel.useAuthenticator( factory.getJDBCHandler()
                                          .getAuthenticator() );
        JOptionPane optPane =
            new JOptionPane( sqlPanel, JOptionPane.QUESTION_MESSAGE,
                             JOptionPane.OK_CANCEL_OPTION );
        JDialog dialog = optPane.createDialog( parent, "Open JDBC table" );
        while ( true ) {
            dialog.setVisible( true );
            if ( optPane.getValue() instanceof Integer &&
                 ((Integer) optPane.getValue()).intValue()
                   == JOptionPane.OK_OPTION ) {
                String qtext = sqlPanel.getRef();
                final String url = sqlPanel.getFullURL();
                new LoadWorker( eater, qtext ) {
                    public StarTable[] attemptLoads() throws IOException {
                        return new StarTable[] { factory.makeStarTable( url ) };
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
     * Returns a lazily-constructed SQLPanel.
     *
     * @return  SQLPanel for use with this component
     */
    private SQLPanel getSqlPanel() {
        if ( sqlPanel_ == null ) {
            sqlPanel_ = new SQLPanel( "SQL query" );
        }
        return sqlPanel_;
    }
}
