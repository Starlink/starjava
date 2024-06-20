package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
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
     * @param  opts  layer options
     * @param  compositor  compositor for combining colours (relevant only
     *                     if some transparency is present)
     * @return  paper type
     */
    public abstract PaperType getPixelPaperType( LayerOpt[] opts,
                                                 Compositor compositor );

    /**
     * Constructs the default selector for 2D plots.
     *
     * @return  2d selector
     */
    private static PaperTypeSelector createSelector2D() {

        /* We currently use a quantised vector paper type here for consistency
         * with the other paper types, which are all snapped to the pixel grid.
         * However, it would be possible to use one with continuous
         * coordinates insteead. */
        PaperType vectorType = PaintPaperType2D.createPaperType( true );
        PaperType pixelOpaqueType = new OverPaperType2D();
        return new BasicSelector( vectorType, pixelOpaqueType ) {
            PaperType createGeneralPixelPaperType( Compositor compos ) {
                return new CompositePaperType2D( compos );
            }
        };
    }

    /**
     * Constructs the default selector for 3D plots.
     *
     * @return  3d selector
     */
    private static PaperTypeSelector createSelector3D() {
        return new BasicSelector( new SortedPaperType3D(),
                                  new ZBufferPaperType3D() ) {
            PaperType createGeneralPixelPaperType( Compositor compos ) {
                return new PixelStackPaperType3D( compos, 1e-4f );
            }
        };
    }

    /**
     * Returns a selector which always returns a fixed paper type.
     * This is only useful for debugging.
     *
     * @param   ptype  fixed paper type
     * @return   selector
     */
    public static PaperTypeSelector
            createSingleSelector( final PaperType ptype ) {
        return new PaperTypeSelector() {
            public PaperType getVectorPaperType( LayerOpt[] opts ) {
                return ptype;
            }
            public PaperType getPixelPaperType( LayerOpt[] opts,
                                                Compositor compositor ) {
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
            rgbSet.add( Integer.valueOf( color.getRGB() & 0x00ffffff ) );
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
    private static abstract class BasicSelector extends PaperTypeSelector {

        private final PaperType vector_;
        private final PaperType pixelOpaque_;

        /**
         * Constructor.
         *
         * @param  vector  paper type for vector output
         * @param  pixelOpaque  paper type for pixel output with no transparency
         */
        BasicSelector( PaperType vector, PaperType pixelOpaque ) {
            vector_ = vector;
            pixelOpaque_ = pixelOpaque;
        }

        public PaperType getVectorPaperType( LayerOpt[] opts ) {
            return vector_;
        }

        public PaperType getPixelPaperType( LayerOpt[] opts,
                                            Compositor compos ) {
            if ( isOpaque( opts ) ) {
                return pixelOpaque_;
            }
            Color color = getMonochromeColor( opts );
            if ( color != null ) {
                return new MonoPaperType( color, compos );
            }
            return createGeneralPixelPaperType( compos );
        }

        /**
         * Returns a paper type for rendering pixel output without
         * constraints.  A compositor is supplied.
         *
         * @param  compos  compositor for compositing transparent pixels
         * @return   paper type
         */
        abstract PaperType createGeneralPixelPaperType( Compositor compos );
    }
}
