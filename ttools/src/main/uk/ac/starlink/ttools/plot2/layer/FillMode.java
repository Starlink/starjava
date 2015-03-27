package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Describes how a region above the axis is represented visually.
 * It can be represented by a line, a filled region, or both.
 * Both parts may be drawn with variable transparency.
 *
 * @author   Mark Taylor
 * @since    27 Mar 2015
 */
@Equality
public class FillMode {

    private final String name_;
    private final String description_;
    private final float lineAlpha_;
    private final float fillAlpha_;

    /** Solid fill area, no boundary. */
    public static final FillMode SOLID =
        new FillMode( "Solid", 0, 1,
            "area between level and axis is filled with solid colour" );

    /* Solid boundary, no fill area. */
    public static final FillMode LINE =
        new FillMode( "Line", 1, 0,
            "level is marked by a wiggly line" );

    /** Solid boundary, fill area coloured in with transparency. */
    public static final FillMode SEMI =
        new FillMode( "Semi", 1, 0.25,
            "level is marked by a wiggly line, "
          + "and area below it is filled with a transparent colour" );

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  lineAlpha   alpha for line drawing (zero means no line)
     * @param  fillAlpha   alpha for area filling (zero means no fill)
     * @param  description   plain text description
     */
    public FillMode( String name, double lineAlpha, double fillAlpha,
                     String description ) {
        name_ = name;
        lineAlpha_ = normalise( lineAlpha );
        fillAlpha_ = normalise( fillAlpha );
        description_ = description;
    }

    /**
     * Returns the name for this mode.
     *
     * @return   name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short description for this mode.
     *
     * @return   plain text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns the alpha value for drawing a line.
     *
     * @return  line alpha in range 0..1, zero for no line
     */
    public float getLineAlpha() {
        return lineAlpha_;
    }

    /**
     * Returns the alpha value for filling the area.
     *
     * @return  fill alpha in range 0..1, zero for no fill
     */
    public float getFillAlpha() {
        return fillAlpha_;
    }

    /**
     * Indicates whether a line is drawn.
     *
     * @return   lineAlpha&gt;0
     */
    public boolean hasLine() {
        return lineAlpha_ > 0;
    }

    /**
     * Indicates whether the area is filled.
     *
     * @return  fillAlpha&gt;0
     */
    public boolean hasFill() {
        return fillAlpha_ > 0;
    }

    /**
     * Indicates whether this mode represents opaque drawing.
     *
     * @return  true only if no transparency is applied
     */
    public boolean isOpaque() {
        return ( lineAlpha_ == 0 || lineAlpha_ == 1 )
            && ( fillAlpha_ == 0 || fillAlpha_ == 1 );
    }

    /**
     * Returns an icon representing this fill mode.
     *
     * @param  data   Y data values as integer pixel levels above 0
     * @param  color   base colour
     * @param  stroke  line stroke
     * @param  pad    number of pixels on all sides to pad
     * @return  icon
     */
    public Icon createIcon( int[] data, Color color, final Stroke stroke,
                            final int pad ) {
        final int max = (int) uk.ac.starlink.ttools.func.Arrays.maximum( data );
        final int np = data.length + 2;
        final int[] xs = new int[ np ];
        final int[] ys = new int[ np ];
        for ( int ip = 1; ip < np - 1; ip++ ) {
            xs[ ip ] = ip + pad;
            ys[ ip ] = max - data[ ip - 1 ] + pad;
        }
        xs[ 0 ] = pad;
        ys[ 0 ] = max + pad;
        xs[ np - 1 ] = np - 1 + pad;
        ys[ np - 1 ] = max + pad;;
        float[] rgba = color.getComponents( null );
        final float cr = rgba[ 0 ];
        final float cg = rgba[ 1 ];
        final float cb = rgba[ 2 ];
        final float alpha = rgba[ 3 ];
        return new Icon() {
            public int getIconWidth() {
                return np + 2 * pad;
            }
            public int getIconHeight() {
                return max + 2 * pad;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g = null;
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                int x0 = x + pad;
                int y0 = y + pad;
                g2.translate( x0, y0 );
                g2.clipRect( -pad, -pad, np * 2 + pad, max + pad );
                if ( lineAlpha_ > 0 ) {
                    g2.setStroke( stroke );
                    g2.setColor( new Color( cr, cg, cb, alpha * lineAlpha_ ) );
                    g2.drawPolyline( xs, ys, np );
                }
                if ( fillAlpha_ > 0 ) {
                    g2.setColor( new Color( cr, cg, cb, alpha * fillAlpha_ ) );
                    g2.fillPolygon( xs, ys, np );
                }
            }
        };
    }

    /**
     * Ensures that a given value is in the range 0..1.
     *
     * @param  alpha  input value
     * @return   value in range 0..1
     */
    private static float normalise( double alpha ) {
        if ( alpha >= 0 && alpha <= 1 ) {
            return (float) alpha;
        }
        else if ( alpha > 1 ) {
            return 1f;
        }
        else {
            return 0f;
        }
    }

    @Override
    public int hashCode() {
        int code = 234202;
        code = 23 * code + Float.floatToIntBits( lineAlpha_ );
        code = 23 * code + Float.floatToIntBits( fillAlpha_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof FillMode ) {
            FillMode other = (FillMode) o;
            return this.lineAlpha_ == other.lineAlpha_
                && this.fillAlpha_ == other.fillAlpha_;
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return name_;
    }
}
