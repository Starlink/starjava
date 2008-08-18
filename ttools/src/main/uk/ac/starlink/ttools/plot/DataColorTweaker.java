package uk.ac.starlink.ttools.plot;

import java.awt.Color;

/**
 * ColorTweaker which can adjust plotting colours on the
 * basis of a supplied array of values (auxiliary data coordinates).
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public interface DataColorTweaker extends ColorTweaker {

    /**
     * Configures this object with a coordinate array which determines
     * what colour adjustments subsequent calls to {@link #tweakColor}
     * will perform.
     *
     * <p>The return value indicates whether the supplied coordinates are
     * within the visible data ranges; iff they are outside this range 
     * false will be returned.  Null auxiliary coordinates do not cause 
     * a false return, and neither do they cause any change to the 
     * input colour.  In case of a false return this object is left
     * in an undefined state, so <code>tweakColor</code> should only be
     * called following a successful (true) call of this method.
     *
     * @param  coords   full coordinate array
     * @return  true iff this object has been set to a usable state
     */
    abstract boolean setCoords( double[] coords );

    /**
     * Returns the size of coordinate array which should be
     * submitted to {@link #setCoords}.
     *
     * @return   coordinate size array
     */
    abstract int getNcoord();
}
