package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Table load dialogue based on a FileChooser.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public class FileChooserTableLoadDialog extends AbstractTableLoadDialog {

    private JFileChooser chooser_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public FileChooserTableLoadDialog() {
        super( "File Browser",
               "Load tables from files on the local filesystem" );
        setIconUrl( StarTable.class.getResource( "gui/filechooser.png" ) );
    }

    protected Component createQueryComponent() {
        chooser_ = new JFileChooser();

        /* Customise chooser. */
        chooser_.setFileSelectionMode( JFileChooser.FILES_ONLY );
        chooser_.setMultiSelectionEnabled( false );
        chooser_.setCurrentDirectory( new File( "." ) );
        chooser_.setControlButtonsAreShown( false );

        /* Place format selector. */
        JComponent labelLine = Box.createHorizontalBox();
        labelLine.add( new JLabel( "Format: " ) );
        labelLine.add( Box.createHorizontalGlue() );
        JComponent selectorLine = Box.createHorizontalBox();
        selectorLine.add( new ShrinkWrapper( createFormatSelector() ) );
        selectorLine.add( Box.createHorizontalGlue() );
        JComponent formatBox = Box.createVerticalBox();
        formatBox.add( Box.createVerticalGlue() );
        formatBox.add( labelLine );
        formatBox.add( selectorLine );
        formatBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 0 ) );
        chooser_.setAccessory( formatBox );

        /* Fix it so that actions which indicate selection in the chooser
         * cause a submit action in this dialogue. */
        chooser_.addActionListener( getSubmitAction() );

        /* Ensure a refresh whenever the component is made visible. */
        chooser_.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {
                chooser_.rescanCurrentDirectory();
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
        } );
        return chooser_;
    }

    public TableLoader createTableLoader() {
        final File file = chooser_.getSelectedFile();
        if ( file == null ) {
            return null;
        }
        final String format = getSelectedFormat();
        return new TableLoader() {
            public String getLabel() {
                return file.getName();
            }
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                return tfact.makeStarTables( new FileDataSource( file ),
                                             format );
            }
        };
    }

    public boolean isAvailable() {
        return true;
    }
}
