package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;

/**
 * MultiController that works with AxisController instances.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2016
 */
public class MultiAxisController<P,A>
        extends MultiController<AxisController<P,A>> {

    private final SurfaceFactory<P,A> surfFact_;

    /**
     * Constructor.
     *
     * @param  plotType   plot type
     * @param  zfact     zone id factory
     * @param  configger   manages global and per-zone axis config items
     */
    public MultiAxisController( PlotTypeGui<P,A> plotType,
                                SurfaceFactory<P,A> surfFact,
                                ZoneFactory zfact,
                                MultiConfigger configger ) {
        super( new AxisControllerFactory<P,A>( plotType ), zfact, configger );
        surfFact_ = surfFact;
    }

    /**
     * Sets the surface aspect to use for a given zone.
     *
     * @param   ganger   object that defines multi-zone positioning
     * @param   zid    zone whose aspect is to be updated;
     *                 can, but probably shouldn't, be null
     * @param   aspect  new aspect
     */
    public void setAspect( Ganger<P,A> ganger, ZoneId zid, A aspect ) {

        /* Assemble an array of existing aspects. */
        ZoneId[] zones = getZones();
        int nz = zones.length;
        List<AxisController<P,A>> axControllers =
            new ArrayList<AxisController<P,A>>( nz );
        A[] aspects = PlotUtil.createAspectArray( surfFact_, nz );
        int iz0 = -1;
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneId zid1 = zones[ iz ];
            AxisController<P,A> ac = getController( zid1 );
            axControllers.add( ac );
            aspects[ iz ] = ac.getAspect();
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
            axControllers.get( iz ).setAspect( aspects[ iz ] );
        }
    }

    /**
     * Resets aspects of all the current per-zone controllers.
     */
    public void resetAspects() {
        for ( AxisController<P,A> ac : getControllerMap().values() ) {
            ac.setAspect( null );
            ac.setRanges( null );
            ac.clearAspect();
        }
    }

    /**
     * ControllerFactory implementation for AxisControllers.
     */
    private static class AxisControllerFactory<P,A>
            implements ControllerFactory<AxisController<P,A>> {
        final PlotTypeGui<P,A> plotType_;
        final int nControl_;

        /**
         * Constructor.
         *
         * @param  plotType  plot type
         */
        AxisControllerFactory( PlotTypeGui<P,A> plotType ) {
            plotType_ = plotType;
            nControl_ = plotType.createAxisController().getControls().length;
        }

        public int getControlCount() {
            return nControl_;
        }

        public AxisController<P,A> createController() {
            return plotType_.createAxisController();
        }

        public Control[] getControls( AxisController<P,A> controller ) {
            return controller.getControls();
        }

        public Configger getConfigger( AxisController<P,A> controller ) {
            return controller;
        }
    }
}
