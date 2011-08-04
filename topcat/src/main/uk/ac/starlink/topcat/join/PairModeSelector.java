package uk.ac.starlink.topcat.join;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import uk.ac.starlink.table.join.RowMatcher;

/**
 * Panel for selecting matching mode for a pairwise crossmatch.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class PairModeSelector extends Box {

    private final JLabel label_;
    private final JComboBox comboBox_;

    /**
     * Constructor.
     */
    public PairModeSelector() {
        super( BoxLayout.X_AXIS );
        comboBox_ = new JComboBox( RowMatcher.PairMode.values() );
        comboBox_.setRenderer( new DefaultListCellRenderer() {
            public Component getListCellRendererComponent( JList list,
                                                           Object value, int ix,
                                                           boolean isSel,
                                                           boolean hasFocus ) {
                Component c =
                    super.getListCellRendererComponent( list, value, ix, isSel,
                                                        hasFocus );
                if ( value instanceof RowMatcher.PairMode ) {
                    setToolTipText( ((RowMatcher.PairMode) value)
                                   .getSummary() );
                }
                return c;
            }
        } );
        comboBox_.setSelectedItem( RowMatcher.PairMode.BEST );
        label_ = new JLabel( "Match Selection: " );
        add( label_ );
        add( comboBox_ );
    }

    /**
     * Returns matching mode.
     *
     * @return  mode
     */
    public RowMatcher.PairMode getMode() {
        return (RowMatcher.PairMode) comboBox_.getSelectedItem();
    }

    @Override
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        label_.setEnabled( enabled );
        comboBox_.setEnabled( enabled );
    }
}
