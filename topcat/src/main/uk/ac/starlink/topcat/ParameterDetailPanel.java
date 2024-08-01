package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Array;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Panel which displays the detail of a single table parameter value.
 * Works in conjunction with the TableModel used by the
 * {@link ParameterWindow}.
 *
 * @author   Mark Taylor
 * @since    9 Nov 2007
 */
public class ParameterDetailPanel extends JPanel {

    private final TableModel model_;
    private final LineField[] lineFields_;
    private final JComponent valuePanel_;
    private final int iValueCol_;
    private ValueDisplay lineDisplay_;
    private ValueDisplay textDisplay_;
    private ValueDisplay vectorDisplay_;
    private JComponent valueDisplay_;
    private int irow_;
    private DescribedValue dval_;

    private static final ValueInfo DUMMY_INFO =
        new DefaultValueInfo( "", String.class, "" );

    /**
     * Constructor.
     *
     * @param  model   table model representing parameter attributes
     */
    @SuppressWarnings("this-escape")
    public ParameterDetailPanel( TableModel model ) {
        model_ = model;
        irow_ = -1;
        dval_ = new DescribedValue( DUMMY_INFO, null );

        /* Assemble a list of fields to display parameter attributes.
         * The data will come directly from the supplied table model.
         * Note this model is (for some cells) writable as well as readable.
         * All of the values here are of String type.  */
        String[] fieldNames = new String[] {
            ParameterWindow.NAME_NAME,
            ParameterWindow.CLASS_NAME,
            ParameterWindow.SHAPE_NAME,
            ParameterWindow.UNITS_NAME,
            ParameterWindow.DESC_NAME,
            ParameterWindow.UCD_NAME,
            ParameterWindow.UTYPE_NAME,
        };
        lineFields_ = new LineField[ fieldNames.length ];
        for ( int ifield = 0; ifield < lineFields_.length; ifield++ ) {
            int icol = findColumn( model, fieldNames[ ifield ] );
            assert String.class.equals( model.getColumnClass( icol ) );
            lineFields_[ ifield ] = new LineField( icol );
        }

        /* Prepare consituents and lay them out in this component. */
        valuePanel_ = Box.createHorizontalBox();
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        layoutForm( this, model_, lineFields_, valuePanel_ );
        updateValue();

        /* Listen on the provided table model so that values in this
         * component can be updated as required. */
        iValueCol_ = findColumn( model, ParameterWindow.VALUE_NAME );
        model.addTableModelListener( new TableModelListener() {
            public void tableChanged( TableModelEvent evt ) {
                if ( evt.getFirstRow() <= irow_ && evt.getLastRow() >= irow_ ) {
                    int icol = evt.getColumn();
                    if ( icol == TableModelEvent.ALL_COLUMNS ) {
                        updateValue();
                        updateFields();
                    }
                    else if ( icol == iValueCol_ ) {
                        updateValue();
                    }
                    else {
                        updateFields();
                    }
                }
            }
        } );
    }

    public Dimension getMinimumSize() {
        Dimension size = new Dimension( super.getMinimumSize() );
        size.height += 24;
        return size;
    }

    public Dimension getPreferredSize() {
        Dimension size = new Dimension( super.getPreferredSize() );
        size.height += 60;
        return size;
    }

    /**
     * Sets the parameter to be displayed in this component.
     *
     * @param  irow   row index of supplied table model corresponding to the
     *         current parameter
     * @param  dval   the parameter description and value itself
     */
    public void setItem( int irow, DescribedValue dval ) {
        irow_ = irow;
        dval_ = dval == null ? new DescribedValue( DUMMY_INFO, null ) : dval;
        updateFields();
        updateValue();
    }

    /**
     * Ensures that the non-value parameter info fields in this component
     * are up to date.
     */
    private void updateFields() {
        for ( int i = 0; i < lineFields_.length; i++ ) {
            LineField field = lineFields_[ i ];
            int icol = field.getColumnIndex();
            JTextField textField = field.getTextField();
            Object val = irow_ >= 0 ? model_.getValueAt( irow_, icol )
                                    : null;
            String text = val instanceof String ? (String) val
                                                : "";
            if ( ! text.equals( textField.getText() ) ) {
                textField.setEditable( false );
                textField.setText( text );
                textField.setCaretPosition( 0 );
            }
            textField.setEditable( irow_ >= 0 &&
                                   model_.isCellEditable( irow_, icol ) );
        }
    }

    /**
     * Ensures that the parameter value field in this component is up to date.
     */
    private void updateValue() {
        JComponent display =
            getDisplayComponent( dval_.getInfo(), dval_.getValue() );
        if ( display != valueDisplay_ ) {
            valuePanel_.removeAll();
            valuePanel_.add( display );
            valueDisplay_ = display;
            valuePanel_.repaint();
            valuePanel_.revalidate();
        }
        display.revalidate();
    }

