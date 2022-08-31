package uk.ac.starlink.ttools.build;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot2.config.ClippedShader;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.util.IconUtils;
import uk.ac.starlink.util.LogUtils;

/**
 * Generates a graphic in SVG format showing labelled colourmaps.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2022
 */
public class ShaderLegend {

    private final int ncol_;
    private final int colWidth_;
    private final int textWidth_;
    private final int rampWidth_;
    private final int rowHeight_;
    private final int rampHeight_;
    private final int pad_;
    private final boolean forcePixelated_;

    /**
     * Constructor.
     *
     * @param  ncol  number of columns for output
     */
    public ShaderLegend( int ncol ) {
        ncol_ = ncol;
        colWidth_ = 300;
        textWidth_ = 130;
        rampWidth_ = 120;
        rowHeight_ = 30;
        rampHeight_ = 20;
        pad_ = 15;
        forcePixelated_ = false;
    }

    /**
     * Returns an SVG representation of a legend labelling a given
     * list of shaders.
     *
     * @param  shaders  shader list
     * @return  SVG output
     */
    public String toSvg( Shader[] shaders ) {
        Base64.Encoder b64encoder = Base64.getEncoder();
        int ns = shaders.length;
        int nrow = ( ns + ncol_ - 1 ) / ncol_;
        int wpix = colWidth_ * ncol_ + 2 * pad_;
        int hpix = rowHeight_ * nrow + 2 * pad_;
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( String.join( "\n",
            "<svg xmlns='http://www.w3.org/2000/svg'",
            "     xmlns:xlink='http://www.w3.org/1999/xlink'",
            "     width='" + wpix + "px' height='" + hpix + "px'"
              + " viewBox='0 0 " + wpix + " " + hpix + "'>",
        "" ) );

        /* In some cases, it seems to be necessary to set image-rendering
         * to "pixelated" to prevent the renderer doing some horrible
         * antialiasing of single-pixel-high images.  But it's not supported
         * by all SVG renderers. */
        if ( forcePixelated_ ) {
            sbuf.append( "  <g style='image-rendering: pixelated;'>\n" );
        }
        for ( int is = 0; is < ns; is++ ) {
            Shader shader = shaders[ is ];
            int ir = is % nrow;
            int ic = is / nrow;
            int x0 = pad_ + colWidth_ * ic;
            int y0 = pad_ + rowHeight_ * ( ir + 1 );
            int iconHeight = shader.isAbsolute() ? 1 : 16;
            byte[] rampPng = createShaderPng( shader, 256, iconHeight );
            sbuf.append( "    <text x='" + x0 + "' y='" + y0 + "'>" )
                .append( shader.getName() )
                .append( "</text>\n" );
            sbuf.append( "    <image preserveAspectRatio='none'" )
                .append( " width='" + rampWidth_ + "'" )
                .append( " height='" + rampHeight_ + "'" )
                .append( " x='" + ( x0 + textWidth_ ) + "'" )
                .append( " y='" + ( y0 - rampHeight_ ) + "'\n" )
                .append( "           xlink:href='data:image/png;base64," )
                .append( b64encoder.encodeToString( rampPng ) )
                .append( "'/>\n" );
        }
        if ( forcePixelated_ ) {
            sbuf.append( "</g>\n" );
        }
        sbuf.append( "</svg>\n" );
        return sbuf.toString();
    }

    /**
     * Returns a byte array giving a PNG representation of the colour ramp
     * associated with a given shader.
     *
     * @param  shader  shader
     * @param  w   width in pixels (varying with ramp variable)
     * @param  h   height in pixels (not varying with ramp variable)
     * @return  PNG bytes
     */
    private static byte[] createShaderPng( Shader shader, int w, int h ) {
        Icon icon = Shaders.createShaderIcon( shader, true, w, h, 0, 0 );
        BufferedImage img = IconUtils.createImage( icon );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write( img, "PNG", out );
            out.flush();
            out.close();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "I/O error: " + e, e );
        }
        return out.toByteArray();
    }

    /**
     * Returns all the shaders generally available from Plot2 graphics.
     *
     * @return   somewhat ordered list of shaders
     */
    public static Shader[] getAllShaders() {
        Collection<Shader> shaders = new LinkedHashSet<>();
        RampKeySet[] ramps = {
            StyleKeys.AUX_RAMP, StyleKeys.DENSITY_RAMP,
        };
        for ( RampKeySet ramp : ramps ) {
            for ( ClippedShader cshader : ramp.getShaders() ) {
                Shader shader = cshader.getShader();
                if ( ! shaders.contains( shader ) ) {
                    shaders.add( shader );
                }
            }
        }
        return shaders.toArray( new Shader[ 0 ] );
    }

    /**
     * Main method invoked to write SVG to standard output.
     * Use -help flag for usage information.
     *
     * @param  args  flag array
     */
    public static void main( String[] args ) {
        LogUtils.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
        String usage = "\n   Usage: "
                     + ShaderLegend.class.getName()
                     + " [-ncol <n>]"
                     + " [-abs|-noabs]"
                     + "\n";
        List<String> argList = new ArrayList<>( Arrays.asList( args ) );
        Predicate<Shader> filter = s -> true;
        int ncol = 2;
        boolean writeHelp = false;
        for ( Iterator<String> argIt = argList.iterator(); argIt.hasNext(); ) {
            String arg = argIt.next();
            if ( "-abs".equals( arg ) ) {
                argIt.remove();
                filter = s -> s.isAbsolute();
            }
            else if ( "-noabs".equals( arg ) ) {
                argIt.remove();
                filter = s -> ! s.isAbsolute();
            }
            else if ( "-ncol".equals( arg ) && argIt.hasNext() ) {
                argIt.remove();
                ncol = Integer.parseInt( argIt.next() );
                argIt.remove();
            }
            else {
                writeHelp = true;
            }
        }
        if ( writeHelp || argList.size() > 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        List<Shader> list = new ArrayList<>();
        for ( Shader shader : getAllShaders() ) {
            if ( filter.test( shader ) ) {
                list.add( shader );
            }
        }
        Shader[] shaders = list.toArray( new Shader[ 0 ] );
        System.out.println( new ShaderLegend( ncol ).toSvg( shaders ) );
    }
}
