package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

public class FileChooserLoader extends JFileChooser implements TableLoadDialog {

    private boolean isEnabled_;
    private final JComboBox formatSelector_;
    private final ComboBoxModel dummyModel_;

    public FileChooserLoader() {

        /* See if we have permissions to operate. */
        try {
            SecurityManager manager = System.getSecurityManager();
            if ( manager != null ) {
                manager.checkRead( getCurrentDirectory().toString() );
            }
            isEnabled_ = true;
        }
        catch ( SecurityException e ) {
            isEnabled_ = false;
        }

        /* Customise chooser. */
        setFileSelectionMode( JFileChooser.FILES_ONLY );
        setMultiSelectionEnabled( false );
        setCurrentDirectory( new File( "." ) );

        /* Construct and install format selector. */
        Box formatBox = Box.createVerticalBox();
        Box labelLine = Box.createHorizontalBox();
        labelLine.add( new JLabel( "Format: " ) );
        labelLine.add( Box.createHorizontalGlue() );
        formatBox.add( Box.createVerticalGlue() );
        formatBox.add( labelLine );
        formatBox.add( Box.createVerticalStrut( 5 ) );
        formatSelector_ = new JComboBox();
        dummyModel_ = formatSelector_.getModel();
        Box selectorLine = Box.createHorizontalBox();
        selectorLine.add( formatSelector_ );
        selectorLine.add( Box.createHorizontalGlue() );
        formatBox.add( selectorLine );
        formatBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 0 ) );
        setAccessory( formatBox );
    }

    public String getName() {
        return "Browse Files";
    }

    public String getDescription() {
        return "Load table from files on the local filesystem";
    }

    public boolean isEnabled() {
        return isEnabled_;
    }

    public StarTable loadTableDialog( Component parent,
                                      StarTableFactory factory,
                                      ComboBoxModel formatModel ) {
        formatSelector_.setModel( formatModel );
        formatSelector_.setMaximumSize( formatSelector_.getPreferredSize() );
        StarTable table = null;
        while ( showOpenDialog( parent ) == APPROVE_OPTION ) {
            File file = getSelectedFile();
            if ( file != null ) {
                try {
                    DataSource datsrc = new FileDataSource( file );
                    String format = (String) formatModel.getSelectedItem();
                    table = StarTableChooser
                           .attemptMakeTable( parent, factory, format, datsrc );
                    if ( table != null ) {
                        return table;
                    }
                }
                catch ( IOException e ) {
                    Object msg = new String[] { 
                        "Can't open file \"" + file + "\"", e.getMessage() };
                    JOptionPane.showMessageDialog( parent, msg,
                                                   "Table Load Error",
                                                   JOptionPane.ERROR_MESSAGE );
                }
            }
        }
        return table;
    }
}
