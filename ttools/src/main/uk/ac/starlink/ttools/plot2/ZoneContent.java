package uk.ac.starlink.ttools.plot2;

/**
 * Gives the layer content and surface construction details
 * for one zone of a Gang.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public class ZoneContent<P,A> {

    private final P profile_;
    private final A aspect_;
    private final PlotLayer[] layers_;

    /**
     * Constructor.
     *
     * @param   profile  profile of surface
     * @param   aspect   aspect of surface
     * @param   layers   plot layers to be painted
     */
    public ZoneContent( P profile, A aspect, PlotLayer[] layers ) {
        profile_ = profile;
        aspect_ = aspect;
        layers_ = layers.clone();
    }

    /**
     * Returns surface profile.
     *
     * @return  profile of zone surface
     */
    public P getProfile() {
        return profile_;
    }

    /**
     * Returns surface aspect.
     *
     * @return  aspect of zone surface
     */
    public A getAspect() {
        return aspect_;
    }

    /**
     * Returns plot layers.
     *
     * @return   layers to paint in zone
     */
    public PlotLayer[] getLayers() {
        return layers_.clone();
    }
}