    /**
     * Returns a component displaying a given parameter.
     * This component may or may not be the same as one which is currently
     * in use.
     *
     * @param   info  parameter metadata
     * @param   value  parameter value
     */
    private JComponent getDisplayComponent( ValueInfo info, Object value ) {
        Class<?> clazz = info.getContentClass();
        ValueDisplay display;
        boolean editable = irow_ >= 0
                        && model_.isCellEditable( irow_, iValueCol_ );
        if ( Number.class.isAssignableFrom( clazz ) ||
             Boolean.class.isAssignableFrom( clazz ) ||
             irow_ == 0 ) {
            display = getLineDisplay();
            display.setEditable( editable );
        }
        else if ( clazz.isArray() ) {
            display = getVectorDisplay();
        }
        else {
            display = getTextDisplay();
            display.setEditable( editable );
        }
        display.setValue( info, value );
        return display.getComponent();
    }

    /**
     * Returns a component suitable for displaying a value, not necessarily
     * a string, which has a one-line representation.
     *
     * @return   new or reused display component
     */
    private ValueDisplay getLineDisplay() {
        if ( lineDisplay_ == null ) {
            final JTextField field = new JTextField() {
                public Dimension getMaximumSize() {
                    return new Dimension( Integer.MAX_VALUE,
                                          super.getPreferredSize().height );
                }
            };
            Box fieldBox = Box.createVerticalBox();
            fieldBox.add( field );
            fieldBox.add( Box.createVerticalGlue() );
            lineDisplay_ = new TextValueDisplay( fieldBox, field );
        }
        return lineDisplay_;
    }

    /**
     * Returns a component suitable for displaying a value with a 
     * potentially multi-line string represetation.
     *
     * @return  new or reused display component
     */
    private ValueDisplay getTextDisplay() {
        if ( textDisplay_ == null ) {
            JEditorPane editor = new JEditorPane();
            editor.setFont( new Font( "Monospaced", Font.PLAIN,
                                      getFont().getSize() ) );
            textDisplay_ =
                new TextValueDisplay( new JScrollPane( editor ), editor );
        }
        return textDisplay_;
    }

