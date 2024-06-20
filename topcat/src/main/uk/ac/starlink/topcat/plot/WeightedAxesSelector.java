package uk.ac.starlink.topcat.plot;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConstantColumn;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperColumn;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;

/**
 * AxesSelector implementation which adds a weighting axis to the
 * basic ones.  The output is the same as that of the supplied base
 * selector, but if weighting is in effect an additional weighting column
 * is added. 
 *
 * @author   Mark Taylor
 * @since    20 Jun 2007
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class WeightedAxesSelector implements AxesSelector {

    private final AxesSelector baseSelector_;
    private final CartesianAxesSelector weightSelector_;
    private final JComponent selectorContainer_;
    private final JComponent selectorPanel_;
    private boolean weightEnabled_;

    private static final ColumnData UNIT_WEIGHT =
         new ConstantColumn( new ColumnInfo( "Unity", Integer.class,
                                             "Unit weight" ),
                             Integer.valueOf( 1 ) );

    /**
     * Constructor.
     *
     * @param   baseSelector   selector without weighting on which this is based
     */
    public WeightedAxesSelector( AxesSelector baseSelector ) {
        baseSelector_ = baseSelector;
        weightSelector_ =
            new CartesianAxesSelector( new String[] { "Weight" }, null, null,
                                       new ErrorModeSelectionModel[ 0 ] );
        selectorPanel_ = Box.createVerticalBox();
        selectorPanel_.add( baseSelector_.getColumnSelectorPanel() );
        selectorContainer_ = Box.createHorizontalBox();
        selectorContainer_.add( Box.createRigidArea( new Dimension( 0, 60 ) ) );
        selectorContainer_.add( selectorPanel_ );
    }

    /**
     * Sets whether the weighting selector is visible, and also whether any
     * selection in it affects the output of this selector.
     *
     * @param   weightEnabled  true iff weighting is enabled
     */
    public void enableWeights( boolean weightEnabled ) {
        JComponent weightPanel = weightSelector_.getColumnSelectorPanel();
        selectorPanel_.remove( weightPanel );
        if ( weightEnabled ) {
            selectorPanel_.add( weightPanel );
        }
        weightEnabled_ = weightEnabled;
        selectorContainer_.revalidate();
    }

    /**
     * Gets the combo box used to select weighting columns.
     *
     * @return  weight selector
     */
    public JComboBox getWeightSelector() {
        return weightSelector_.getColumnSelector( 0 );
    }

    public JComponent getColumnSelectorPanel() {
        return selectorContainer_;
    }

    public JComboBox[] getColumnSelectors() {
        JComboBox[] baseSelectors = baseSelector_.getColumnSelectors();
        JComboBox[] weightSelectors = weightSelector_.getColumnSelectors();
        JComboBox[] selectors =
            new JComboBox[ baseSelectors.length + weightSelectors.length ];
        System.arraycopy( baseSelectors, 0, selectors, 0,
                          baseSelectors.length );
        System.arraycopy( weightSelectors, 0, selectors, baseSelectors.length,
                          weightSelectors.length );
        return selectors;
    }

    public void setTable( TopcatModel tcModel ) {
        baseSelector_.setTable( tcModel );
        weightSelector_.setTable( tcModel );
    }

    public void initialiseSelectors() {
        baseSelector_.initialiseSelectors();
    }

    public void addActionListener( ActionListener listener ) {
        baseSelector_.addActionListener( listener );
        weightSelector_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        baseSelector_.removeActionListener( listener );
        weightSelector_.removeActionListener( listener );
    }

    public int getNdim() {
        return baseSelector_.getNdim() + 1;
    }

    public boolean isReady() {
        return baseSelector_.isReady();
    }

    public StarTable getData() {
        ColumnDataTable baseData = (ColumnDataTable) baseSelector_.getData();
        baseData.addColumn( getWeightData() );
        return baseData;
    }

    public StarTable getErrorData() {
        StarTable errorData = baseSelector_.getErrorData();
        assert errorData.getColumnCount() == 0
             : "Weighting of errors is not implemented";
        return errorData;
    }

    public StarTable getLabelData() {
        return baseSelector_.getLabelData();
    }

    public AxisEditor[] createAxisEditors() {
        return baseSelector_.createAxisEditors();
    }

    public PointStore createPointStore( int npoint ) {

        /* Could be more efficient than this, since if there is no weighting
         * the weighting column does not need to be stored 
         * (it is always unity). */
        return new CartesianPointStore( getNdim(), getErrorModes(), npoint );
    }

    public ErrorMode[] getErrorModes() {
        return baseSelector_.getErrorModes();
    }

    /**
     * Indicates whether non-unit weighting is in effect.
     *
     * @return   if false, every element of the weighting column will be unity
     */
    public boolean hasWeights() {
        return weightEnabled_ 
            && weightSelector_.getColumnSelector( 0 )
                              .getSelectedItem() instanceof ColumnData;
    }

    /**
     * Returns the column representing the weight values.
     *
     * @return weight column
     */
    private ColumnData getWeightData() {
        if ( hasWeights() ) {
            final ColumnData cdata =
                (ColumnData) weightSelector_.getColumnSelector( 0 )
                                            .getSelectedItem();
            final ColumnInfo info = new ColumnInfo( cdata.getColumnInfo() );
            info.setName( "Weighted count" );
            return new WrapperColumn( cdata ) {
                public ColumnInfo getColumnInfo() {
                    return info;
                }
                public boolean equals( Object other ) {
                    return other instanceof WrapperColumn 
                         ? this.getBaseColumn()
                          .equals( ((WrapperColumn) other).getBaseColumn() )
                         : super.equals( other );
                }
                public int hashCode() {
                    return cdata.hashCode();
                }
            };
        }
        else {
            return UNIT_WEIGHT;
        }
    }
}
