package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.ComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;

/**
 * Table save dialogue based on the native system file dialogue.
 * This is a wrapper around {@link java.awt.FileDialog}, which is in turn
 * a thin wrapper around whatever the native platform uses for its
 * default file browser.  Looks ugly on my fvwm-based Linux desktop,
 * but for Mac and MS users it may be much preferable to the java-coded
 * alternatives.
 *
 * @author   Mark Taylor
 * @since    29 Jun 2010
 */
public class SystemTableSaveDialog implements TableSaveDialog {

    private String filename_;
    private String dirname_;

    public String getName() {
        return "System Browser";
    }

    public String getDescription() {
        return "Save table(s) to location chosen using "
             + "system default file browser";
    }

    public Icon getIcon() {
        return SystemBrowser.getSystemBrowserIcon();
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean showSaveDialog( Component parent, final StarTableOutput sto,
                                   ComboBoxModel formatModel,
                                   StarTable[] tables ) {

        /* Construct a FileDialog instance. */
        Dialog dial = parent instanceof Dialog
                    ? (Dialog) parent
                    : (Dialog) SwingUtilities.getAncestorOfClass( Dialog.class,
                                                                  parent );
        Frame frame = parent instanceof Frame
                    ? (Frame) parent
                    : (Frame) SwingUtilities.getAncestorOfClass( Frame.class,
                                                                 parent );
        FileDialog fd = dial != null
                      ? new FileDialog( dial, "Save Table", FileDialog.SAVE )
                      : new FileDialog( frame, "Save Table", FileDialog.SAVE );

        /* Keep displaying the modal dialogue until a file destination
         * has been selected. */
        File ofile = null;
        while ( ofile == null ) {

            /* Restore dialogue state from last invocation, either in this
             * loop or in a previous invocation of this method. */
            fd.setDirectory( dirname_ );
            if ( filename_ != null ) {
                fd.setFile( filename_ );
            }

            /* Post the modal dialogue and wait for some user response. */
            fd.setVisible( true );

            /* Record values selected. */
            String filename = fd.getFile();
            String dirname = fd.getDirectory();
            filename_ = filename;
            if ( dirname != null ) {
                dirname_ = dirname;
            }
            if ( dirname == null ) {
                dirname = "";
            }

            /* Null filename indicates that the user has selected Cancel -
             * return false. */
            if ( filename == null || filename.trim().length() == 0 ) {
                return false;
            }

            /* Check file is writable. */
            File file = new File( dirname, filename );
            if ( file.isDirectory() ) {
                JOptionPane.showMessageDialog( parent,
                                               "Selected file is a directory",
                                               "Save Error",
                                               JOptionPane.ERROR_MESSAGE );
            }

            /* Seek confirmation for overwrite. */
            else {
                if ( ! file.exists() ||
                     FilestoreTableSaveDialog
                    .confirmOverwrite( parent, file.toString() ) ) {
                     ofile = new File( dirname, filename );
                }
            }
        }

        /* Initiate a save in the file we have acquired. */
        final String location = ofile.toString();
        final String format = (String) formatModel.getSelectedItem();
        new SaveWorker( parent, tables, ofile.toString() ) {
            protected void attemptSave( StarTable[] tables )
                    throws IOException {
                sto.writeStarTables( tables, location, format );
            }
            protected void done( boolean success ) {
            }
        }.invoke();
        return true;
    }
}
