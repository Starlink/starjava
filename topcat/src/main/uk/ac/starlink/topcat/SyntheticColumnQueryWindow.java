package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.UCDSelector;

/**
 * A dialogue window which queries the user for the characteristics of a
 * new column and then appends it to the table.
 */
public class SyntheticColumnQueryWindow extends QueryWindow {

    private final TopcatModel tcModel;
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
        this.subsets = tcModel.getSubsets();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField = new JTextField();
        stack.addLine( "Name", nameField );

        /* Expression field. */
        expressionField = new JTextField();
        stack.addLine( "Expression", expressionField );

        /* Units field. */
        unitField = new JTextField();
        stack.addLine( "Units", unitField );

        /* Description field. */
        descriptionField = new JTextField();
        stack.addLine( "Description", descriptionField );

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
        getToolBar().add( MethodWindow.getWindowAction( this, false ) );
        getToolBar().addSeparator();

        /* Add help information. */
        addHelp( "SyntheticColumnQueryWindow" );
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
     * Sets the value entered into the units field.
     *
     * @param  units  unit string
     */
    public void setUnit( String units ) {
        unitField.setText( units );
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
     * Sets the value entered into the description field.
     *
     * @param   desc  description string
     */
    public void setDescription( String desc ) {
        descriptionField.setText( desc );
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
     * Sets the string in the UCD selector.
     *
     * @param  ucd  UCD string
     */
    public void setUCD( String ucd ) {
        ucdField.setID( ucd );
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

    /**
     * Constructs and returns the new synthetic column specified by the
     * state of this window.  If it constitutes an erroneous specification,
     * null is returned (and the user is notified).
     *
     * @return   new synthetic column as specified, or <tt>null</tt>
     */
    protected SyntheticColumn makeColumn() {
        String name = getName();
        String desc = getDescription();
        String unit = getUnit();
        String expr = getExpression();
        String ucd = getUCD();
        Class clazz = getType();
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
            return new SyntheticColumn( info, expr, null,
                                        tcModel.createJELRowReader() );
        }
        catch ( CompilationException e ) {
            String[] msg = new String[] {
                "Syntax error in synthetic column expression \"" + expr + "\":",
                e.getMessage(),
            };
            JOptionPane.showMessageDialog( this, msg, "Expression Syntax Error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }
    }

    /**
     * Invokes {@link #makeColumn} and adds the resulting column to the 
     * topcatModel.
     *
     * @return  whether a column was successfully added
     */
    protected boolean perform() {
        SyntheticColumn col = makeColumn();
        if ( col == null ) {
            return false;
        }
        else {
            tcModel.appendColumn( col, getIndex() );
            return true;
        }
    }

    /**
     * Constructs a query window which on completion will replace an
     * existing column.  This means that when (if) the user hits OK,
     * the column it's based on will be hidden, and the new one will
     * be added in the same place, with the same name.  The old (hidden)
     * one will be given a new 'retirement' name.
     *
     * @param  tcModel  topcat model
     * @param  baseCol     column to be replaced
     * @param  parent   parent window, used for positioning
     * @return   a window ready for user interaction 
     */
    public static SyntheticColumnQueryWindow replaceColumnDialog(
            final TopcatModel tcModel, final StarTableColumn baseCol,
            Component parent ) {
        final String OLD_SUFFIX = "_old";
        final ColumnList columnList = tcModel.getColumnList();
        final ColumnInfo baseInfo = baseCol.getColumnInfo();
        final String baseName = 
            TopcatUtils.getBaseName( baseInfo.getName(), OLD_SUFFIX );
        int pos = columnList.getModelIndex( columnList.indexOf( baseCol ) );
        SyntheticColumnQueryWindow qwin = 
            new SyntheticColumnQueryWindow( tcModel, pos, parent ) {
                protected boolean perform() {

                    /* Create a new column based on the current state of this
                     * window. */
                    SyntheticColumn col = makeColumn();
                    if ( col == null ) {
                        return false;
                    }

                    /* Check if any column in the table has the same name
                     * as the one we're adding.  If so, rename it. */
                    String newName = col.getColumnInfo().getName();
                    int ncol = columnList.size();
                    for ( int i = 0; i < ncol; i++ ) {
                        TableColumn tcol = columnList.getColumn( i );
                        if ( tcol instanceof StarTableColumn ) {
                            StarTableColumn stcol = (StarTableColumn) tcol;
                            ColumnInfo cinfo = stcol.getColumnInfo();
                            String cname = cinfo.getName();
                            if ( cname.equals( newName ) ) {
                                String bname = TopcatUtils
                                              .getBaseName( cname, OLD_SUFFIX );
                                String rname = TopcatUtils
                                              .getDistinctName( columnList,
                                                                bname, 
                                                                OLD_SUFFIX );
                                tcModel.renameColumn( stcol, rname );
                            }
                        }
                    }

                    /* Hide the old column. */
                    tcModel.getColumnModel().removeColumn( baseCol );

                    /* Add the new column. */
                    tcModel.appendColumn( col, getIndex() );
                    return true;
                }
            };
        qwin.setName( baseName );
        qwin.setUnit( baseInfo.getUnitString() );
        qwin.setDescription( TopcatUtils.getBaseDescription( baseInfo ) );
        qwin.setUCD( baseInfo.getUCD() );
        DescribedValue colId = baseInfo.getAuxDatum( TopcatUtils.COLID_INFO );
        if ( colId != null ) {
            qwin.setExpression( colId.getValue().toString() );
        }
        qwin.setVisible( true );
        return qwin;
    }

}
