package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.UCDSelector;
import uk.ac.starlink.util.ErrorDialog;

/**
 * A dialogue window which queries the user for the characteristics of a
 * new column and then appends it to the table.
 */
public class SyntheticColumnQueryWindow extends QueryWindow {

    private final TableViewer tv;
    private final PlasticStarTable dataModel;
    private final OptionsListModel subsets;
    private final TableColumnModel columnModel;
    private JTextField nameField;
    private JTextField unitField;
    private JTextField descriptionField;
    private JTextField expressionField;
    private UCDSelector ucdField;
    private ColumnIndexSpinner indexSpinner;

    /**
     * Constructs a new query window, which on user completion will 
     * append a new column to the viewer <tt>tableviewer<tt> at the
     * column index <tt>insertIndex</tt>.
     *
     * @param   tableviewer  the tableviewer
     * @param   insertIndex  the postion for the new column
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public SyntheticColumnQueryWindow( TableViewer tableviewer,
                                       int insertIndex, Component parent ) {
        super( "Define Synthetic Column", parent );
        this.tv = tableviewer;
        this.columnModel = tv.getColumnModel();
        this.dataModel = tv.getDataModel();
        this.subsets = tv.getSubsets();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField = new JTextField( 24 );
        stack.addLine( "Name", nameField );

        /* Units field. */
        unitField = new JTextField( 24 );
        stack.addLine( "Units", unitField );

        /* Description field. */
        descriptionField = new JTextField( 24 );
        stack.addLine( "Description", descriptionField );

        /* Expression field. */
        expressionField = new JTextField( 24 );
        stack.addLine( "Expression", expressionField );

        /* UCD field. */
        ucdField = new UCDSelector();
        stack.addLine( "UCD", ucdField );

        /* Index field. */
        indexSpinner = new ColumnIndexSpinner( columnModel );
        indexSpinner.setColumnIndex( insertIndex );
        stack.addLine( "Index", indexSpinner );

        /* Add help information. */
        addHelp( "SyntheticColumn" );

        /* Show the window. */
        pack();
        setVisible( true );
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
     * Sets the contents of the name field.
     *
     * @param  name new contents of the name field
     */
    public void setName( String name ) {
        nameField.setText( name );
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
     * Sets the contents of the expression field.
     *
     * @param   expr   new contents of the expression field
     */
    public void setExpression( String expr ) {
        expressionField.setText( expr );
    }

    /**
     * Returns the string that the user has chosen for the UCD field.
     *
     * @return  UCD identifier
     */
    public String getUCD() {
        return ucdField.getID();
    }

    /**
     * Sets the index at which the new column should be inserted.
     *
     * @return  index
     */
    public int getIndex() {
        return indexSpinner.getColumnIndex();
    }

    protected boolean perform() {
        String name = getName();
        String desc = getDescription();
        String unit = getUnit();
        String expr = getExpression();
        String ucd = getUCD();
        int index = getIndex();
        DefaultValueInfo info = new DefaultValueInfo( name );
        if ( desc != null ) {
            info.setDescription( desc );
        }
        if ( ucd != null ) {
            info.setUCD( ucd );
        }
        if ( info != null ) {
            info.setUnitString( unit );
        }
        try {
            ColumnData col = new SyntheticColumn( info, dataModel, subsets,
                                                  expr, null );
            tv.appendColumn( col, index );
            return true;
        }
        catch ( Exception e ) {
            ErrorDialog.showError( e, "Bad column definition: " + expr,
                                   this );
            return false;
        }
    }

}
