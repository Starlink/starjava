package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.sql.SQLException;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.jdbc.JDBCStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A popup dialog which interrogates the user about an SQL query on 
 * a JDBC database.
 */
public class SQLReadDialog extends SQLDialog {

    /**
     * Constructs a new <tt>SQLReadDialog</tt>.
     */
    public SQLReadDialog() {
        super( "SQL query" );
    }

    /**
     * Pops up a modal dialog box and asks the user to open a JDBC
     * connection resulting in a StarTable.  If there is an error the
     * user is shown the error message and is given another chance.
     *
     * @param   parent  the parent component - used for window positioning
     * @return  a StarTable as specified by the user's inputs, or
     *          <tt>null</tt> if the user cancels the dialog
     */
    public StarTable readTableDialog( Component parent ) {
        JDialog dialog = createDialog( parent, "Open JDBC table" );
        while ( true ) {
            dialog.show();
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                try {
                    StarTable tab = 
                        new JDBCStarTable( getConnector(), getRef() );
                    dialog.dispose();
                    return tab;
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( e, "Can't read table", dialog );
                }
            }
            else {
                dialog.dispose();
                return null;
            }
        }
    }


}
