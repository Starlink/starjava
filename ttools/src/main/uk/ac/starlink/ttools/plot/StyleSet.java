package uk.ac.starlink.ttools.plot;

/**
 * Defines a sequence of styles intended for use within a single plot.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2005
 */
public interface StyleSet {

    /**
     * Returns a marker style corresponding to a particular index.
     * Successive calls for the same <code>index</code> must give the 
     * same style (or one equivalent in the sense of <code>equals</code>),
     * and ideally different values of <code>index</code> should give
     * unequal ones, but for indices beyond a certain value the markers
     * may wrap around.
     * A given implementation may change the marker it dispenses for a
     * given index at different stages of its lifetime.
     *
     * @param  index  code for the requested style
     * @return  style for code <code>index</code>
     */
    Style getStyle( int index );

    /**
     * Returns the name of this set.
     *
     * @return   set name
     */
    String getName();
}
