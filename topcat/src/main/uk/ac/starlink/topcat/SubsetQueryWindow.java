package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * A dialogue window which obtains information to define a new Row Subset
 * and then appends it to the subsets list.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2006
 */
public class SubsetQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private final JComboBox<String> nameSelector_;
    private final JTextField exprField_;

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tcModel      model containing the table data
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     * @param   title        window title
     */
    protected SubsetQueryWindow( TopcatModel tcModel, Component parent,
                                 String title ) {
        super( title, parent );
        tcModel_ = tcModel;
        nameSelector_ = tcModel.createNewSubsetNameSelector();
        exprField_ = new JTextField();

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this, false ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "SyntheticSubsetQueryWindow" );
    }

    /**
     * Returns the component with which the user selects the name of the
     * new subset.
     *
     * @return  name field
     */
    public JComponent getNameField() {
        return nameSelector_;
    }

    /**
     * Sets the name of the RowSubset which the action of this window will
     * be to create (or replace).
     *
     * @param   name   subset name
     */
    public void setSelectedName( String name ) {
        nameSelector_.setSelectedItem( name );
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

    /**
     * Attempts to construct a RowSubset based on the state of this window.
     * If the state does not describe a subset, the user is informed
     * via a JOptionPane popup and null is returned.
     *
     * @return   new subset, or null
     */
    protected SyntheticRowSubset createSubset() {

        /* Get the name object and the expression, checking that neither is
         * a null value. */
        Object selected = nameSelector_.getSelectedItem();
        if ( selected == null || selected.toString().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "No subset name entered",
                                           "Missing Name Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
        String expr = getExpressionField().getText();
        if ( expr == null || expr.trim().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "No expression entered",
                                           "Missing Expression Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
        String name = selected.toString();

        /* Try to construct a synthetic row subset as requested. */
        if ( TopcatJELUtils.isSubsetReferenced( tcModel_, name, expr ) ) {
            String[] msg = new String[] {
                "Recursive subset expression disallowed:",
                "\"" + expr + "\"" +
                " directly or indirectly references subset " + name,
            };
            JOptionPane.showMessageDialog( this, msg, "Expression Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
        else {
            try {
                return new SyntheticRowSubset( name, tcModel_, expr );
            }
            catch ( CompilationException e ) {
                String[] msg = new String[] {
                    "Syntax error in algebraic subset expression" 
                    + " \"" + expr + "\":",
                    e.getMessage(),
                };
                JOptionPane.showMessageDialog( this, msg,
                                               "Expression Syntax Error",
                                               JOptionPane.ERROR_MESSAGE );
                return null;
            }
        }
    }

    protected boolean perform() {
        SyntheticRowSubset exprSubset = createSubset();
        if ( exprSubset != null ) {
            tcModel_.addSubset( exprSubset );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Constructs a new query window, which on user completion will
     * try to construct a new synthetic RowSubset and add it to the list.
     *
     * @param   tcModel      model containing the table data
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public static SubsetQueryWindow
           newSubsetDialog( TopcatModel tcModel, Component parent ) {
        SubsetQueryWindow qwin =
            new SubsetQueryWindow( tcModel, parent, "Define Row Subset" );
        LabelledComponentStack stack = qwin.getStack();
        stack.addLine( "Subset Name", qwin.getNameField() );
        stack.addLine( "Expression", qwin.getExpressionField() );
        return qwin;
    }
}
