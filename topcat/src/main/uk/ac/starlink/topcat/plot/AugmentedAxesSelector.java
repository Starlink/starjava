package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;

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
     *          per-axis log scaling
     * @param   flipModels  <code>naux</code>-element array of models flagging
     *          per-axis sense inversion
     */
    public AugmentedAxesSelector( AxesSelector baseSelector, int naux,
                                  ToggleButtonModel[] logModels,
                                  ToggleButtonModel[] flipModels ) {
        baseSelector_ = baseSelector;
        naux_ = naux;
        logModels_ = logModels;
        flipModels_ = flipModels;
        if ( logModels.length != naux || flipModels.length != naux ) {
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

            /* To combine the base and aux selector data tables we split
             * them into their constituent columns and reassemble them.
             * The more straightforward route of using a 
             * uk.ac.starlink.table.JoinStarTable is no good since that
             * would not implement equals() properly.  This makes some
             * assumptions about the implementations of component getData()
             * methods (their return values need to be of certain types). */
            StarTable auxData = auxSelector_.getData();
            if ( baseData instanceof ColumnStarTable && 
                 auxData instanceof ColumnStarTable ) {
                assert baseData instanceof ColumnDataTable;
                assert auxData instanceof ColumnDataTable;
                int nbase = baseData.getColumnCount();
                ColumnData[] cols = new ColumnData[ nbase + nVisible_ ];
                for ( int i = 0; i < nbase; i++ ) {
                    cols[ i ] =
                        ((ColumnStarTable) baseData).getColumnData( i );
                }
                for ( int i = 0; i < nVisible_; i++ ) {
                    cols[ i + nbase ] =
                        ((ColumnStarTable) auxData).getColumnData( i );
                }
                return new ColumnDataTable( tcModel_, cols );
            }
            else {
                throw new UnsupportedOperationException(
                              "Only works with ColumnDataTables" );
            }
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
                // need to support general (spherical) case too
                throw new UnsupportedOperationException();
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
}
