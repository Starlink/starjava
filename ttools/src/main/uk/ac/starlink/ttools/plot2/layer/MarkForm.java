package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that just draws a fixed-size marker for each
 * position.  One or more positions per tuple may be marked.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public abstract class MarkForm implements ShapeForm {

    private final int npos_;
    private final String name_;
    private final Icon icon_;

    /** Minimum size of marker in legend, even if plot size is smaller. */
    public static final int MIN_LEGEND_SIZE = 2;

    /** MarkForm instance for a single point per tuple. */
    public static final MarkForm SINGLE = createMarkForm( 1 );

    /** MarkForm instance for two points per tuple. */
    public static final MarkForm PAIR = createMarkForm( 2 );

    /** MarkForm instance for four points per tuple. */
    public static final MarkForm QUAD = createMarkForm( 4 );

    private static final Color DUMMY_COLOR = Color.GRAY;

    /**
     * Constructor.
     *
     * @param   npos   number of points to mark per tuple
     * @param   name   form name
     * @param   icon   form icon
     */
    protected MarkForm( int npos, String name, Icon icon ) {
        npos_ = npos;
        name_ = name;
        icon_ = icon;
    }

    public int getPositionCount() {
        return npos_;
    }

    public String getFormName() {
        return name_;
    }

    public Icon getFormIcon() {
        return icon_;
    }

    public String getFormDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a marker of fixed size and shape",
            "at each position.",
            "</p>",
        } );
    }

    public Coord[] getExtraCoords() {
        return new Coord[ 0 ];
    }

    public DataGeom adjustGeom( DataGeom geom ) {
        return geom;
    }

    @Override
    public int hashCode() {
        return npos_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MarkForm ) {
            MarkForm other = (MarkForm) o;
            return this.npos_ == other.npos_;
        }
        else {
            return false;
        }
    }

    /**
     * Factory method to create an instance of this class.
     *
     * @param   npos   number of positions per tuple
     * @return  form instance
     */
    public static MarkForm createMarkForm( final int npos ) {
        if ( npos == 1 ) {
            return new MarkForm( 1, "Mark", ResourceIcon.FORM_MARK ) {
                public ConfigKey<?>[] getConfigKeys() {
                    return new ConfigKey<?>[] {
                        StyleKeys.MARKER_SHAPE,
                        StyleKeys.SIZE,
                    };
                }
                public Outliner createOutliner( ConfigMap config ) {
                    MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
                    int size = config.get( StyleKeys.SIZE );
                    return createMarkOutliner( shape, size );
                }
            };
        }
        else {
            final Icon icon;
            if ( npos == 2 ) {
                icon = ResourceIcon.FORM_MARKS2;
            }
            else if ( npos == 3 ) {
                icon = ResourceIcon.FORM_MARKS3;
            }
            else {
                icon = ResourceIcon.FORM_MARKS4;
            }
            return new MarkForm( npos, "Mark" + npos, icon ) {
                public String getFormDescription() {
                    return PlotUtil.concatLines( new String[] {
                        "<p>Plots " + npos + " similar markers",
                        "of fixed size and shape",
                        "representing " + npos + " separate positions",
                        "from the same input table row.",
                        "This is a convenience option that can be used with",
                        "other plot layers based on " + npos + " positions.",
                        "</p>",
                    } );
                }
                public ConfigKey<?>[] getConfigKeys() {
                    return new ConfigKey<?>[] {
                        StyleKeys.MARKER_SHAPE,
                        StyleKeys.SIZE,
                    };
                }
                public Outliner createOutliner( ConfigMap config ) {
                    MarkerShape shape = config.get( StyleKeys.MARKER_SHAPE );
                    int size = config.get( StyleKeys.SIZE );
                    return createMultiMarkOutliner( shape, size, npos );
                }
            };
        }
    }

    /**
     * Returns an outliner for use with single points.
     *
     * @param   shape  marker shape
     * @param   size   marker size
     * @return  single-point outliner
     */
    public static Outliner createMarkOutliner( MarkerShape shape, int size ) {
        return new SingleMarkOutliner( shape, size );
    }

    /**
     * Returns an outliner for use with multiple points.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  npos   number of positions per tuple
     * @return  multi-point outliner
     */
    public static Outliner createMultiMarkOutliner( MarkerShape shape, int size,
                                                    int npos ) {
        return new MultiMarkOutliner( shape, size, npos );
    }

    /**
     * Creates a MarkStyle with a given size.
     *
     * @param  shape  marker shape
     * @param  size   marker size in pixels
     * @return   marker style
     */
    public static MarkerStyle createMarkStyle( MarkerShape shape, int size ) {
        return size == 0 ? MarkerShape.POINT.getStyle( DUMMY_COLOR, 0 )
                         : shape.getStyle( DUMMY_COLOR, size );
    }

    /**
     * Creates a MarkStyle for use in generating legend images.
     * This may be limited to a more visible size than the style used
     * for plotting the actual data.
     *
     * @param  shape  marker shape
     * @param  size   marker size in pixels
     * @return   marker style
     */
    private static MarkerStyle createLegendMarkStyle( MarkerShape shape,
                                                      int size ) {
        if ( size == 0 ) {
            return MarkerShape.FILLED_SQUARE
                  .getStyle( DUMMY_COLOR, MIN_LEGEND_SIZE );
        }
        else {
            return shape
                  .getStyle( DUMMY_COLOR, Math.max( size, MIN_LEGEND_SIZE ) );
        }
    }

    /**
     * Creates a Glyph representing a marker.
     *
     * @param  shape  marker shape
     * @param  size  marker size
     * @param  isMultipix  if true, optimise for an instance that may have
     *                     createPixer called multiple times
     * @return  marker glyph
     */
    public static Glyph createMarkGlyph( MarkerShape shape, int size,
                                         boolean isMultipix ) {
        final MarkerStyle style = createMarkStyle( shape, size );
        final PixerFactory pfact =
              isMultipix
            ? Pixers.createPixerCopier( style.getPixerFactory().createPixer() )
            : style.getPixerFactory();
        return new Glyph() {
            public void paintGlyph( Graphics g ) {
                style.drawShape( g );
            }
            public Pixer createPixer( Rectangle clip ) {
                return Pixers.createClippedPixer( pfact, clip );
            }
        };
    }

    /**
     * Returns a legend icon suitable for a single marker.
     *
     * @param  shape  marker shape
     * @param  size  marker size
     * @return   legend icon
     */
    public static Icon createLegendIcon( MarkerShape shape, int size ) {
        final MarkerStyle style = createLegendMarkStyle( shape, size );
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
     * Returns a legend icon suitable for multiple markers.
     *
     * @param   shape  marker shape
     * @param   size   marker size
     * @param   npos   number of positions to show in icon
     */
    private static Icon createMultiLegendIcon( MarkerShape shape, int size,
                                               final int npos ) {
        final MarkerStyle style = createLegendMarkStyle( shape, size );
        return new MultiPosIcon( npos ) {
            protected void paintPositions( Graphics g, Point[] positions ) {
                for ( int ip = 0; ip < npos; ip++ ) {
                    Point pos = positions[ ip ];
                    g.translate( pos.x, pos.y );
                    style.drawLegendShape( g );
                    g.translate( -pos.x, -pos.y );
                }
            }
        };
    }

    /**
     * Partial outliner implementation for use with MarkForm.
     * This is an abstract class with concrete implementations for
     * single and multiple position markers.
     * The implementations are not much different, and it would be
     * possible to use a single implementation with no special casing
     * for the single-position case.  However, the glyph placement done
     * by the one-point case is inside the single most time-critical
     * loop in the whole application, so we take any opportunity to
     * shave time off it.
     */
    private static abstract class MarkOutliner implements Outliner {
        final MarkerStyle style_;
        final Glyph glyph_;
        final Icon icon_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         * @param  icon   legend icon
         */
        protected MarkOutliner( MarkerShape shape, int size, Icon icon ) {
            style_ = createMarkStyle( shape, size );
            glyph_ = createMarkGlyph( shape, size, true );
            icon_ = icon;
        }

        /**
         * Returns a point cloud representing all the positions that
         * need to be plotted.
         *
         * @param  dataGeom  acquires data positions from tuple
         * @param  dataSpec  data spec
         * @return  point cloud
         */
        protected abstract PointCloud createPointCloud( DataGeom dataGeom,
                                                        DataSpec dataSpec );
 
        public Icon getLegendIcon() {
            return icon_;
        }

        public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
            return new HashMap<AuxScale,AuxReader>();
        }

        public Object calculateBinPlan( Surface surface, DataGeom geom,
                                        Map<AuxScale,Span> auxSpans,
                                        DataStore dataStore, DataSpec dataSpec,
                                        Object[] knownPlans ) {
            return BinPlan
                  .calculatePointCloudPlan( createPointCloud( geom, dataSpec ),
                                            surface, dataStore, knownPlans );
        }

        public int[] getBinCounts( Object plan ) {
            BinPlan binPlan = (BinPlan) plan;
            Rectangle bigRect =
                new Rectangle( Integer.MIN_VALUE / 4, Integer.MIN_VALUE / 4,
                               Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2 );
            return convolve( binPlan.getBinner(), binPlan.getGridder(),
                             glyph_.createPixer( bigRect ) );
        }

        public long getPointCount( Object plan ) {
            return ((BinPlan) plan).getBinner().getTotal();
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
         * @param  pixer  marker shape in terms of pixel iterator
         */
        private static int[] convolve( Binner binner, Gridder gridder,
                                       Pixer pixer ) {
            int nx = gridder.getWidth();
            int ny = gridder.getHeight();
            int[] buf = new int[ gridder.getLength() ];
            while ( pixer.next() ) {
                int px = pixer.getX();
                int py = pixer.getY();
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
    }

    /**
     * Mark outliner for a single point per tuple.
     */
    private static class SingleMarkOutliner extends MarkOutliner {

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         */
        public SingleMarkOutliner( MarkerShape shape, int size ) {
            super( shape, size, createLegendIcon( shape, size ) );
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point2D.Double gp = new Point2D.Double();
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphics( dpos, true, gp ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y,
                                              glyph_, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final GPoint3D gp = new GPoint3D();
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    if ( geom.readDataPos( tuple, 0, dpos ) &&
                         surface.dataToGraphicZ( dpos, true, gp ) ) {
                        paperType.placeGlyph( paper, gp.x, gp.y, gp.z,
                                              glyph_, color );
                    }
                }
            };
        }

        protected PointCloud createPointCloud( DataGeom geom, DataSpec spec ) {
            return new PointCloud( new SubCloud( geom, spec, 0 ) );
        }


        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SingleMarkOutliner ) {
                MarkOutliner other = (SingleMarkOutliner) o;
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
    }

    /**
     * Mark outliner for multiple points per tuple.
     */
    private static class MultiMarkOutliner extends MarkOutliner {

        private final int npos_;

        /**
         * Constructor.
         *
         * @param  shape  marker shape
         * @param  size   marker size in pixels
         * @param  npos   number of points per tuple
         */
        public MultiMarkOutliner( MarkerShape shape, int size, int npos ) {
            super( shape, size, createMultiLegendIcon( shape, size, npos ) );
            npos_ = npos;
        }

        public ShapePainter create2DPainter( final Surface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType2D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final Point2D.Double gp = new Point2D.Double();
            final int npc = geom.getPosCoords().length;
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    for ( int ip = 0; ip < npos_; ip++ ) {
                        if ( geom.readDataPos( tuple, ip * npc, dpos ) &&
                             surface.dataToGraphics( dpos, true, gp ) ) {
                            paperType.placeGlyph( paper, gp.x, gp.y, glyph_,
                                                  color );
                        }
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Span> auxSpans,
                                             final PaperType3D paperType ) {
            final double[] dpos = new double[ surface.getDataDimCount() ];
            final GPoint3D gp = new GPoint3D();
            final int npc = geom.getPosCoords().length;
            return new ShapePainter() {
                public void paintPoint( Tuple tuple, Color color,
                                        Paper paper ) {
                    for ( int ip = 0; ip < npos_; ip++ ) {
                        if ( geom.readDataPos( tuple, ip * npc, dpos ) &&
                             surface.dataToGraphicZ( dpos, true, gp ) ) {
                            paperType.placeGlyph( paper, gp.x, gp.y, gp.z,
                                                  glyph_, color );
                        }
                    }
                }
            };
        }

        protected PointCloud createPointCloud( DataGeom geom, DataSpec spec ) {
            return new PointCloud( SubCloud.createSubClouds( geom, spec,
                                                             npos_, false ) );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof MultiMarkOutliner ) {
                MultiMarkOutliner other = (MultiMarkOutliner) o;
                return this.npos_ == other.npos_
                    && this.style_.equals( other.style_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 332;
            code = 23 * code + npos_;
            code = 23 * code + style_.hashCode();
            return code;
        }
    }
}
