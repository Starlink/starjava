package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;

/**
 * Object which configures details of a plot's axes, including surface
 * aspect and ranges.
 * As well as methods which are used by the plot classes to interrogate
 * and configure the plot programmatically, this supplies one or more
 * controls to be placed in the GUI allowing user control of these things.
 *
 * @author   Mark Taylor
 * @since    22 Jan 2014
 */
public abstract class AxisController<P,A> implements Configger {

    private final SurfaceFactory<P,A> surfFact_;
    private final ConfigControl mainControl_;
    private final ActionForwarder actionForwarder_;
    private final List<ConfigControl> controlList_;
    private final Set<DataId> seenDataIdSet_;
    private ConfigMap aspectConfig_;
    private Range[] ranges_;
    private A aspect_;
    private P lastProfile_;
    private PlotLayer[] lastLayers_;
    private List<ActionSpecifierPanel> aspectPanels_;
    private ConfigSpecifier navSpecifier_;

    /**
     * Constructor.
     * The surface factory is supplied.  This is not actually required by
     * the AxisControl class, but most subclasses will need it so it's
     * convenient to store it here.
     *
     * @param  surfFact  plot surface factory
     */
    protected AxisController( SurfaceFactory<P,A> surfFact ) {
        surfFact_ = surfFact;
        mainControl_ = new ConfigControl( "Axes", ResourceIcon.AXIS_CONFIG );
        actionForwarder_ = new ActionForwarder();
        controlList_ = new ArrayList<ConfigControl>();
        addControl( mainControl_ );
        lastLayers_ = new PlotLayer[ 0 ];
        lastProfile_ = surfFact.createProfile( new ConfigMap() );
        aspectPanels_ = new ArrayList<ActionSpecifierPanel>();
        seenDataIdSet_ = new HashSet<DataId>();
    }

    /**
     * Returns this control's surface factory.
     *
     * @return   plot surface factory
     */
    public SurfaceFactory<P,A> getSurfaceFactory() {
        return surfFact_;
    }

    /**
     * Returns the control that provides the main part of the GUI
     * configurability.  Subclasses may provide additional controls.
     *
     * @return  main control
     */
    public ConfigControl getMainControl() {
        return mainControl_;
    }

    /**
     * Returns all the controls for user configuration of this controller.
     * This includes the main control and possibly others.
     *
     * @return  user controls
     */
    public Control[] getControls() {
        return controlList_.toArray( new Control[ 0 ] );
    }

    /**
     * Adds a control to the list of controls managed by this object.
     *
     * @param  control   control to add
     */
    public void addControl( ConfigControl control ) {
        controlList_.add( control );
        control.addActionListener( actionForwarder_ );
    }

    /**
     * Returns the configuration defined by all this object's controls.
     *
     * @return   config map
     */
    public ConfigMap getConfig() {
        ConfigMap config = new ConfigMap();
        for ( ConfigControl control : controlList_ ) {
            config.putAll( control.getConfig() );
        }
        return config;
    }

    /**
     * Adds a listener notified when any of the controls changes.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added by addActionListener.
     *
     * @param   listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    /**
     * Returns an object which will forward actions to listeners registered
     * with this panel.
     *
     * @return  action forwarder
     */
    public ActionListener getActionForwarder() {
        return actionForwarder_;
    }

    /**
     * Sets fixed data position coordinate ranges.
     * If these are not set then they may need to be calculated by
     * examining the data to work out the plot aspect.
     * Setting them to null ensures a re-range if required next time.
     *
     * @param  ranges  fixed data position coordinate ranges, or null to clear
     */
    public void setRanges( Range[] ranges ) {
        ranges_ = ranges;
    }

    /**
     * Returns the current fixed data coordinate ranges.
     * If not known, null is returned.
     *
     * @return   fixed data position coordinate ranges, or null if not known
     */
    public Range[] getRanges() {
        return ranges_;
    }

    /**
     * Sets the plot aspect which defines the view on the data.
     * If not set, it may have to be worked out from config and range inputs.
     *
     * @param  aspect  fixed aspect, or null to clear
     */
    public void setAspect( A aspect ) {
        aspect_ = aspect;
    }

    /**
     * Returns the plot aspect to use for setting up the plot surface.
     * If not known, null is returned.
     *
     * @return  fixed aspect, or null if none set
     */
    public A getAspect() {
        return aspect_;
    }

    /**
     * This abstract method provides a hook for implementations to adjust
     * their GUI state based on the layer controls which will be supplying
     * layers for them to plot.
     *
     * @param   layerControls   layer controls expected to provide layers
     *                          to be plotted on these axes
     */
    public abstract void configureForLayers( LayerControl[] layerControls );

