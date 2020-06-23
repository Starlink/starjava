package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.IOUtils;

/**
 * Provides a dialogue that disposes of a URL by downloading the resource
 * to a location specified interactively by the user.
 *
 * @author   Mark Taylor
 * @since    15 Jun 2018
 */
public abstract class DownloadDialog {

    private final ContentCoding coding_ = ContentCoding.GZIP;

    /**
     * Acquires a destination File for a downloaded resource
     * by interacting with the user.
     * This method is invoked on the Event Dispatch Thread.
     *
     * @return   user-selected destination file,
     *           or null if the operation is cancelled
     */
    public abstract File chooseFileEdt();

    /**
     * Downloads the resource from a given URL to a file selected
     * interactively by the user.
     * This method is not invoked on the Event Dispatch Thread.
     *
     * @param  url  location of resource to download
     * @return   operation outcome
     */
    public Outcome userDownload( URL url ) {

        /* Interact with the user to get a download File destination. */
        final AtomicReference<File> fileRef = new AtomicReference<File>();
        try {
            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                    File file = chooseFileEdt();
                    fileRef.set( file );
                }
            } );
        }
        catch ( InterruptedException e ) {
            return Outcome.failure( "User cancelled download" );
        }
        catch ( InvocationTargetException e ) {
            return Outcome.failure( "User interaction failure: " + e );
        }
        File file = fileRef.get();
        if ( file == null ) {
            return Outcome.failure( "User cancelled download" );
        }

        /* Prepare to retrieve the remote resource. */
        InputStream in;
        try {
            in = coding_.openStreamAuth( url, AuthManager.getInstance() );
        }
        catch ( IOException e ) { 
            return Outcome.failure( "URL access failed: " + e 
                                  + " (" + url + ")" );
        }

        /* Prepare to write to the local file. */
        OutputStream out;
        try {
            out = new FileOutputStream( file );
        }
        catch ( IOException e ) {
            return Outcome.failure( "File creation failed: " + e
                                  + " (" + file + ")" );
        }

        /* Copy remote resource to local file. */
        try {
            IOUtils.copy( in, out );
        }
        catch ( IOException e ) {
            return Outcome.failure( e );
        }
        finally {
            try {
                in.close();
            }
            catch ( IOException e ) {
            }
            try {
                out.close();
            }
            catch ( IOException e ) {
            }
        }
        return Outcome.success( url + " -> " + file );
    }

    /**
     * Returns an instance of this class based on an AWT FileDialog.
     * This provides less consistency, but possibly tighter system interaction.
     *
     * @param   parent   parent component
     * @return   dialog
     */
    public static DownloadDialog createSystemDialog( Component parent ) {
        Window w = parent == null ? null
                                  : SwingUtilities.windowForComponent( parent );
        final Frame parentFrame = w instanceof Frame ? (Frame) w : null;
        return new DownloadDialog() {
            private FileDialog fdialog_;
            public File chooseFileEdt() {
                if ( fdialog_ == null ) {
                    fdialog_ = new FileDialog( parentFrame, "Download Resource",
                                               FileDialog.SAVE );
                }
                fdialog_.setVisible( true );
                String filename = fdialog_.getFile();
                String dirname = fdialog_.getDirectory();
                return filename == null ? null : new File( dirname, filename );
            }
        };
    }

    /**
     * Returns an instance of this class based on a Swing JFileChooser.
     * This provides a GUI that is consistent between different platforms.
     *
     * @param   parent   parent component
     * @return   dialog
     */
    public static DownloadDialog createSwingDialog( final Component parent ) {
        return new DownloadDialog() {
            private JFileChooser chooser_;
            public File chooseFileEdt() {
                if ( chooser_ == null ) {
                    chooser_ = new JFileChooser( new File( "." ) );
                    chooser_.setDialogType( JFileChooser.SAVE_DIALOG );
                    chooser_.setDialogTitle( "Download Resource" );
                    chooser_.setFileSelectionMode( JFileChooser.FILES_ONLY );
                    chooser_.setMultiSelectionEnabled( false );
                }
                int status = chooser_.showSaveDialog( parent );
                return status == JFileChooser.APPROVE_OPTION
                     ? chooser_.getSelectedFile()
                     : null;
            }
        };
    }
}
