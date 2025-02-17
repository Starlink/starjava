package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;

/**
 * Selector component for SortOrder instances.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2025
 */
public class SortOrderSelector extends JPanel {

    private final List<ColumnDataComboBox> cdataBoxes_;
    private final ActionForwarder forwarder_;
    private Model model_;

    /**
     * Constructor.
     */
    public SortOrderSelector() {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        cdataBoxes_ = new ArrayList<ColumnDataComboBox>();
        forwarder_ = new ActionForwarder();
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
            for ( ColumnDataComboBox cdataBox : cdataBoxes_ ) {
                cdataBox.removeActionListener( forwarder_ );
            }
            cdataBoxes_.clear();
            model_ = model;
            for ( ColumnDataComboBoxModel cdataModel : model.cdataModels_ ) {
                ColumnDataComboBox cdataBox = new ColumnDataComboBox();
                cdataBox.addActionListener( forwarder_ );
                cdataBox.setModel( cdataModel );
                cdataBoxes_.add( cdataBox );
            }
            updateGui();
            forwarder_.actionPerformed( new ActionEvent( this, 1,
                                                         "model-change" ) );
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
     * internal state.
     */
    private void updateGui() {

        /* Replace any existing selection boxes with new ones appropriate
         * to the current state. */
        removeAll();
        for ( ColumnDataComboBox cdataBox : cdataBoxes_ ) {
            add( cdataBox );
        }

        /* Just for cosmetic reasons, paint a useless JComboBox in case of
         * a table-less model. */
        if ( cdataBoxes_.size() == 0 ) {
            JComboBox<?> dummyBox = new JComboBox<Object>();
            dummyBox.setEnabled( false );
            add( dummyBox );
        }

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
        boolean isEnabled = isEnabled();
        for ( ColumnDataComboBox cdataBox : cdataBoxes_ ) {
            cdataBox.setEnabled( isEnabled );
        }
    }

    /**
     * Model for SortOrderSelector.
     */
    public static class Model {

        private final TopcatModel tcModel_;
        private final List<ColumnDataComboBoxModel> cdataModels_;
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
            if ( tcModel != null ) {
                ColumnDataComboBoxModel boxModel0 = createComboBoxModel();
                cdataModels_.add( createComboBoxModel() );
            }
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
            if ( ! sortOrder.equals( getSelectedSortOrder() ) ) {
                String[] exprs = sortOrder.getExpressions();
                int nexpr = exprs.length;
                while ( cdataModels_.size() > Math.max( 1, nexpr ) ) {
                    cdataModels_.remove( cdataModels_.size() - 1 );
                }
                while ( cdataModels_.size() < nexpr ) {
                    cdataModels_.add( createComboBoxModel() );
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
            }
        }

        /**
         * Creates a combo box model that can select one expression part
         * of a sort order for this model.
         *
         * @return   new combo box model
         */
        private ColumnDataComboBoxModel createComboBoxModel() {
            return new ColumnDataComboBoxModel( tcModel_, comparableFilter_,
                                                true, false );
        }
    }
}
