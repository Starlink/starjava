package uk.ac.starlink.ttools.plot2.layer;

import gov.fnal.eag.healpix.PixTools;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import javax.vecmath.Vector3d;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;

/**
 * Understands the geometry of HEALPix tiles on a given SkySurface.
 *
 * <p><strong>Note:</strong> instances of this class are not
 * safe for concurrent use from multiple threads.
 *
 * @author   Mark Taylor
 * @since    31 Mar 2016
 */
public class SkySurfaceTiler {

    private final SkySurface surf_;
    private final long nside_;
    private final Rotation rotation_;
    private final PixTools pixTools_;
    private final boolean hasSubpixelTiles_;
    private final Rectangle plotBox_;
    private final double[] dpos_;
    private final Point2D.Double gpos_;

    /**
     * Constructor.
     *
     * @param  surf   sky surface
     * @param  healpixLevel   healpix level (0, 1, 2, ..)
     * @param  rotation  additional rotation to apply to sky positions, not null
     */
    public SkySurfaceTiler( SkySurface surf, int healpixLevel,
                            Rotation rotation ) {
        surf_ = surf;
        nside_ = 1L << healpixLevel;
        rotation_ = rotation;
        pixTools_ = new PixTools();
        hasSubpixelTiles_ =
            healpixLevel >= SkyDensityPlotter.getPixelLevel( surf );
        plotBox_ = surf_.getPlotBounds();
        dpos_ = new double[ 3 ];
        gpos_ = new Point2D.Double();
    }

    /**
     * Indicates whether the center of a given tile is visible on this
     * tiler's plot surface.
     * This may be faster to execute than {@link #getTileShape}.
     *
     * @param  hpxIndex  HEALPix index
     * @return   true iff center of index tile is visible
     */
    public boolean isCenterVisible( long hpxIndex ) {
        Vector3d v3 = pixTools_.pix2vect_nest( nside_, hpxIndex );
        dpos_[ 0 ] = v3.x;
        dpos_[ 1 ] = v3.y;
        dpos_[ 2 ] = v3.z;
        rotation_.rotate( dpos_ );
        return surf_.dataToGraphics( dpos_, true, gpos_ );
    }

    /**
     * Returns the shape of the given tile on the sky surface.
     * The result is an approximation using integer graphics coordinates.
     * It will be one of the following:
     * <ul>
     * <li>{@link java.awt.Polygon} (most likely a quadrilateral),
     *     if the tile is large compared to screen pixel size</li>
     * <li>{@link java.awt.Rectangle}
     *     with <code>width</code>=<code>height</code>=1,
     *     if the tile is approximately equal to or smaller than a screen pixel
     * <li><code>null</code></li> if the tile is not visible
     * </ul>
     *
     * <p>In any case, you can pass the result to this object's
     * {@link #fillTile fillTile} method.
     *
     * @param   hpxIndex  HEALPix index
     * @return   shape of indicated tile on graphics plane, or null
     */
    public Shape getTileShape( long hpxIndex ) {
        Vector3d v3 = pixTools_.pix2vect_nest( nside_, hpxIndex );
        dpos_[ 0 ] = v3.x;
        dpos_[ 1 ] = v3.y;
        dpos_[ 2 ] = v3.z;
        rotation_.rotate( dpos_ );
        if ( hasSubpixelTiles_ ) {
            return surf_.dataToGraphics( dpos_, true, gpos_ )
                 ? new Rectangle( PlotUtil.ifloor( gpos_.x ),
                                  PlotUtil.ifloor( gpos_.y ), 1, 1 )
                 : null;
        }
        else {
            if ( surf_.dataToGraphics( dpos_, false, gpos_ ) ) {
                double[][] vertices =
                    pixTools_.pix2vertex_nest( nside_, hpxIndex );
                int[] gxs = new int[ 4 ];
                int[] gys = new int[ 4 ];
                double[] dpos1 = new double[ 3 ];
                Point2D.Double gpos1 = new Point2D.Double();
                int np = 0;
                int nInvisible = 0;
                int gxmin = Integer.MAX_VALUE;
                int gxmax = Integer.MIN_VALUE;
                int gymin = Integer.MAX_VALUE;
                int gymax = Integer.MIN_VALUE;
                for ( int i = 0; i < 4; i++ ) {
                    dpos1[ 0 ] = vertices[ 0 ][ i ];
                    dpos1[ 1 ] = vertices[ 1 ][ i ];
                    dpos1[ 2 ] = vertices[ 2 ][ i ];
                    rotation_.rotate( dpos1 );
                    if ( surf_.dataToGraphicsOffset( dpos_, gpos_, dpos1,
                                                     false, gpos1 ) ) {
                        assert ! Double.isNaN( gpos1.x );
                        assert ! Double.isNaN( gpos1.y );
                        int gx = PlotUtil.ifloor( gpos1.x );
                        int gy = PlotUtil.ifloor( gpos1.y );
                        gxs[ np ] = PlotUtil.ifloor( gx );
                        gys[ np ] = PlotUtil.ifloor( gy );
                        gxmin = Math.min( gxmin, gx );
                        gxmax = Math.max( gxmax, gx );
                        gymin = Math.min( gymin, gy );
                        gymax = Math.max( gymax, gy );
                        np++;
                    }
                    else {
                        if ( ++nInvisible > 1 ) {
                            return null;
                        }
                    }
                }
                assert np >= 1;
                return plotBox_
                      .intersects( gxmin, gymin, gxmax - gxmin, gymax - gymin )
                     ? new Polygon( gxs, gys, np )
                     : null;
            }
            else {
                return null;
            }
        }
    }

    /**
     * Paints a filled tile shape to a graphics context.
     *
     * @param  g   graphics context
     * @param  shape  shape returned by {@link #getTileShape getTileShape}
     */
    public void fillTile( Graphics g, Shape shape ) {
        if ( shape instanceof Rectangle ) {
            Rectangle rect = (Rectangle) shape;
            g.fillRect( rect.x, rect.y, rect.width, rect.height );
        }
        else if ( shape instanceof Polygon ) {
            g.fillPolygon( (Polygon) shape );
        }
        else if ( shape == null ) {
        }
        else {
            throw new IllegalArgumentException( "not my tile: " + shape );
        }
    }
}
