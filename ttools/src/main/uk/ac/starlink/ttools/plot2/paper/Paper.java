package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Graphics;

/**
 * Marker interface labelling objects which are used to store rendering
 * data specific to a given PaperType.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 * @see   PaperType
 */
public interface Paper {

    /**
     * Returns the PaperType which generated and can write to this paper.
     *
     * @return   paper type
     */
    PaperType getPaperType();

    /**
     * Indicates whether this Paper instance is capable of splitting and
     * merging to facilitate parallel processing.
     * Iff this method returns true, then the
     * {@link #createSheet} and {@link #mergeSheet mergeSheet} methods
     * may be used.
     *
     * @return   true iff sheet split/merge is supported
     */
    boolean canMerge();

    /**
     * Returns a blank Paper instance that is compatible with this one.
     * That essentially means an instance like this but with nothing
     * yet painted on it.
     *
     * <p>May only be invoked if {@link #canMerge} returns true.
     *
     * @return   new compatible paper instance
     */
    Paper createSheet();

    /**
     * Merges the contents of a compatible paper instance with this one.
     * The supplied sheet is assumed to have been created by an earlier
     * invocation of {@link #createSheet} on this instance or on a
     * compatible instance.
     *
     * <p>The effect is as if everything that has been painted to the supplied
     * sheet will now be painted on this one.
     *
     * <p>This is intended for use in parallelising painting of a
     * large number of 2D or 3D {@link uk.ac.starlink.ttools.plot2.Glyph}s.
     * Merging papers on which {@link uk.ac.starlink.ttools.plot2.Decal}s
     * have been placed may or may not work.
     * 
     * <p>May only be invoked if {@link #canMerge} returns true.
     *
     * @param  sheet   compatible paper instance
     */
    void mergeSheet( Paper sheet );
}
