package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A dialogue window which queries the user for the characteristics of
 * a new Row Subset and then appends it to the subsets list.
 */
public class SyntheticSubsetQueryWindow extends QueryWindow {

    private final TableViewer tv;
    private final OptionsListModel subsets;
    private final PlasticStarTable dataModel;
    private JTextField nameField;
    private JTextField exprField;

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tv           the tableviewer
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public SyntheticSubsetQueryWindow( TableViewer tv, Component parent ) {
        super( "Define Row Subset", parent );
        this.tv = tv;
        this.subsets = tv.getSubsets();
        this.dataModel = tv.getDataModel();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField = new JTextField( 32 );
        stack.addLine( "Subset Name", nameField );

        /* Expression field. */
        exprField = new JTextField( 32 );
        stack.addLine( "Expression", exprField );

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "ExpressionSyntax" );

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
        catch ( Exception e ) {
            ErrorDialog.showError( e, "Bad subset definition: " + expr,
                                   this );
            return false;
        }
    }
      
}
