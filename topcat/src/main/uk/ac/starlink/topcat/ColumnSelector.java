package uk.ac.starlink.topcat;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
    private final JComboBox colComboBox_;
    private final JComboBox convComboBox_;
    private final Component[] components_;

    /**
     * Constructs a new selector ready to select columns corresponding to
     * a given ValueInfo.  It is initialised with no data model.
     *
     * @param  info  describes the columns to be selected by this component
     */
    public ColumnSelector( ValueInfo info ) {
        info_ = info;
        String units = info_.getUnitString();
        List compList = new ArrayList();

        /* Set up label. */
        JLabel label = new JLabel( info_.getName() + " column:" );
        label.setToolTipText( "Select column for " + info_.getDescription() );

        /* Set up column selector box. */
        colComboBox_ = new JComboBox();
        colComboBox_.setRenderer( new ColumnCellRenderer( colComboBox_ ) );

        /* Set up converter selector box if necessary. */
        ColumnConverter[] converters = ColumnConverter.getConverters( info_ );
        if ( converters.length > 1 ) {
            convComboBox_ = new JComboBox( converters );
            convComboBox_.setSelectedIndex( 0 );
            convComboBox_.setToolTipText( "Units for column " + 
                                          info_.getName() );
        }
        else {
            convComboBox_ = null;
        }

        /* Lay out components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( label );
        compList.add( label );
        add( Box.createHorizontalStrut( 5 ) );
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
        components_ = (Component[]) compList.toArray( new Component[ 0 ] );

        /* Not ready yet. */
        setEnabled( false );
    }

    /**
     * Constructs a new selector with a given data model.
     *
     * @param  model   data model
     */
    public ColumnSelector( ColumnSelectorModel model ) {
        this( model.getValueInfo() );
        setModel( model );
    }

    /**
     * Sets the model for this selector.  
     * <tt>model</tt> must have the same ValueInfo as the one this 
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
            ComboBoxModel dummy = new DefaultComboBoxModel();
            colComboBox_.setModel( dummy );
            if ( convComboBox_ != null ) {
                convComboBox_.setModel( dummy );
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
        setModel( tcModel.getColumnSelectorModel( info_ ) );
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
        return model_.getColumnData();
    }

    /**
     * Returns the model for this selector.
     *
     * @return  data model
     */
    public ColumnSelectorModel getModel() {
        return model_;
    }

    public void setEnabled( boolean enabled ) {
        for ( int i = 0; i < components_.length; i++ ) {
            components_[ i ].setEnabled( enabled );
        }
        super.setEnabled( enabled );
    }
}
