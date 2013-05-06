package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Component;
import java.util.Set;
import java.util.HashSet;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;

/**
 * Provides interface and logic for determining what PaperType
 * (rendering machinery) to use to render a given selection of
 * plot layers to a given output medium.
 * A {@link uk.ac.starlink.ttools.plot2.PlotType}
 * provides an instance of this class.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class PaperTypeSelector {

    private static final Compositor DEFAULT_COMPOSITOR =
        Compositor.createBoostCompositor( 0.1f );

    /** Default selector for 2d output. */
    public static PaperTypeSelector SELECTOR_2D = createSelector2D();

    /** Default selector for 3d output. */
    public static PaperTypeSelector SELECTOR_3D = createSelector3D();

    /**
     * Supplies a PaperType appropriate for rendering to a vector
     * (non-pixellated) output medium.
     *
     * @param  opts   layer options
     * @return  paper type
     */
    public abstract PaperType getVectorPaperType( LayerOpt[] opts );

    /**
     * Supplies a PaperType appropriate for rendering to a bitmap
     * (pixellated) output medium.
     *
     * <p>If a component is supplied, it indicates the component on which this
     * paper will be rendered.  It is legal to supply a null component if
     * the destination component is unavailable (including if it is headless).
     * Note the supplied component may not be the actual one it's going to be
     * rendered on, but it should be similar in terms of graphics configuration,
     * background colour etc.
     *
     * @param  opts  layer options
     * @param  c   destination component, or component similar to destination
     *             component, or null
     * @return  paper type
     */
    public abstract PaperType getPixelPaperType( LayerOpt[] opts, Component c );

    /**
     * Constructs the default selector for 2D plots.
     *
     * @return  2d selector
     */
    private static PaperTypeSelector createSelector2D() {
        return
            new BasicSelector( new PaintPaperType2D(),
                               new OverPaperType2D(),
                               new CompositePaperType2D( DEFAULT_COMPOSITOR ) );
    }

    /**
     * Constructs the default selector for 3D plots.
     *
     * @return  3d selector
     */
    private static PaperTypeSelector createSelector3D() {
        return new BasicSelector( new SortedPaperType3D(),
                                  new ZBufferPaperType3D(),
                                  new PixelStackPaperType3D( DEFAULT_COMPOSITOR,
                                                             1e-4f ) );
    }

    /**
     * Returns a selector which always returns a fixed paper type.
     * This is only useful for debugging.
     *
     * @param   ptype  fixed paper type
     * @return   selector
     */
    private static PaperTypeSelector
            createSingleSelector( final PaperType ptype ) {
        return new PaperTypeSelector() {
            public PaperType getVectorPaperType( LayerOpt[] opts ) {
                return ptype;
            }
            public PaperType getPixelPaperType( LayerOpt[] opts,
                                                Component c ) {
                return ptype;
            }
        };
    }

    /**
     * Returns an array of layer options corresponding to an array of layers.
     *
     * @param  layers  layers
     * @return   layer options, same length as layers array
     */
    public static LayerOpt[] getOpts( PlotLayer[] layers ) {
        int nl = layers.length;
        LayerOpt[] opts = new LayerOpt[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            opts[ il ] = layers[ il ].getOpt();
        }
        return opts;
    }

    /**
     * Examines a list of layer options and if they only have a single
     * colour between them, returns that.
     * If there is not a unique colour, returns null.
     *
     * @param  opts   layer options
     * @return  unique colour, or null
     */
    private static Color getMonochromeColor( LayerOpt[] opts ) {
        Set<Integer> rgbSet = new HashSet<Integer>();
        for ( int il = 0; il < opts.length; il++ ) {
            Color color = opts[ il ].getSingleColor();
            if ( color == null ) {
                return null;
            }
            rgbSet.add( new Integer( color.getRGB() & 0x00ffffff ) );
        }
        return rgbSet.size() == 1
             ? new Color( rgbSet.iterator().next().intValue() )
             : null;
    }

    /**
     * Indicates whether all of a list of layer options are opaque.
     *
     * @param  opts  layer options
     * @return  true if all layers are opaque, false if any has transparency
     */
    private static boolean isOpaque( LayerOpt[] opts ) {
        for ( int il = 0; il < opts.length; il++ ) {
            if ( ! opts[ il ].isOpaque() ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Basic PaperTypeSelector implementation.
     */
    private static class BasicSelector extends PaperTypeSelector {

        private final PaperType vector_;
        private final PaperType pixelOpaque_;
        private final PaperType pixelGeneral_;

        /**
         * Constructor.
         *
         * @param  vector  paper type for vector output
         * @param  pixelOpaue  paper type for pixel output with no transparency
         * @param  pixelGeneral  paper type for pixel output without constraints
         */
        BasicSelector( PaperType vector, PaperType pixelOpaque,
                       PaperType pixelGeneral ) {
            vector_ = vector;
            pixelOpaque_ = pixelOpaque;
            pixelGeneral_ = pixelGeneral;
        }

        public PaperType getVectorPaperType( LayerOpt[] opts ) {
            return vector_;
        }

        public PaperType getPixelPaperType( LayerOpt[] opts, Component c ) {
            if ( isOpaque( opts ) ) {
                return pixelOpaque_;
            }
            Color color = getMonochromeColor( opts );
            if ( color != null ) {
                return new MonoPaperType( color, DEFAULT_COMPOSITOR );
            }
            return pixelGeneral_;
        }
    }
}
