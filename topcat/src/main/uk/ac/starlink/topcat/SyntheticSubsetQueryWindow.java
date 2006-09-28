package uk.ac.starlink.topcat;

import java.awt.Component;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * A dialogue window which queries the user for the characteristics of
 * a new Row Subset and then appends it to the subsets list.
 */
public class SyntheticSubsetQueryWindow extends AbstractSubsetQueryWindow {

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tcModel      model containing the table data
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public SyntheticSubsetQueryWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, parent, "Define Row Subset" );
        LabelledComponentStack stack = getStack();
        stack.addLine( "Subset Name", getNameField() );
        stack.addLine( "Expression", getExpressionField() );
    }
}