    /** 
     * Returns a component suitable for displaying an array-valued value.
     *
     * @return  new or reused display component
     */
    private ValueDisplay getVectorDisplay() {
        if ( vectorDisplay_ == null ) {
            final JTable table = new JTable();
            JScrollPane scroller = new JScrollPane( table );
            vectorDisplay_ = new ValueDisplay( scroller ) {
                public void setValue( final ValueInfo info, Object value ) {
                    final Object array =
                        value != null && value.getClass().isArray()
                            ? value
                            : new String[ 0 ];
                    TableModel arrayModel = new AbstractTableModel() {
                        public int getColumnCount() {
                            return 2;
                        }
                        public int getRowCount() {
                            return Array.getLength( array );
                        }
                        public String getColumnName( int icol ) {
                            switch ( icol ) {
                                case 0:
                                    return "Index";
                                case 1:
                                    return "Value";
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                        public Object getValueAt( int irow, int icol ) {
                            switch ( icol ) {
                                case 0:
                                    return Integer.valueOf( irow + 1 );
                                case 1:
                                    return Array.get( array, irow );
                                default:
                                    throw new IllegalArgumentException();
                            }
                        }
                    };
                    table.setModel( arrayModel );
                    table.getColumnModel().getColumn( 0 ).setMaxWidth( 100 );
                }
                public void setEditable( boolean editable ) {
                }
            };
        }
        return vectorDisplay_;
    }

    /**
     * Does the layout for this component.
     * Called from constructor.
     *
     * @param   form  component to contain fields
     * @param   model  table model containing parameter information
     * @param   lineFields  fields for parameter metadata display
     * @param   valuePanel  container for parameter value display components
     */
    private static void layoutForm( JComponent form, TableModel model,
                                    LineField[] lineFields,
                                    JComponent valuePanel ) {
        GridBagLayout layer = new GridBagLayout();
        form.setLayout( layer );
 
        int gridy = 0;
        for ( int ifield = 0; ifield < lineFields.length; ifield++ ) {
            LineField field = lineFields[ ifield ];

            GridBagConstraints cl = new GridBagConstraints();
            cl.gridy = gridy;
            cl.gridx = 1;
            cl.anchor = GridBagConstraints.EAST;
            JLabel label =
                new JLabel( model.getColumnName( field.getColumnIndex() )
                          + ":  " );
            layer.setConstraints( label, cl );
            form.add( label );

            GridBagConstraints ct = new GridBagConstraints();
            ct.gridy = gridy;
            ct.gridx = 2;
            ct.anchor = GridBagConstraints.WEST;
            ct.weightx = 1.0;
            ct.fill = GridBagConstraints.HORIZONTAL;
            ct.gridwidth = GridBagConstraints.REMAINDER;
            JTextField textField = field.getTextField();
            layer.setConstraints( textField, ct );
            form.add( textField );

            gridy++;
            GridBagConstraints cs = new GridBagConstraints();
            cs.gridx = 0;
            cs.gridy = gridy;
            Component strut = Box.createVerticalStrut( 5 );
            layer.setConstraints( strut, cs );
            form.add( strut );
            gridy++;
        }

        GridBagConstraints cv = new GridBagConstraints();
        cv.gridy = gridy;
        cv.weighty = 1.0;

        cv.gridx = 1;
        cv.anchor = GridBagConstraints.NORTHEAST;
        JLabel label = new JLabel( "Value" + ":  " );
        layer.setConstraints( label, cv );
        form.add( label );

        cv.gridx = 2;
        cv.anchor = GridBagConstraints.NORTHWEST;
        cv.weightx = 1.0;
        cv.fill = GridBagConstraints.BOTH;
        cv.gridwidth = GridBagConstraints.REMAINDER;
        cv.gridheight = GridBagConstraints.REMAINDER;
        layer.setConstraints( valuePanel, cv );
        form.add( valuePanel );
    }

    /**
     * Returns the column index in a table model for a given column name.
     *
     * @param  model  table model
     * @param  colName  column name to search for
     * @return  colum index, or -1 if not found
     */
    private static int findColumn( TableModel model, String colName ) {
        int ncol = model.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( model.getColumnName( icol ).equals( colName ) ) {
                return icol;
            }
        }
        return -1;
    }

    /**
     * Abstract superclass for an object which can display parameter values.
     */
    private static abstract class ValueDisplay {
        private final JComponent comp_;

        /**
         * Constructor.
         *
         * @param  comp   the parameter display component
         */
        protected ValueDisplay( JComponent comp ) {
            comp_ = comp;
        }

        /**
         * Returns the component which will do the display.
         *
         * @return  parameter display component
         */
        public JComponent getComponent() {
            return comp_;
        }

        /**
         * Configures this object's display component to display a given value.
         *
         * @param  info  value metadata
         * @param  info  value data
         */
        public abstract void setValue( ValueInfo info, Object value );

        /**
         * Configures whether the displayed parameter value should be editable.
         * Setting it true does not guarantee editability, if this object
         * does not provide that facility.
         *
         * @param  editable  if false, editing will not be possible
         */
        public abstract void setEditable( boolean editable );
    }

    /**
     * ValueDisplay implementation based on a <code>JTextComponent</code>.
     */
    private class TextValueDisplay extends ValueDisplay {
        private final JTextComponent tcomp_;
        private ValueInfo info_;
        private boolean changing_;

        /**
         * Constructor.
         * The supplied <code>comp</code> will presumably contain or be
         * identical with <code>tcomp</code>
         *
         * @param  comp   value display component
         * @param  tcomp  text component
         */
        TextValueDisplay( JComponent comp, JTextComponent tcomp ) {
            super( comp );
            tcomp_ = tcomp;
            tcomp_.addCaretListener( new CaretListener() {
                public void caretUpdate( CaretEvent evt ) {
                    if ( ! changing_ && tcomp_.isEditable() ) {
                        changing_ = true;
                        updateValue();
                        changing_ = false;
                    }
                }
            } );
        }

        public void setValue( ValueInfo info, Object value ) {
            info_ = info;
            if ( ! changing_ ) {
                changing_ = true;
                tcomp_.setText( info_.formatValue( value, Integer.MAX_VALUE ) );
                tcomp_.setCaretPosition( 0 );
                changing_ = false;
            }
        }

        public void setEditable( boolean isEditable ) {
            tcomp_.setEditable( isEditable );
        }

        /**
         * Ensures that this object's display is up to date with the
         * current parameter value.
         */
        private void updateValue() {
            String text = tcomp_.getText();
            if ( text != null ) {
                Object val1 = info_.unformatString( text );
                Object val2 = model_.getValueAt( irow_, iValueCol_ );
                boolean changed = val1 == null
                                ? val2 != null
                                : ! val1.equals( val2 );
                if ( changed ) {
                    model_.setValueAt( val1, irow_, iValueCol_ );
                }
            }
        }
    }

    /**
     * Object which displays an item of String-valued parameter metadata.
     * Editing of the text may be possible.
     */
    private class LineField {
        private final int icol_;
        private final JTextField textField_;

        /**
         * Constructor.
         *
         * @param   icol   column in the table model from which this field takes
         *                 its data
         */
        LineField( int icol ) {
            icol_ = icol;
            textField_ = new JTextField();
            textField_.setEditable( false );
            textField_.addCaretListener( new CaretListener() {
                public void caretUpdate( CaretEvent evt ) {
                    if ( textField_.isEditable() ) {
                        String text = textField_.getText();
                        if ( text != null && 
                             ! text.equals( model_
                                           .getValueAt( irow_, icol_ ) ) ) {
                            model_.setValueAt( text, irow_, icol_ );
                        }
                    }
                }
            } );
        }

        /**
         * Returns this object's column index.
         *
         * @return  column index
         */
        public int getColumnIndex() {
            return icol_;
        }

        /**
         * Returns the actual text field used for the value display.
         *
         * @return   text field
         */
        public JTextField getTextField() {
            return textField_;
        }
    }

}
