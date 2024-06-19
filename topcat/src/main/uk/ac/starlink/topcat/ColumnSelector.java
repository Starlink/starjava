package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ValueInfo;

/**
 * Allows selection of a column from a table which represents a requested
 * kind of value.  The column may not actually exist in the table, but
 * may be based on one that does, modified by a ColumnConverter.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Oct 2004
 * @see      ColumnSelectorModel
 */
public class ColumnSelector extends JComponent {

    private ColumnSelectorModel model_;
    private final ValueInfo info_;
    private final JComboBox<ColumnData> colComboBox_;
    private final JComboBox<ColumnConverter> convComboBox_;
    private final Component[] components_;
    private final JLabel label_;

    /**
     * Constructs a new selector ready to select columns corresponding to
     * a given ValueInfo.  It is initialised with no data model.
     *
     * @param  info  describes the columns to be selected by this component
     * @param  showLabel  true iff you want the axis label to be displayed
     *         with the selectors
     */
    public ColumnSelector( ValueInfo info, boolean showLabel ) {
        info_ = info;
        String units = info_.getUnitString();
        List<Component> compList = new ArrayList<Component>();

        /* Set up label. */
        label_ = new JLabel( info_.getName() + " column:" );
        label_.setToolTipText( "Select column for " + info_.getDescription() );

        /* Set up column selector box. */
        colComboBox_ = new ColumnDataComboBox();

        /* Set up converter selector box if necessary. */
        ColumnConverter[] converters = ColumnConverter.getConverters( info_ );
        if ( converters.length > 1 ) {
            convComboBox_ = new JComboBox<ColumnConverter>( converters );
            convComboBox_.setSelectedIndex( 0 );
            convComboBox_.setToolTipText( "Units for column " + 
                                          info_.getName() );
        }
        else {
            convComboBox_ = null;
        }

        /* Lay out components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        if ( showLabel ) {
            add( label_ );
            compList.add( label_ );
            add( Box.createHorizontalStrut( 5 ) );
        }
        add( colComboBox_ );
        compList.add( colComboBox_ );
        if ( convComboBox_ != null ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( convComboBox_ );
            compList.add( convComboBox_ );
        }
        else if ( units != null && units.toString().length() > 0 ) {
            add( Box.createHorizontalStrut( 5 ) );
            JLabel ulabel = new JLabel( " (" + units.trim() + ") " );
            add( ulabel );
            compList.add( ulabel );
        }
        components_ = compList.toArray( new Component[ 0 ] );

        /* Not ready yet. */
        setEnabled( false );
    }

    /**
     * Constructs a new selector with a given data model.
     *
     * @param  model   data model
     * @param  showLabel  true iff you want the axis label to be displayed
     *         with the selectors
     */
    public ColumnSelector( ColumnSelectorModel model, boolean showLabel ) {
        this( model.getValueInfo(), showLabel );
        setModel( model );
    }

    /**
     * Sets the model for this selector.  
     * <code>model</code> must have the same ValueInfo as the one this 
     * component was set up with.
     *
     * @param  model  new data model
     */
    public void setModel( ColumnSelectorModel model ) {
        if ( model != null && model.getValueInfo() != info_ ) {
            throw new IllegalArgumentException( 
                          "Model ValueInfo doesn't match this selector" );
        }
        model_ = model;
        if ( model == null ) {
            setEnabled( false );
            colComboBox_.setModel( new DefaultComboBoxModel<ColumnData>() );
            if ( convComboBox_ != null ) {
                convComboBox_
               .setModel( new DefaultComboBoxModel<ColumnConverter>() );
            }
        }
        else {
            setEnabled( true );
            colComboBox_.setModel( model.getColumnModel() );
            if ( convComboBox_ != null ) {
                convComboBox_.setModel( model.getConverterModel() );
            }
        }
    }

    /**
     * Convenience method which sets this selector's model to the one
     * appropriate for its ValueInfo and the given TopcatModel.
     *
     * @param  tcModel  table model
     */
    public void setTable( TopcatModel tcModel ) {
        setModel( new ColumnSelectorModel( tcModel, info_ ) );
    }

    /**
     * Returns the (effective) column currently selected by the user.
     * It takes into account the column and (if any) conversion selected
     * by the user.
     *
     * @return  ColumnData representing the currently-selected column,
     *          or null if none is selected
     */
    public ColumnData getColumnData() {
        return model_ == null ? null
                              : model_.getColumnData();
    }

    /**
     * Sets the currently selected column data value.
     * Does not have to be to one of the values in the selection model.
     *
     * @param  colData  new column data selection
     */
    public void setColumnData( ColumnData colData ) {
        model_.getColumnModel().setSelectedItem( colData );
    }

    /**
     * Sets the value of the selector progrmmatically as a string.
     * This should be a legal JEL expression in the context of the
     * selector's table.
     *
     * @param  txt  text value
     */
    public void setStringValue( String txt ) {
        ComboBoxEditor editor = colComboBox_.getEditor();
        colComboBox_.configureEditor( editor, txt );
        colComboBox_.setSelectedItem( editor.getItem() );
        colComboBox_.actionPerformed( new ActionEvent( this, 0, null ) );
    }

    /**
     * Returns the value of the selector as a string.
     *
     * @return  text value
     */
    public String getStringValue() {
        Object val = colComboBox_.getEditor().getItem();
        return val == null ? null : val.toString();
    }

    /**
     * Returns the model for this selector.
     *
     * @return  data model
     */
    public ColumnSelectorModel getModel() {
        return model_;
    }

    /**
     * Returns the label which annotates this selector (though it may
     * or may not be displayed in this component according to how
     * the constructor was called).
     *
     * @return  label   annotating label
     */
    public JLabel getLabel() {
        return label_;
    }

    /**
     * Returns the component displaying selection of the actual column or
     * expression.
     *
     * @return   column selection component
     */
    public JComboBox<ColumnData> getColumnComponent() {
        return colComboBox_;
    }

    /**
     * Returns the component displaying selection of the unit, if any.
     *
     * @return  converter selection component
     */
    public JComboBox<ColumnConverter> getUnitComponent() {
        return convComboBox_;
    }

    /**
     * Adds a listener for changes on the state of this component.
     *
     * @param   listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        colComboBox_.addActionListener( listener );
        if ( convComboBox_ != null ) {
            convComboBox_.addActionListener( listener );
        }
    }

    /**
     * Removes a listener for changes on the state of this component.
     *
     * @param   listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        colComboBox_.removeActionListener( listener );
        if ( convComboBox_ != null ) {
            convComboBox_.removeActionListener( listener );
        }
    }

    public void setEnabled( boolean enabled ) {
        for ( int i = 0; i < components_.length; i++ ) {
            components_[ i ].setEnabled( enabled );
        }
        super.setEnabled( enabled );
    }
}
