package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Load dialog based on a normal file chooser component.
 * 
 * @author   Mark Taylor
 * @since    1 Dec 2004
 */
public class FileChooserLoader extends JFileChooser implements TableLoadDialog {

    private final boolean isAvailable_;
    private final JComboBox formatSelector_;
    private final ComboBoxModel dummyModel_;
    private JFileChooser fileChooser_;
    private static Icon icon_;

    /**
     * Constructor.
     */
    public FileChooserLoader() {

        /* See if we have permissions to operate. */
        boolean isAvailable;
        try {
            SecurityManager manager = System.getSecurityManager();
            if ( manager != null ) {
                manager.checkRead( getCurrentDirectory().toString() );
            }
            isAvailable = true;
        }
        catch ( SecurityException e ) {
            isAvailable = false;
        }
        isAvailable_ = isAvailable;

        /* Set up some components. */
        formatSelector_ = new JComboBox();
        dummyModel_ = formatSelector_.getModel();
    }

    public String getName() {
        return "File Browser";
    }

    public String getDescription() {
        return "Load table from files on the local filesystem";
    }

    public Icon getIcon() {
        return getFileChooserIcon();
    }

    public boolean isAvailable() {
        return isAvailable_;
    }

    public boolean showLoadDialog( Component parent, 
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        formatSelector_.setModel( formatModel );
        formatSelector_.setMaximumSize( formatSelector_.getPreferredSize() );
        JFileChooser chooser = getFileChooser();
        while ( chooser.showOpenDialog( parent ) == APPROVE_OPTION ) {
            File file = chooser.getSelectedFile();
            if ( file != null ) {
                try {
                    final DataSource datsrc = new FileDataSource( file );
                    final String format = (String) 
                                          formatModel.getSelectedItem();
                    new LoadWorker( eater, file.toString() ) {
                        protected StarTable attemptLoad() throws IOException {
                            return factory.makeStarTable( datsrc, format );
                        }
                    }.invoke();
                    return true;
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( parent, "Load Error", e,
                                           "Can't open file " + file );
                }
            }
        }
        return false;
    }

    /**
     * Returns a lazily-constructed file chooser for use with this dialogue.
     *
     * @return  chooser
     */
    private JFileChooser getFileChooser() {
        if ( fileChooser_ == null ) {
            fileChooser_ = new JFileChooser();

            /* Customise chooser. */
            fileChooser_.setFileSelectionMode( JFileChooser.FILES_ONLY );
            fileChooser_.setMultiSelectionEnabled( false );
            fileChooser_.setCurrentDirectory( new File( "." ) );

            /* Place format selector. */
            Box formatBox = Box.createVerticalBox();
            Box labelLine = Box.createHorizontalBox();
            labelLine.add( new JLabel( "Format: " ) );
            labelLine.add( Box.createHorizontalGlue() );
            formatBox.add( Box.createVerticalGlue() );
            formatBox.add( labelLine );
            formatBox.add( Box.createVerticalStrut( 5 ) );
            Box selectorLine = Box.createHorizontalBox();
            selectorLine.add( formatSelector_ );
            selectorLine.add( Box.createHorizontalGlue() );
            formatBox.add( selectorLine );
            formatBox.setBorder( BorderFactory
                                .createEmptyBorder( 0, 5, 0, 0 ) );
            fileChooser_.setAccessory( formatBox );
        }
        return fileChooser_;
    }

    /**
     * Returns the icon for this loader.
     *
     * @return  dialogue icon
     */
    public static Icon getFileChooserIcon() {
        if ( icon_ == null ) {
            icon_ = new ImageIcon( FileChooserLoader.class
                                  .getResource( "filechooser.gif" ) );
        }
        return icon_;
    }
}
