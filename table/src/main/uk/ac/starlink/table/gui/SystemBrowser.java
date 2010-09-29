package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Uses the native system file dialogue to provide a TableLoader.
 * This is a wrapper around {@link java.awt.FileDialog}, which is in turn
 * a thin wrapper around whatever the native platform uses for its
 * default file browser.  Looks ugly on my fvwm-based Linux desktop,
 * but for Mac and MS users it may be much preferable to the java-coded
 * alternatives.
 *
 * <p>An instance of this class retains state (current directory) between
 * invocations of its sole method.
 *
 * @author   Mark Taylor
 * @since    23 Sept 2010
 */
public class SystemBrowser {

    private String filename_;
    private String dirname_;
    private static Icon icon_;

    /**
     * Returns a TableLoader based on the file selected by a user.
     * If the user hits the Cancel button (or equivalent), null is returned.
     *
     * @param  parent  parent component
     * @param  format  table load format
     */
    public TableLoader showLoadDialog( Component parent, final String format ) {

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
                      ? new FileDialog( dial, "Load Table", FileDialog.LOAD )
                      : new FileDialog( frame, "Load Table", FileDialog.LOAD );

        /* Keep displaying the modal dialogue until a readable file has been
         * selected. */
        FileDataSource datsrc = null;
        while ( datsrc == null ) {

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

            /* Null filename return indicates that the user has
             * selected Cancel - return false. */
            if ( filename == null || filename.trim().length() == 0 ) {
                return null;
            }

            /* Get a data source from the selection; this also checks that
             * it is readable. */
            try {
                datsrc = new FileDataSource( new File( dirname, filename ) );
            }
            catch ( IOException e ) {
                ErrorDialog.showError( parent, "Load Error", e,
                                       "Nonexistent or unreadable file "
                                     + filename );
                datsrc = null;
            }
        }

        /* Return a TableLoader based on the selected file. */
        assert datsrc != null;
        final FileDataSource datsrc1 = datsrc;
        return new TableLoader() {
            public String getLabel() {
                return datsrc1.getFile().toString();
            }
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                return tfact.makeStarTables( datsrc1, format );
            }
        };
    }

    /**
     * Returns an icon which represents system browsing.
     *
     * @return   icon
     */
    public static Icon getSystemBrowserIcon() {
        if ( icon_ == null ) {
            icon_ = new ImageIcon( StarTable.class
                                  .getResource( "gui/sysbrowser.gif" ) );
        }
        return icon_;
    }
}
