package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Tweaks the graphics context to render objects at a distance from the
 * viewer.
 * The current implementation can interpose fog of variable intensity.  
 *
 * @author   Mark Taylor
 * @since    23 Nov 2005
 */
public class DepthTweaker implements GraphicsTweaker {

    private final double scale_;
    private final float[] fogRgb_ = new float[] { 0.9f, 0.9f, 0.9f };
    private final float[] work3 = new float[ 3 ];
    private double fogScale_;
    float clarity_;

    /**
     * Constructs a new depth tweaker for rendering distances of the order
     * of a given scale.
     *
     * @param  scale  depth scale distance
     */
    public DepthTweaker( double scale ) {
        scale_ = scale;
    }

    /**
     * Returns the depth scale distance.
     *
     * @return  depth scale distance
     */
    public double getScale() {
        return scale_;
    }

    /**
     * Sets the thickness of the fog. 
     * A given value of fogginess corresponds to a given amount of 
     * obscuration of a mark at a depth of the given multiple 
     * of the scale distance.
     * Default value is zero (no fog).
     *
     * @param  fogginess  intensity of fog per scale distance
     */
    public void setFogginess( double fogginess ) {
        fogScale_ = fogginess / scale_;
    }

    /**
     * Returns the thickness of the fog.
     *
     * @return  intensity of fog per scale distance
     */
    public double getFogginess() {
        return fogScale_ * scale_;
    }

    /**
     * Sets the distance in the depth direction for which subsequent
     * {@link #tweak}s of the graphics context will configure.
     * For reasonable rendering, <code>z</code> should be greater than
     * or equal to zero, and of order of this renderer's scale distance.
     *
     * @param   z  depth distance
     */
    public void setZ( double z ) {

        /* Calculate the scaled distance. */
        double ked = fogScale_ * z;

        /* Set the clarity value.  Take care not to do unnecessary 
         * calculations in the case of no fog, since exp() may be expensive. */
        clarity_ = ked <= 0 ? 1f 
                            : (float) Math.exp( - ked );
    }

    public Graphics tweak( Graphics g ) {

        /* Don't work if there's nothing to do. */
        if ( clarity_ == 1f ) {
            return g;
        }

        /* Modify the context's current colour as if there was z units
         * of fog in front of it.  Algorithm from Eric Dumont's fog FAQ! */
        float fogness = 1f - clarity_;
        float[] rgb = g.getColor().getRGBColorComponents( work3 );
        for ( int i = 0; i < 3; i++ ) {
            rgb[ i ] = clarity_ * rgb[ i ] + fogness * fogRgb_[ i ];
        }
        Color fogged = new Color( rgb[ 0 ], rgb[ 1 ], rgb[ 2 ] );
        g = g.create();
        g.setColor( fogged );
        return g;
    }
}
