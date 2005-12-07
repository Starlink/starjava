package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.connect.Node;
import uk.ac.starlink.connect.FilestoreChooser;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Table load dialogue based on a FilestoreChooser.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class FilestoreTableLoadDialog extends BasicTableLoadDialog {

    private final FilestoreChooser chooser_;
    private final JComboBox formatSelector_;
    private final JTextField posField_;

    public FilestoreTableLoadDialog() {
        super( "Filestore Browser", 
               "Loader for files from local or remote filespace" );
        final FilestoreTableLoadDialog tld = this;
        chooser_ = new FilestoreChooser() {
            public void leafSelected( Leaf leaf ) {
                tld.getOkAction()
                   .actionPerformed( new ActionEvent( tld, 0, "OK" ) );
            }
        };
        chooser_.addDefaultBranches();
        add( chooser_, BorderLayout.CENTER );
        formatSelector_ = new JComboBox();
        JLabel posLabel = new JLabel( "Position in file: #" );
        posField_ = new JTextField( 6 );
        posField_.addActionListener( getOkAction() );
        String posHelp = "HDU index for FITS files or TABLE index for VOTables"
                       + " (optional)";
        posField_.setToolTipText( posHelp );
        posLabel.setToolTipText( posHelp );
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Table Format: " ) );
        formatBox.add( new ShrinkWrapper( formatSelector_ ) );
        formatBox.add( Box.createHorizontalStrut( 10 ) );
        formatBox.add( Box.createHorizontalGlue() );
        formatBox.add( new JLabel( "Position in file: #" ) );
        formatBox.add( new ShrinkWrapper( posField_ ) );
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
            String posText = posField_.getText();
            final String pos = posText != null && posText.trim().length() > 0
                             ? posText.trim()
                             : null;
            return new TableSupplier() {

                public StarTable getTable( StarTableFactory factory,
                                           String format )
                        throws IOException {
                    DataSource datsrc = leaf.getDataSource();
                    if ( pos != null ) {
                        datsrc.setPosition( pos );
                    }
                    return factory.makeStarTable( datsrc, format );
                }

                public String getTableID() {
                    return leaf.toString()
                         + ( pos != null ? ( "#" + pos ) : "" );
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

    public void setEnabled( boolean enabled ) {
        if ( enabled != isEnabled() ) {
            chooser_.setEnabled( enabled );
            formatSelector_.setEnabled( enabled );
        }
        super.setEnabled( enabled );
    }

    public boolean showLoadDialog( Component parent, StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer consumer ) {
        chooser_.refreshList();
        return super.showLoadDialog( parent, factory, formatModel, consumer );
    }

}
