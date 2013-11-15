package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;

/**
 * Abstract superclass for control that configures details of a plot's
 * axes, including surface aspect and ranges.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public abstract class AxisControl<P,A> extends ConfigControl {

    private final SurfaceFactory<P,A> surfFact_;
    private final ToggleButtonModel stickyModel_;
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
    protected AxisControl( SurfaceFactory<P,A> surfFact ) {
        super( "Axes", ResourceIcon.AXIS_EDIT );
        surfFact_ = surfFact;
        stickyModel_ =
            new ToggleButtonModel( "Lock Axes", ResourceIcon.AXIS_LOCK,
                                   "Do not auto-rescale axes" );
        lastLayers_ = new PlotLayer[ 0 ];
        lastProfile_ = surfFact.createProfile( new ConfigMap() );
        aspectPanels_ = new ArrayList<ActionSpecifierPanel>();
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
     * Returns a toggler which controls whether auto-rescaling should be
     * inhibited.  May be overridden to return null if the axis control
     * does not honour the setting of such a model.
     *
     * @return   axis lock model, or null
     * @see   #clearRange
     */
    public ToggleButtonModel getAxisLockModel() {
        return stickyModel_;
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
     * Adds a tab for selecting navigator options.
     * These are determined by the surface factory.
     */
    protected void addNavigatorTab() {
        ConfigSpecifier navSpecifier =
            new ConfigSpecifier( surfFact_.getNavigatorKeys() );
        addSpecifierTab( "Navigation", navSpecifier );
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
     * Adds a tab for specifying the aspect.
     * This is like a config tab for the aspect keys, but has additional
     * submit decoration.
     *
     * @param  label  tab label
     * @param  aspectSpecifier   config specifier for aspect keys
     */
    protected void addAspectConfigTab( String label,
                                       Specifier<ConfigMap> aspectSpecifier ) {
        ActionSpecifierPanel aspectPanel =
                new ActionSpecifierPanel( aspectSpecifier ) {
            protected void doSubmit( ActionEvent evt ) {
                setAspect( null );
            }
        };
        aspectPanels_.add( aspectPanel );
        addSpecifierTab( label, aspectPanel );
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
     * Configures this axis control for a given set of plot layers.
     * This may trigger a resetting of the aspect and ranges, generally
     * if the new plot is sufficiently different from most recent one.
     * Whether that's the case is determined by calling
     * {@link #clearRange clearRange}.
     *
     * <p>This isn't perfect, since it only allows to clear the range or not.
     * Sometimes you might want finer control, e.g. to clear the
     * range in one dimension and retain it in others.  It may be
     * possible to fit that into the configureForLayers API, but it
     * would require more work.
     *
     * @param  profile   surface profile
     * @param  layers   layers which will be plotted
     */
    public void configureForLayers( P profile, PlotLayer[] layers ) {
        if ( clearRange( lastProfile_, profile, lastLayers_, layers,
                         stickyModel_.isSelected() ) ) {
            setRanges( null );
            setAspect( null );
        }
        lastProfile_ = profile;
        lastLayers_ = layers;
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
    protected abstract boolean clearRange( P oldProfile, P newProfile,
                                           PlotLayer[] oldLayers,
                                           PlotLayer[] newLayers,
                                           boolean lock );
}
