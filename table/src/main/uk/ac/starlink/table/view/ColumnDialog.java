package uk.ac.starlink.table.view;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarTableModel;

/**
 * Widget to ask the user how he would like to add a new column.
 *
 * @author   Mark Taylor (Starlink)
 */
public class ColumnDialog extends JOptionPane {

    private ExtendedStarTableModel stmodel;
    private JTextField nameField;
    private JTextField unitField;
    private JTextField descriptionField;
    private JTextField expressionField;

    public ColumnDialog( ExtendedStarTableModel stmodel ) {
        this.stmodel = stmodel;

        /* JOptionPane configuration. */
        LabelledComponentStack stack = new LabelledComponentStack();
        setMessage( stack );
        setOptionType( OK_CANCEL_OPTION );

        /* Name field. */
        nameField = new JTextField( 16 );
        stack.addLine( "Column name", nameField );

        /* Units field. */
        unitField = new JTextField( 16 );
        stack.addLine( "Column units", unitField );

        /* Description field. */
        descriptionField = new JTextField( 32 );
        stack.addLine( "Column description", descriptionField );

        /* Expression field. */
        expressionField = new JTextField( 32 );
        stack.addLine( "Expression", expressionField );
    }

    /**
     * Returns the string that the user has entered in the Name field.
     *
     * @return  name
     */
    public String getName() {
        return nameField.getText();
    }

    /**
     * Returns the string that the user has entered in the Units field.
     *
     * @return  units
     */
    public String getUnit() {
        return unitField.getText();
    }

    /**
     * Returns the string that the user has entered in the Description field.
     *
     * @return  description
     */
    public String getDescription() {
        return descriptionField.getText();
    }

    /**
     * Returns the string that the user has entered in the Expression field.
     *
     * @return  expression
     */
    public String getExpression() {
        return expressionField.getText();
    }

    /**
     * Pops up a modal dialog box and asks the user for the relevant
     * information to create a new table column.  If there is an error
     * the user is shown the error message and given another chance.
     *
     * @param  parent  the parent component - used for window positioning
     * @return  a new ColumnData object constructed from the user's responses,
     *          or <tt>null</tt> if the user bailed out
     */
    public ColumnData getColumnDialog( Component parent ) {
        JDialog dialog = createDialog( parent, "Create new column" );
        while ( true ) {
            dialog.show();
            if ( getValue() instanceof Integer &&
                 ((Integer) getValue()).intValue() == OK_OPTION ) {
                DefaultValueInfo info = new DefaultValueInfo( getName() );
                String desc = getDescription();
                String unit = getUnit();
                String expr = getExpression();
                if ( desc != null ) {
                    info.setDescription( desc );
                }
                if ( info != null ) {
                    info.setUnitString( unit );
                }
                try {
                    ColumnData col = 
                        new SyntheticColumn( info, stmodel, expr, null );
                    dialog.dispose();
                    return col;
                }
                catch ( Exception e ) {
                    showMessageDialog( dialog, e.toString(), 
                                       "Bad column definition", ERROR_MESSAGE );
                }
            }
            else {
                dialog.dispose();
                return null;
            }
        }
    }

}
