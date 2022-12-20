package uk.ac.starlink.ttools.plot;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import org.jfree.graphics2d.svg.MeetOrSlice;
import org.jfree.graphics2d.svg.PreserveAspectRatio;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUnits;
import org.jfree.graphics2d.svg.ViewBox;
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
    private final boolean isVector_;
    private final String description_;
    private final String[] fileSuffixes_;

    /**
     * Constructor.
     *
     * @param   name  exporter name (usually graphics format name)
     * @param   mimeType  MIME type for this exporter's output format
     * @param   isVector  true for vector formats, false for bitmapped
     * @param   description  minimal description of format (may just be name)
     * @param   fileSuffixes  file suffixes which usually indicate the
     *          export format used by this instance (may be null)
     */
    protected GraphicExporter( String name, String mimeType, boolean isVector,
                               String description, String[] fileSuffixes ) {
        name_ = name;
        mimeType_ = mimeType;
        isVector_ = isVector;
        description_ = description;
        fileSuffixes_ = fileSuffixes == null ? new String[ 0 ]
                                             : fileSuffixes.clone();
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
     * Indicates whether this exports to a vector or bitmapped graphics format.
     *
     * @return  true for vector graphics, false for bitmapped
     */
    public boolean isVector() {
        return isVector_;
    }

    /**
     * Returns a minimal description of this exporter.
     * This may just be the format's name if there's nothing else to say.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
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
        return fileSuffixes_.clone();
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
         new ImageIOExporter( "jpeg", "image/jpeg", "JPEG",
                              new String[] { ".jpg", ".jpeg" }, "jpeg", false );

    /** Exports to PNG format with a standard (currently opaque) background. */
    public static final GraphicExporter PNG =
         new ImageIOExporter( "png", "image/png", "PNG",
                              new String[] { ".png" }, "png", false );

    /** Exports to PNG format with a transparent background. */
    public static final GraphicExporter PNG_TRANSPARENT =
         new ImageIOExporter( "png-transp", "image/png",
                              "PNG with transparent background",
                              null, "png", true );

    /** Exports to GIF format. */
    public static final GraphicExporter GIF =
        new ImageIOExporter( "gif", "image/gif", "GIF",
                             new String[] { ".gif" }, "gif", false );

    /** Exports to SVG format. */
    public static final GraphicExporter SVG =
            new GraphicExporter( "svg", "image/svg+xml", true,
                                 "Scalable Vector Graphics",
                                 new String[] { ".svg", } ) {
        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
            int w = picture.getPictureWidth();
            int h = picture.getPictureHeight();
            SVGGraphics2D g2 = new SVGGraphics2D( w, h, SVGUnits.PX );
            picture.paintPicture( g2 );
            Writer writer = new OutputStreamWriter( out, "UTF-8" );

            /* Export to suitable XML.  There are some subtleties in getting
             * this right; following advice and experimentation, setting both
             * the viewBox attribute and dimensions (width/height) attributes,
             * with preserveAspectRatio left to defaults, seems to provide the
             * right behaviour for default and rescaled image size in both
             * IMG and OBJECT elements.  Output is to a single SVG element,
             * undecorated by an XML or DOCTYPE declaration.
             * These details are subject to change if expert SVG users decide
             * they're doing the wrong thing. */
            String id = null;
            boolean includeDimensions = true;
            ViewBox viewBox = new ViewBox( 0, 0, w, h );
            PreserveAspectRatio aspect = null;
            MeetOrSlice meet = null;
            writer.write( g2.getSVGElement( id, includeDimensions, viewBox,
                                            aspect, meet ) );
            writer.close();
        }
    };

    /** Exports to Encapsulated PostScript. */
    public static final GraphicExporter EPS =
            new GraphicExporter( "eps", "application/postscript", true,
                                 "Encapsulated PostScript",
                                 new String[] { ".eps", ".ps", } ) {
        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
        
            /* Scale to a pixel size which makes the bounding box sit
             * sensibly on an A4 or letter page.  EpsGraphics2D default
             * scale is 72dpi. */
            int width = picture.getPictureWidth();
            int height = picture.getPictureHeight();
            double padfrac = 0.0;
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

    /**
     * Returns a standard list of available GraphicExporter objects.
     * However, the one for exporting PDFs must be supplied explicitly,
     * since which to choose (if any) depends on configuration.
     *
     * @param  pdfEx   exporter for PDF graphics, or null if none required
     * @return   list of available exporters including the supplied PDF one
     */
    public static GraphicExporter[]
           getKnownExporters( PdfGraphicExporter pdfEx ) {
        List<GraphicExporter> list = new ArrayList<GraphicExporter>();
        list.add( GraphicExporter.PNG );
        list.add( GraphicExporter.PNG_TRANSPARENT );
        list.add( GraphicExporter.GIF );
        list.add( GraphicExporter.JPEG );
        if ( pdfEx != null ) {
            list.add( pdfEx );
        }
        list.add( GraphicExporter.SVG );

        /* Note there is another option for postscript - net.sf.epsgraphics.
         * On brief tests seems to work, may or may not produce more compact
         * output than jibble implementation.  At time of testing, it was
         * using J2SE5 and codebase was at J2SE1.4, so didn't investigate
         * further. */
        list.add( GraphicExporter.EPS );
        list.add( GraphicExporter.EPS_GZIP );

        return list.toArray( new GraphicExporter[ 0 ] );
    }

    /**
     * GraphicExporter implementation which uses the ImageIO framework.
     */
    private static class ImageIOExporter extends GraphicExporter {
        private final String iioName_;
        private final boolean transparentBg_;
        private final boolean isSupported_;

        /**
         * Constructor.
         *
         * @param  name   exporter name
         * @param  mimeType  MIME type for this exporter's output format
         * @param  description  minimal format description (may just be name)
         * @param  fileSuffixes  file suffixes which usually indicate the
         *         export format used by this instance (may be null)
         * @param  iioName  ImageIO format name
         * @param  transparentBg  true to use a transparent background,
         *              only permissible if format supports transparency
         */
        ImageIOExporter( String name, String mimeType, String description,
                         String[] fileSuffixes, String iioName,
                         boolean transparentBg ) {
            super( name, mimeType, false, description, fileSuffixes );
            iioName_ = iioName;
            transparentBg_ = transparentBg;
            isSupported_ =
                ImageIO.getImageWritersByFormatName( iioName ).hasNext();
        }

        public void exportGraphic( Picture picture, OutputStream out )
                throws IOException {
            if ( ! isSupported_ ) {
                throw new IOException( "Graphics export to " + iioName_
                                     + " not supported" );
            }

            /* Create an image buffer on which to paint. */
            int w = picture.getPictureWidth();
            int h = picture.getPictureHeight();
            int imageType = transparentBg_ ? BufferedImage.TYPE_INT_ARGB
                                           : BufferedImage.TYPE_INT_RGB;
            BufferedImage image = new BufferedImage( w, h, imageType );
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint( RenderingHints.KEY_RENDERING,
                                 RenderingHints.VALUE_RENDER_QUALITY );

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
            boolean done = ImageIO.write( image, iioName_, out );
            out.flush();
            g2.dispose();
            if ( ! done ) {
                throw new IOException( "No handler for format " + iioName_ +
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
                   baseExporter.isVector(),
                   "Gzipped " + baseExporter.getDescription(),
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
}
