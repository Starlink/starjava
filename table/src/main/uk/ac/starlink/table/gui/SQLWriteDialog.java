package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.jdbc.JDBCFormatter;

/**
 * A popup dialog for querying the user about the location of a new
 * JDBC table to write.
 */
public class SQLWriteDialog extends SQLDialog {

    /**
     * Constructs a new SQLWriteDialog.
     */
    public SQLWriteDialog() {
        super( "New table name" );
    }

    /**
     * Pops up a modal dialog box and asks the user for a spec at which
     * to save a StarTable.  If there is an error the user is shown 
     * the error message and given another chance. 
     *
     * @param  startab  the table to write
     * @param  parent  the parent component - used for window positioning
     * @return  true if the table was saved, false if it is not because
     *          the user cancelled the dialog
     */
    public boolean writeTableDialog( StarTable startab, Component parent ) {
        JDialog dialog = createDialog( parent, "Save table to JDBC" );
        while ( true ) {
            dialog.show();
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                try {
                    Connection conn = getConnector().getConnection();
                    JDBCFormatter jfmt = new JDBCFormatter( conn );
                    jfmt.createJDBCTable( startab, getRef() );
                    dialog.dispose();
                    return true;
                }
                catch ( Exception e ) {
                    JOptionPane.showMessageDialog( dialog, e.toString(),
                                                   "Can't write table",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
            else {
                dialog.dispose();
                return false;
            }
        }
    }

}
