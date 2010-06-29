package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.FileDialog;
import java.io.File;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.ComboBoxModel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Table load dialogue based on the native system file dialogue.
 * This is a wrapper around {@link java.awt.FileDialog}, which is in turn
 * a thin wrapper around whatever the native platform uses for its
 * default file browser.  Looks ugly on my fvwm-based Linux desktop,
 * but for Mac and MS users it may be much preferable to the java-coded
 * alternatives.
 *
 * @author   Mark Taylor
 * @since    29 Jun 2010
 */
public class SystemTableLoadDialog implements TableLoadDialog {

    private static Icon icon_;
    private String filename_;
    private String dirname_;

    public String getName() {
        return "System File Browser";
    }

    public String getDescription() {
        return "Load table from files using system-native file browser";
    }

    public Icon getIcon() {
        return getSystemBrowserIcon();
    }

    public boolean isAvailable() {
        return true;
    }

    public boolean showLoadDialog( final Component parent,
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   final TableConsumer consumer ) {

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
        FileDataSource filedatsrc = null;
        while ( filedatsrc == null ) {

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
                return false;
            }

            /* Get a data source from the selection; this also checks that
             * it is readable. */
            try {
                filedatsrc =
                    new FileDataSource( new File( dirname, filename ) );
            }
            catch ( IOException e ) {
                ErrorDialog.showError( parent, "Load Error", e,
                                       "Nonexistent or unreadable file "
                                     + filename );
            }
        }

        /* Attempt to load table(s) from the file selected and pass them to
         * the table consumer on the event dispatch thread as appropriate. */
        final String fname = filedatsrc.getFile().getName();
        final DataSource datsrc = filedatsrc;
        Object formatItem = formatModel.getSelectedItem();
        final String format = formatItem == null ? null : formatItem.toString();
        new Thread( "System Table Loader" ) {
            public void run() {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        consumer.loadStarted( fname );
                    }
                } );
                StarTable[] tables = new StarTable[ 0 ];
                try {
                    tables = factory.makeStarTables( datsrc, format );
                    if ( tables.length == 0 ) {
                        throw new IOException( "No tables in file" );
                    }
                    else {
                        final StarTable table = tables[ 0 ];
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                consumer.loadSucceeded( table );
                            }
                        } );
                    }
                }
                catch ( final IOException e ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            consumer.loadFailed( e );
                        }
                    } );
                }
                for ( int i = 1; i < tables.length; i++ ) {
                    final String id = fname + "-" + ( i + 1 );
                    final StarTable table = tables[ i ];
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            consumer.loadStarted( id );
                            consumer.loadSucceeded( table );
                        }
                    } );
                }
            }
        }.start();
        return true;
    }

    /**
     * Returns the icon used for this load dialogue.
     *
     * @return  system browser icon
     */
    public static Icon getSystemBrowserIcon() {
        if ( icon_ == null ) {
            icon_ = new ImageIcon( SystemTableLoadDialog.class
                                  .getResource( "sysbrowser.gif" ) );
        }
        return icon_;
    }
}
