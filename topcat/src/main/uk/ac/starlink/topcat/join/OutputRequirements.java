package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import uk.ac.starlink.table.join.MultiJoinType;

/**
 * Defines the per-table requirements for type of output in a multi-table
 * match.  This class provides graphical components that the user can
 * interact with to define what the output table should look like.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class OutputRequirements {

    private final JComponent rowBox_;
    private final ButtonGroup rowGrp_;
    private MatchOption rowOption_;

    /**
     * Constructs a new OutputRequirements object.
     */
    public OutputRequirements() {

        /* Set up the component which holds the row requirement controls. */
        rowBox_ = Box.createHorizontalBox();
        rowGrp_ = new ButtonGroup();
        MatchOption[] options = new MatchOption[] { 
            MatchOption.MATCHED, MatchOption.ANY };
        for ( int i = 0; i < options.length; i++ ) {
            final MatchOption opt = options[ i ];
            JRadioButton butt =
                    new JRadioButton( new AbstractAction( opt.toString() ) {
                public void actionPerformed( ActionEvent evt ) {
                    rowOption_ = opt;
                }
            } );
            if ( i == 0 ) {
                butt.doClick();
            }
            rowGrp_.add( butt );
            rowBox_.add( butt );
        }
    }

    /**
     * Returns a component (suitable for vertical stacking) which allows
     * the user to select which rows will be included in the output.
     *
     * @return  selection window
     */
    public JComponent getRowLine() {
        return rowBox_;
    }

    /**
     * Returns the selected MatchOption for this object
     *
     * @return  match option
     */
    public MatchOption getRowOption() {
        return rowOption_;
    }

    /**
     * Returns the selected join type for this object.
     *
     * @return  join type
     */
    public MultiJoinType getJoinType() {
        return rowOption_.getJoinType();
    }
}
