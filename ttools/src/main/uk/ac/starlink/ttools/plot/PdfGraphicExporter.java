package uk.ac.starlink.ttools.plot;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;

/**
 * GraphicExporter implementation that exports to PDF format.
 * The best way to turn fonts are turned into graphics in PDFs
 * is not obvious, so this class parameterises the options.
 * Static members provide ways of getting useful instances.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 */
public abstract class PdfGraphicExporter extends GraphicExporter {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Exporter which uses the default font mapper.  OK for standard fonts.
     */
    public static PdfGraphicExporter BASIC = new PdfGraphicExporter() {
        public Graphics2D createGraphics( PdfContentByte pcb,
                                          int width, int height ) {
            return pcb.createGraphics( width, height, new DefaultFontMapper() );
        }
    };

    /**
     * Exporter which writes text as shapes not using fonts.
     * Will generate reasonable output for any font without additional
     * preparation.  However, the output shapes are not perfect (visible only
     * at huge magnification) and it may be less efficient if there's
     * a lot of text (though possibly more efficient if there's very little).
     */
    public static PdfGraphicExporter GLYPH_TEXT = new PdfGraphicExporter() {
        public Graphics2D createGraphics( PdfContentByte pcb,
                                          int width, int height ) {
            return pcb.createGraphicsShapes( width, height );
        }
    };

    /**
     * Returns an exporter which uses externally stored fonts.
     * Output is perfect.
     * A location must be provided for a resource
     * giving a list of font locations.
     * If the fonts can't be found, behaviour reverts to that of
     * {@link #GLYPH_TEXT}.
     *
     * @param   fontsUrl  location of a text file of font resource strings
     *                    (typically file names or URLs of .ttf files)
     * @return  new exporter using external fonts
     */
    public static PdfGraphicExporter
                  createExternalFontExporter( URL fontsUrl ) {
        return new ExternalFontExporter( fontsUrl );
    }

    /**
     * Constructor.
     */
    protected PdfGraphicExporter() {
        super( "pdf", "application/pdf", "Portable Document Format",
               new String[] { "pdf" } );
    }

    @Override
    public void exportGraphic( Picture picture, OutputStream out )
            throws IOException {
        int width = picture.getPictureWidth();
        int height = picture.getPictureHeight();
        Document doc =
            new Document( new com.lowagie.text.Rectangle( width, height ) );
        try {
            PdfWriter pWriter = PdfWriter.getInstance( doc, out );
            doc.open();
            Graphics2D g2 =
                createGraphics( pWriter.getDirectContent(), width, height );
            picture.paintPicture( g2 );
            g2.dispose();
            doc.close();
        }
        catch ( DocumentException e ) {
            throw (IOException)
                  new IOException( e.getMessage() ).initCause( e );
        }
    }

    /**
     * Returns a graphics context which can be used to write to a given
     * PDF content object.
     * There is not a single obvious implementation of this method;
     * the best way to do it depends on how text glyphs are to be rendered.
     *
     * @param  pcb  PDF content object
     * @param  width in pixels
     * @param  height in pixels
     * @return  new graphics context
     */
    public abstract Graphics2D createGraphics( PdfContentByte pcb,
                                               int width, int height );

    /**
     * PdfGraphicExporter instance that uses externally stored LaTeX fonts.
     */
    private static class ExternalFontExporter extends PdfGraphicExporter {
        private final URL fontsUrl_;
        private FontMapper mapper_;

        /**
         * Constructor.
         *
         * @param  fontsLoc  location of resource listing .ttf font files
         */
        public ExternalFontExporter( URL fontsUrl ) {
            fontsUrl_ = fontsUrl;
        }

        public Graphics2D createGraphics( PdfContentByte pcb,
                                          int width, int height ) {
            FontMapper mapper = getMapper();
            return mapper == null ? pcb.createGraphicsShapes( width, height )
                                  : pcb.createGraphics( width, height, mapper );
        }

        /**
         * Returns a font mapper for use with this exporter.
         *
         * @return  lazily constructed font mapper
         */
        private synchronized FontMapper getMapper() {
            if ( mapper_ == null ) {
                ExternalFontMapper efm = null;
                if ( fontsUrl_ != null ) {
                    try {
                        efm = ExternalFontMapper
                             .createMapperFromResourceList( fontsUrl_
                                                           .openStream() );
                    }
                    catch ( Exception e ) {
                        logger_.warning( "Error reading external fonts at "
                                       + fontsUrl_ + ": " + e );
                    }
                }
                else {
                    logger_.warning( "Can't find external fonts at "
                                   + fontsUrl_ );
                }
                mapper_ = efm;
            }
            return mapper_;
        }
    }
}
