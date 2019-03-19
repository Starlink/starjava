package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Stroke;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Scaling;
import uk.ac.starlink.ttools.plot2.Subrange;

/**
 * LineStyle subclass that can vary the line's colour along its length.
 *
 * @author   Mark Taylor
 * @since    24 Jul 2018
 */
public class AuxLineStyle extends LineStyle {

    private final Shader shader_;
    private final Scaling scaling_;
    private final Subrange dataclip_;
    private final Color nullColor_;

    /**
     * Constructor.
     *
     * @param  color  line colour
     * @param  stroke  line stroke
     * @param  antialias  whether line is to be antialiased
     *                    (only likely to make a difference on bitmapped paper)
     * @param  shader   colour ramp
     * @param  scaling  colour ramp metric
     * @param  dataclip  colour ramp input data subrange
     * @param  nullColor  colour to use for null aux values;
     *                    if null, such segments are not plotted
     */
    public AuxLineStyle( Color color, Stroke stroke, boolean antialias,
                         Shader shader, Scaling scaling, Subrange dataclip,
                         Color nullColor ) {
        super( color, stroke, antialias );
        shader_ = shader;
        scaling_ = scaling;
        dataclip_ = dataclip;
        nullColor_ = nullColor;
    }

    /**
     * Returns the shader.
     *
     * @return  shader
     */
    public Shader getShader() {
        return shader_;
    }

    /**
     * Returns the colour scaling.
     *
     * @return  scaling
     */
    public Scaling getScaling() {
        return scaling_;
    }

    /**
     * Returns the adjustment for the input data scale.
     *
     * @return  data clipping subrange
     */
    public Subrange getDataClip() {
        return dataclip_;
    }

    /**
     * Returns the colour to use for null aux values.
     *
     * @return  null colour
     */
    public Color getNullColor() {
        return nullColor_;
    }

    @Override
    public int hashCode() {
        int code = super.hashCode();
        code = 23 * code + shader_.hashCode();
        code = 23 * code + scaling_.hashCode();
        code = 23 * code + dataclip_.hashCode();
        code = 23 * code + PlotUtil.hashCode( nullColor_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof AuxLineStyle ) {
            AuxLineStyle other = (AuxLineStyle) o;
            return super.equals( other )
                && this.shader_.equals( other.shader_ )
                && this.scaling_.equals( other.scaling_ )
                && this.dataclip_.equals( other.dataclip_ )
                && PlotUtil.equals( this.nullColor_, other.nullColor_ );
        }
        else {
            return false;
        }
    }
}
