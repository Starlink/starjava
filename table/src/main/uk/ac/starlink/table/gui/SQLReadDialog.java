package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.ComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.jdbc.JDBCStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A table load dialogue which interrogates the user about an SQL query on 
 * a JDBC database.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public class SQLReadDialog extends SQLDialog implements TableLoadDialog {

    /**
     * Constructs a new <tt>SQLReadDialog</tt>.
     */
    public SQLReadDialog() {
        super( "SQL query" );
    }

    public String getName() {
        return "SQL Query";
    }

    public String getDescription() {
        return "Get table as result of an SQL query on a relational database";
    }

    public boolean isEnabled() {
        return DriverManager.getDrivers().hasMoreElements();
    }

    public boolean showLoadDialog( Component parent, 
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        JDialog dialog = createDialog( parent, "Open JDBC table" );
        while ( true ) {
            dialog.show();
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                String qtext = getRef();
                final String url = getFullURL();
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


}
