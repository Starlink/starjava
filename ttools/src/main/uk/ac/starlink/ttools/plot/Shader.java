package uk.ac.starlink.ttools.plot;

/**
 * Defines an object which can modify an sRGB colour as defined by a scalar
 * parameter.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public interface Shader {

    /**
     * Modifies the elements of an sRGB colour definition array in place
     * according to a supplied parameter.
     * The supplied <code>rgba</code> array has 4-elements giving 
     * red, green blue, alpha values respetively, each element in the
     * range 0 to 1.  The <code>value</code> parameter is a value in the
     * range 0 to 1 which parameterises how the <code>rgba</code> 
     * array is to be modified.
     *
     * @param  rgba  4-element (red,green,blue,alpha) array
     * @param  value  adjustment parameter in the range 0..1
     */
    void adjustRgba( float[] rgba, float value );

    /**
     * Indicates whether the shading performed by this shader is dependent
     * on the input colour or not.
     *
     * @return  true if {@link #adjustRgba} depends on the input colour
     */
    boolean isAbsolute();

    /**
     * Returns a name for this shader.
     *
     * @return  short human-readable label
     */
    String getName();
}
