package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
    private final JComboBox nameSelector_;
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
    public AbstractSubsetQueryWindow( TopcatModel tcModel, Component parent,
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
        for ( int i = 0; i < nameSelector_.getItemCount(); i++ ) {
            Object item = nameSelector_.getItemAt( i );
            RowSubset rset = (RowSubset) item;
            if ( rset.getName().equals( name ) ) {
                nameSelector_.setSelectedItem( rset );
                return;
            }
        }
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

    protected boolean perform() {

        /* Get the name object and the expression, checking that neither is
         * a null value. */
        Object selected = nameSelector_.getSelectedItem();
        if ( selected == null || selected.toString().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "No subset name entered",
                                           "Missing Name Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }
        String expr = getExpressionField().getText();
        if ( expr == null || expr.trim().length() == 0 ) {
            JOptionPane.showMessageDialog( this, "No expression entered",
                                           "Missing Expression Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }

        /* The item selected in the name selector will either be a string
         * or an existing RowSubset from the subsets list belonging to 
         * the tcModel.  Get the name as a string either way. */
        assert selected instanceof RowSubset || selected instanceof String;
        String name = selected instanceof RowSubset
                    ? ((RowSubset) selected).getName()
                    : selected.toString();

        /* Construct a new RowSubset with the given name and add it to the
         * model. */
        try {
            tcModel_.addSubset(
                new SyntheticRowSubset( name, expr,
                                        tcModel_.createJELRowReader() ) );
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
