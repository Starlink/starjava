package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;

/**
 * Selector component for SortOrder instances.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2025
 */
public class SortOrderSelector extends JPanel {

    private final ActionForwarder forwarder_;
    private final List<ColumnDataComboBox> cdataBoxes_;
    private final JButton addButton_;
    private final JButton removeButton_;
    private final PropertyChangeListener modelListener_;
    
    private Model model_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public SortOrderSelector() {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        forwarder_ = new ActionForwarder();
        cdataBoxes_ = new ArrayList<ColumnDataComboBox>();
        Action addAct = BasicAction
                       .create( "Add column selector", ResourceIcon.ADD,
                                "Add selector for subordinate sort term",
                                evt -> {
                                    model_.addSelector();
                                    updateGui();
                                } );
        Action removeAct =
            BasicAction.create( "Remove column selector", ResourceIcon.SUBTRACT,
                                "Remove least significant sort term selector",
                                evt -> {
                                    model_.removeSelector();
                                    updateGui();
                                } );
        addButton_ = new JButton( addAct );
        removeButton_ = new JButton( removeAct );
        for ( JButton butt : new JButton[] { addButton_, removeButton_ } ) {
            butt.setHideActionText( true );
            butt.setBorder( javax.swing.BorderFactory.createEmptyBorder() );
            butt.setContentAreaFilled( false );
        }
        modelListener_ = evt -> updateGui();
        setModel( Model.DUMMY_MODEL );
    }

    /**
     * Adds a listener for selection events.
     *
     * @param  l  action listener
     */
    public void addActionListener( ActionListener l ) {
        forwarder_.addActionListener( l );
    }

