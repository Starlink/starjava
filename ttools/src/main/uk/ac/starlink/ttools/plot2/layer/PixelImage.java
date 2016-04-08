package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot.Shader;

/**
 * Aggregates information for painting a colour-mapped image.
 *
 * <p>Some utility methods are provided for generating suitable colour models.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2015
 */
public class PixelImage {

    private final Dimension size_;
    private final int[] pixels_;
    private final IndexColorModel colorModel_;

    private static final int NBIT = 8;
    private static final int IMAGE_TYPE = BufferedImage.TYPE_BYTE_INDEXED;
    private static final int MAP_SIZE = 128;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.layer" );

    /**
     * Constructor.
     *
     * @param  size    dimensions of the image
     * @param  pixels  pixel array, fits <code>size</code>
     *                 all values must fall in range of colour model
     * @param  colorModel  indexed colour model
     */
    public PixelImage( Dimension size, int[] pixels,
                       IndexColorModel colorModel ) {
        if ( size.width * size.height != pixels.length ) {
            throw new IllegalArgumentException( "pixel array wrong length" );
        }
        size_ = new Dimension( size );
        pixels_ = pixels;
        colorModel_ = colorModel;
    }

    /**
     * Returns the dimensions of this image.
     *
     * @return  size
     */
    public Dimension getSize() {
        return new Dimension( size_ );
    }

    /**
     * Returns the pixel array.
     *
     * @return   pixel array
     */
    public int[] getPixels() {
        return pixels_;
    }

    /**
     * Returns the colour model.
     *
     * @return  colour model
     */
    public IndexColorModel getColorModel() {
        return colorModel_;
    }

    /**
     * Paints this image to a given graphics context.
     *
     * @param  g  graphics context
     * @param  origin  target position for origin of painted image
     */
    public void paintPixels( Graphics g, Point origin ) {
        paintScaledPixels( g, origin, 1 );
    }

    /**
     * Paints this image to a given graphics context, with each pixel
     * scaled by an integer factor.
     *
     * @param  g  graphics context
     * @param  origin  target position for origin of painted image
     * @param  scale   scaling factor
     */
    public void paintScaledPixels( Graphics g, Point origin, int scale ) {
        int width = size_.width;
        int height = size_.height;
        BufferedImage image =
            new BufferedImage( width, height, IMAGE_TYPE, colorModel_ );
        WritableRaster raster = image.getRaster();
        assert raster.getNumBands() == 1;
        raster.setSamples( 0, 0, width, height, 0, pixels_ );
        assert raster.getWidth() == width;
        assert raster.getHeight() == height;
        if ( scale == 1 ) {
            g.drawImage( image, origin.x, origin.y, null );
        }
        else {
            g.drawImage( image, origin.x, origin.y,
                         width * scale, height * scale, null );
        }
    }

    /**
     * Returns an indexed colour model whose entries range from one end
     * to the other of a given shader object.
     *
     * @param  shader   shader; should be absolute
     * @param  zeroTransparent  if true, the first entry in the returned
     *                          colour map is transparent
     * @return  colour model
     */
    public static IndexColorModel createColorModel( Shader shader,
                                                    boolean zeroTransparent ) {
        if ( ! shader.isAbsolute() ) {
            logger_.warning( "Using non-absolute shader for indexed color map"
                           + " is a bad idea" );
        }
        byte[] red = new byte[ MAP_SIZE ];
        byte[] green = new byte[ MAP_SIZE ];
        byte[] blue = new byte[ MAP_SIZE ];
        byte[] alpha = new byte[ MAP_SIZE ];
        float[] rgba = new float[ 4 ];
        int iStart = zeroTransparent ? 1 : 0;
        float scale = 1f / ( MAP_SIZE - 1 );
        int iTransparent = zeroTransparent ? 0 : -1;
        boolean hasAlpha = false;
        for ( int i = iStart; i < MAP_SIZE; i++ ) {
            assert i != iTransparent;
            rgba[ 3 ] = 1f;
            double level = ( i - iStart ) * scale;
            shader.adjustRgba( rgba, (float) level );
            red[ i ] = (byte) ( rgba[ 0 ] * 255 );
            green[ i ] = (byte) ( rgba[ 1 ] * 255 );
            blue[ i ] = (byte) ( rgba[ 2 ] * 255 );
            alpha[ i ] = (byte) ( rgba[ 3 ] * 255 );
            hasAlpha = hasAlpha || alpha[ i ] != (byte) 255;
        }
        final IndexColorModel model;
        if ( iTransparent >= 0 ) {

            /* Set the transparent colour to transparent white
             * not transparent black.
             * In most cases this makes no difference, but for rendering
             * targets which ignore transparency (PostScript) it can
             * help a bit, though such renderers are not going to work
             * well for multi-layer plots. */
            red[ iTransparent ] = (byte) 0xff;
            green[ iTransparent ] = (byte) 0xff;
            blue[ iTransparent ] = (byte) 0xff;
            alpha[ iTransparent ] = (byte) 0x00;
        }
        if ( hasAlpha ) {
            model = new IndexColorModel( NBIT, MAP_SIZE, red, green, blue,
                                         alpha );
            assert model.getTransparency() == Transparency.TRANSLUCENT;
        }
        else if ( iTransparent >= 0 ) {
            model = new IndexColorModel( NBIT, MAP_SIZE, red, green, blue,
                                         iTransparent );
            assert model.getTransparency() == Transparency.BITMASK;
            assert model.getTransparentPixel() == 0;
        }
        else {
            model = new IndexColorModel( NBIT, MAP_SIZE, red, green, blue );
            assert model.getTransparency() == Transparency.OPAQUE;
        }
        assert model.getMapSize() == MAP_SIZE;
        assert model.getPixelSize() == NBIT;
        return model;
    }

    /**
     * Returns a 2-colour indexed colour model.
     *
     * @param  color  non-blank colour
     * @return  colour map with two entries:
     *          0=transparent, 1=<code>color</code>
     */
    public static IndexColorModel createMaskColorModel( Color color ) {
        IndexColorModel model =
            new IndexColorModel( 1, 2,
                                 new byte[] { 0, (byte) color.getRed() },
                                 new byte[] { 0, (byte) color.getGreen() },
                                 new byte[] { 0, (byte) color.getBlue() },
                                 0 );
        assert model.getTransparency() == Transparency.BITMASK;
        assert model.getTransparentPixel() == 0;
        return model;
    }
}
