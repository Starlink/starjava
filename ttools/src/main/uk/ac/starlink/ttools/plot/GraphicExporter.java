package uk.ac.starlink.ttools.plot;

import Acme.JPM.Encoders.GifEncoder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Exports painted graphics to an output file in some graphics format.
 *
 * @author   Mark Taylor
 * @since    1 Aug 2008
 */
public abstract class GraphicExporter {

    private final String name_;
    private final String mimeType_;
    private final String[] fileSuffixes_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plot" );

    /**
     * Constructor.
     *
     * @param   name  exporter name (usually graphics format name)
     * @param   mimeType  MIME type for this exporter's output format
     * @param   fileSuffixes  file suffixes which usually indicate the
     *          export format used by this instance (may be null)
     */
    protected GraphicExporter( String name, String mimeType,
                               String[] fileSuffixes ) {
        name_ = name;
        mimeType_ = mimeType;
        fileSuffixes_ = fileSuffixes == null ? new String[ 0 ]
                                             : (String[]) fileSuffixes.clone();
    }

    /**
     * Paints the given picture to an output stream using some graphics format
     * or other.
     * This method should not close the stream.
     *
     * @param  picture  picture to draw
     * @param  out   destination output stream
     */
    public abstract void exportGraphic( Picture picture, OutputStream out )
            throws IOException;

    /**
     * Returns the name of this exporter (usually the graphics format name).
     *
     * @return  exporter name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns the MIME type for the graphics format used by this exporter.
     *
     * @return  MIME type string
     */
    public String getMimeType() {
        return mimeType_;
    }

    /**
     * Returns the content encoding for the output used by this exporter.
     * The default implementation returns null, meaning no special encoding.
     *
     * @return  content encoding
     */
    public String getContentEncoding() {
        return null;
    }

    /**
     * Returns an array of file suffixes which usually indicate a file with
     * an export format used by this instance.
     *
     * @return  copy of file suffix list; may be empty but will not be null
     */
    public String[] getFileSuffixes() {
        return (String[]) fileSuffixes_.clone();
    }

    public String toString() {
        return name_;
    }

    /**
     * Utility method to acquire a Picture object which can paint the content
     * of a screen component.
     * The component should not be altered while the picture is in use.
     *
     * @param   comp  screen component
     * @return   object to draw comp's content
     */
    public static Picture toPicture( final JComponent comp ) {
        final Rectangle bounds = comp.getBounds();
        return new Picture() {
            public int getPictureWidth() {
                return bounds.width;
            }
            public int getPictureHeight() {
                return bounds.height;
            }
            public void paintPicture( Graphics2D g ) {
                int xoff = - bounds.x;
                int yoff = - bounds.y;
                g.translate( xoff, yoff );
                comp.print( g );
                g.translate( -xoff, -yoff );
            }
        };
    }

    /** Exports to JPEG format. */
    public static final GraphicExporter JPEG =
         new ImageIOExporter( "jpeg", "image/jpeg",
                              new String[] { ".jpg", ".jpeg" }, false );

    /** Exports to PNG format. */
    public static final GraphicExporter PNG =
         new ImageIOExporter( "png", "image/png",
                              new String[] { ".png" }, false );

