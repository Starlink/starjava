package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.util.Wrapper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * AxesSelector implementation that wraps an existing one and adds some
 * auxiliary axes of its own.  These auxiliary axes are optional and
 * may be rendered visible or not using the {@link #setAuxVisible} method.
 *
 * @author   Mark Taylor
 * @since    4 Jun 2007
 */
public class AugmentedAxesSelector implements AxesSelector, Wrapper {

    private final AxesSelector baseSelector_;
    private final int naux_;
    private final ToggleButtonModel[] logModels_;
    private final ToggleButtonModel[] flipModels_;
    private final CartesianAxesSelector auxSelector_;
    private final ComboBoxModel[] shaderModels_;
    private final JComponent selectorPanel_;
    private final JComponent auxPanel_;
    private TopcatModel tcModel_;
    private int nVisible_;

    /**
     * Constructor.  Initially none of the auxiliary axes are visible.
     *
     * @param   baseSelector  the base selector to which this will append
     *          auxiliary axes
     * @param   naux  number of auxiliary axes to append
     * @param   logModels  <code>naux</code>-element array of models flagging
     *          per-axis log scaling (or null)
     * @param   flipModels  <code>naux</code>-element array of models flagging
     *          per-axis sense inversion (or null)
     * @param   shaderModels <code>naux</code>-element array of combo box
     *          models to display with each aux axis (or null)
     */
    public AugmentedAxesSelector( AxesSelector baseSelector, int naux,
                                  ToggleButtonModel[] logModels,
                                  ToggleButtonModel[] flipModels,
                                  ComboBoxModel[] shaderModels ) {
        baseSelector_ = baseSelector;
        naux_ = naux;
        logModels_ = logModels;
        flipModels_ = flipModels;
        shaderModels_ = shaderModels;
        if ( logModels != null && logModels.length != naux ||
             flipModels != null &&  flipModels.length != naux ||
             shaderModels != null && shaderModels.length != naux ) {
            throw new IllegalArgumentException();
        }

        /* Define names for axes. */
        String[] auxNames = new String[ naux ];
        for ( int i = 0; i < naux; i++ ) {
            auxNames[ i ] = "Aux " + ( i + 1 );
        }

        /* Construct a subsidiary selector which will handle the auxiliary
         * axes only.  The behaviour of this object will combine the base
         * selector and this new one. */
        auxSelector_ =
            new CartesianAxesSelector( auxNames, logModels, flipModels,
                                       new ErrorModeSelectionModel[ 0 ] );

        /* Add selectors for choosing per-aux axis shaders if required. */
        if ( shaderModels_ != null ) {
            for ( int i = 0; i < naux; i++ ) {
                AxisDataSelector selector = auxSelector_.getDataSelector( i );
                JComboBox shaderSelector = new JComboBox( shaderModels_[ i ] );
                shaderSelector
                   .setRenderer( new ShaderListCellRenderer( shaderSelector ) );
                JComponent box = Box.createHorizontalBox();
                box.add( new ShrinkWrapper( shaderSelector ) );
                box.add( Box.createHorizontalStrut( 5 ) );
                box.add( new ComboBoxBumper( shaderSelector ) );
                box.add( Box.createHorizontalGlue() );
                selector.add( box );
            }
        }

        /* Prepare a panel to hold the auxiliary axes. */
        for ( int i = 0; i < naux; i++ ) {
            auxSelector_.getDataSelector( i )
                        .setBorder( BorderFactory
                                   .createEmptyBorder( 0, 0, 5, 0 ) );
        }
        selectorPanel_ = Box.createVerticalBox();
        selectorPanel_.add( baseSelector.getColumnSelectorPanel() );
        auxPanel_ = Box.createVerticalBox();
        selectorPanel_.add( auxPanel_ );
    }

    /**
     * Returns the selector that this one is augmenting (before the
     * auxiliary axes are added to it).
     *
     * @return  base axes selector
     */
    public Object getBase() {
        return baseSelector_;
    }

    /**
     * Returns the constituent selector which deals only with the auxiliary
     * axes.
     *
     * @return  auxiliary-only axes selector
     */
    public CartesianAxesSelector getAuxSelector() {
        return auxSelector_;
    }

    /**
     * Sets the number of auxiliary axis selectors which are visible.
     * The initial value is zero.  Calling this method will make the 
     * first <code>nVis</code> axes visible.
     *
     * @param  nVis  number of auxiliary axis selectors to show
     */
    public void setAuxVisible( int nVis ) {
        for ( int i = 0; i < naux_; i++ ) {
            JComponent csel = auxSelector_.getDataSelector( i );
            boolean isVis = i < nVisible_;
            boolean wantVis = i < nVis;
            if ( isVis && ! wantVis ) {
                auxPanel_.remove( csel );
            }
            else if ( ! isVis && wantVis ) {
                auxPanel_.add( csel );
            }
            else {
                assert isVis == wantVis;
            }
        }
        nVisible_ = nVis;
        auxPanel_.revalidate();
    }

    public JComponent getColumnSelectorPanel() {
        return selectorPanel_;
    }

    public JComboBox[] getColumnSelectors() {
        JComboBox[] baseSelectors = baseSelector_.getColumnSelectors();
        JComboBox[] auxSelectors = auxSelector_.getColumnSelectors();
        JComboBox[] selectors =
            new JComboBox[ baseSelectors.length + auxSelectors.length ];
        System.arraycopy( baseSelectors, 0, selectors, 0,
                          baseSelectors.length );
        System.arraycopy( auxSelectors, 0,
                          selectors, baseSelectors.length,
                          auxSelectors.length );
        return selectors;
    }

    public int getNdim() {
        return baseSelector_.getNdim() + nVisible_;
    }

    public boolean isReady() {
        return baseSelector_.isReady();
    }

    public StarTable getData() {
        StarTable baseData = baseSelector_.getData();

        /* If there are no auxiliary columns, the base table can be used
         * on its own. */
        if ( nVisible_ == 0 ) {
            return baseData;
        }

        /* If there are auxiliary columns, join the base table and the
         * auxiliary table together.  Some additional work is done here too:
         * we mark any auxiliary columns with data (i.e. with a non-blank
         * column selection) as 'required' when constructing the output table.
         * The effect of this is that if the value in any of those columns
         * is blank, the rest of the values in that row will be set blank
         * as well.  This prevents points corresponding to rows with bad
         * values for defined auxiliary axes from being plotted.
         * This slight use of subterfuge is necessary because otherwise the
         * resulting Points object can't distinguish between a point with
         * no column assigned to an auxiliary axis (that should be plotted)
         * and a point with a column assigned but a blank value (that should
         * not be plotted). */
        else {
            StarTable auxData = auxSelector_.getData();
            boolean[] reqAuxCols = new boolean[ naux_ ];
            for ( int iaux = 0; iaux < naux_; iaux++ ) {
                reqAuxCols[ iaux ] =
                    auxSelector_.getColumnSelector( iaux ).getSelectedItem()
                    != null;
            }
            return new AugmentedDataTable( baseData, auxData, reqAuxCols );
        }
    }

    public StarTable getErrorData() {
        return baseSelector_.getErrorData();
    }

    public ErrorMode[] getErrorModes() {
        return baseSelector_.getErrorModes();
    }

    public StarTable getLabelData() {
        return baseSelector_.getLabelData();
    }

    public AxisEditor[] createAxisEditors() {
        AxisEditor[] baseEds = baseSelector_.createAxisEditors();
        AxisEditor[] auxEds = auxSelector_.createAxisEditors();
        AxisEditor[] eds = new AxisEditor[ baseEds.length + auxEds.length ];
        System.arraycopy( baseEds, 0, eds, 0, baseEds.length );
        System.arraycopy( auxEds, 0, eds, baseEds.length, auxEds.length );
        return eds;
    }

    public PointStore createPointStore( int npoint ) {
        PointStore baseStore = baseSelector_.createPointStore( npoint );
        if ( nVisible_ == 0 ) {
            return baseStore;
        }
        else {
            if ( baseSelector_ instanceof CartesianAxesSelector &&
                 ( ! baseStore.hasLabels() ) ) {
                return new CartesianPointStore( getNdim(), getErrorModes(),
                                                npoint );
            }
            else {
                return new AugmentedPointStore( baseStore, nVisible_ );
            }
        }
    }

    public void setTable( TopcatModel tcModel ) {
        baseSelector_.setTable( tcModel );
        auxSelector_.setTable( tcModel );
        tcModel_ = tcModel;
    }

    public void initialiseSelectors() {
        baseSelector_.initialiseSelectors();
    }

    public void addActionListener( ActionListener listener ) {
        baseSelector_.addActionListener( listener );
        auxSelector_.addActionListener( listener );
    }

    public void removeActionListener( ActionListener listener ) {
        baseSelector_.removeActionListener( listener );
        auxSelector_.removeActionListener( listener );
    }

    /**
     * StarTable implementation which provides the data from this AxesSelector.
     * The main job it does is to stick the tables from the base columns
     * and the auxiliary columns side by side.  However, it also 
     * blanks out the base columns (replaces the values with nulls) in cases
     * where one of the required auxiliary columns is blank.
     * It implements equals and hashCode correctly, as is required for
     * tables returned by AxesSelectors.
     */
    private static class AugmentedDataTable extends WrapperStarTable {
        private final StarTable baseTable_;
        private final StarTable auxTable_;
        private final int[] icolReqs_;
        private final int nbase_;
        private final int nreq_;

        /**
         * Constructor.
         *
         * @param  baseTable  data table for base values
         * @param  auxTable   data table for auxiliary values
         * @param  reqAuxCols  naux-element array of flags for auxiliary 
         *         columns which are required to be present in data rows -
         *         if any of these is null the base values will be nulled 
         *         as well
         */
        public AugmentedDataTable( StarTable baseTable, StarTable auxTable,
                                   boolean[] reqAuxCols ) {
            super( new JoinStarTable( new StarTable[] { baseTable,
                                                        auxTable } ) );
            baseTable_ = baseTable;
            auxTable_ = auxTable;
            nbase_ = baseTable.getColumnCount();

            /* Prepare a lookup table of column indices which are required
             * to be non-null. */
            int[] reqs = new int[ reqAuxCols.length ];
            int jaux = 0;
            for ( int iaux = 0; iaux < reqAuxCols.length; iaux++ ) {
                if ( reqAuxCols[ iaux ] ) {
                    reqs[ jaux++ ] = nbase_ + iaux;
                }
            }
            nreq_ = jaux;
            icolReqs_ = new int[ nreq_ ];
            System.arraycopy( reqs, 0, icolReqs_, 0, nreq_ );
        }

        public Object[] getRow( long irow ) throws IOException {
            Object[] row = super.getRow( irow );
            boolean ok = true;
            for ( int ireq = 0; ok && ireq < nreq_; ireq++ ) {
                int ic = icolReqs_[ ireq ];
                ok = ok && notBlank( row[ ic ] );
            }
            if ( ! ok ) {
                for ( int ic = 0; ic < nbase_; ic++ ) {
                    row[ ic ] = null;
                }
            }
            return row;
        }

        public Object getCell( long irow, int icol ) throws IOException {
            if ( icol < nbase_ ) {
                boolean ok = true;
                for ( int ireq = 0; ok && ireq < nreq_; ireq++ ) {
                    int ic = icolReqs_[ ireq ];
                    ok = ok && notBlank( super.getCell( irow, ic ) );
                }
                return ok ? super.getCell( irow, icol ) : null;
            }
            else {
                return super.getCell( irow, icol );
            }
        }

        public RowSequence getRowSequence() throws IOException {
            return new WrapperRowSequence( super.getRowSequence() ) {

                public Object[] getRow() throws IOException {
                    Object[] row = super.getRow();
                    boolean ok = true;
                    for ( int ireq = 0; ok && ireq < nreq_; ireq++ ) {
                        int ic = icolReqs_[ ireq ];
                        ok = ok && notBlank( row[ ic ] );
                    }
                    if ( ! ok ) {
                        for ( int ic = 0; ic < nbase_; ic++ ) {
                            row[ ic ] = null;
                        }
                    }
                    return row;
                }

                public Object getCell( int icol ) throws IOException {
                    if ( icol < nbase_ ) {
                        boolean ok = true;
                        for ( int ireq = 0; ok && ireq < nreq_; ireq++ ) {
                            int ic = icolReqs_[ ireq ];
                            ok = ok && notBlank( super.getCell( ic ) );
                        }
                        return ok ? super.getCell( icol ) : null;
                    }
                    else {
                        return super.getCell( icol );
                    }
                }
            };
        }

        public boolean equals( Object o ) {
            if ( o instanceof AugmentedDataTable ) {
                AugmentedDataTable other = (AugmentedDataTable) o;
                return other.baseTable_.equals( this.baseTable_ )
                    && other.auxTable_.equals( this.auxTable_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 5501;
            code = 23 * code + baseTable_.hashCode();
            code = 23 * code + auxTable_.hashCode();
            return code;
        }

        /**
         * Determines whether the content of a cell represents a valid
         * number or not.
         *
         * @param  value  cell contents
         * @return  true if the value is a valid number
         */
        private boolean notBlank( Object value ) {
            return value instanceof Number
                && ! Double.isNaN( ((Number) value).doubleValue() );
        }
    }

    /**
     * PointStore implementation which augments a base point store with 
     * some additional axes.  These additional axes have no error data.
     */
    private static class AugmentedPointStore implements PointStore, Wrapper {
        final PointStore baseStore_;
        final PointStore augStore_;
        final int baseDim_;
        final int augDim_;
        final double[] point_;
        final Object[] baseCoords_;
        final Object[] augCoords_;
        final Object[] augErrors_;

        /**
         * Constructor.
         *
         * @param  base  base point store
         * @parm  nExtra  number of additional auxiliary axes to support
         */
        AugmentedPointStore( PointStore base, int nExtra ) {
            baseStore_ = base;
            augStore_ = new CartesianPointStore( nExtra, new ErrorMode[ 0 ],
                                                 base.getCount() );
            baseDim_ = baseStore_.getNdim();
            augDim_ = nExtra;
            point_ = new double[ baseDim_ + augDim_ ];
            baseCoords_ = new Object[ baseDim_ ];
            augCoords_ = new Object[ augDim_ ];
            augErrors_ = new Object[ 0 ];
        }

        public Object getBase() {
            return baseStore_;
        }

        public int getCount() {
            return baseStore_.getCount();
        }

        public int getNdim() {
            return baseDim_ + augDim_;
        }

        public int getNerror() {
            return baseStore_.getNerror();
        }

        public double[] getPoint( int ipoint ) {
            System.arraycopy( baseStore_.getPoint( ipoint ), 0, point_, 0,
                              baseDim_ );
            System.arraycopy( augStore_.getPoint( ipoint ), 0, point_, baseDim_,
                              augDim_ );
            return point_;
        }

        public double[][] getErrors( int ipoint ) {
            return baseStore_.getErrors( ipoint );
        }

        public boolean hasLabels() {
            return baseStore_.hasLabels();
        }

        public String getLabel( int ipoint ) {
            return baseStore_.getLabel( ipoint );
        }

        public void storePoint( Object[] coordRow, Object[] errorRow,
                                String label ) {
            System.arraycopy( coordRow, 0, baseCoords_, 0, baseDim_ );
            System.arraycopy( coordRow, baseDim_, augCoords_, 0, augDim_ );
            baseStore_.storePoint( baseCoords_, errorRow, label );
            augStore_.storePoint( augCoords_, augErrors_, null );
        }
    }
}