    /**
     * Adds a tab to the main control for selecting navigator options.
     * These are determined by the surface factory.
     */
    protected void addNavigatorTab() {
        ConfigSpecifier navSpecifier =
            new ConfigSpecifier( surfFact_.getNavigatorKeys() );
        mainControl_.addSpecifierTab( "Navigation", navSpecifier );
        navSpecifier_ = navSpecifier;
    }

    /**
     * Returns the navigator specified by this control.
     *
     * @return  current navigator
     */
    public Navigator<A> getNavigator() {
        ConfigMap config = navSpecifier_ == null
                         ? new ConfigMap()
                         : navSpecifier_.getSpecifiedValue();
        return surfFact_.createNavigator( config );
    }

    /**
     * Adds a tab to the main control for specifying the aspect.
     * This is like a config tab for the aspect keys, but has additional
     * submit decoration.
     *
     * @param  label  tab label
     * @param  aspectSpecifier   config specifier for aspect keys
     */
    protected void addAspectConfigTab( String label,
                                       Specifier<ConfigMap> aspectSpecifier ) {
        ActionSpecifierPanel aspectPanel =
                new ActionSpecifierPanel( aspectSpecifier );
        aspectPanel.addActionListener( evt -> setAspect( null ) );
        aspectPanels_.add( aspectPanel );
        mainControl_.addSpecifierTab( label, aspectPanel );
    }

    /**
     * Clears any settings in tabs added by the
     * {@link #addAspectConfigTab addAspectConfigTab} method.
     */
    public void clearAspect() {
        for ( ActionSpecifierPanel aspectPanel : aspectPanels_ ) {
            aspectPanel.clear();
        }
    }

    /**
     * Configures this controller for a given set of plot layers.
     * This may trigger a resetting of the aspect and ranges, generally
     * if the new plot is sufficiently different from most recent one.
     * Whether that's the case is determined by calling
     * {@link #clearRange clearRange}.
     *
     * <p>This isn't perfect, since it only allows to clear the range or not.
     * Sometimes you might want finer control, e.g. to clear the
     * range in one dimension and retain it in others.  It may be
     * possible to fit that into the updateState API, but it
     * would require more work.
     *
     * @param  profile   surface profile
     * @param  layers   layers which will be plotted
     * @param  axisLock  whether re-ranging is inhibited;
     *                   normally, if true axes will not be reset by this
     *                   method, but the implementation can override that
     *                   if it needs to
     */
    public void updateState( P profile, PlotLayer[] layers, boolean axisLock ) {
        if ( clearRange( lastProfile_, profile, lastLayers_, layers,
                         axisLock ) ) {
            setRanges( null );
            setAspect( null );
        }
        lastProfile_ = profile;
        lastLayers_ = layers;
    }

    /**
     * Utility method to assert that all of a given set of keys
     * are actually being obtained by this controller.
     *
     * @param  requiredKeys   list of keys this control should obtain
     * @return  true iff the <code>getConfig</code> method contains entries
     *          for all the required keys
     * @throws  AssertionError if the result would be false and assertions
     *                         are enabled
     */
    public boolean assertHasKeys( ConfigKey<?>[] requiredKeys ) {
        Set<ConfigKey<?>> reqSet =
            new HashSet<ConfigKey<?>>( Arrays.asList( requiredKeys ) );
        Set<ConfigKey<?>> gotSet = getConfig().keySet();
        reqSet.removeAll( gotSet );
        assert reqSet.isEmpty() : "Missing required keys " + reqSet;
        return reqSet.isEmpty();
    }

    /**
     * Accepts report information generated by plotting layers.
     * Null map values are permitted, with the same meaning as an empty map.
     *
     * <p>The default implementation does nothing, but subclasses may
     * override it to enquire about plot results.
     *
     * @param   reports  per-layer plot reports for layers generated
     *                   by the most recent plot
     */
    public void submitReports( Map<LayerId,ReportMap> reports ) {
    }

    /**
     * Accepts information about the latest plotting surface to be plotted
     * by the panel associated with this controller.
     * This method is intended for passing information back to this controller,
     * which does not otherwise have access to the asynchronously constructed
     * Surface object it has configured,
     * it is not an instruction to adjust the current state.
     *
     * <p>The default implementation does nothing, but subclasses may
     * override it to acquire information about plot state.
     *
     * @param  surface  latest plotting surface.
     */
    public void setLatestSurface( Surface surface ) {
    }

