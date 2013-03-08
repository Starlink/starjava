package uk.ac.starlink.ttools.plot;

import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.DefaultFontMapper;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * IText FontMapper implementation that works with externally supplied
 * TTF files.  It is suitable for use with JLatexMath.
 * The paths of externally stored TTF files are supplied to the
 * mapper at construction time.
 * There are utility methods for extracting lists of the locations
 * of such paths from supplied zip (or jar) files.
 *
 * @author   Mark Taylor
 * @since    4 May 2012
 */
public class ExternalFontMapper extends DefaultFontMapper {

    private final Set<String> registeredFonts_;
    private final Map<String,BaseFont> fontMap_;
    private static final Pattern FONTPATH_REGEX =
        Pattern.compile( "^.*[:/\\\\](.*)\\.ttf" );
    private static final String CHARSET = "UTF-8";
    private static final Logger logger_ =
        Logger.getLogger( ExternalFontMapper.class.getName() );

    static {
        FontFactory.defaultEmbedding = true;
    }

    /**
     * Constructor.
     *
     * @param   fontLocations  locations for font TTF files as supplied to the
     *          FontFactory.register method; URLs work (probably files too)
     */
    public ExternalFontMapper( String[] fontLocations ) {
        fontMap_ = new HashMap<String,BaseFont>();
        registeredFonts_ = new HashSet<String>();
        for ( int il = 0; il < fontLocations.length; il++ ) {
            String loc = fontLocations[ il ];
            Matcher matcher = FONTPATH_REGEX.matcher( loc );
            if ( matcher.matches() ) {
                String fname = matcher.group( 1 );
                FontFactory.register( loc );
                registeredFonts_.add( fname );
            }
            else {
                throw new IllegalArgumentException( "Not a TTF font path: "
                                                  + loc );
            }
        }
    }

    @Override
    public BaseFont awtToPdf( Font font ) {
        String fname = font.getFontName();
        if ( ! fontMap_.containsKey( fname ) ) {
            final BaseFont baseFont;
            if ( registeredFonts_.contains( fname ) ) {
                com.lowagie.text.Font itextFont = FontFactory.getFont( fname );
                BaseFont bf = itextFont == null ? null
                                                : itextFont.getBaseFont();
                if ( bf == null ) {
                    logger_.warning( "Failed to get registered font " + fname );
                    bf = super.awtToPdf( font );
                }
                else {
                    logger_.config( "Embedding registered PDF font " + fname );
                }
                baseFont = bf;
            }
            else {
                logger_.config( "Embedding default PDF font " + fname );
                baseFont = super.awtToPdf( font );
            }
            fontMap_.put( fname, baseFont );
        }
        return fontMap_.get( fname );
    }

    /**
     * Creates a font mapper given a list of font resource paths,
     * for resources available on the classpath.
     * These are mapped to URLs and passed to the constructor.
     *
     * @param  paths  absolute resource path strings for TTF files
     * @return  new font mapper
     */
    public static ExternalFontMapper
            createMapperFromResourcePaths( String[] paths ) {
        List<String> resourceList = new ArrayList<String>();
        Class clazz = ExternalFontMapper.class;
        for ( int ip = 0; ip < paths.length; ip++ ) {
            URL url = clazz.getResource( paths[ ip ] );
            if ( url != null ) {
                resourceList.add( url.toString() );
            }
        }
        return new ExternalFontMapper( resourceList
                                      .toArray( new String[ 0 ] ) );
    }

    /**
     * Creates a font mapper given a stream of strings giving resource paths,
     * for resources available on the classpath.
     * The stream is read and closed,
     * and {@link #createMapperFromResourcePaths} is called.
     * Each resource is on a separate line, encoding is UTF-8.
     *
     * @param  in  input stream
     * @return  new font mapper
     */
    public static ExternalFontMapper
            createMapperFromResourceList( InputStream in )
            throws IOException {
        return createMapperFromResourcePaths( readLines( in ) );
    }

    /**
     * Reads lines of text from a stream.
     *
     * @param  in  input stream
     * @return   lines
     */
    public static String[] readLines( InputStream in ) throws IOException {
        List<String> lineList = new ArrayList<String>();
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( in, CHARSET ) );
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                lineList.add( line );
            }
        }
        finally {
            rdr.close();
        }
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Writes lines of text to a stream.
     *
     * @param  lines  lines to write
     * @param  out  output stream
     */
    public static void writeLines( String[] lines, OutputStream out )
            throws IOException {
        BufferedWriter writer =
            new BufferedWriter( new OutputStreamWriter( out, CHARSET ) );
        try {
            for ( int il = 0; il < lines.length; il++ ) {
                writer.write( lines[ il ] );
                writer.newLine();
            }
        }
        finally {
            writer.close();
        }
    }

    /**
     * Examines a zip file (or jar file) and extracts the absolute path
     * name for each entry which appears to be a TTF file.
     *
     * @param  zip  zip file
     * @return  array of TTF paths
     */
    private static String[] getFontPaths( ZipFile zip ) {
        List<String> pathList = new ArrayList<String>();
        for ( Enumeration<? extends ZipEntry> zen = zip.entries();
              zen.hasMoreElements(); ) {
            ZipEntry entry = zen.nextElement();
            String path = "/" + entry.getName();
            if ( FONTPATH_REGEX.matcher( path ).matches() ) {
                pathList.add( path );
            }
        }
        return pathList.toArray( new String[ 0 ] );
    }

    /**
     * When given the names of one or more zip/jar files as arguments,
     * this writes to standard output the absolute paths of any entries
     * that appear to be a TTF file.  The output of this is suitable
     * for use with the {@link #createMapperFromResourceList} method.
     *
     * @param   args  list of zip file names
     */
    public static void main( String[] args ) throws IOException {
        String usage = "Usage: " + ExternalFontMapper.class.getName()
                                 + " <font-zipfile> ...";
        if ( args.length == 0 ) {
            System.err.println( usage );
            System.exit( 1 );
            return;
        }
        if ( args[ 0 ].startsWith( "-h" ) ) {
            System.err.println( usage );
            return;
        }
        for ( int iz = 0; iz < args.length; iz++ ) {
            ZipFile zip;
            try {
                zip = new ZipFile( args[ iz ] );
            }
            catch ( IOException e ) {
                e.printStackTrace();
                System.err.println( "\n" + usage );
                System.exit( 1 );
                return;
            }
            String[] fontPaths = getFontPaths( zip );
            for ( int ip = 0; ip < fontPaths.length; ip++ ) {
                System.out.println( fontPaths[ ip ] );
            }
        }
    }
}
