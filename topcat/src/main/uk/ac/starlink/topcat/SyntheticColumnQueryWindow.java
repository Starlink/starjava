package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.table.gui.UCDSelector;

/**
 * A dialogue window which queries the user for the characteristics of a
 * new column and then appends it to the table.
 */
public abstract class SyntheticColumnQueryWindow extends QueryWindow {

    private final TopcatModel tcModel_;
    private final TableColumnModel columnModel_;
    private JTextField nameField_;
    private JTextField unitField_;
    private JTextField descriptionField_;
    private JTextField expressionField_;
    private JComboBox<Class<?>> typeField_;
    private UCDSelector ucdField_;
    private ColumnIndexSpinner indexSpinner_;

    /**
     * Constructor.
     *
     * @param   tcModel      model containing the table data
     * @param   insertIndex  the default position for the new column
     * @param   isAdd        true for adding a new column,
     *                       false to edit an existing one
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     */
    private SyntheticColumnQueryWindow( TopcatModel tcModel, int insertIndex,
                                        boolean isAdd, Component parent ) {
        super( "Define Synthetic Column", parent );
        tcModel_ = tcModel;
        columnModel_ = tcModel.getColumnModel();
        LabelledComponentStack stack = getStack();

        /* Name field. */
        nameField_ = new JTextField();
        stack.addLine( "Name", nameField_ );

        /* Expression field. */
        expressionField_ = new JTextField();
        stack.addLine( "Expression", expressionField_ );

        /* Units field. */
        unitField_ = new JTextField();
        stack.addLine( "Units", unitField_ );

        /* Description field. */
        descriptionField_ = new JTextField();
        stack.addLine( "Description", descriptionField_ );

        /* Class selector. */
        typeField_ = new JComboBox<Class<?>>();
        typeField_.addItem( null );
        typeField_.addItem( byte.class );
        typeField_.addItem( short.class );
        typeField_.addItem( int.class );
        typeField_.addItem( long.class );
        typeField_.addItem( float.class );
        typeField_.addItem( double.class );
        typeField_.setRenderer( new ClassComboBoxRenderer( "(auto)" ) );
        typeField_.setSelectedIndex( 0 );

        // Don't add this option for now - it's not that useful, since
        // narrowing conversions cause an error in any case, which, 
        // while sensible, is not what the user is going to expect.
        // stack.addLine( "Numeric Type", typeField_ );

        /* UCD field. */
        ucdField_ = new UCDSelector();
        stack.addLine( "UCD", ucdField_ );

        /* Index field. */
        indexSpinner_ = new ColumnIndexSpinner( columnModel_, isAdd );
        indexSpinner_.setColumnIndex( insertIndex );
        stack.addLine( "Index", indexSpinner_ );

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
    public String getColumnName() {
        return nameField_.getText();
    }

    /**
     * Sets the contents of the name field.
     *
     * @param  name new contents of the name field
     */
    public void setColumnName( String name ) {
        nameField_.setText( name );
    }

    /**
     * Returns the string that the user has entered in the Units field.
     *
     * @return  units
     */
    public String getUnit() {
        return unitField_.getText();
    }

    /**
     * Sets the value entered into the units field.
     *
     * @param  units  unit string
     */
    public void setUnit( String units ) {
        unitField_.setText( units );
    }

    /**
     * Returns the string that the user has entered in the Description field.
     *
     * @return  description
     */
    public String getDescription() {
        return descriptionField_.getText();
    }

    /**
     * Sets the value entered into the description field.
     *
     * @param   desc  description string
     */
    public void setDescription( String desc ) {
        descriptionField_.setText( desc );
    }

    /**
     * Returns the string that the user has entered in the Expression field.
     *
     * @return  expression
     */
    public String getExpression() {
        return expressionField_.getText();
    }

    /**
     * Sets the contents of the expression field.
     *
     * @param   expr   new contents of the expression field
     */
    public void setExpression( String expr ) {
        expressionField_.setText( expr );
    }

    /**
     * Returns the string that the user has chosen for the UCD field.
     *
     * @return  UCD identifier
     */
    public String getUCD() {
        return ucdField_.getID();
    }

    /**
     * Sets the string in the UCD selector.
     *
     * @param  ucd  UCD string
     */
    public void setUCD( String ucd ) {
        ucdField_.setID( ucd );
    }

    /**
     * Sets the class that the expression result will be converted to.
     * If null, automatic class resolution should be used.
     *
     * @param   clazz  forced expression type, or null
     */
    public void setExpressionType( Class<?> clazz ) {
        typeField_.setSelectedItem( clazz );
    }

    /**
     * Returns the class that the user has selected for the expression.
     * If null, automatic class resolution should be used.
     *
     * @return  forced expression type, or null
     */
    public Class<?> getExpressionType() {
        return (Class<?>) typeField_.getSelectedItem();
    }

    /**
     * Sets the index at which the new column should be inserted.
     *
     * @return  index
     */
    public int getIndex() {
        return indexSpinner_.getColumnIndex();
    }

    /**
     * Constructs and returns the new synthetic column specified by the
     * state of this window.  If it constitutes an erroneous specification,
     * null is returned (and the user is notified).
     *
     * @return   new synthetic column as specified, or <code>null</code>
     */
    protected SyntheticColumn makeColumn() {
        String name = getColumnName();
        String desc = getDescription();
        String unit = getUnit();
        String expr = getExpression();
        String ucd = getUCD();
        Class<?> clazz = getExpressionType();
        ColumnInfo info = new ColumnInfo( name );
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
            return new SyntheticColumn( tcModel_, info, expr, null );
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
     * Constructs a query window which on completion will add a new column.
     *
     * @param   tcModel      model containing the table data
     * @param   insertIndex  the default position for the new column
     * @param   parent       the parent window for this dialogue (used for
     *                       window positioning)
     * @return   a window ready for user interaction 
     */
    public static SyntheticColumnQueryWindow
            newColumnDialog( TopcatModel tcModel, int insertIndex,
                             Component parent ) {
        return new SyntheticColumnQueryWindow( tcModel, insertIndex, true,
                                               parent ) {
            public boolean perform() {
                SyntheticColumn col = makeColumn();
                if ( col == null ) {
                    return false;
                }
                else {
                    tcModel.appendColumn( col, getIndex() );
                    return true;
                }
            }
        };
    }

    /**
     * Constructs a query window which will edit an existing column in the
     * model.
     *
     * @param  tcModel  topcat model
     * @param  icol     model index of column to edit
     * @param  parent   parent window, used for positioning
     * @param  onChange   callback to be run on the EDT;
     *                    the boolean parameter will be true only if the
     *                    expression defining a (synthetic) column has changed
     * @return    window ready for user interaction
     */
    public static SyntheticColumnQueryWindow
            editColumnDialog( TopcatModel tcModel, int icol, Component parent,
                              Consumer<Boolean> onChange ) {
        PlasticStarTable dataModel = tcModel.getDataModel();
        TableColumnModel colModel = tcModel.getColumnModel();
        ColumnData cdata = dataModel.getColumnData( icol );
        ColumnInfo cinfo = cdata.getColumnInfo();
        SyntheticColumn synthCol = cdata instanceof SyntheticColumn
                                 ? (SyntheticColumn) cdata
                                 : null;
        int ipos = getPositionIndex( colModel, icol );
        SyntheticColumnQueryWindow qwin =
                new SyntheticColumnQueryWindow( tcModel, ipos, false, parent ) {
            public boolean perform() {
                String expr = getExpression();
                final boolean expressionChanged;
                if ( synthCol == null ||
                     synthCol.getExpression().equals( expr ) ) {
                    expressionChanged = false;
                }
                else {
                    if ( TopcatJELUtils
                        .isColumnReferenced( tcModel, icol, expr ) ) {
                        String[] msg = new String[] {
                            "Recursive column expression disallowed:",
                            "\"" + expr + "\"" +
                            " directly or indirectly references column " +
                            cinfo.getName(),
                        };
                        JOptionPane
                       .showMessageDialog( this, msg, "Expression Error",
                                           JOptionPane.ERROR_MESSAGE );
                        return false;
                    }
                    try {
                        synthCol.setExpression( expr, (Class<?>) null );
                    }
                    catch ( CompilationException e ) {
                        String[] msg = new String[] {
                            "Syntax error in synthetic column expression \"" +
                            expr + "\":",
                            e.getMessage(),
                        };
                        JOptionPane
                       .showMessageDialog( this, msg,
                                           "Expression Syntax Error",
                                           JOptionPane.ERROR_MESSAGE );
                        return false;
                    }
                    expressionChanged = true;
                }
                cinfo.setName( getColumnName() );
                cinfo.setUnitString( getUnit() );
                cinfo.setDescription( getDescription() );
                cinfo.setUCD( getUCD() );
                onChange.accept( Boolean.valueOf( expressionChanged ) );
                int posIndex0 = getPositionIndex( colModel, icol );
                int posIndex1 = getIndex();
                if ( posIndex0 >= 0 && posIndex0 != posIndex1 ) {
                    colModel.moveColumn( posIndex0, posIndex1 );
                }
                return true;
            }
        };
        qwin.setColumnName( cinfo.getName() );
        qwin.setUnit( cinfo.getUnitString() );
        qwin.setDescription( cinfo.getDescription() );
        qwin.setUCD( cinfo.getUCD() );
        if ( synthCol != null ) {
            qwin.setExpression( synthCol.getExpression() );
        }
        qwin.expressionField_.setEnabled( synthCol != null );
        return qwin;
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
    public static SyntheticColumnQueryWindow
            replaceColumnDialog( final TopcatModel tcModel,
                                 final StarTableColumn baseCol,
                                 Component parent ) {
        final String OLD_SUFFIX = "_old";
        final ColumnList columnList = tcModel.getColumnList();
        final ColumnInfo baseInfo = baseCol.getColumnInfo();
        final String baseName = 
            TopcatUtils.getBaseName( baseInfo.getName(), OLD_SUFFIX );
        int pos = columnList.getModelIndex( columnList.indexOf( baseCol ) );
        SyntheticColumnQueryWindow qwin = 
            new SyntheticColumnQueryWindow( tcModel, pos, true, parent ) {
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
        qwin.setColumnName( baseName );
        qwin.setUnit( baseInfo.getUnitString() );
        qwin.setDescription( baseInfo.getDescription() );
        qwin.setUCD( baseInfo.getUCD() );
        DescribedValue colId = baseInfo.getAuxDatum( TopcatUtils.COLID_INFO );
        if ( colId != null ) {
            qwin.setExpression( colId.getValue().toString() );
        }
        return qwin;
    }

    /**
     * Returns the position in a given column model at which a column with
     * a given model index appears.
     *
     * @param  colModel  column model
     * @param  icol   target model index
     * @return  index in column model of target column, or -1 if not found
     */
    private static int getPositionIndex( TableColumnModel colModel, int icol ) {
        for ( int ic = 0; ic < colModel.getColumnCount(); ic++ ) {
            if ( colModel.getColumn( ic ).getModelIndex() == icol ) {
                return ic;
            }
        }
        return -1;
    }
}