    /**
     * Removes a listener for selection events.
     *
     * @param  l  action listener
     */
    public void removeActionListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }

    /**
     * Sets the model for this selector.
     *
     * @param  model  new model
     */
    public void setModel( Model model ) {
        if ( model != model_ ) {
            if ( model != null ) {
                model.removePropertyChangeListener( modelListener_ );
            }
            model_ = model;
            if ( model_ != null ) {
                model_.addPropertyChangeListener( modelListener_ );
            }
            updateGui();
        }
    }

    /**
     * Returns the model installed in this selector.
     *
     * @return  model
     */
    public Model getModel() {
        return model_;
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        updateEnabled();
    }

    /**
     * Updates the content of this component according to its current
     * internal state.  Also notifies listeners that a change may have
     * been made.
     */
    private void updateGui() {

        /* Release and remove all existing components. */
        for ( ColumnDataComboBox cdataBox : cdataBoxes_ ) {
            cdataBox.removeActionListener( forwarder_ );
        }
        cdataBoxes_.clear();
        removeAll();

        /* While an update is in progress, replace controls with a message. */
        if ( model_.isUpdating() ) {
            add( new JLabel( "Sorting ..." ) );
        }

        /* Otherwise, post the controls. */
        else {

            /* Add new combo boxes for each selector in the model. */
            for ( ColumnDataComboBoxModel cdataModel : model_.cdataModels_ ) {
                ColumnDataComboBox cdataBox = new ColumnDataComboBox();
                cdataBox.setModel( cdataModel );
                cdataBox.addActionListener( forwarder_ );
                add( cdataBox );
                add( Box.createHorizontalStrut( 5 ) );
                cdataBoxes_.add( cdataBox );
            }
        
            /* Just for cosmetic reasons, paint a useless JComboBox in case of
             * a table-less model. */
            if ( cdataBoxes_.size() == 0 ) {
                JComboBox<?> dummyBox = new JComboBox<Object>();
                dummyBox.setEnabled( false );
                add( dummyBox );
                add( Box.createHorizontalStrut( 5 ) );
            }

            /* Add buttons for adding/removing selectors. */
            if ( cdataBoxes_.size() > 1 ) {
                add( removeButton_ );
            }
            add( addButton_ );
        }

        /* Inform listeners that a change may have been made. */
        forwarder_.actionPerformed( new ActionEvent( this, 1, "change" ) );

        /* Update the visual state. */
        updateEnabled();
        revalidate();
        repaint();
    }

    /**
     * Updates the enableness states of the subcomponents of this component
     * to match this component's state.
     */
    private void updateEnabled() {
        boolean isEnabled = isEnabled() && !model_.isUpdating();
        for ( ColumnDataComboBox cdataBox : cdataBoxes_ ) {
            cdataBox.setEnabled( isEnabled );
        }
        addButton_.setEnabled( isEnabled );
        removeButton_.setEnabled( isEnabled );
    }

    /**
     * Model for SortOrderSelector.
     */
    public static class Model {

        private final TopcatModel tcModel_;
        private final List<ColumnDataComboBoxModel> cdataModels_;
        private final List<PropertyChangeListener> listeners_;
        private boolean isUpdating_;
        private static final ColumnDataComboBoxModel.Filter comparableFilter_ =
            info -> Comparable.class.isAssignableFrom( info.getContentClass() );

        /** Model that can be used when no selections are possible. */
        public static final Model DUMMY_MODEL = new Model( null );

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model that this selector can sort,
         *                  may be null for a dummy instance
         */
        public Model( TopcatModel tcModel ) {
            tcModel_ = tcModel;
            cdataModels_ = new ArrayList<ColumnDataComboBoxModel>();
            listeners_ = new ArrayList<PropertyChangeListener>();
            addSelector();
        }

        /**
         * Returns the sort order currently selected.
         *
         * @return  selection, not null
         */
        public SortOrder getSelectedSortOrder() {
            List<String> exprList = new ArrayList<>();
            for ( ColumnDataComboBoxModel cdataModel : cdataModels_ ) {
                Object sel = cdataModel.getSelectedItem();
                if ( sel instanceof ColumnData ) {
                    String expr = ((ColumnData) sel).getColumnInfo().getName();
                    if ( expr != null && expr.trim().length() > 0 ) {
                        exprList.add( expr );
                    }
                }
            }
            return new SortOrder( exprList.toArray( new String[ 0 ] ) );
        }

        /**
         * Sets the selected sort order.
         *
         * @param  sortOrder  new selection, not null
         */
        public void setSelectedSortOrder( SortOrder sortOrder ) {
            SortOrder oldOrder = getSelectedSortOrder();
            if ( ! sortOrder.equals( oldOrder ) ) {
                String[] exprs = sortOrder.getExpressions();
                int nexpr = exprs.length;
                while ( cdataModels_.size() > Math.max( 1, nexpr ) ) {
                    removeSelector();
                }
                while ( cdataModels_.size() < nexpr ) {
                    addSelector();
                }
                for ( int i = 0; i < cdataModels_.size(); i++ ) {
                    String expr = i < nexpr ? exprs[ i ] : null;
                    ColumnDataComboBoxModel cdataModel = cdataModels_.get( i );
                    ColumnData cdata;
                    try {
                        cdata = cdataModel.stringToColumnData( expr );
                    }
                    catch ( CompilationException e ) {
                        cdata = null;
                    }
                    cdataModel.setSelectedItem( cdata );
                }
                PropertyChangeEvent evt =
                    new PropertyChangeEvent( this, "selectedSortOrder",
                                             oldOrder, sortOrder );
                for ( PropertyChangeListener listener : listeners_ ) {
                    listener.propertyChange( evt );
                }
            }
        }

        /**
         * Sets whether this model is in the middle of an update.
         *
         * @param  isUpdating  true iff an update is in progress
         */
        public void setUpdating( boolean isUpdating ) {
            if ( isUpdating != isUpdating_ ) {
                PropertyChangeEvent evt =
                    new PropertyChangeEvent( this, "updating",
                                             isUpdating_, isUpdating );
                isUpdating_ = isUpdating;
                for ( PropertyChangeListener listener : listeners_ ) {
                    listener.propertyChange( evt );
                }
            }
        }

        /**
         * Indicates whether this model is in the middle of an update.
         *
         * @return  true iff an update is in progress
         */
        public boolean isUpdating() {
            return isUpdating_;
        }

        /**
         * Adds a listener that will be informed if the selection has changed.
         *
         * @param  l  listener
         */
        public void addPropertyChangeListener( PropertyChangeListener l ) {
            listeners_.add( l );
        }

        /**
         * Removes a previously added listener.
         *
         * @param  l  listener
         */
        public void removePropertyChangeListener( PropertyChangeListener l ) {
            listeners_.remove( l );
        }

        /**
         * Adds a new selector model.
         * If this model has no associated TopcatModel, does nothing.
         */
        private void addSelector() {
            if ( tcModel_ != null ) {
                cdataModels_
               .add( new ColumnDataComboBoxModel( tcModel_, comparableFilter_,
                                                  true, false ) );
            }
        }

        /**
         * Removes the least significant selector model,
         * if more than one exists.
         */
        private void removeSelector() {
            if ( cdataModels_.size() > 0 ) {
                cdataModels_.remove( cdataModels_.size() - 1 );
            }
        }
    }
}
