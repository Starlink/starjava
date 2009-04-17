package uk.ac.starlink.topcat.join;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

/**
 * Panel for selecting between Best and All matches for a pairwise crossmatch.
 *
 * @author   Mark Taylor
 * @since    17 Apr 2009
 */
public class BestSelector extends Box {

    private final JRadioButton bestButton_;
    private final JRadioButton allButton_;

    /**
     * Constructor.
     */
    BestSelector() {
        super( BoxLayout.X_AXIS );
        bestButton_ = new JRadioButton( "Best Match Only" );
        allButton_ = new JRadioButton( "All Matches" );
        ButtonGroup grp = new ButtonGroup();
        grp.add( bestButton_ );
        grp.add( allButton_ );
        bestButton_.setSelected( true );

        add( new JLabel( "Match Selection: " ) );
        add( bestButton_ );
        add( allButton_ );
    }

    /**
     * Indicates status.
     *
     * @return  true for best only, false for all
     */
    public boolean isBest() {
        return bestButton_.isSelected();
    }
}
