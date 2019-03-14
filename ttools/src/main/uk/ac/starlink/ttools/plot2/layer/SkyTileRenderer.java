package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.IndexColorModel;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot2.Ranger;
import uk.ac.starlink.ttools.plot2.Scaler;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkySurface;

/**
 * Defines the strategy for rendering HEALPix tiles to a graphics context.
 *
 * @author   Mark Taylor
 * @since    16 Sep 2016
 */
public abstract class SkyTileRenderer {

    private static final int MIN_PAINT_PIXELS = 24;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Modifies the range of aux values found within a given surface.
     *
     * @param  ranger   range object to be modified
     * @param  binResult   tile bin contents
     */
    public abstract void extendAuxRange( Ranger ranger,
                                         BinList.Result binResult );

    /**
     * Performs the rendering of a prepared bin list on a graphics surface.
     *
     * @param  g  graphics context
     * @param  binResult   histogram containing sky pixel values
     * @param  shader   colour shading
     * @param  scaler   value scaling
     */
    public abstract void renderBins( Graphics g, BinList.Result binResult,
                                     Shader shader, Scaler scaler );

    /**
     * Returns a SkyTileRenderer suitable for use on a given sky surface.
     *
     * @param  surface  sky surface
     * @param  rotation   view rotation state
     * @param  viewLevel  HEALPix level for view
     * @param  binFactor  factor by which all bin values should be multiplied
     * @return   tile renderer
     */
    public static SkyTileRenderer createRenderer( SkySurface surface,
                                                  Rotation rotation,
                                                  int viewLevel,
                                                  double binFactor ) {

        /* We have two strategies for colouring in the tiles:
         * either paint each one as a polygon or resample the plot
         * area pixel by pixel.  Painting is generally faster if
         * the tiles are not too small.  However, getting the edges
         * right is very difficult with painting, so use resampling
         * if the boundary of the celestial sphere will be visible. */
        final boolean isPaint;
        final String reason;
        boolean boundsVisible =
            ! surface.getSkyShape().contains( surface.getPlotBounds() );
        if ( boundsVisible ) {
            isPaint = false;
            reason = "sky bounds are visible";
        }
        else {
            double srPerTile = 4 * Math.PI / ( 12L << 2 * viewLevel );
            double srPerPixel = surface.pixelAreaSteradians();
            int pixelsPerTile = (int) ( srPerTile / srPerPixel );
            if ( pixelsPerTile >= MIN_PAINT_PIXELS ) {
                isPaint = true;
                reason = "pixels per tile " + pixelsPerTile
                       + ">=" + MIN_PAINT_PIXELS;
            }
            else {
                isPaint = false;
                reason = "pixels per tile " + pixelsPerTile
                       + "<" + MIN_PAINT_PIXELS;
            }
        }
        String msg = new StringBuffer()
           .append( "Rendering mode: " )
           .append( isPaint ? "paint" : "resample" )
           .append( " (" )
           .append( reason )
           .append( ")" )
           .toString();
        logger_.info( msg );
        return isPaint
             ? new PaintTileRenderer( surface, viewLevel, rotation, binFactor )
             : new ResampleTileRenderer( surface, viewLevel, rotation,
                                         binFactor );
    }

    /**
     * Given a prepared data structure, paints the results it
     * represents onto a graphics context appropriate for this drawing.
     *
     * @param  g  graphics context
     * @param  binResult   histogram containing sky pixel values
     * @param  binFactor   factor by which all bin values must be multiplied
     * @param  surface   plot surface
     * @param  skyPixer  maps sky positions to HEALPix indices
     * @param  shader   colour shading
     * @param  scaler   value scaling
     */
    public static void paintBins( Graphics g, BinList.Result binResult,
                                  double binFactor,
                                  SkySurface surface, SkyPixer skyPixer,
                                  Shader shader, Scaler scaler ) {
        Rectangle bounds = surface.getPlotBounds();

        /* Work out how to scale binlist values to turn into
         * entries in a colour map.  The first entry in the colour map
         * (index zero) corresponds to transparency. */
        IndexColorModel colorModel =
            PixelImage.createColorModel( shader, true );
        int ncolor = colorModel.getMapSize() - 1;

        /* Prepare a screen pixel grid. */
        int nx = bounds.width;
        int ny = bounds.height;
        Gridder gridder = new Gridder( nx, ny );
        int npix = gridder.getLength();
        int[] pixels = new int[ npix ];

        /* Iterate over screen pixel grid pulling samples from the
         * sky pixel grid for each screen pixel.  Note this is only
         * a good strategy if the screen oversamples the sky grid
         * (i.e. if screen pixels are smaller than the sky pixels). */
        Point2D.Double point = new Point2D.Double();
        double x0 = bounds.x + 0.5;
        double y0 = bounds.y + 0.5;
        for ( int ip = 0; ip < npix; ip++ ) {
            point.x = x0 + gridder.getX( ip );
            point.y = y0 + gridder.getY( ip );
            double[] dpos = surface.graphicsToData( point, null );

            /* Positions on the sky always have a value >= 1.
             * Positions outside the sky coord range are untouched,
             * so have a value of 0 (transparent). */
            if ( dpos != null ) {
                double dval =
                    binResult.getBinValue( skyPixer.getIndex( dpos ) );
                if ( ! Double.isNaN( dval ) ) {
                    pixels[ ip ] =
                        Math.min( 1 + (int) ( scaler.scaleValue( binFactor *
                                                                 dval )
                                              * ncolor ),
                                  ncolor - 1 );
                }
            }
        }

        /* Copy the pixel grid to the graphics context using the
         * requested colour map. */
        new PixelImage( bounds.getSize(), pixels, colorModel )
           .paintPixels( g, bounds.getLocation() );
    }

