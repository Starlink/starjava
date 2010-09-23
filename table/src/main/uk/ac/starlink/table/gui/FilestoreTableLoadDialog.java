package uk.ac.starlink.table.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.connect.Node;
import uk.ac.starlink.connect.FilestoreChooser;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Load dialogue based on a FilestoreChooser.
 *
 * @author   Mark Taylor 
 * @since    13 Sept 2010
 */
public class FilestoreTableLoadDialog extends AbstractTableLoadDialog {

    private FilestoreChooser chooser_;
    private JTextField posField_;

    /**
     * Constructor.
     */
    public FilestoreTableLoadDialog() {
        super( "Filestore Browser",
               "Loader for files from local or remote filespace" );
        setIconUrl( StarTable.class.getResource( "gui/filestore.gif" ) );
    }

    protected Component createQueryComponent() {
        JComponent panel = new JPanel( new BorderLayout() );
        chooser_ = new FilestoreChooser( false ) {
            public void leafSelected( Leaf leaf ) {
                submit();
            }
        };
        chooser_.addDefaultBranches();
        Action[] navActs = chooser_.getNavigationActions();
        setToolbarActions( navActs );
        JMenu navMenu = new JMenu( "Navigation" );
        navMenu.setMnemonic( KeyEvent.VK_N );
        for ( int i = 0; i < navActs.length; i++ ) {
            navMenu.add( navActs[ i ] );
        }
        setMenus( new JMenu[] { navMenu } );
        panel.add( chooser_, BorderLayout.CENTER );
        JLabel posLabel = new JLabel( "Position in file: #" );
        posField_ = new JTextField( 6 );
        posField_.addActionListener( getSubmitAction() );
        String posHelp = "HDU index for FITS files or TABLE index for VOTables"
                       + " (optional)";
        posField_.setToolTipText( posHelp );
        posLabel.setToolTipText( posHelp );
        JComponent formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Table Format: " ) );
        formatBox.add( new ShrinkWrapper( createFormatSelector() ) );
        formatBox.add( Box.createHorizontalStrut( 10 ) );
        formatBox.add( Box.createHorizontalGlue() );
        formatBox.add( new JLabel( "Position in file: #" ) );
        formatBox.add( new ShrinkWrapper( posField_ ) );
        formatBox.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        panel.add( formatBox, BorderLayout.SOUTH );
        return panel;
    }

    public TableLoader createTableLoader() {
        Node node = chooser_.getSelectedNode();
        if ( ! ( node instanceof Leaf ) ) {
            throw new IllegalStateException( "No table selected" );
        }
        final Leaf leaf = (Leaf) node;
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( node.getName() );
        String posText = posField_.getText();
        final String pos = posText != null && posText.trim().length() > 0
                         ? posText.trim()
                         : null;
        if ( pos != null ) {
            sbuf.append( '#' )
                .append( pos );
        }
        final String format = getSelectedFormat();
        final String label = sbuf.toString();
        return new TableLoader() {
            public String getLabel() {
                return label;
            }
            public StarTable[] loadTables( StarTableFactory tfact )
                    throws IOException {
                DataSource datsrc = leaf.getDataSource();
                if ( pos != null ) {
                    datsrc.setPosition( pos );
                    return new StarTable[] {
                        tfact.makeStarTable( datsrc, format ),
                    };
                }
                else {
                    return tfact.makeStarTables( datsrc, format );
                }
            }
        };
    }

    /**
     * Returns the filestore chooser used by this dialogue.
     *
     * @return  chooser
     */
    public FilestoreChooser getChooser() {
        return chooser_;
    }
}
