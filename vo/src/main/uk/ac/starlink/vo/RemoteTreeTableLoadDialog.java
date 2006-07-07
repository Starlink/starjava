package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;

/**
 * Table load dialogue implementation based on a {@link RemoteTreeBrowser}.
 * Using this you can implement a table loader dialogue having only a
 * TreeModel representing the remote data.
 *
 * @author   Mark Taylor (Starlink)
 * @since    31 Jan 2005
 */
public abstract class RemoteTreeTableLoadDialog extends BasicTableLoadDialog {

    private final RemoteTreeBrowser browser_;
    private final JComboBox formatComboBox_;

    /**
     * Constructor.
     *
     * @param   name  dialogue name
     * @param   description  dialogue description
     * @param   browser   tree browser holding the remote file system
     */
    public RemoteTreeTableLoadDialog( String name, String description,
                                      RemoteTreeBrowser browser ) {
        super( name, description );
        browser_ = browser;
        add( browser );
        browser.setAllowContainerSelection( false );
        formatComboBox_ = new JComboBox();
        Box formatBox = Box.createHorizontalBox();
        formatBox.add( new JLabel( "Format: " ) );
        formatBox.add( formatComboBox_ );
        formatBox.add( Box.createHorizontalGlue() );
        browser.getExtraPanel().add( formatBox, BorderLayout.WEST );
        browser.getExtraPanel().add( Box.createVerticalStrut( 5 ),
                                    BorderLayout.SOUTH );
    }

    /**
     * Turns a table node into a TableSupplier.
     * This should work with whatever kind of leaf node is present in the
     * tree provided by the RemoteTreeBrowser on which this load dialogue
     * is based.
     * 
     * @param   node  tree node object
     * @return  table supplier appropriate to <tt>node</tt>
     */
    protected abstract TableSupplier makeTableSupplier( Object node );

    protected void setFormatModel( ComboBoxModel formatModel ) {
        formatComboBox_.setModel( formatModel );
    }

    protected TableSupplier getTableSupplier() {
        return makeTableSupplier( browser_.getSelectedNode() );
    }

}