    /**
     * Exports to GIF format.
     */
    public static final GraphicExporter GIF =
            new GraphicExporter( "gif", "image/gif",
                                 new String[] { ".gif", } ) {

        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {

            /* Get component dimensions. */
            int w = picture.getPictureWidth();
            int h = picture.getPictureHeight();

            /* Create a BufferedImage to draw it onto. */
            BufferedImage image =
                new BufferedImage( w, h, BufferedImage.TYPE_3BYTE_BGR );

            /* Clear the background. */
            Graphics2D g = image.createGraphics();
            Color color = g.getColor();
            g.setColor( Color.WHITE );
            g.fillRect( 0, 0, w, h );
            g.setColor( color );

            /* Draw the component onto the image. */
            picture.paintPicture( g );
            g.dispose();

            /* Count the number of colours represented in the resulting
             * image. */
            Set colors = new HashSet();
            for ( int ix = 0; ix < w; ix++ ) {
                for ( int iy = 0; iy < h; iy++ ) {
                    colors.add( new Integer( image.getRGB( ix, iy ) ) );
                }
            }

            /* If there are too many, redraw the image into an indexed image
             * instead.  This is necessary since the GIF encoder we're using
             * here just gives up if there are too many. */
            if ( colors.size() > 254 ) {
                logger_.warning( "GIF export colour map filled up - "
                               + "JPEG or PNG might do a better job" );

                /* Create an image with a suitable colour model. */
                IndexColorModel gifColorModel = getGifColorModel();
                image = new BufferedImage( w, h,
                                           BufferedImage.TYPE_BYTE_INDEXED,
                                           gifColorModel );

                /* Zero all pixels to the transparent colour. */
                WritableRaster raster = image.getRaster();
                int itrans = gifColorModel.getTransparentPixel();
                if ( itrans >= 0 ) {
                    byte[] pixValue = new byte[] { (byte) itrans };
                    for ( int ix = 0; ix < w; ix++ ) {
                        for ( int iy = 0; iy < h; iy++ ) {
                            raster.setDataElements( ix, iy, pixValue );
                        }
                    }
                }

                /* Draw the component on it. */
                Graphics2D gifG = image.createGraphics();

                /* Set dithering false.  But it still seems to dither on a
                 * drawImage!  Can't get to the bottom of it. */
                gifG.setRenderingHint( RenderingHints.KEY_DITHERING,
                                       RenderingHints.VALUE_DITHER_DISABLE );
                picture.paintPicture( gifG );
                gifG.dispose();
            }

            /* Write the image as a gif down the provided stream. */
            new GifEncoder( image, out ).encode();
        }
    };

