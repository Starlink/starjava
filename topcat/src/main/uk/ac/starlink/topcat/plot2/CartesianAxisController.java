package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Abstract superclass for simple Cartesian plots.
 * This mostly just handles the axis labelling GUI, and leaves subclasses
 * to adjust the details of the other configuration options.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public abstract class CartesianAxisController<P,A> extends AxisController<P,A> {

    private final AutoConfigSpecifier labelSpecifier_;
    private final Set<DataId> seenDataIdSet_;

    /**
     * Constructor.
     *
     * @param  surfFact  plot surface factory
     * @param  axisLabelKeys  config keys for axis labels
     * @param  stack   control stack, used to get default axis label strings
     */
    public CartesianAxisController( SurfaceFactory<P,A> surfFact,
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
        getMainControl().addSpecifierTab( "Labels", labelSpecifier_ );
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
            CoordGroup cgrp = layer.getPlotter().getCoordGroup();
            if ( geom != null && spec != null &&
                 cgrp.getRangeCoordIndices( geom ).length > 0 ) {
                DataId did = new DataId( geom, spec, cgrp );
                assert did.equals( new DataId( geom, spec, cgrp ) );
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
        final DataGeom geom_;
        final StarTable srcTable_;
        final Object[] coordIds_;

        /**
         * Constructor.
         *
         * @param  geom  data geom, not null
         * @param  dataSpec  data spec, not null
         * @param  cgrp   coordinate group
         */
        DataId( DataGeom geom, DataSpec dataSpec, CoordGroup cgrp ) {
            geom_ = geom;
            srcTable_ = dataSpec.getSourceTable();

            /* A CoordinateGroup explicitly labels those coordinates that
             * are relevant for ranging.  Use these. */
            int[] rcis = cgrp.getRangeCoordIndices( geom );
            coordIds_ = new Object[ rcis.length ];
            for ( int i = 0; i < rcis.length; i++ ) {
                coordIds_[ i ] = dataSpec.getCoordId( rcis[ i ] );
            }
        }
        @Override
        public int hashCode() {
            int code = 33771;
            code = 23 * code + geom_.hashCode();
            code = 23 * code + srcTable_.hashCode();
            code = 23 * code + Arrays.hashCode( coordIds_ );
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof DataId ) {
                DataId other = (DataId) o;
                return this.geom_.equals( other.geom_ )
                    && this.srcTable_.equals( other.srcTable_ )
                    && Arrays.equals( this.coordIds_, other.coordIds_ );
            }
            else {
                return false;
            }
        }
    }
}
