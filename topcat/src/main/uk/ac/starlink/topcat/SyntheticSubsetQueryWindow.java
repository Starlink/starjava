package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * A dialogue window which queries the user for the characteristics of
 * a new Row Subset and then appends it to the subsets list.
 */
public class SyntheticSubsetQueryWindow extends QueryWindow {

    private final TopcatModel tcModel;
    private final OptionsListModel subsets;
    private final PlasticStarTable dataModel;
    private JTextField nameField;
    private JTextField exprField;

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tcModel      model containing the table data
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public SyntheticSubsetQueryWindow( TopcatModel tcModel, Component parent ) {
        super( "Define Row Subset", parent );
        this.tcModel = tcModel;
        this.subsets = tcModel.getSubsets();
        this.dataModel = tcModel.getDataModel();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField = new JTextField();
        stack.addLine( "Subset Name", nameField );

        /* Expression field. */
        exprField = new JTextField();
        stack.addLine( "Expression", exprField );

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "SyntheticSubsetQueryWindow" );

        /* Show the window. */
        pack();
        setVisible( true );
    }

    protected boolean perform() {
        String name = nameField.getText();
        String expr = exprField.getText();
        try {
            RowSubset rset = new SyntheticRowSubset( dataModel, subsets,
                                                     name, expr );
            subsets.add( rset );
            return true;
        }
        catch ( CompilationException e ) {
            String[] msg = new String[] {
                "Syntax error in algebraic subset expression \"" + expr + "\":",
                e.getMessage(),
            };
            JOptionPane.showMessageDialog( this, msg, "Expression Syntax Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }
    }
      
}
