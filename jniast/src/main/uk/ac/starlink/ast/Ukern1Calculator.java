package uk.ac.starlink.ast;

/**
 * Interface for user-provided 1-d kernel interpolation functions.
 * An object implementing this class is required in order to
 * perform custom interpolation using a 1-dimensional kernel in the
 * <code>resample*</code> methods of the {@link Mapping} class.
 *
 * @see  Mapping.Interpolator
 *
 * @author   Mark Taylor 
 * @version  $Id$
 */
public interface Ukern1Calculator {

    /**
     * Calculates the value of a 1-dimensional sub-pixel interpolation
     * kernel.  This determines how the weight given to neighbouring 
     * pixels in calculating an interpolated value depends on the
     * pixel's offset from the interpolation point.  In more than one 
     * dimension, the weight assigned to a pixel is formed by 
     * evaluating this 1-dimensional kernel using the offset along 
     * each dimension in turn. The product of the returned values is 
     * then used as the pixel weight. 
     *
     * @param  offset  the offset of the pixel from the interpolation point,
     *                 measured in pixels.  This value may be positive or 
     *                 negative, but for most practical interpolation
     *                 schemes its sign should be ignored. 
     * @return  the calculated kernel value, which may be positive or negative
     * @throws  Exception  The method may throw an exception if any 
     *                     error occurs during the calculation.  In this
     *                     case, resampling will terminate with an exception.
     */
    public double ukern1( double offset ) throws Exception;
}
