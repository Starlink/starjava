package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.ComboBoxModel;
import javax.swing.JDialog;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCFormatter;

/**
 * A popup dialog for querying the user about the location of a new
 * JDBC table to write.
 */
public class SQLWriteDialog extends SQLDialog implements TableSaveDialog {

    private JDialog dialog_; 

    /**
     * Constructs a new SQLWriteDialog.
     */
    public SQLWriteDialog() {
        super( "Write New SQL Table" );
    }

    public String getName() {
        return "SQL Table";
    }

    public String getDescription() {
        return "Write table as a new table in an SQL relational database";
    }

    public boolean showSaveDialog( Component parent, StarTableOutput sto,
                                   ComboBoxModel formatModel,
                                   StarTable table ) {
        JDialog dialog = createDialog( parent, "Write New SQL Table" );
        dialog_ = dialog;
        setEnabled( true );
        dialog.show();
        while ( dialog_ == dialog ) {
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                SaveWorker worker = new SaveWorker( parent, table, getRef() ) {
                    public void attemptSave( StarTable table )
                            throws IOException {
                        try {
                            Connection conn  = getConnector().getConnection();
                            JDBCFormatter jfmt = new JDBCFormatter( conn );
                            jfmt.createJDBCTable( table, getRef() );
                        }
                        catch ( SQLException e ) {
                            throw (IOException)
                                  new IOException( e.getMessage() )
                                 .initCause( e );
                        }
                    }
                    public void done( boolean success ) {
                        if ( success ) {
                            dialog_ = null;
                            dialog_.dispose();
                        }
                        else {
                            SQLWriteDialog.this.setEnabled( true );
                        }
                    }
                };
                setEnabled( false );
                worker.invoke();
                dialog_.show();
            }
            else {
                return false;
            }
        }
        return true;
    }

}
