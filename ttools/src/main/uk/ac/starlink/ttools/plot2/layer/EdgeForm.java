package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.StorageType;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * ShapeForm implementation that draws lines between different points
 * in the same row.  Absolute coordinates are used.
 *
 * @author   Mark Taylor
 * @since    27 Feb 2013
 */
public class EdgeForm implements ShapeForm {

    private final int nEdge_;
    private final DataGeom geom_;
    private final Coord[] extraCoords_;

    /**
     * Constructor.
     * A DataGeom has to be specified so that the required coordinates
     * can be reported.
     *
     * @param  nEdge  number of edges; 1 for pair, 2 for triple etc
     * @param  geom  position coordinate geometry
     */
    public EdgeForm( int nEdge, DataGeom geom ) {
        nEdge_ = nEdge;
        geom_ = geom;
        int nGeomCoord = geom.getPosCoords().length;
        extraCoords_ = new Coord[ nGeomCoord * nEdge ];
        for ( int ie = 0; ie < nEdge_; ie++ ) {
            Coord[] eCoords = geom_.getPosCoords();
            for ( int ic = 0; ic < nGeomCoord; ic++ ) {
                extraCoords_[ ie * nGeomCoord + ic ] =
                    relabel( eCoords[ ic ], ie );
            }
        }
    }

    public String getFormName() {
        return nEdge_ == 1 ? "Pair" : "Link" + ( nEdge_ + 1 );
    }

    public Icon getFormIcon() {
        return nEdge_ == 1 ? ResourceIcon.FORM_LINK2
                           : ResourceIcon.FORM_LINK3;
    }

    public Coord[] getExtraCoords() {
        return extraCoords_;
    }

    public ConfigKey[] getConfigKeys() {
        return new ConfigKey[ 0 ];
    }

    public Outliner createOutliner( ConfigMap config ) {
        return new EdgeOutliner( nEdge_ );
    }

    /**
     * Returns a Coord like a given one but with modified metadata.
     *
     * <p>The returned Coord is not of the right subclass, hence does not
     * have the appropriate type-specific read*Coord method.
     * However that doesn't matter, because we will use the DataGeom
     * based on the original coords to do the reading, not these ones.
     *
     * @param  baseCoord  coord on which to base the copy
     * @param  point index, used to label the coordinate
     * @return   new coord like the input one
     */
    private Coord relabel( final Coord baseCoord, int iPoint ) {
        String iptxt = Integer.toString( iPoint + 2 );
        final ValueInfo[] infos = baseCoord.getUserInfos().clone();
        int nuc = infos.length;
        for ( int iuc = 0; iuc < nuc; iuc++ ) { 
            DefaultValueInfo info = new DefaultValueInfo( infos[ iuc ] );
            info.setName( info.getName() + iptxt );
            info.setDescription( info.getDescription()
                               + " for point " + iptxt );
            infos[ iuc ] = info;
        }
        return new Coord() {
            public ValueInfo[] getUserInfos() {
                return infos;
            }
            public boolean isRequired() {
                return nEdge_ <= 1;
            }
            public StorageType getStorageType() {
                return baseCoord.getStorageType();
            }
            public List<Class<? extends DomainMapper>> getUserDomains() {
                return baseCoord.getUserDomains();
            }
            public Object userToStorage( Object[] userCoords,
                                         DomainMapper[] userMappers ) {
                return baseCoord.userToStorage( userCoords, userMappers );
            }
        };
    }

    /**
     * Outliner implementation for use with this form.
     */
    private static class EdgeOutliner extends PixOutliner {
        private final int nPos_;
        private final Icon icon_;

        /**
         * Constructor.
         *
         * @param  nEdge  edge count
         */
        EdgeOutliner( int nEdge ) {
            nPos_ = nEdge + 1;
            icon_ = new EdgeIcon( nEdge );
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
            final int nGeomCoord = geom.getPosCoords().length;
            final double[] dpos = new double[ geom.getDataDimCount() ];
            final Point gpos = new Point();
            final int[] xs = new int[ nPos_ ];
            final int[] ys = new int[ nPos_ ];
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {

                    /* Only add a new point if it is different from the
                     * last one.  Equal points (adjacent or not) are
                     * harmless but possibly inefficient.  This check
                     * cuts out work when all the positions fall in the
                     * same pixel.  That will happen in the common case
                     * when you're viewing many crossmatched points
                     * from a long way away - you only need to spend
                     * significant effort when the things are big enough
                     * to see.  This check doesn't throw out cases where
                     * non-adjacent points are identical, but if you're
                     * in that regime you probably don't have millions
                     * of them. */
                    int np = 0;
                    for ( int ip = 0; ip < nPos_; ip++ ) {
                        if ( geom.readDataPos( tseq, nGeomCoord * ip, dpos ) &&
                             surface.dataToGraphics( dpos, true, gpos ) &&
                             ( np == 0 || gpos.x != xs[ np - 1 ]
                                       || gpos.y != ys[ np - 1 ] ) ) {
                            xs[ np ] = gpos.x;
                            ys[ np ] = gpos.y;
                            np++;
                        }
                    }
                    Glyph glyph = createPolyGlyph( xs, ys, np );
                    if ( glyph != null ) {
                        paperType.placeGlyph( paper, xs[ 0 ], ys[ 0 ],
                                              glyph, color );
                    }
                }
            };
        }

