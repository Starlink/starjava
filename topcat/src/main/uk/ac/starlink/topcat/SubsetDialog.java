package uk.ac.starlink.topcat;

import java.awt.Component;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.util.ErrorDialog;

/**
 * Widget to ask the user for a new row subset to define.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SubsetDialog extends JOptionPane {

    private StarTable stable;
    private List subsets;
    private JTextField nameField;
    private JTextField exprField;

    public SubsetDialog( StarTable stable, List subsets ) {
        this.stable = stable;
        this.subsets = subsets;

        /* JOptionPane configuration. */
        setOptionType( OK_CANCEL_OPTION );
        JPanel panel = new JPanel();
        panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
        setMessage( panel );

        /* Add a table containing the columns. */
        panel.add( new JLabel( "Table columns", SwingConstants.LEFT ) );
        panel.add( new ColumnsDisplay( stable ) );

        /* Add a table containing the existing subsets. */
        panel.add( new JLabel( "Existing subsets", SwingConstants.LEFT ) );
        panel.add( new SubsetsDisplay( subsets ) );

        /* Add the components for user interaction. */
        LabelledComponentStack stack = new LabelledComponentStack();
        panel.add( stack );
        nameField = new JTextField( 24 );
        exprField = new JTextField( 24 );
        stack.addLine( "Subset name:", nameField );
        stack.addLine( "Subset expression", exprField );
    }

    /**
     * Returns the name that the user has entered.
     *
     * @return  new subset name
     */
    public String getName() {
        return nameField.getText();
    }

    /**
     * Returns the expression that the user has entered.
     *
     * @return new subset expression
     */
    public String getExpression() {
        return exprField.getText();
    }

    /**
     * Pops up a modal dialog box and asks the user for the relevant
     * information to create a new subset.  If there is an error the
     * user is shown the error message and given another chance.
     *
     * @param  parent  the parent component - used for window positioning
     * @return  a new RowSubset constructed from the user's responses,
     *          or <tt>null</tt> if the user bailed out
     */
    public RowSubset obtainSubset( Component parent ) {
        JDialog dialog = createDialog( parent, "Create new subset" );
        while ( true ) {
            dialog.show();
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                String name = getName();
                String expr = getExpression();
                if ( name.trim().length() == 0 ) {
                    reportError( dialog, "Name field not filled in" );
                }
                else if ( expr.trim().length() == 0 ) {
                    reportError( dialog, "Expression field not filled in" );
                }
                else {
                    try {
                        RowSubset rset = 
                            new SyntheticRowSubset( stable, subsets, name, 
                                                    expr );
                        dialog.dispose();
                        return rset;
                    }
                    catch ( Exception e ) {
                        ErrorDialog.showError( e, "Can't make sense of " + expr,
                                               dialog );
                    }
                }
            }
            else {
                dialog.dispose();
                return null;
            }
        }
    }

    private static void reportError( Component parent, String message ) {
        showMessageDialog( parent, message, "Bad subset definition",
                           ERROR_MESSAGE );
    }
}
