package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.connect.Node;
import uk.ac.starlink.connect.FilestoreChooser;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Table load dialogue based on a FilestoreChooser.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class FilestoreTableLoadDialog extends BasicTableLoadDialog {

    private final FilestoreChooser chooser_;
    private final JComboBox formatSelector_;

    public FilestoreTableLoadDialog() {
        super( "Filestore Browser", 
               "Loader for files from local or remote filespace" );
        chooser_ = new FilestoreChooser();
        chooser_.addDefaultBranches();
        add( chooser_, BorderLayout.CENTER );
        formatSelector_ = new JComboBox();
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Table Format: " ) );
        formatBox.add( formatSelector_ );
        formatBox.add( Box.createHorizontalGlue() );
        formatBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        add( formatBox, BorderLayout.SOUTH );
    }

    protected void setFormatModel( ComboBoxModel formatModel ) {
        formatSelector_.setModel( formatModel );
    }

    protected TableSupplier getTableSupplier() {
        Node node = chooser_.getSelectedNode();
        if ( node instanceof Leaf ) {
            final Leaf leaf = (Leaf) node;
            return new TableSupplier() {

                public StarTable getTable( StarTableFactory factory,
                                           String format )
                        throws IOException {
                    return factory.makeStarTable( leaf.getDataSource(),
                                                  format );
                }

                public String getTableID() {
                    return leaf.toString();
                }
            };
        }
        else {
            throw new IllegalArgumentException( "No file selected" );
        }
    }

    public boolean isAvailable() {
        return true;
    }

    public FilestoreChooser getChooser() {
        return chooser_;
    }

}
