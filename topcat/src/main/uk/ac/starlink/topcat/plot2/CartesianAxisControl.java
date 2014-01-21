package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Abstract superclass for simple Cartesian plots.
 * This mostly just handles the axis labelling GUI, and leaves subclasses
 * to adjust the details of the other configuration options.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public abstract class CartesianAxisControl<P,A> extends AxisControl<P,A> {

    private final AutoConfigSpecifier labelSpecifier_;
    private final Set<DataId> seenDataIdSet_;

    /**
     * Constructor.
     *
     * @param  surfFact  plot surface factory
     * @param  axisLabelKeys  config keys for axis labels
     * @param  stack   control stack, used to get default axis label strings
     */
    public CartesianAxisControl( SurfaceFactory<P,A> surfFact,
                                 final ConfigKey<String>[] axisLabelKeys,
                                 ControlStack stack ) {
        super( surfFact );
        final int ndim = axisLabelKeys.length;
        seenDataIdSet_ = new HashSet<DataId>();

        /* Set up a specifier component to get axis label values.
         * Each specifier has an automatic default. */
        labelSpecifier_ = new AutoConfigSpecifier( axisLabelKeys );
        final String[] axisNames = new String[ ndim ];
        for ( int id = 0; id < ndim; id++ ) {
            axisNames[ id ] = axisLabelKeys[ id ].getDefaultValue();
        }

        /* Fix it so that the default values for the axis labels are
         * taken from coordinate labels (table column names or expressions)
         * for data layers contributing to the plot.  These have to be
         * updated when the content of the plot control stack changes. */
        final ControlStackModel stackModel = stack.getStackModel();
        ActionListener stackListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                adjustDefaultAxisLabels();
            }
            private void adjustDefaultAxisLabels() {
                String[] axLabels = getAxisLabels( getLeadControl() );
                for ( int id = 0; id < ndim; id++ ) {
                    labelSpecifier_.getAutoSpecifier( axisLabelKeys[ id ] )
                                   .setAutoValue( axLabels[ id ] );
                }
            }
            private LayerControl getLeadControl() {
                LayerControl[] controls = stackModel.getActiveLayerControls();
                return controls.length > 0 ? controls[ 0 ] : null;
            }
            private String[] getAxisLabels( LayerControl control ) {
                if ( control == null ) {
                    return axisNames;
                }
                String[] labels = new String[ ndim ];
                for ( int id = 0; id < ndim; id++ ) {
                    String label = control.getCoordLabel( axisNames[ id ] );
                    labels[ id ] = label == null ? axisNames[ id ] : label;
                }
                return labels;
            }
        };
        stackModel.addPlotActionListener( stackListener );
        stackListener.actionPerformed( null );
    }

    /**
     * Adds the axis label configuration tab set up by this component.
     * It's not done in the constructor so that subclasses can decide
     * where it goes in terms of the other config tabs.
     */
    protected void addLabelsTab() {
        addSpecifierTab( "Labels", labelSpecifier_ );
    }

    /**
     * Returns the specifier used for axis labels.
     *
     * @return  axis label specifier
     */
    public AutoConfigSpecifier getLabelSpecifier() {
        return labelSpecifier_;
    }

    @Override
    protected boolean clearRange( P oldProfile, P newProfile,
                                  PlotLayer[] oldLayers, PlotLayer[] newLayers,
                                  boolean lock ) {

        /* Assemble a set of objects that characterise the datasets being
         * plotted by these layers.  This includes the tables and axes
         * but not the subset masks. */
        Set<DataId> dataIdSet = new HashSet<DataId>();
        for ( int il = 0; il < newLayers.length; il++ ) {
            PlotLayer layer = newLayers[ il ];
            DataGeom geom = layer.getDataGeom();
            DataSpec spec = layer.getDataSpec();
            if ( geom != null && spec != null ) {
                DataId did = new DataId( geom, spec );
                assert did.equals( new DataId( geom, spec ) );
                dataIdSet.add( did );
            }
        }

        /* Does this contain any datasets we've never seen before? */
        boolean hasNewLayers = ! seenDataIdSet_.containsAll( dataIdSet );

        /* If log scaling of some axes has changed, we unconditionally
         * re-range.  This might not always be the best thing to do
         * (could be improved), but as it stands if not we run the risk
         * of trying to plot negative logarithmic values, which will
         * cause an exception in the plotting. */
        if ( logChanged( oldProfile, newProfile ) ) {
            seenDataIdSet_.clear();
            seenDataIdSet_.addAll( dataIdSet );
            return true;
        }

        /* Otherwise, if the axis lock is in place, don't re-range. */
        else if ( lock ) {
            seenDataIdSet_.clear();
            seenDataIdSet_.addAll( dataIdSet );
            return false;
        }

        /* Otherwise, try to make an intelligent decision;
         * re-range only if there are new data sets present. */
        else if ( hasNewLayers ) {
            seenDataIdSet_.clear();
            seenDataIdSet_.addAll( dataIdSet );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Indicates whether the scaling has changed to or from logarithmic
     * for any of the cartesian axes between two profiles.
     * If so, it's going to be necessary to rescale, since attempting
     * a log plot with negative values would fail.
     *
     * @param  prof1  first profile
     * @param  prof2  second profile
     * @return  true iff some of the axes are log in prof1 and linear in prof2
     *               or vice versa
     */
    protected abstract boolean logChanged( P prof1, P prof2 );

    /**
     * Characterises a plotted data set for the purposes of working out
     * whether this is new data we need to re-range for.
     * DataIds are equal if they have the same table and the same
     * positional coordinates
     * (it's like a {@link uk.ac.starlink.ttools.plot2.PointCloud}
     * but without reference to the mask).
     * We leave the mask out because probably (though not for sure)
     * the first time a dataset is plotted the mask will be ALL,
     * so any subsequent changes will just remove data, which doesn't
     * warrant a re-range.
     */
    @Equality
    private static class DataId {
        List<Object> list_;

        /**
         * Constructor.
         *
         * @param  geom  data geom, not null
         * @param  dataSpec  data spec, not null
         */
        DataId( DataGeom geom, DataSpec dataSpec ) {
            int nc = geom.getPosCoords().length;
            int ni = nc + 1;
            list_ = new ArrayList<Object>( ni );
            list_.add( dataSpec.getSourceTable() );
            for ( int ic = 0; ic < nc; ic++ ) {
                list_.add( dataSpec.getCoordId( ic ) );
            }
            assert list_.size() == ni;
        }
        @Override
        public int hashCode() {
            return list_.hashCode();
        }
        @Override
        public boolean equals( Object other ) {
            return other instanceof DataId
                && this.list_.equals( ((DataId) other).list_ );
        }
    }
}
