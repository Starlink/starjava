package uk.ac.starlink.topcat.plot2;

import java.util.Map;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;

/**
 * ZoneController implementation for use with one-zone plots.
 * This is a wrapper around the legacy {@link AxisController} class.
 *
 * @author   Mark Taylor
 * @since    18 Aug 2023
 */
public class SingleAdapterZoneController<P,A> implements ZoneController<P,A> {

    private final AxisController<P,A> axisController_;

    /**
     * Constructor.
     *
     * @param  axisController  object to which behaviour is delegated
     */
    public SingleAdapterZoneController( AxisController<P,A> axisController ) {
        axisController_ = axisController;
    }

    protected AxisController<P,A> getAxisController() {
        return axisController_;
    }

    public void setRanges( Range[] ranges ) {
        axisController_.setRanges( ranges );
    }

    public Range[] getRanges() {
        return axisController_.getRanges();
    }

    public void setAspect( A aspect ) {
        axisController_.setAspect( aspect );
    }

    public A getAspect() {
        return axisController_.getAspect();
    }

    public void clearAspect() {
        axisController_.clearAspect();
    }

    public void updateState( P profile, PlotLayer[] layers,
                             boolean axisLock ) {
        axisController_.updateState( profile, layers, axisLock );
    }

    public void submitReports( Map<LayerId,ReportMap> reports ) {
        axisController_.submitReports( reports );
    }

    public void setLatestSurface( Surface surface ) {
        axisController_.setLatestSurface( surface );
    }

    public Navigator<A> getNavigator() {
        return axisController_.getNavigator();
    }

    public ConfigMap getConfig() {
        return axisController_.getConfig();
    }
}