    /** Exports to Encapsulated PostScript. */
    public static final GraphicExporter EPS =
            new GraphicExporter( "eps", "application/postscript",
                                 new String[] { ".eps", ".ps", } ) {
        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
        
            /* Scale to a pixel size which makes the bounding box sit
             * sensibly on an A4 or letter page.  EpsGraphics2D default
             * scale is 72dpi. */
            int width = picture.getPictureWidth();
            int height = picture.getPictureHeight();
            double padfrac = 0.05;       
            double xdpi = width / 6.0;
            double ydpi = height / 9.0;
            double scale;
            int pad;
            if ( xdpi > ydpi ) {
                scale = 72.0 / xdpi;     
                pad = (int) Math.ceil( width * padfrac * scale );
            }           
            else {
                scale = 72.0 / ydpi;
                pad = (int) Math.ceil( height * padfrac * scale );
            }
            int xlo = - pad;
            int ylo = - pad;
            int xhi = (int) Math.ceil( scale * width ) + pad;
            int yhi = (int) Math.ceil( scale * height ) + pad;
            
            /* Construct a graphics object which will write postscript
             * down this stream. */
            EpsGraphics2D g2 = 
                new FixedEpsGraphics2D( "Plot", out, xlo, ylo, xhi, yhi );
            g2.scale( scale, scale );

            /* Do the drawing. */
            picture.paintPicture( g2 );

            /* Note this close call *must* be made, otherwise the
             * eps file is not flushed or correctly terminated.
             * This closes the output stream too. */ 
            g2.close();
        }
    };

    /** Exports to gzipped Encapsulated PostScript. */
    public static final GraphicExporter EPS_GZIP = new GzipExporter( EPS );

    /** Exports to PDF. */
    public static final GraphicExporter PDF =
            new GraphicExporter( "pdf", "application/pdf",
                                 new String[] { "pdf", } ) {
        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
            int width = picture.getPictureWidth();
            int height = picture.getPictureHeight();
            Document doc =
                new Document( new com.lowagie.text.Rectangle( width, height ) );
            try {
                PdfWriter pWriter = PdfWriter.getInstance( doc, out );
                doc.open();
                Graphics2D g = pWriter.getDirectContent()
                              .createGraphics( width, height );
                picture.paintPicture( g );
                g.dispose();
                doc.close();
            }
            catch ( DocumentException e ) {
                throw (IOException)
                      new IOException( e.getMessage() ).initCause( e );
            }
        }
    };

    /**
     * GraphicExporter implementation which uses the ImageIO framework.
     */
    private static class ImageIOExporter extends GraphicExporter {
        private final String formatName_;
        private final boolean transparentBg_;
        private final boolean isSupported_;

        /**
         * Constructor.
         *
         * @param  formatName  ImageIO format name
         * @param  mimeType  MIME type for this exporter's output format
         * @param  transparentBg  true to use a transparent background,
         *              only permissible if format supports transparency
         * @param   fileSuffixes  file suffixes which usually indicate the
         *          export format used by this instance (may be null)
         */
        ImageIOExporter( String formatName, String mimeType, 
                         String[] fileSuffixes, boolean transparentBg ) {
            super( formatName, mimeType, fileSuffixes );
            formatName_ = formatName;
            transparentBg_ = transparentBg;
            isSupported_ =
                ImageIO.getImageWritersByFormatName( formatName ).hasNext();
        }

        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
            if ( ! isSupported_ ) {
                throw new IOException( "Graphics export to " + formatName_
                                     + " not supported" );
            }

            /* Create an image buffer on which to paint. */
            int w = picture.getPictureWidth();
            int h = picture.getPictureHeight();
            int imageType = transparentBg_ ? BufferedImage.TYPE_INT_ARGB
                                           : BufferedImage.TYPE_INT_RGB;
            BufferedImage image = new BufferedImage( w, h, imageType );
            Graphics2D g2 = image.createGraphics();

            /* Clear the background.  Failing to do this can leave junk. */
            Color color = g2.getColor();
            Composite compos = g2.getComposite();
            if ( transparentBg_ ) {

                /* Attempt to clear to transparent white, but this doesn't
                 * seem to work well, at least for PNG (looks like
                 * transparent black). */
                g2.setComposite( AlphaComposite.Src );
                g2.setColor( new Color( 1f, 1f, 1f, 0f ) );
            }
            else {
                g2.setColor( Color.WHITE );
            }
            g2.fillRect( 0, 0, w, h );
            g2.setColor( color );
            g2.setComposite( compos );

            /* Paint the graphics to the buffer. */
            picture.paintPicture( g2 );

            /* Export. */
            boolean done = ImageIO.write( image, formatName_, out );
            out.flush();
            g2.dispose();
            if ( ! done ) {
                throw new IOException( "No handler for format " + formatName_ +
                                       " (surprising - thought there was)" );
            }
        }
    }

    /**
     * Exporter which wraps another one to provide gzip compression of output.
     */
    private static class GzipExporter extends GraphicExporter {
        private final GraphicExporter baseExporter_;

        /**
         * Constructor.
         *
         * @param  baseExporter  exporter whose output is to be compressed
         */
        GzipExporter( GraphicExporter baseExporter ) {
            super( baseExporter.getName() + "-gzip", baseExporter.getMimeType(),
                   appendGzipSuffix( baseExporter.getFileSuffixes() ) );
            baseExporter_ = baseExporter;
        }

        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
            GZIPOutputStream gzout = new GZIPOutputStream( out );
            baseExporter_.exportGraphic( picture, gzout );
            gzout.finish();
        }

        public String getContentEncoding() {
            return "gzip";
        }

        private static String[] appendGzipSuffix( String[] names ) {
            String[] sNames = new String[ names.length ];
            for ( int i = 0; i < names.length; i++ ) {
                sNames[ i ] = names[ i ] + ".gz";
            }
            return sNames;
        }
    }

    /**
     * Returns a colour model suitable for use with GIF images.
     * It has a selection of RGB colours and one transparent colour.
     *
     * @return  standard GIF indexed colour model
     */
    private static IndexColorModel getGifColorModel() {

        /* Acquire a standard general-purpose 256-entry indexed colour model. */
        IndexColorModel rgbModel =
            (IndexColorModel)
            new BufferedImage( 1, 1, BufferedImage.TYPE_BYTE_INDEXED )
           .getColorModel();

        /* Get r/g/b entries from it. */
        byte[][] rgbs = new byte[ 3 ][ 256 ];
        rgbModel.getReds( rgbs[ 0 ] );
        rgbModel.getGreens( rgbs[ 1 ] );
        rgbModel.getBlues( rgbs[ 2 ] );

        /* Set one entry transparent. */
        int itrans = 254; 
        rgbs[ 0 ][ itrans ] = (byte) 255;
        rgbs[ 1 ][ itrans ] = (byte) 255;
        rgbs[ 2 ][ itrans ] = (byte) 255;
        IndexColorModel gifModel =
            new IndexColorModel( 8, 256, rgbs[ 0 ], rgbs[ 1 ], rgbs[ 2 ],
                                 itrans );

        /* Return the  model. */
        return gifModel;
    }
}
