package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.topcat.ActionForwarder;

/**
 * Manages control of GUI components that work with multiple plotting zones.
 *
 * <p>In this implementation, it adds new fixed Axes controls per zone to
 * the control stack, one for each zone currently active.
 * Future versions may do something more visually manageable,
 * but can probably retain a similar API.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2016
 */
public class MultiController<P,A> {

    private final PlotTypeGui<P,A> plotType_;
    private final ControlStackPanel stackPanel_;
    private final MultiConfigger configger_;
    private final List<ZoneId> lastZones_;
    private final Map<ZoneId,AxisController<P,A>> axisControllers_;
    private final ActionForwarder actionForwarder_;

    /**
     * Constructor.
     *
     * @param  plotType   plot type
     * @param  stackPanel   container for fixed (not per-layer) stack controls
     * @param  configger   manages global and per-zone axis config items
     */
    public MultiController( PlotTypeGui<P,A> plotType,
                            ControlStackPanel stackPanel,
                            MultiConfigger configger ) {
        plotType_ = plotType;
        stackPanel_ = stackPanel;
        configger_ = configger;
        axisControllers_ = new java.util.HashMap<ZoneId,AxisController<P,A>>();
        lastZones_ = new ArrayList<ZoneId>();
        actionForwarder_ = new ActionForwarder();
    }

    /**
     * Sets the list of zone obects that are to be visible in the current 
     * state of the GUI.
     *
     * @param   zones   read-only list of zoneIds whose configuration will
     *                  be accessible from the GUI
     */
    public void setZones( Collection<ZoneId> zones ) {
        List<ZoneId> removables = new ArrayList<ZoneId>( lastZones_ );
        removables.removeAll( zones );
        List<ZoneId> addables = new ArrayList<ZoneId>( zones );
        addables.removeAll( lastZones_ );
        lastZones_.clear();
        lastZones_.addAll( zones );
        for ( ZoneId zid : removables ) {
            for ( Control c : axisControllers_.get( zid ).getControls() ) {
                stackPanel_.removeFixedControl( c );
            }
        }
        for ( ZoneId zid : addables ) {
            if ( ! axisControllers_.containsKey( zid ) ) {
                AxisController<P,A> ac = plotType_.createAxisController();
                axisControllers_.put( zid, ac );
                configger_.addZoneConfigger( zid, ac );
                ac.addActionListener( actionForwarder_ );
            }
            for ( Control c : axisControllers_.get( zid ).getControls() ) {
                stackPanel_.addFixedControl( c );
            }
        }
    }

    /**
     * Sets the surface aspect to use for a given zone.
     *
     * @param   ganger   object that defines multi-zone positioning
     * @param   zid    zone whose aspect is to be updated;
     *                 can, but probably shouldn't, be null
     * @param   aspect  new aspect
     */
    public void setAspect( Ganger<A> ganger, ZoneId zid, A aspect ) {

        /* Assemble an array of existing aspects. */
        int nz = lastZones_.size();
        AxisController<P,A>[] axControllers =
            (AxisController<P,A>[]) new AxisController[ nz ];
        A[] aspects = (A[]) new Object[ nz ];
        int iz0 = -1;
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneId zid1 = lastZones_.get( iz );
            axControllers[ iz ] = axisControllers_.get( zid1 );
            aspects[ iz ] = axControllers[ iz ].getAspect();
            if ( zid != null && zid.equals( zid1 ) ) {
                iz0 = iz;
            }
        }

        /* Update the requested one. */
        if ( iz0 >= 0 ) {
            aspects[ iz0 ] = aspect;
        }

        /* Ensure aspects of all zones are consistent. */
        aspects = ganger.adjustAspects( aspects, iz0 );

        /* Write the updated aspects to their controller objects. */
        for ( int iz = 0; iz < nz; iz++ ) {
            axControllers[ iz ].setAspect( aspects[ iz ] );
        }
    }

    /**
     * Resets aspects of all the current per-zone controllers.
     */
    public void resetAspects() {
        for ( AxisController<P,A> ac : axisControllers_.values() ) {
            ac.setAspect( null );
            ac.setRanges( null );
            ac.clearAspect();
        }
    }

    /**
     * Returns the axis controller for a given zone.
     * This should be one included in the most recent call to
     * {@link #setZones setZones}
     *
     * @param   zid  known zone id
     * @return  axis controller for zone
     */
    public AxisController<P,A> getAxisController( ZoneId zid ) {
        return axisControllers_.get( zid );
    }

    /**
     * Adds a listener that will be notified when any of the managed
     * GUI components is updated.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }
}
