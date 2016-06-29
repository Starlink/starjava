package uk.ac.starlink.ttools.plot2.config;

import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * Aggregates a Shader with a Subrange.
 * The subrange indicates how much of the shader's ramp should be
 * used in some kind of default context.
 * This is typically used to clip off parts of the ramp that are
 * indistinguishable from a (white) background.
 *
 * @author   Mark Taylor
 * @since    29 Jun 2016
 */
public interface ClippedShader {

    /**
     * Returns the shader.
     *
     * @return  shader
     */
    Shader getShader();

    /**
     * Returns the default subrange to use with the shader in a default
     * context.
     *
     * @return  subrange
     */
    Subrange getSubrange();
}
