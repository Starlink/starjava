package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * AxesSelector implementation that wraps an existing one and adds some
 * auxiliary axes of its own.  These auxiliary axes are optional and
 * may be rendered visible or not using the {@link #setAuxVisible} method.
 *
 * @author   Mark Taylor
 * @since    4 Jun 2007
 */
public class AugmentedAxesSelector implements AxesSelector {

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
                shaderSelector.setRenderer( Shaders.SHADER_RENDERER );
                JComponent box = Box.createHorizontalBox();
                box.add( new ShrinkWrapper( shaderSelector ) );
                box.add( Box.createHorizontalStrut( 5 ) );
                box.add( new ComboBoxBumper( shaderSelector ) );
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
    public AxesSelector getBaseSelector() {
        return baseSelector_;
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

    /**
     * Returns the number of columns which may have non-blank data.
     * High-numbered columns which have not been filled in are excluded
     * from this count.  Low-numbered ones (ones for which there is a filled
     * one with a higher index) are included so as not to confuse the arrays.
     *
     * @return  one plus the index of the highest-numbered visible 
     *          auxiliary column which has (or may have) non-blank data
     */
    public int getFilledAuxColumnCount() {
        int count = 0;
        for ( int icol = 0; icol < nVisible_; icol++ ) {
            if ( auxSelector_.getColumnSelector( icol )
                             .getSelectedItem() != null ) {
                count = icol + 1;
            }
        }
        return count;
    }

    public JComponent getColumnSelectorPanel() {
        return selectorPanel_;
    }

    public int getNdim() {
        return baseSelector_.getNdim() + nVisible_;
    }

    public boolean isReady() {
        return baseSelector_.isReady();
    }

    public StarTable getData() {
        StarTable baseData = baseSelector_.getData();
        if ( nVisible_ == 0 ) {
            return baseData;
        }
        else {

            /* The augmented table is just a join of the base and auxiliary
             * data tables.  However we need to make sure that it implements
             * equals properly, which JoinStarTable doesn't. */
            StarTable auxData = auxSelector_.getData();
            return new JoinStarTable( new StarTable[] { baseData, auxData, } ) {
                public boolean equals( Object o ) {
                    if ( o instanceof JoinStarTable ) {
                        JoinStarTable other = (JoinStarTable) o;
                        return this.getTables().equals( other.getTables() );
                    }
                    else {
                        return false;
                    }
                }
                public int hashCode() {
                    int code = 9901;
                    for ( Iterator it = getTables().iterator();
                          it.hasNext(); ) {
                        code = 23 * code + it.next().hashCode();
                    }
                    return code;
                }
            };
        }
    }

    public StarTable getErrorData() {
        return baseSelector_.getErrorData();
    }

    public ErrorMode[] getErrorModes() {
        return baseSelector_.getErrorModes();
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
            if ( baseSelector_ instanceof CartesianAxesSelector ) {
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
     * PointStore implementation which augments a base point store with 
     * some additional axes.  These additional axes have no error data.
     */
    private static class AugmentedPointStore implements PointStore {
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

        public void storePoint( Object[] coordRow, Object[] errorRow ) {
            System.arraycopy( coordRow, 0, baseCoords_, 0, baseDim_ );
            System.arraycopy( coordRow, baseDim_, augCoords_, 0, augDim_ );
            baseStore_.storePoint( baseCoords_, errorRow );
            augStore_.storePoint( augCoords_, augErrors_ );
        }
    }
}
