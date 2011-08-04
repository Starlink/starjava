package uk.ac.starlink.topcat.join;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import uk.ac.starlink.table.join.RowMatcher;

/**
 * Panel for selecting matching mode for a pairwise crossmatch.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class PairModeSelector extends Box {

    private final JLabel label_;
    private final JRadioButton bestButton_;
    private final JRadioButton allButton_;

    /**
     * Constructor.
     */
    public PairModeSelector() {
        super( BoxLayout.X_AXIS );
        bestButton_ = new JRadioButton( "Best Match Only" );
        allButton_ = new JRadioButton( "All Matches" );
        ButtonGroup grp = new ButtonGroup();
        grp.add( bestButton_ );
        grp.add( allButton_ );
        bestButton_.setSelected( true );
        label_ = new JLabel( "Match Selection: " );

        add( label_ );
        add( bestButton_ );
        add( allButton_ );
    }

    /**
     * Returns matching mode.
     *
     * @return mode
     */
    public RowMatcher.PairMode getMode() {
        return bestButton_.isSelected() ? RowMatcher.PairMode.BEST
                                        : RowMatcher.PairMode.ALL;
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        label_.setEnabled( enabled );
        bestButton_.setEnabled( enabled );
        allButton_.setEnabled( enabled );
    }
}
