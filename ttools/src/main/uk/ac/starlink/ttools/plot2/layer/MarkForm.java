package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that just draws a fixed-size marker.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public class MarkForm implements ShapeForm {

    private static final Color DUMMY_COLOR = Color.GRAY;

    public String getFormName() {
        return "Mark";
    }

    public Icon getFormIcon() {
        return uk.ac.starlink.ttools.plot2.PlotUtil.icon( "form-mark.gif" );
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public ConfigKey[] getConfigKeys() {
        return new ConfigKey[] {
            StyleKeys.MARK_SHAPE,
            StyleKeys.SIZE,
        };
    }

    public Outliner createOutliner( ConfigMap config ) {
        MarkShape shape = config.get( StyleKeys.MARK_SHAPE );
        int size = config.get( StyleKeys.SIZE );
        return new MarkOutliner( shape, size );
    }

    public static Icon createLegendIcon( MarkShape shape, int size ) {
        final MarkStyle style = size == 0
                              ? MarkShape.POINT.getStyle( DUMMY_COLOR, 0 )
                              : shape.getStyle( DUMMY_COLOR, size );
        final Icon baseIcon = style.getLegendIcon();
        final int width = baseIcon.getIconWidth();
        final int height = baseIcon.getIconHeight();
        return new Icon() {
            public int getIconWidth() {
                return width;
            }
            public int getIconHeight() {
                return height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                int xoff = x + width / 2;
                int yoff = y + height / 2;
                g.translate( xoff, yoff );
                style.drawLegendShape( g );
                g.translate( -xoff, -yoff );
            }
        };
    }

    /**
     * Creates a MarkStyle with a given size.
     *
     * @param  shape  marker shape
     * @param  size   marker size in pixels
     * @return   marker style
     */
    public static MarkStyle createMarkStyle( MarkShape shape, int size ) {
        return size == 0 ? MarkShape.POINT.getStyle( DUMMY_COLOR, 0 )
                         : shape.getStyle( DUMMY_COLOR, size );
    }

    /**
     * Creates a Glyph from a MarkStyle.
     *
     * @param  style  marker style
     * @return  marker glyph
     */
    public static Glyph createMarkGlyph( final MarkStyle style ) {
        final Pixellator pixer = style.getPixelOffsets();
        return new Glyph() {
            public void paintGlyph( Graphics g ) {
                style.drawShape( g );
            }
            public Pixellator getPixelOffsets( Rectangle clip ) {
                return ClipPixellator.clip( pixer, clip );
            }
        };
    }

    /**
     * Outliner implementation for this class.
     */
    private static class MarkOutliner implements Outliner {
        private final MarkStyle style_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         */
        MarkOutliner( MarkShape shape, int size ) {
            style_ = createMarkStyle( shape, size );
            icon_ = createLegendIcon( shape, size );
        }
 
        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<AuxScale,AuxReader>();
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            final Glyph glyph = createMarkGlyph( style_ );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gp ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y, glyph, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point gp = new Point();
            final double[] dz = new double[ 1 ];
            final Glyph glyph = createMarkGlyph( style_ );
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tseq, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gp, dz ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y, dz[ 0 ],
                                              glyph, color );
                    }
                }
            };
        }

        public Object calculateBinPlan( Surface surface, DataGeom geom,
                                        Map<AuxScale,Range> auxRanges,
                                        DataStore dataStore, DataSpec dataSpec,
                                        Object[] knownPlans ) {
            return BinPlan
                  .calculatePointCloudPlan( new PointCloud( geom, dataSpec ),
                                            surface, dataStore, knownPlans );
        }

        public int[] getBinCounts( Object plan ) {
            BinPlan binPlan = (BinPlan) plan;
            return convolve( binPlan.getBinner(), binPlan.getGridder(),
                             style_.getPixelOffsets() );
        }

        public long getPointCount( Object plan ) {
            return ((BinPlan) plan).getBinner().getTotal();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MarkOutliner ) {
                MarkOutliner other = (MarkOutliner) o;
                return this.style_.equals( other.style_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return style_.hashCode();
        }

        /**
         * Convolves a grid of bin counts with a marker shape to produce
         * a grid of values indicating how many pixels would be plotted
         * per grid position if the given shape had been stamped down once
         * for each entry in the bin count grid.
         * To put it another way, the marker shape acts as a (shaped top hat)
         * smoothing kernel.
         *
         * @param   binner  contains pixel counts per grid point
         * @param   gridder  contains grid geometry
         * @param  pixellator  marker shape in terms of pixel iterator
         */
        private static int[] convolve( Binner binner, Gridder gridder,
                                       Pixellator pixellator ) {
            int nx = gridder.getWidth();
            int ny = gridder.getHeight();
            int[] buf = new int[ gridder.getLength() ];
            for ( pixellator.start(); pixellator.next(); ) {
                int px = pixellator.getX();
                int py = pixellator.getY();
                int ix0 = Math.max( 0, px );
                int ix1 = Math.min( nx, nx + px );
                int iy0 = Math.max( 0, py );
                int iy1 = Math.min( ny, ny + py );
                for ( int iy = iy0; iy < iy1; iy++ ) {
                    int jy = iy - py;
                    for ( int ix = ix0; ix < ix1; ix++ ) {
                        int jx = ix - px;
                        buf[ gridder.getIndex( ix, iy ) ] +=
                             binner.getCount( gridder.getIndex( jx, jy ) );
                    }
                }
            }
            return buf;
        }
    };
}
