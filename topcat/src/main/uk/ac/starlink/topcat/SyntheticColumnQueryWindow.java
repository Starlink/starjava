package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.UCDSelector;

/**
 * A dialogue window which queries the user for the characteristics of a
 * new column and then appends it to the table.
 */
public class SyntheticColumnQueryWindow extends QueryWindow {

    private final TopcatModel tcModel;
    private final PlasticStarTable dataModel;
    private final OptionsListModel subsets;
    private final TableColumnModel columnModel;
    private JTextField nameField;
    private JTextField unitField;
    private JTextField descriptionField;
    private JTextField expressionField;
    private JComboBox typeField;
    private UCDSelector ucdField;
    private ColumnIndexSpinner indexSpinner;

    /**
     * Constructs a new query window, which on user completion will 
     * append a new column to the viewer <tt>tableviewer<tt> at the
     * column index <tt>insertIndex</tt>.
     *
     * @param   tcModel      model containing the table data
     * @param   insertIndex  the postion for the new column
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    public SyntheticColumnQueryWindow( TopcatModel tcModel,
                                       int insertIndex, Component parent ) {
        super( "Define Synthetic Column", parent );
        this.tcModel = tcModel;
        this.columnModel = tcModel.getColumnModel();
        this.dataModel = tcModel.getDataModel();
        this.subsets = tcModel.getSubsets();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField = new JTextField();
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

        /* Class selector. */
        typeField = new JComboBox();
        typeField.addItem( null );
        typeField.addItem( byte.class );
        typeField.addItem( short.class );
        typeField.addItem( int.class );
        typeField.addItem( long.class );
        typeField.addItem( float.class );
        typeField.addItem( double.class );
        CustomComboBoxRenderer renderer = new ClassComboBoxRenderer();
        renderer.setNullRepresentation( "(auto)" );
        typeField.setRenderer( renderer );
        typeField.setSelectedIndex( 0 );

        // Don't add this option for now - it's not that useful, since
        // narrowing conversions cause an error in any case, which, 
        // while sensible, is not what the user is going to expect.
        // stack.addLine( "Numeric Type", typeField );

        /* UCD field. */
        ucdField = new UCDSelector();
        stack.addLine( "UCD", ucdField );

        /* Index field. */
        indexSpinner = new ColumnIndexSpinner( columnModel );
        indexSpinner.setColumnIndex( insertIndex );
        stack.addLine( "Index", indexSpinner );

        /* Add tools. */
        getToolBar().add( MethodWindow.getWindowAction( this ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "SyntheticColumnQueryWindow" );

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
     * Sets the class that the expression result will be converted to.
     * If null, automatic class resolution should be used.
     *
     * @param   clazz  forced expression type, or null
     */
    public void setType( Class clazz ) {
        typeField.setSelectedItem( clazz );
    }

    /**
     * Returns the class that the user has selected for the expression.
     * If null, automatic class resolution should be used.
     *
     * @return  forced expression type, or null
     */
    public Class getType() {
        return (Class) typeField.getSelectedItem();
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
        Class clazz = getType();
        int index = getIndex();
        DefaultValueInfo info = new DefaultValueInfo( name );
        if ( desc != null ) {
            info.setDescription( desc );
        }
        if ( ucd != null ) {
            info.setUCD( ucd );
        }
        if ( unit != null ) {
            info.setUnitString( unit );
        }
        try {
            ColumnData col = new SyntheticColumn( info, dataModel, subsets,
                                                  expr, clazz );
            tcModel.appendColumn( col, index );
            return true;
        }
        catch ( CompilationException e ) {
            String[] msg = new String[] {
                "Syntax error in synthetic column expression \"" + expr + "\":",
                e.getMessage(),
            };
            JOptionPane.showMessageDialog( this, msg, "Expression Syntax Error",
                                           JOptionPane.ERROR_MESSAGE );
            return false;
        }
    }

}