    /**
     * TileRenderer that resamples values, interrogating the bin list
     * for each screen pixel.
     * This is correct if healpix tiles are bigger than screen pixels
     * (otherwise averaging is not done correctly - should be drizzled
     * in that case really), and it is efficient if tiles are not too much
     * bigger than screen pixels (in that case painting would be faster).
     */
    private static class ResampleTileRenderer extends SkyTileRenderer {
        private final SkySurface surface_;
        private final SkyPixer skyPixer_;
        private final double binFactor_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  viewLevel   healpix level of painted tiles
         * @param  rotation  sky rotation to be applied before plotting
         * @param  binFactor   factor by which all bin values must be multiplied
         */
        ResampleTileRenderer( SkySurface surface, int viewLevel,
                              Rotation rotation, double binFactor ) {
            surface_ = surface;
            final Rotation unrotation = rotation.invert();
            binFactor_ = binFactor;
            skyPixer_ = new SkyPixer( viewLevel ) {
                public long getIndex( double[] v3 ) {
                    unrotation.rotate( v3 );
                    return super.getIndex( v3 );
                }
            };
        }

        public void extendAuxRange( Ranger ranger, BinList.Result binResult ) {
            Rectangle bounds = surface_.getPlotBounds();
            Gridder gridder = new Gridder( bounds.width, bounds.height );
            int npix = gridder.getLength();
            Point2D.Double point = new Point2D.Double();
            double x0 = bounds.x + 0.5;
            double y0 = bounds.y + 0.5;
            long hpix0 = -1;
            for ( int ip = 0; ip < npix; ip++ ) {
                point.x = x0 + gridder.getX( ip );
                point.y = y0 + gridder.getY( ip );
                double[] dpos = surface_.graphicsToData( point, null );
                if ( dpos != null ) {
                    long hpix = skyPixer_.getIndex( dpos );
                    if ( hpix != hpix0 ) {
                         hpix0 = hpix;
                        ranger.submitDatum( binFactor_ *
                                            binResult.getBinValue( hpix ) );
                    }
                }
            }
        }

        public void renderBins( Graphics g, BinList.Result binResult,
                                Shader shader, Scaler scaler ) {
            paintBins( g, binResult, binFactor_, surface_, skyPixer_,
                       shader, scaler );
        }
    }

    /**
     * TileRenderer that paints tiles one at a time using graphics context
     * primitives.  This is correct and efficient if healpix tiles are
     * larger (maybe significantly larger) than screen pixels.
     */
    private static class PaintTileRenderer extends SkyTileRenderer {
        private final SkySurfaceTiler tiler_;
        private final double binFactor_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  viewLevel   healpix level of painted tiles
         * @param  rotation  sky rotation to be applied before plotting
         * @param  binFactor   factor by which all bin values must be multiplied
         */
        PaintTileRenderer( SkySurface surface, int viewLevel,
                           Rotation rotation, double binFactor ) {
            tiler_ = new SkySurfaceTiler( surface, rotation, viewLevel );
            binFactor_ = binFactor;
        }

        public void extendAuxRange( Ranger ranger, BinList.Result binResult ) {
            for ( Long hpxObj : tiler_.visiblePixels() ) {
                long hpx = hpxObj.longValue();
                double value = binResult.getBinValue( hpx );
                if ( ! Double.isNaN( value ) ) {
                    ranger.submitDatum( binFactor_ * value );
                }
            }
        }

        public void renderBins( Graphics g, BinList.Result binResult,
                                Shader shader, Scaler scaler ) {
            Color color0 = g.getColor();
            float[] rgba = new float[ 4 ];
            for ( Long hpxObj : tiler_.visiblePixels() ) {
                long hpx = hpxObj.longValue();
                double value = binResult.getBinValue( hpx );
                if ( ! Double.isNaN( value ) ) {
                    Polygon shape = tiler_.getTileShape( hpx );
                    if ( shape != null ) {
                        rgba[ 0 ] = 0.5f;
                        rgba[ 1 ] = 0.5f;
                        rgba[ 2 ] = 0.5f;
                        rgba[ 3 ] = 1.0f;
                        float sval = (float)
                                     scaler.scaleValue( binFactor_ * value );
                        shader.adjustRgba( rgba, sval );
                        g.setColor( new Color( rgba[ 0 ], rgba[ 1 ],
                                               rgba[ 2 ], rgba[ 3 ] ) );
                        g.fillPolygon( shape );
                    }
                }
            }
            g.setColor( color0 );
        }
    }
}
