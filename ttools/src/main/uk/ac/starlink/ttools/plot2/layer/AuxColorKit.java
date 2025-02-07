package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Encapsulates information about how to colour data points for
 * Aux-like shading.
 *
 * @author   Mark Taylor
 * @since    23 Jul 2018
 */
public class AuxColorKit implements ColorKit {

    private final int icShade_;
    private final Shader shader_;
    private final Scaler scaler_;
    private final Color scaledNullColor_;
    private final float scaleAlpha_;
    private final float[] baseRgba_;
    private final float[] rgba_;
    private Color lastColor_;
    private float lastScale_;

    /**
     * Constructor.
     *
     * @param  icShade  column index in tuple sequence at which
     *                  shade values are found
     * @param  shader   colour shader
     * @param  scaler   scales data values to normalised shader range
     * @param  baseColor  colour to adjust for non-absolute shaders
     * @param  nullColor  colour to use in case of null
     *                    aux coordinate; if null, such points are
     *                    not plotted
     * @param  scaleAlpha  alpha scaling for output colours;
     *                     1 means opaque
     */
    public AuxColorKit( int icShade, Shader shader, Scaler scaler,
                        Color baseColor, Color nullColor, float scaleAlpha ) {
        icShade_ = icShade;
        shader_ = shader;
        scaler_ = scaler;
        scaleAlpha_ = scaleAlpha;
        scaledNullColor_ = nullColor == null
                         ? null
                         : toOutputColor( nullColor.getRGBComponents( null ) );
        baseRgba_ = baseColor.getRGBComponents( null );
        rgba_ = new float[ 4 ];
        lastColor_ = scaledNullColor_;
        lastScale_ = Float.NaN;
    }

    public Color readColor( Tuple tuple ) {
        double auxVal = tuple.getDoubleValue( icShade_ );
        float scaleVal = (float) scaler_.scaleValue( auxVal );

        /* If null input return special null output value. */
        if ( Float.isNaN( scaleVal ) ) {
            return scaledNullColor_;
        }

        /* If no change in input return the last output for
         * efficiency (may get many the same in sequence). */
        else if ( lastScale_ == scaleVal ) {
            return lastColor_;
        }

        /* Otherwise calculate, store and return a colour based on
         * scaled input aux coordinate. */
        else {
            System.arraycopy( baseRgba_, 0, rgba_, 0, 4 );
            shader_.adjustRgba( rgba_, scaleVal );
            Color color = toOutputColor( rgba_ );
            lastScale_ = scaleVal;
            lastColor_ = color;
            return color;
        }
    }

    /**
     * Returns a colour for output based on RGBA values.
     * Alpha scaling is applied.  Null is returned for a completely
     * transparent colour.
     *
     * @param  4-element R,G,B,A array (elements in range 0..1)
     * @return  colour for output, or null
     */
    private Color toOutputColor( float[] rgba ) {
        float alpha = rgba[ 3 ];
        return alpha > 0 ? new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ],
                                      alpha * scaleAlpha_ )
                         : null;
    }
}
