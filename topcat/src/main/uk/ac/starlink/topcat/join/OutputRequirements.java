package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
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

    private final JComponent rowBox;
    private final JToggleButton useCols;
    private final ButtonGroup rowGrp;
    private MatchOption rowOption;

    /**
     * Constructs a new OutputRequirements object.
     */
    public OutputRequirements() {

        /* Set up the component which holds the column requirement controls. */
        useCols = new JCheckBox();
        useCols.setSelected( true );

        /* Set up the component which holds the row requirement controls. */
        rowBox = Box.createHorizontalBox();
        rowGrp = new ButtonGroup();
        MatchOption[] options = new MatchOption[] { 
            MatchOption.MATCHED, MatchOption.ANY };
        for ( int i = 0; i < options.length; i++ ) {
            JRadioButton butt =
                new JRadioButton( new OptionAction( options[ i ] ) );
            if ( i == 0 ) {
                butt.doClick();
            }
            rowGrp.add( butt );
            rowBox.add( butt );
        }
    }

    /**
     * Returns a component (suitable for vertical stacking) which allows
     * the user to select which rows will be included in the output.
     *
     * @return  selection window
     */
    public JComponent getRowLine() {
        return rowBox;
    }

    /**
     * Returns a button which allows the user to select which columns
     * will be included in the output.
     *
     * @return  selection button
     */
    public JToggleButton getUseCols() {
        return useCols;
    }

    /**
     * Returns the selected MatchOption for this object
     *
     * @return  match option
     */
    public MatchOption getRowOption() {
        return rowOption;
    }

    /**
     * Returns the selected join type for this object.
     *
     * @return  join type
     */
    public MultiJoinType getJoinType() {
        return rowOption.getJoinType();
    }

    private class OptionAction extends AbstractAction {
        MatchOption option;
        OptionAction( MatchOption opt ) {
            super( opt.toString() );
            this.option = opt;
        }
        public void actionPerformed( ActionEvent evt ) {
            OutputRequirements.this.rowOption = option;
            if ( option == MatchOption.UNMATCHED ) {
                useCols.setEnabled( false );
                useCols.setSelected( false );
            }
            else {
                useCols.setEnabled( true );
            }
        }
    }
}
