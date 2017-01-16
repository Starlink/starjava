package uk.ac.starlink.ttools.plot2.task;

/**
 * Encapsulates some choices about what caching is performed when
 * preparing a plot.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2017
 */
public class PlotCaching {

    private boolean cacheImage_;
    private boolean reuseRanges_;
    private boolean usePlans_;

    /**
     * Constructs an instance with no caching.
     */
    public PlotCaching() {
    }

    /**
     * Sets image caching policy.
     * If true, plot image will be cached where applicable,
     * if false it will be regenerated from the data on every repaint.
     *
     * @param   cacheImage  image caching policy
     */
    public void setCacheImage( boolean cacheImage ) {
        cacheImage_ = cacheImage;
    }

    /**
     * Returns images caching policy.
     * If true, plot image will be cached where applicable,
     * if false it will be regenerated from the data on every repaint.
     *
     * @return  image caching policy
     */
    public boolean getCacheImage() {
        return cacheImage_;
    }

    /**
     * Sets aux range caching policy.
     * If true, aux ranges will be calculated only once,
     * if false they will be recalculated when the surface changes.
     *
     * @param  reuseRanges  aux range caching policy
     */
    public void setReuseRanges( boolean reuseRanges ) {
        reuseRanges_ = reuseRanges;
    }

    /**
     * Returns the aux range caching policy.
     * If true, aux ranges will be calculated only once,
     * if false they will be recalculated when the surface changes.
     *
     * @return  aux range caching policy
     */
    public boolean getReuseRanges() {
        return reuseRanges_;
    }

    /**
     * Sets plan caching policy.
     * If true, plan objects will be retained following a plot and
     * used as input to the next plotting attempt;
     * if false, no plans will be cached.
     *
     * @param  usePlans   drawing plan caching policy
     * @see   uk.ac.starlink.ttools.plot2.Drawing#calculatePlan
     */
    public void setUsePlans( boolean usePlans ) {
        usePlans_ = usePlans;
    }

    /**
     * Returns the plan caching policy.
     * If true, plan objects will be retained following a plot and
     * used as input to the next plotting attempt;
     * if false, no plans will be cached.
     *
     * @return  drawing plan caching policy
     * @see   uk.ac.starlink.ttools.plot2.Drawing#calculatePlan
     */
    public boolean getUsePlans() {
        return usePlans_;
    }

    /**
     * Returns an instance in which as much as possible is cached between
     * plot frames.
     *
     * @return  aggressively caching instance
     */
    public static PlotCaching createFullyCached() {
        PlotCaching c = new PlotCaching();
        c.setCacheImage( true );
        c.setReuseRanges( true );
        c.setUsePlans( true );
        return c;
    }

    /**
     * Returns an instance in which nothing is cached between plot frames.
     *
     * @return   non-caching instance
     */
    public static PlotCaching createUncached() {
        PlotCaching c = new PlotCaching();
        c.setCacheImage( false );
        c.setReuseRanges( false );
        c.setUsePlans( false );
        return c;
    }
}
