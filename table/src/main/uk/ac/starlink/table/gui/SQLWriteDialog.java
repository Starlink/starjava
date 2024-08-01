package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.jdbc.JDBCFormatter;
import uk.ac.starlink.table.jdbc.WriteMode;

/**
 * A popup dialog for querying the user about the location of a new
 * JDBC table to write.
 */
public class SQLWriteDialog extends JPanel implements TableSaveDialog {

    private SQLPanel sqlPanel_;
    private JDialog dialog_; 
    private JComboBox<WriteMode> modeSelector_;
    private static Icon icon_;

    /**
     * Constructs a new SQLWriteDialog.
     */
    @SuppressWarnings("this-escape")
    public SQLWriteDialog() {
        super( new BorderLayout() );
        sqlPanel_ = new SQLPanel( "Write New SQL Table", false );
        add( sqlPanel_, BorderLayout.CENTER );
        modeSelector_ = new JComboBox<WriteMode>( WriteMode.getAllModes() );
        modeSelector_.setSelectedItem( WriteMode.CREATE );
        sqlPanel_.getStack().addLine( "Write Mode", null, modeSelector_ );
    }

    public String getName() {
        return "SQL Table";
    }

    public String getDescription() {
        return "Write table as a new table in an SQL relational database";
    }

    public Icon getIcon() {
        if ( icon_ == null ) {
            icon_ = new ImageIcon( getClass().getResource( "sqlread.png" ) );
        }
        return icon_;
    }

    public boolean isAvailable() {
        return sqlPanel_.isAvailable();
    }

    public boolean showSaveDialog( Component parent, StarTableOutput sto,
                                   ComboBoxModel<String> formatModel,
                                   StarTable[] tables ) {
        if ( tables.length != 1 ) {
            String[] msg = new String[] {
                "It is only possible to write one table at a time to SQL;",
                "you are trying to save " + tables.length + " tables.",
            };
            JOptionPane.showMessageDialog( parent, msg, "Save Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }
        sqlPanel_.useAuthenticator( sto.getJDBCHandler().getAuthenticator() );
        JOptionPane optPane =
            new JOptionPane( sqlPanel_, JOptionPane.QUESTION_MESSAGE,
                             JOptionPane.OK_CANCEL_OPTION );
        JDialog dialog = optPane.createDialog( parent, "Write New SQL Table" );
        final boolean[] done = new boolean[ 1 ];
        while ( ! done[ 0 ] ) {
            dialog.setVisible( true );
            if ( optPane.getValue() instanceof Integer &&
                 ((Integer) optPane.getValue()).intValue()
                  == JOptionPane.OK_OPTION ) {
                SaveWorker worker = new SaveWorker( parent, tables,
                                                    sqlPanel_.getRef() ) {
                    public void attemptSave( StarTable[] tables )
                            throws IOException {
                        assert tables.length == 1;
                        StarTable table = tables[ 0 ];
                        WriteMode mode =
                            modeSelector_.getItemAt( modeSelector_
                                                    .getSelectedIndex() );
                        Connection conn = null;
                        try {
                            conn = sqlPanel_.getConnector().getConnection();
                            new JDBCFormatter( conn, table )
                               .createJDBCTable( sqlPanel_.getRef(), mode );
                        }
                        catch ( SQLException e ) {
                            throw (IOException) 
                                  new IOException( e.getMessage() )
                                 .initCause( e );
                        }
                        finally {
                            if ( conn != null ) {
                                try {
                                    conn.close();
                                }
                                catch ( SQLException e ) {
                                    // never mind
                                }
                            }
                        }
                    }
                    public void done( boolean success ) {
                        done[ 0 ] = success;
                    }
                };
                setEnabled( false );
                worker.invoke();
                setEnabled( true );
            }
            else {
                return false;
            }
        }
        return true;
    }
}
