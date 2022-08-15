package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.task.PixSample;
import uk.ac.starlink.ttools.task.PixSampler;

/**
 * Filter for sampling pixel data from a HEALPix all-sky table file.
 *
 * @author   Mark Taylor
 * @since    5 Dec 2011
 */
public class PixSampleFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public PixSampleFilter() {
        super( "addpixsample",
               "[-radius <expr-rad>] [-systems <in-sys> <pix-sys>]\n"
             + "<expr-lon> <expr-lat> <healpix-file>" );
    }

    public String[] getDescriptionLines() {
        String addSkyCoordsName = new AddSkyCoordsFilter().getName();
        return new String[] {
            "<p>Samples pixel data from an all-sky image file", 
            "in HEALPix format.",
            "The <code>&lt;healpix-file&gt;</code> argument must be",
            "the filename of a table containing HEALPix pixel data.",
            "The URL of such a file can be used instead, but local files",
            "are likely to be more efficient.",
            "</p>",
            "<p>The <code>&lt;expr-lon&gt;</code>",
            "and <code>&lt;expr-lat&gt;</code> arguments",
            "give expressions for the longitude and latitude in degrees",
            "for each row of the input table;",
            "this is usually just the column names.",
            "The long/lat must usually be in the same coordinate system",
            "as that used for the HEALPix data, so if the one is in",
            "galactic coordinates the other must be as well.",
            "If this is not the case, use the <code>-systems</code> flag",
            "to give the input long/lat and healpix data coordinate system",
            "names respectively.",
            "The available coordinate system names are:",
            SkySystem.getSystemUsage(),
            "</p>",
            "<p>The <code>&lt;expr-rad&gt;</code>, if present,",
            "is a constant or expression",
            "giving the radius in degrees over which",
            "pixels will be averaged to obtain the result values.",
            "Note that this averaging is somewhat approximate;",
            "pixels partly covered by the specified disc are weighted",
            "the same as those fully covered.",
            "If no radius is specified, the value of the pixel covering",
            "the central position will be used.",
            "</p>",
            "<p>The <code>&lt;healpix-file&gt;</code> file is a table",
            "with one row per HEALPix pixel and one or more columns",
            "representing pixel data.",
            "A new column will be added to the output table",
            "corresponding to each of these pixel columns.",
            "This type of data is available in FITS tables for a number of",
            "all-sky data sets, particularly from the",
            "<webref url='https://lambda.gsfc.nasa.gov/' "
                  + "plaintextref='yes'>LAMBDA</webref> archive;",
            "see for instance the page on",
            "<webref url='https://lambda.gsfc.nasa.gov/product/"
                       + "foreground/f_products.cfm'>"
                       + "foreground products</webref>",
            "(including dust emission, reddening etc)",
            "or",
            "<webref url='https://lambda.gsfc.nasa.gov/product/"
                      + "map/dr4/ilc_map_get.cfm'>WMAP 7 year data</webref>.",
            "If the filename given does not appear to point to a file",
            "of the appropriate format, an error will result.",
            "Note the LAMBDA files mostly (all?) use galactic coordinates,",
            "so coordinate conversion using the <code>-systems</code> flag",
            "may be appropriate, see above.",
            "</p>",
            explainSyntax( new String[] { "expr-lon", "expr-lat",
                                          "expr-rad" } ),
            "<p>This filter is somewhat experimental, and its usage may be",
            "changed or replaced in a future version.",
            "</p>",
            "<p><strong>Note: you may prefer to use the",
            "<code><ref id='pixsample'>pixsample</ref></code>",
            "command instead.",
            "</strong>",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {

        /* Parse arguments. */
        String sRadius = null;
        String sLon = null;
        String sLat = null;
        String sPixfile = null;
        String[] sysStrings = null;
        List<String> unknownFlags = new ArrayList<String>();
        while ( argIt.hasNext() &&
                ( sLon == null || sLat == null || sPixfile == null ) ) {
            String arg = argIt.next();
            if ( arg.equals( "-radius" ) && argIt.hasNext() ) {
                argIt.remove();
                sRadius = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-systems" ) && argIt.hasNext() ) {
                argIt.remove();
                List<String> syslist = new ArrayList<String>();
                syslist.add( argIt.next() );
                argIt.remove();
                if ( argIt.hasNext() ) {
                    syslist.add( argIt.next() );
                    argIt.remove();
                }
                sysStrings = syslist.toArray( new String[ 0 ] );
            }
            else if ( sLon == null ) {
                argIt.remove();
                sLon = arg;
            }
            else if ( sLat == null ) {
                argIt.remove();
                sLat = arg;
            }
            else if ( sPixfile == null ) {
                argIt.remove();
                sPixfile = arg;
            }
            else if ( arg.startsWith( "-" ) ) {
                unknownFlags.add( arg );
            }
        }
        if ( sLon == null || sLat == null || sPixfile == null ) {
            String msg = unknownFlags.size() > 0
                       ? "Unknown flag? \"" + unknownFlags.get( 0 ) + "\""
                       : "Not enough arguments supplied";
            throw new ArgException( msg );
        }

        /* Prepare inputs for the table calculation. */
        final PixSampler.StatMode statMode =
              ( sRadius == null || "0".equals( sRadius ) )
            ? PixSampler.POINT_MODE
            : PixSampler.MEAN_MODE;
        final PixSample.CoordReader coordReader =
              sysStrings == null
            ? PixSample.createCoordReader( null, null )
            : PixSample.createCoordReader( getSystem( sysStrings[ 0 ] ),
                                           getSystem( sysStrings[ 1 ] ) );
        final String lonExpr = sLon;
        final String latExpr = sLat;
        final String radExpr = sRadius == null ? "0" : sRadius;
        final PixSampler pixSampler;
        try {
            StarTable pixdataTable = 
                Tables.randomTable( new StarTableFactory()
                                   .makeStarTable( sPixfile ) );
            pixSampler = PixSampler.createPixSampler( pixdataTable );
        }
        catch ( IOException e ) {
            throw new ArgException( "Error using pixel data file " + sPixfile,
                                    e );
        }

        /* Return a new processing step that does the work. */
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                ColumnSupplement sampleSup =
                    PixSample
                   .createSampleSupplement( base, pixSampler,
                                            statMode, coordReader,
                                            lonExpr, latExpr, radExpr );
                return new AddColumnsTable( base, sampleSup );
            }
        };
    }

    /**
     * Turns a coordinate system string into a SkySystem.
     *
     * @param  sysString  coordinate system string representation
     * @return  coordinate system object
     */
    private static SkySystem getSystem( String sysString ) throws ArgException {
        try {
            return SkySystem.getSystemFor( sysString );
        }
        catch ( IllegalArgumentException e ) {
            throw new ArgException( "Unknown coordinate system"
                                  + "\"" + sysString + "\"", e );
        }
    }
}
