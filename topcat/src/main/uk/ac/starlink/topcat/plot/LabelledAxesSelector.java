package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.util.Wrapper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * AxesSelector implementation which wraps an existing one and adds 
 * a point label axis of its own.  The labeller axis is optional and
 * can be rendered visible or not using the {@link #enableLabels} method.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2007
 */
public class LabelledAxesSelector implements AxesSelector, Wrapper {

    private final AxesSelector baseSelector_;
    private final JComboBox labelSelector_;
    private final JComponent labelPanel_;
    private final JComponent labelContainer_;
    private final JComponent selectorPanel_;
    private TopcatModel tcModel_;
    private boolean labelsEnabled_;

    /**
     * Constructor.
     *
     * @param   baseSelector   selector which this one is decorating
     */
    public LabelledAxesSelector( AxesSelector baseSelector ) {
        baseSelector_ = baseSelector;
        labelSelector_ = ColumnDataComboBoxModel.createComboBox();
        selectorPanel_ = Box.createVerticalBox();
        selectorPanel_.add( baseSelector.getColumnSelectorPanel() );
        labelContainer_ = Box.createVerticalBox();
        labelPanel_ = Box.createHorizontalBox();
        labelPanel_.add( Box.createHorizontalStrut( 5 ) );
        labelPanel_.add( new JLabel( "Point Labels: " ) );
        labelPanel_.add( new ShrinkWrapper( labelSelector_ ) );
        labelPanel_.add( Box.createHorizontalGlue() );
        selectorPanel_.add( labelContainer_ );
    }

    /**
     * Returns the selector which this one is decorating.
     *
     * @return   base selector
     */
    public Object getBase() {
        return baseSelector_;
    }

    /**
     * Determine whether the labels selector will be visible and used.
     *
     * @param  enabled  true to enable point labelling
     */
    public void enableLabels( boolean enabled ) {
        if ( enabled != labelsEnabled_ ) {
            if ( enabled ) {
                labelContainer_.add( labelPanel_ );
            }
            else {
                labelContainer_.remove( labelPanel_ );
            }
            labelsEnabled_ = enabled;
            labelContainer_.revalidate();
        }
    }

    public JComponent getColumnSelectorPanel() {
        return selectorPanel_;
    }

    public JComboBox[] getColumnSelectors() {
        JComboBox[] baseSelectors = baseSelector_.getColumnSelectors();
        JComboBox[] selectors = new JComboBox[ baseSelectors.length + 1 ];
        System.arraycopy( baseSelectors, 0, selectors, 0,
                          baseSelectors.length );
        selectors[ baseSelectors.length ] = labelSelector_;
        return selectors;
    }

    public int getNdim() {
        return baseSelector_.getNdim();
    }

    public boolean isReady() {
        return baseSelector_.isReady();
    }

    public StarTable getData() {
        return baseSelector_.getData();
    }

    public StarTable getErrorData() {
        return baseSelector_.getErrorData();
    }

    public ErrorMode[] getErrorModes() {
        return baseSelector_.getErrorModes();
    }

    public StarTable getLabelData() {
        if ( labelsEnabled_ ) {
            Object item = labelSelector_.getSelectedItem();
            if ( item instanceof ColumnData ) {
                ColumnData col = (ColumnData) item;
                return new ColumnDataTable( tcModel_,
                                            new ColumnData[] { col } );
            }
            else {
                return null; 
            }
        }
        else {
            return null;
        }
    }

    public AxisEditor[] createAxisEditors() {
        return baseSelector_.createAxisEditors();
    }

    public PointStore createPointStore( int npoint ) {
        PointStore baseStore = baseSelector_.createPointStore( npoint );
        return labelsEnabled_ ? new LabelledPointStore( baseStore )
                              : baseStore;
    }

    public void setTable( TopcatModel tcModel ) {
        baseSelector_.setTable( tcModel );
        if ( tcModel == null ) {
            labelSelector_.setSelectedItem( null );
            labelSelector_.setEnabled( false );
        }
        else {
            labelSelector_
                .setModel( new ColumnDataComboBoxModel( tcModel, Object.class,
                                                        true ) );
            labelSelector_.setEnabled( true );
        }
        tcModel_ = tcModel;
    }

    public void addActionListener( ActionListener listener ) {
        baseSelector_.addActionListener( listener );
        labelSelector_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        baseSelector_.removeActionListener( listener );
        baseSelector_.removeActionListener( listener );
    }

    public void initialiseSelectors() {
        baseSelector_.initialiseSelectors();
    }

    /**
     * PointStore implementation which adds an optional label axis to 
     * a base point store.
     */
    private static class LabelledPointStore implements PointStore, Wrapper {

        private final PointStore base_;
        private final String[] labels_;
        private int nStore_;

        /**
         * Constructor.
         *
         * @param  base   base point store
         */
        LabelledPointStore( PointStore base ) {
            base_ = base;
            labels_ = new String[ base.getCount() ];
        }

        public Object getBase() {
            return base_;
        }

        public int getCount() {
            return base_.getCount();
        }

        public int getNdim() {
            return base_.getNdim();
        }

        public double[] getPoint( int ipoint ) {
            return base_.getPoint( ipoint );
        }

        public int getNerror() {
            return base_.getNerror();
        }

        public double[][] getErrors( int ipoint ) {
            return base_.getErrors( ipoint );
        }

        public boolean hasLabels() {
            return true;
        }

        public void storePoint( Object[] coordRow, Object[] errorRow,
                                String label ) {
            base_.storePoint( coordRow, errorRow, label );
            labels_[ nStore_++ ] = label;
        }

        public String getLabel( int ipoint ) {
            return labels_[ ipoint ];
        }
    }
}
