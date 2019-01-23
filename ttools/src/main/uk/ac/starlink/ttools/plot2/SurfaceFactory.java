package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Generates members of a family of Surface objects.
 * Surface configuration is provided by two objects of parameterised
 * types, a Profile (type P) and an Aspect (type A).
 * The profile provides fixed configuration items, and the aspect
 * provides items that may change according to different views of the
 * same surface, in particular as the result of  pan/zoom type operations.
 * This object is self-documenting, in that it can report the
 * the configuration keys required to specify profile/aspect.
 *
 * @author   Mark Taylor
 * @since    11 Feb 2013
 */
public interface SurfaceFactory<P,A> {

    /**
     * Returns a new plot surface.
     *
     * @param   plotBounds   rectangle to containing actual plot data
     *                       (not insets)
     * @param   profile   configuration object defining plot style
     * @param   aspect    configuration object defining plot viewpoint
     * @return   new plot surface
     */
    Surface createSurface( Rectangle plotBounds, P profile, A aspect );

    /**
     * Returns the configuration keys used to configure profile for this
     * surface factory.
     * The returned keys are used in the map supplied to the
     * {@link #createProfile createProfile} method.
     *
     * @return  profile configuration keys
     */
    ConfigKey[] getProfileKeys();

    /**
     * Creates a profile that can be used when creating a plot surface.
     * The keys that are significant in the supplied config map
     * are those returned by {@link #getProfileKeys getProfileKeys}.
     * The return value can be used as input to
     * {@link #createSurface createSurface} and other methods in this class.
     *
     * @param  config  map of profile configuration items
     * @return  factory-specific plot surface profile
     */
    P createProfile( ConfigMap config );

    /**
     * Returns the configuration keys that may be used to configure aspect
     * for this surface factory.
     * The returned keys are used in the map supplied to the
     * {@link #useRanges useRanges} and
     * {@link #createAspect createAspect} methods.
     *
     * @return   aspect configuration keys
     */
    ConfigKey[] getAspectKeys();

    /**
     * Indicates whether ranges should be provided to generate an aspect.
     * If true, it is beneficial to pass the result of
     * {@link #readRanges readRanges} to {@link #createAspect createAspect}
     * alongside the arguments of this method.
     * If false, any such ranges will be ignored.
     *
     * @param   profile  surface configuration profile
     * @param  aspectConfig  configuration map that may contain keys from
     *                       <code>getAspectKeys</code>
     * @return   true iff calculated ranges will be of use
     */
    boolean useRanges( P profile, ConfigMap aspectConfig );

    /**
     * Provides the ranges that may be passed to
     * {@link #createAspect createAspect}.
     * There is only any point calling this if {@link #useRanges useRanges}
     * returns true.
     *
     * @param   profile  surface configuration profile
     * @param  layers   plot layers to be plotted
     * @param  dataStore  contains actual data
     * @return   data ranges covered by the given layers filled in from data
     */
    @Slow
    Range[] readRanges( P profile, PlotLayer[] layers, DataStore dataStore );

    /**
     * Creates an aspect from configuration information.
     * The ranges argument will be used only if {@link #useRanges useRanges}
     * returns true.
     * It is legal to give the ranges argument as null in any case.
     * In all cases, the returned value must be non-null and usable by
     * {@link #createSurface createSurface}.
     *
     * @param  profile  surface configuration profile
     * @param  aspectConfig  configuration map that may contain keys from
     *                       <code>getAspectKeys</code>
     * @param  ranges  range data filled in from layers, or null
     * @return   new aspect
     */
    A createAspect( P profile, ConfigMap aspectConfig, Range[] ranges );

    /**
     * Returns a ConfigMap that corresponds to the configuration of
     * the given surface, which must have been created by this factory.
     * The intention is that supplying the returned config items to
     * this object's {@link #createAspect createAspect} method with
     * the right profile should come up with approximately the same
     * surface, preferably without reference to any supplied ranges.
     *
     * <p>The returned config items should be optimised for presentation
     * to the user, so that for instance decimal values are reported
     * to a reasonable level of precision.  Because of this, and
     * perhaps for other reasons related to implementation,
     * a surface resulting from feeding the returned config back to this
     * factory may not be identical to the supplied surface,
     * so round-tripping is not guaranteed to be exact.
     *
     * @param   surface   plot surface; if it was not created by this factory,
     *                    behaviour is undefined
     * @return   config map populated with items that should approximately
     *           reproduce the supplied surface
     */
    ConfigMap getAspectConfig( Surface surface );

    /**
     * Returns the configuration keys that may be used to configure
     * a navigator for use with this surface factory.
     * The returned keys are used in the map supplied to the
     * {@link #createNavigator} method.
     *
     * @return   navigator configuration keys
     */
    ConfigKey[] getNavigatorKeys();

    /**
     * Creates a navigator from configuration information.
     * The returned value will be non-null.
     *
     * @param   navigatorConfig  configuration map that may contain keys from
     *                           <code>getNavigatorKeys</code>
     * @return   navigator for use with surfaces produced by this factory
     */
    Navigator<A> createNavigator( ConfigMap navigatorConfig );

    /**
     * Returns an object that can assess distances between graphic
     * positions on the plot surface.
     * If no such metric exists, null may be returned.
     *
     * @return   plot metric, or null
     */
    PlotMetric getPlotMetric();
}
