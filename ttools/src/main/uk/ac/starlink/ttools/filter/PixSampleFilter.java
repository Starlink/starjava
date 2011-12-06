package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.convert.SkySystem;

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
               "[-radius <expr-rad>] [-equ2gal]"
             + " <expr-lon> <expr-lat>"
             + " <healpix-file>" );
    }

    public String[] getDescriptionLines() {
        String addSkyCoordsName = new AddSkyCoordsFilter().getName();
        return new String[] {
            "<p>Samples pixel data from an all-sky image file", 
            "in HEALPix format.",
            "The <code>&lt;healpix-file&gt;</code> argument must be",
            "the filename of a table containing HEALPix pixel data.",
            "The URL of such a file can be used as well, but local files",
            "are likely to be more efficient.",
            "</p>",
            "<p>The <code>&lt;expr-lon&gt;</code>",
            "and <code>&lt;expr-lat&gt;</code> arguments",
            "give expressions for the longitude and latitude in degrees",
            "for each row of the input table;",
            "this is usually just the column names.",
            "The long/lat must in general be in the same coordinate system",
            "as that used for the HEALPix data, so if the one is in",
            "galactic coordinates the other must be as well.",
            "Since HEALPix pixel data is often in galactic coordinates",
            "and your data may not be, a special convenience flag",
            "<code>-equ2gal</code> is available.",
            "If this is given and the HEALPix data is in galactic coordinates,",
            "then you can use Right Ascension and Declination columns for",
            "the <code>&lt;expr-lon&gt;</code>",
            "and <code>&lt;expr-lat&gt;</code> values respectively.",
            "An alternative, and more general, way to perform the",
            "necessary transformation is by using the",
            "<ref id='" + addSkyCoordsName + "'>"
                + "<code>" + addSkyCoordsName + "</code></ref> filter.",
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
            "<webref url='http://lambda.gsfc.nasa.gov/' "
                  + "plaintextref='yes'>LAMBDA</webref> archive;",
            "see for instance the page on",
            "<webref url='http://lambda.gsfc.nasa.gov/product/"
                       + "foreground/f_products.cfm'>"
                       + "foreground products</webref>",
            "(including dust emission, reddening etc)",
            "or",
            "<webref url='http://lambda.gsfc.nasa.gov/product/"
                      + "map/dr4/ilc_map_get.cfm'>WMAP 7 year data</webref>.",
            "If the filename given does not appear to point to a file",
            "of the appropriate format, an error will result.",
            "Note the LAMBDA files mostly (all?) use galactic coordinates,",
            "so use of the <code>-equ2gal</code> flag may be appropriate,",
            "see above.",
            "</p>",
            explainSyntax( new String[] { "expr-lon", "expr-lat",
                                          "expr-rad" } ),
            "<p>This filter is somewhat experimental, and its usage may be",
            "changed or replaced in a future version.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {

        /* Parse arguments. */
        String sRadius = null;
        String sLon = null;
        String sLat = null;
        String sPixfile = null;
        boolean equToGal = false;
        List<String> unknownFlags = new ArrayList<String>();
        while ( argIt.hasNext() &&
                ( sLon == null || sLat == null || sPixfile == null ) ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-radius" ) && argIt.hasNext() ) {
                argIt.remove();
                sRadius = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-equ2gal" ) ) {
                argIt.remove();
                equToGal = true;
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
        final CoordReader coordReader =
            equToGal ? createCoordReader( SkySystem.FK5, SkySystem.GALACTIC )
                     : createCoordReader( null, null );
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
                return createPixSampleTable( base, pixSampler, statMode,
                                             coordReader, lonExpr, latExpr,
                                             radExpr );
            }
        };
    }

    /**
     * Adds pixel sample columns to an input base table in accordance with
     * supplied parameters.
     *
     * @param   base  base table
     * @param   pixSample  characterises pixel sampling
     * @param   coordReader  turns input coordinate pairs into
     *                       lon/lat coords in the HEALPix coordinate system
     * @param   lonExpr  JEL expression for first input coordinate
     * @param   latExpr  JEL expression for second input coordinate
     * @param   radExpr  JEL expression for averaging radius
     * @return   table with sampled columns added
     */
    private static StarTable
            createPixSampleTable( StarTable base, final PixSampler pixSampler,
                                  final PixSampler.StatMode statMode,
                                  final CoordReader coordReader,
                                  String lonExpr, String latExpr,
                                  String radExpr ) throws IOException {

        /* Put together a table containing just the input lon, lat, radius. */
        StarTable calcInputTable =
            new JELColumnTable( base,
                                new String[] { lonExpr, latExpr, radExpr },
                                null );

        /* Feed it to a calculator table that turns those inputs into the
         * required pixel samples. */
        StarTable sampleTable =
                new CalculatorTable( calcInputTable,
                                     pixSampler.getValueInfos( statMode ) ) {
            protected Object[] calculate( Object[] inRow )
                    throws IOException {
                double[] coords =
                    coordReader.getCoords( getDouble( inRow[ 0 ] ),
                                           getDouble( inRow[ 1 ] ) );
                double lon = coords[ 0 ];
                double lat = coords[ 1 ];
                double radius = getDouble( inRow[ 2 ] );
                return pixSampler
                      .sampleValues( lon, lat, radius, statMode );
            }
        };

        /* Interpolate the calculated columns into the original input table
         * and return. */
        return new AddColumnsTable( base, sampleTable );
    }

    /** 
     * Returns a coordinate reader which converts between a given input
     * and output coordinate system.
     * If no conversion is required, use <code>null</code> for in/out systems.
     *
     * @param   inSys  input sky coordinate system
     * @param  outSsy  output sky coordinate system
     * @return  coordinate reader that converts
     */
    private static CoordReader createCoordReader( final SkySystem inSys,
                                                  final SkySystem outSys ) {
        if ( inSys == null && outSys == null ) {
            return new CoordReader() {
                double[] getCoords( double lonDeg, double latDeg ) {
                    return new double[] { lonDeg, latDeg };
                }
            };
        }
        else if ( inSys != null && outSys != null ) {
            final double epoch = 2000.0;
            return new CoordReader() {
                double[] getCoords( double lonDegIn, double latDegIn ) {
                    double lonRadIn= lonDegIn / 180. * Math.PI;
                    double latRadIn = latDegIn / 180. * Math.PI;
                    double[] fk5Rad = inSys.toFK5( lonRadIn, latRadIn, epoch );
                    double[] radOut = outSys.fromFK5( fk5Rad[ 0 ], fk5Rad[ 1 ],
                                                      epoch );
                    double lonRadOut = radOut[ 0 ];
                    double latRadOut = radOut[ 1 ];
                    double lonDegOut = lonRadOut * 180. / Math.PI;
                    double latDegOut = latRadOut * 180. / Math.PI;
                    return new double[] { lonDegOut, latDegOut };
                }
            };
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Interface to turn input coordinate values into coordinate values
     * suitable for pixel sampling.
     *
     * @param   lonDeg  first input coordinate
     * @param   latDeg  second input coordinate
     * @return   (lon,lat) array of coordinates giving sampling position
     */
    private static abstract class CoordReader {
        abstract double[] getCoords( double lonDeg, double latDeg );
    }
}
