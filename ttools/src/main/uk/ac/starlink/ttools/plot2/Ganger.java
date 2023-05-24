package uk.ac.starlink.ttools.plot2;

import java.awt.Rectangle;

/**
 * Defines how multiple plots can be presented together
 * as a gang of non-overlapping plotting zones.
 * This interface acts as a factory for Gang instances.
 * It takes care of how to align both graphics coordinates
 * and data coordinates so that the plots make sense together.
 * As well as the basic layout of plots for a particular purpose,
 * it may also understand and manage specified user preferences
 * about the details.
 *
 * @author   Mark Taylor
 * @since    25 Jan 2016
 */
public interface Ganger<P,A> {

    /**
     * Creates a gang given the graphics coordinates of the individual zones.
     * This can be used if the layout of the zones is already known.
     *
     * <p>The supplied rectangle arguments give the bounds of the data
     * area for each zone.  This does not include any space for
     * axis labels and other annotations, which are assumed to be
     * available as required.
     * 
     * @param  zonePlotBounds   array of data bounds, one for each zone
     * @return   new gang
     */
    Gang createGang( Rectangle[] zonePlotBounds );

    /**
     * Creates a gang given the external bounds for the whole plotting area
     * and other required information that characterises each zone.
     * The supplied aspects are not modified by this method;
     * any required aspect resolution should be performed before calling it.
     *
     * @param  extBounds  total area enclosing all zones and associated
     *                    axis labels, annotations etc
     * @param  surfFact   surface factory
     * @param  nz        number of zones
     * @param  zoneContents  plot content for each zone (nz-element array)
     * @param  profiles  profile for each zone (nz-element array)
     * @param  aspects   aspect for each zone (nz-element array)
     * @param  shadeAxes   shading axis for each zone
     *                     (nz-element array, elements may be empty)
     * @param  withScroll  true if the positioning should work well
     *                     even after some user scrolling
     * @return   new gang
     */
    Gang createGang( Rectangle extBounds, SurfaceFactory<P,A> surfFact,
                     int nz, ZoneContent[] zoneContents,
                     P[] profiles, A[] aspects,
                     ShadeAxis[] shadeAxes, boolean withScroll );

    /**
     * Constructs an approximate gang instance given only minimal information.
     * This may be sufficient for passing the user a visual indication
     * of roughly how zones are arranged, or making initial guesses
     * at zone dimensions.
     *
     * @param  extBounds  total area enclosing all zones and associated
     *                    axis labels, annotations etc
     * @param  nz        number of zones
     * @return  new approximate gang
     */
    Gang createApproxGang( Rectangle extBounds, int nz );

    /**
     * Adjusts plot surface aspects as required to ensure that plot data
     * regions in a ganged set of zones are consistent.
     *
     * <p>If a reference index greater than or equal to zero is supplied,
     * this denotes the "master" zone, to which the other aspects
     * should be adjusted.
     * Otherwise, the aspects should be adjusted more democratically,
     * treating all their requirements equally.
     * With a reference index the other aspects might be adjusted to
     * equal the master one, and without they might all be adjusted
     * to cover the union of the ranges defined.
     * If the reference index is &gt;= the number of zones,
     * behaviour is undefined.
     *
     * @param  aspects  unadjusted aspects
     * @param  iz    index of reference aspect in array, or -1 for no reference
     * @return   array of consistent aspects based on input array,
     *           same size as input
     */
    A[] adjustAspects( A[] aspects, int iz );

    /**
     * Adjusts plot surface profiles as required for plots appearing
     * in multiple plots within a gang.
     *
     * @param   profiles  unadjusted profiles
     * @return   array of consistent profiles based on input array,
     *           same size as input
     */
    P[] adjustProfiles( P[] profiles );
}
