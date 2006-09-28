package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * A dialogue window which obtains information to define a new Row Subset
 * and then appends it to the subsets list.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2006
 */
public abstract class AbstractSubsetQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private JTextField nameField_;
    private JTextField exprField_;

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tcModel      model containing the table data
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     * @param   title        window title
     */
    public AbstractSubsetQueryWindow( TopcatModel tcModel, Component parent,
                                      String title ) {
        super( title, parent );
        tcModel_ = tcModel;
        nameField_ = new JTextField();
        exprField_ = new JTextField();

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this, false ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "SyntheticSubsetQueryWindow" );
    }

    /**
     * Returns the text component used to store the name of the new subset.
     *
     * @return  name field
     */
    public JTextField getNameField() {
        return nameField_;
    }

    /**
     * Returns the text component used to store the algebraic expression for
     * the new subset.
     *
     * @return  expression field
     */
    public JTextField getExpressionField() {
        return exprField_;
    }

    protected boolean perform() {
        String name = getNameField().getText();
        String expr = getExpressionField().getText();
        OptionsListModel subsets = tcModel_.getSubsets();
        PlasticStarTable dataModel = tcModel_.getDataModel();
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