        public ShapePainter create3DPainter( final CubeSurface surface,
                                             final DataGeom geom,
                                             Map<AuxScale,Range> auxRanges,
                                             final PaperType3D paperType ) {
            final int nGeomCoord = geom.getPosCoords().length;
            final double[] dpos = new double[ geom.getDataDimCount() ];
            final Point gpos = new Point();
            final int[] xs = new int[ nPos_ ];
            final int[] ys = new int[ nPos_ ];
            final double[] zs = new double[ nPos_ ];
            final double[] zloc = new double[ 1 ];
            return new ShapePainter() {
                public void paintPoint( TupleSequence tseq, Color color,
                                        Paper paper ) {
                    int np = 0;
                    for ( int ip = 0; ip < nPos_; ip++ ) {
                        if ( geom.readDataPos( tseq, nGeomCoord * ip, dpos ) &&
                             surface.dataToGraphicZ( dpos, true, gpos, zloc ) &&
                             ( np == 0 || gpos.x != xs[ np - 1 ]
                                       || gpos.y != ys[ np - 1 ] ) ) {
                            xs[ np ] = gpos.x;
                            ys[ np ] = gpos.y;
                            zs[ np ] = zloc[ 0 ];
                            np++;
                        }
                    }
                    Glyph glyph = createPolyGlyph( xs, ys, np );
                    if ( glyph != null ) {
                        paperType.placeGlyph( paper, xs[ 0 ], ys[ 0 ], zs[ 0 ],
                                              glyph, color );
                    }
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 6103;
            code = 23 * code + nPos_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof EdgeOutliner ) {
                EdgeOutliner other = (EdgeOutliner) o;
                return this.nPos_ == other.nPos_;
            }
            else {
                return false;
            }
        }

        /**
         * Returns a glyph representing a polygon connecting a sequence
         * of graphics points.  The reference position is the first coodinate.
         *
         * @param  gxs  array of <code>np</code> X coordinates
         * @param  gys  array of <code>np</code> Y coordinates
         * @param  np   point count
         * @return  glyph
         */
        private static Glyph createPolyGlyph( final int[] gxs, final int[] gys,
                                              final int np ) {

            /* Only one point, don't draw it. */
            if ( np < 2 ) {
                return null;
            }

            /* Two points, treat specially for efficiency. */
            else if ( np == 2 ) {
                final int gx1 = gxs[ 1 ] - gxs[ 0 ];
                final int gy1 = gys[ 1 ] - gys[ 0 ];
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        g.drawLine( 0, 0, gx1, gy1 );
                    }
                    public Pixellator getPixelOffsets( Rectangle clip ) {

                        /* I should reduce clip to its actual size here
                         * since a bitmask of that size is allocated
                         * in Drawing.
                         * And/or compact the returned pixellator. */
                        uk.ac.starlink.ttools.plot.Drawing pdrawing =
                            new uk.ac.starlink.ttools.plot.Drawing( clip );
                        pdrawing.drawLine( 0, 0, gx1, gy1 );
                        return pdrawing;
                    }
                };
            }

            /* More than two points, general polygon. */
            else {
                return new Glyph() {
                    public void paintGlyph( Graphics g ) {
                        int gx0 = gxs[ 0 ];
                        int gy0 = gys[ 0 ];
                        g.translate( -gx0, -gy0 );
                        g.drawPolygon( gxs, gys, np );
                        g.translate( +gx0, +gy0 );
                    }
                    public Pixellator getPixelOffsets( Rectangle clip ) {
                        uk.ac.starlink.ttools.plot.Drawing pdrawing =
                            new uk.ac.starlink.ttools.plot.Drawing( clip );
                        int gx0 = gxs[ 0 ];
                        int gy0 = gys[ 0 ];
                        for ( int i = 1; i < np; i++ ) {
                            pdrawing.drawLine( gxs[ i - 1 ] - gx0,
                                               gys[ i - 1 ] - gy0,
                                               gxs[ i ] - gx0, gys[ i ] - gy0 );
                        }
                        pdrawing.drawLine( gxs[ np - 1 ] - gx0,
                                           gys[ np - 1 ] - gy0,
                                           0, 0 );
                        return pdrawing;
                    }
                };
            }
        }
    }

    /**
     * Legend icon.
     */
    private static class EdgeIcon implements Icon {
        private final int SIZE = 20;
        private final int[] xs_;
        private final int[] ys_;

        /**
         * Constructor.
         *
         * @param   nEdge  edge count
         */
        EdgeIcon( int nEdge ) {
            int size2 = SIZE / 2;
            int nPoint = nEdge + 1;
            xs_ = new int[ nPoint ];
            ys_ = new int[ nPoint ];
            xs_[ 0 ] = size2;
            ys_[ 0 ] = size2;
            for ( int i = 1; i < nPoint; i++ ) {
                double theta = 0.5 * Math.PI * i / nPoint;
                xs_[ i ] = (int) ( size2 + size2 * Math.cos( theta ) );
                ys_[ i ] = (int) ( size2 - size2 * Math.sin( theta ) );
            }
        }

        public int getIconWidth() {
            return SIZE;
        }

        public int getIconHeight() {
            return SIZE;
        }

        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g.translate( x, y );
            g.drawPolygon( xs_, ys_, xs_.length );
            g.translate( -x, -y );
        }
    }

}