    /**
     * Indicates whether a new configuration should result in clearing
     * the current ranges and plot aspect.
     *
     * @param   oldProfile  profile for last plot
     * @param   newProfile  profile for next plot
     * @param   oldLayers   layer set for last plot
     * @param   newLayers   layer set for next plot
     * @param   lock        whether re-ranging is inhibited;
     *                      normally, if <code>lock</code> is true this
     *                      method should return false, but the implementation
     *                      can overrule this and return true even when locked
     *                      if it needs to
     * @return  true iff the range should be re-established for the next plot
     */
    protected boolean clearRange( P oldProfile, P newProfile,
                                  PlotLayer[] oldLayers, PlotLayer[] newLayers,
                                  boolean lock ) {

        /* Assemble a set of objects that characterise the datasets being
         * plotted by these layers. */
        Set<DataId> dataIdSet = new HashSet<DataId>();
        for ( int il = 0; il < newLayers.length; il++ ) {
            DataId did = createDataId( newLayers[ il ] );
            if ( did != null ) {
                assert did.equals( createDataId( newLayers[ il ] ) );
                dataIdSet.add( did );
            }
        }

        /* Does this contain any datasets we've never seen before? */
        boolean hasNewLayers = ! seenDataIdSet_.containsAll( dataIdSet );

        /* A re-range may be required by the change in profile. */
        if ( forceClearRange( oldProfile, newProfile ) ) {
            seenDataIdSet_.clear();
            seenDataIdSet_.addAll( dataIdSet );
            return true;
        }

        /* Otherwise, if the axis lock is in place, do not re-range. */
        else if ( lock ) {
            seenDataIdSet_.clear();
            seenDataIdSet_.addAll( dataIdSet );
            return false;
        }

        /* Otherwise, try to make an intelligent decision:
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
     * Determines whether the change between two profiles forces an
     * unconditional re-range of the plotting surface.
     * This method should return true if the plotting surface will change
     * between the old and new supplied profiles sufficiently to invalidate
     * previously calculated auto range data.
     *
     * @param  oldProfile   profile before change
     * @param  newProfile   profile after change
     * @return  true if a new auto-ranging is required
     */
    protected abstract boolean forceClearRange( P oldProfile, P newProfile );

    /**
     * Returns an object that characterises a plot layer for ranging
     * purposes.  Two layers that return the equivalent ({@link Equality})
     * results from this method should be treated as having the same ranges.
     *
     * @param  layer  plot layer
     * @return   opaque object characterising identity of ranging information
     *           associated with the layer, or null if no range
     */
    @Equality
    private DataId createDataId( PlotLayer layer ) {
        DataGeom geom = layer.getDataGeom();
        DataSpec spec = layer.getDataSpec();
        Plotter<? extends Style> plotter = layer.getPlotter();
        CoordGroup cgrp = plotter.getCoordGroup();
        Object styleKey = getRangeStyleKey( plotter, layer );
        return spec == null ||
               cgrp.getRangeCoordIndices( geom ).length == 0
             ? null 
             : new DataId( geom, spec, cgrp, styleKey );
    }

    /**
     * Utility method to extract a RangeStyleKey from a plot layer.
     * Required to work round generics; PlotLayer is not typed, but Plotter is.
     *
     * @param  plotter  plotter
     * @param  layer   plot layer created by <code>plotter</code>
     * @return  result of calling <code>plotter.getRangeStyleKey(style)</code>
     */
    private static <S extends Style> Object
            getRangeStyleKey( Plotter<S> plotter, PlotLayer layer ) {
        @SuppressWarnings("unchecked")
        S style = (S) layer.getStyle();
        return plotter.getRangeStyleKey( style );
    }

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
        private final String geomVariant_;
        private final StarTable srcTable_;
        private final Object[] coordIds_; 
        private final Object styleKey_;

        /**
         * Constructor.
         *
         * @param  geom  data geom
         * @param  dataSpec  data spec, not null
         * @param  cgrp   coordinate group 
         * @param  styleKey  object characterising style influence on range
         */
        DataId( DataGeom geom, DataSpec dataSpec, CoordGroup cgrp,
                Object styleKey ) {
            srcTable_ = dataSpec.getSourceTable();
            styleKey_ = styleKey;

            /* Use the variant name of the DataGeom rather than the DataGeom's
             * identity itself.  This is a bit of a hack, but it means that
             * DataGeoms having the same name can be used to indicate they
             * are similar enough to each other not to trigger a re-range. */
            geomVariant_ = geom == null ? null : geom.getVariantName();

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
            code = 23 * code + PlotUtil.hashCode( geomVariant_ );
            code = 23 * code + srcTable_.hashCode();
            code = 23 * code + Arrays.hashCode( coordIds_ );
            code = 23 * code + PlotUtil.hashCode( styleKey_ );
            return code; 
        }
        @Override  
        public boolean equals( Object o ) {
            if ( o instanceof DataId ) {
                DataId other = (DataId) o;
                return PlotUtil.equals( this.geomVariant_, other.geomVariant_ )
                    && this.srcTable_.equals( other.srcTable_ )
                    && Arrays.equals( this.coordIds_, other.coordIds_ )
                    && PlotUtil.equals( this.styleKey_, other.styleKey_ );
            }
            else {
                return false;
            }
        }
    }
}
