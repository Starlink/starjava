package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.filter.CalculatorTable;
import uk.ac.starlink.ttools.filter.JELColumnTable;

/**
 * Samples data from a HEALPix pixel file.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2011
 */
public class PixSample extends MapperTask {

    /**
     * Constructor.
     */
    public PixSample() {
        super( "Samples from a HEALPix pixel data file", new ChoiceMode(),
               true, new PixSampleMapper(), new PixSampleTablesInput() );
    }

    /**
     * TableMapper implementation for use with PixSample.
     * It maps (input,pixdata) tables to the result table.
     */
    private static class PixSampleMapper implements TableMapper {
        private final ChoiceParameter<PixSampler.StatMode> modeParam_;
        private final BooleanParameter equ2galParam_;
        private final Parameter lonParam_;
        private final Parameter latParam_;
        private final Parameter radiusParam_;
        private final ChoiceParameter<HealpixScheme> schemeParam_;

        /**
         * Constructor.
         */
        PixSampleMapper() {

            /* Need to set up some parameters with names up front so that
             * the descriptions can refer to each other. */
            PixSampler.StatMode[] statModes = new PixSampler.StatMode[] {
                PixSampler.POINT_MODE,
                PixSampler.MEAN_MODE,
            };
            modeParam_ = new ChoiceParameter<PixSampler.StatMode>( "stat",
                                                                   statModes ) {
                public String stringifyOption( PixSampler.StatMode mode ) {
                    return mode.toString().toLowerCase();
                }
            };
            radiusParam_ = new Parameter( "radius" );
            equ2galParam_ = new BooleanParameter( "equ2gal" );

            modeParam_.setPrompt( "Statistical quantity to gather"
                                + " from pixels" );
            modeParam_.setDescription( new String[] {
                "<p>Determines how the pixel values will be sampled",
                "to generate an output value.",
                "The options are:",
                "<ul>",
                "<li><code>"
                  + modeParam_.stringifyOption( PixSampler.POINT_MODE )
                  + "</code>: ",
                "Uses the value at the pixel covering the supplied position.",
                "In this case the <code>" + radiusParam_.getName() + "</code>",
                "parameter is not used.</li>",
                "<li><code>"
                  + modeParam_.stringifyOption( PixSampler.MEAN_MODE )
                  + "</code>: ",
                "Averages the values over all the pixels within a radius",
                "given by the <code>" + radiusParam_.getName() + "</code>",
                "parameter.",
                "This averaging is somewhat approximate;",
                "all pixels which are mostly within the radius",
                "are averaged with equal weights.</li>",
                "</ul>",
                "</p>",
            } );
            modeParam_.setDefaultOption( PixSampler.POINT_MODE );

            radiusParam_.setPrompt( "Radius for statistical accumulation"
                                  + " in degrees" );
            radiusParam_.setUsage( "<expr>" );
            radiusParam_.setDescription( new String[] {
                "<p>Determines the radius in degrees over which pixels will be",
                "sampled to generate the output statistic",
                "in accordance with the value of the",
                "<code>" + modeParam_.getName() + "</code> parameter.",
                "This will typically be a constant value,",
                "but it may be an algebraic expression based on",
                "columns from the input table.",
                "</p>",
                "<p>Not used if <code>" + modeParam_.getName() + "="
                    + modeParam_.stringifyOption( PixSampler.POINT_MODE )
                    + "</code>.",
                "</p>",
            } );

            lonParam_ = new Parameter( "lon" );
            lonParam_.setPrompt( "Input table longitude in degrees" );
            lonParam_.setUsage( "<expr>" );
            lonParam_.setDescription( new String[] {
                "<p>Expression which evaluates to the longitude coordinate",
                "in degrees in the input table at which",
                "positions are to be sampled from the pixel data table.",
                "This will usually be the name or ID of a column",
                "in the input table, or an expression involving one.",
                "Note that this coordinate must match the coordinate",
                "system used by the pixel data table",
                "(though see also the <code>" + equ2galParam_.getName()
                                              + "</code> parameter).",
 
                "</p>",
            } );

            latParam_ = new Parameter( "lat" );
            latParam_.setPrompt( "Input table latitude in degrees" );
            latParam_.setUsage( "<expr>" );
            latParam_.setDescription( new String[] {
                "<p>Expression which evaluates to the latitude coordinate",
                "in degrees in the input table at which",
                "positions are to be sampled from the pixel data table.",
                "This will usually be the name or ID of a column",
                "in the input table, or an expression involving one.",
                "Note that this coordinate must match the coordinate",
                "system used by the pixel data table",
                "(though see also the <code>" + equ2galParam_.getName()
                                              + "</code> parameter).",
 
                "</p>",
            } );

            schemeParam_ =
                new ChoiceParameter<HealpixScheme>( "pixorder",
                                                    HealpixScheme.values() );
            schemeParam_.setPrompt( "HEALPix pixel ordering scheme" );
            schemeParam_.setDescription( new String[] {
                "<p>Selects the pixel ordering scheme used by the",
                "pixel data file.",
                "There are two different ways of ordering pixels in a",
                "HEALPix file, \"ring\" and \"nested\", and the sampler",
                "needs to know which one is in use.",
                "If you know which is in use, choose the appropriate value",
                "for this parameter;",
                "if <code>" + HealpixScheme.AUTO + "</code> is used",
                "it will attempt to work it out from headers in the file",
                "(the ORDERING header).",
                "If no reliable ordering scheme can be determined,",
                "the command will fail with an error.",
                "</p>",
            } );
            schemeParam_.setDefaultOption( HealpixScheme.AUTO );

            equ2galParam_.setPrompt( "Convert " + lonParam_.getName() + "/"
                                   + latParam_.getName()
                                   + " from RA/Dec to Galactic?" );
            equ2galParam_.setDescription( new String[] {
                "<p>Under normal circumstances, the",
                "<code>" + lonParam_.getName() + "</code> and",
                "<code>" + latParam_.getName() + "</code> parameters",
                "must be specified in the same coordinate system",
                "as that used by the pixel data file.",
                "As a convenience, this flag can be set true for the case",
                "in which the pixel data file uses galactic coordinates",
                "and the <code>" + lonParam_.getName()
                    + "</code>/<code>" + latParam_.getName() + "</code>",
                "parameters are equatorial (RA/Dec).",
                "For more general coordinate transformations you can use",
                "the <ref id='addskycoords'><code>addskycoords</code></ref>",
                "filter.",
                "</p>",
            } );
            equ2galParam_.setDefault( Boolean.FALSE.toString() );
        }

        public Parameter[] getParameters() {
            return new Parameter[] {
                schemeParam_,
                modeParam_,
                lonParam_,
                latParam_,
                radiusParam_,
                equ2galParam_,
            };
        }

        public TableMapping createMapping( Environment env, int nin )
                throws TaskException {
            if ( nin != 2 ) {
                throw new TaskException( "Wrong number of tables" );
            }
            final PixSampler.StatMode statMode = modeParam_.objectValue( env );
            boolean equ2gal = equ2galParam_.booleanValue( env );
            final CoordReader coordReader =
                equ2gal ? createCoordReader( SkySystem.FK5, SkySystem.GALACTIC )
                        : createCoordReader( null, null );
            final String lonExpr = lonParam_.stringValue( env );
            final String latExpr = latParam_.stringValue( env );
            final String radiusExpr = statMode.isPoint()
                                    ? "0"
                                    : radiusParam_.stringValue( env );
            final HealpixScheme scheme = schemeParam_.objectValue( env );
            final JoinFixAction pixJoinFixAction =
                JoinFixAction.makeRenameDuplicatesAction( "_pix" );
            return new TableMapping() {
                public StarTable mapTables( InputTableSpec[] ins )
                        throws TaskException, IOException {
                    StarTable baseTable = ins[ 0 ].getWrappedTable();
                    StarTable pixTable = ins[ 1 ].getWrappedTable();
                    int nside = PixSampler.inferNside( pixTable );
                    final boolean isNested;
                    if ( scheme == HealpixScheme.RING ) {
                        isNested = false;
                    }
                    else if ( scheme == HealpixScheme.NESTED ) {
                        isNested = true;
                    }
                    else {
                        assert scheme == HealpixScheme.AUTO;
                        isNested = PixSampler.inferNested( pixTable );
                    }
                    PixSampler pixSampler =
                        new PixSampler( pixTable, nside, isNested );
                    StarTable sampleTable =
                        createSampleTable( baseTable, pixSampler, statMode,
                                           coordReader, lonExpr, latExpr,
                                           radiusExpr );
                    JoinFixAction[] fixActs = new JoinFixAction[] {
                        JoinFixAction.NO_ACTION,
                        pixJoinFixAction,
                    };
                    return new JoinStarTable( new StarTable[] { baseTable,
                                                                sampleTable },
                                              fixActs );
                }
            };
        }
    }

    /**
     * TablesInput implementation for use with PixSample.
     * It supplies exactly two tables: the input table and a pixdata table.
     */
    private static class PixSampleTablesInput implements TablesInput {

        private final InputTableParameter inTableParam_;
        private final InputTableParameter pixTableParam_;
        private final FilterParameter inFilterParam_;
        private final FilterParameter pixFilterParam_;

        /**
         * Constructor.
         */
        PixSampleTablesInput() {
            inTableParam_ = new InputTableParameter( "in" );
            inFilterParam_ = new FilterParameter( "icmd" );
            inFilterParam_.setDescription( new String[] {
                "<p>Commands to operate on the input table,",
                "before any other processing takes place.",
                "</p>",
                inFilterParam_.getDescription(),
            } );

            pixTableParam_ = new InputTableParameter( "pixdata" );
            pixTableParam_.setUsage( "<pix-table>" );
            pixTableParam_.setPrompt( "HEALPix table location" );
            pixTableParam_.setDescription( new String[] {
                "<p>The location of the table containing the pixel data.",
                "The data must be in the form of a HEALPix table,",
                "with one pixel per row in HEALPix order.",
                "These files are typically, but not necessarily,",
                "FITS tables.",
                "A filename or URL may be used, but a local file will be",
                "more efficient.",
                "</p>",
            } );

            InputFormatParameter pixFmtParam =
                pixTableParam_.getFormatParameter();
            pixFmtParam.setPrompt( "File format for pixel data table" );
            pixFmtParam.setDescription( new String[] {
                "<p>File format for the HEALPix pixel data table.",
                "This is usually, but not necessarily, FITS.",
                "</p>",
            } );
            pixFmtParam.setDefault( "fits" );

            pixFilterParam_ = new FilterParameter( "pcmd" );
            pixFilterParam_.setPrompt( "Processing command(s)"
                                     + " for pixel data table" );
            pixFilterParam_.setDescription( new String[] {
                "<p>Commands to operate on the pixel data table,",
                "before any other processing takes place.",
                "</p>",
                pixFilterParam_.getDescription(),
            } );
        }

        public Parameter[] getParameters() {
            return new Parameter[] {
                inTableParam_,
                inTableParam_.getFormatParameter(),
                inFilterParam_,
                pixTableParam_,
                pixTableParam_.getFormatParameter(),
                pixFilterParam_,
            };
        }

        public InputTableSpec[] getInputSpecs( Environment env )
                throws TaskException {
            final StarTable inTable = inTableParam_.tableValue( env );
            InputTableSpec inSpec =
                    new InputTableSpec( inTableParam_.stringValue( env ),
                                        inFilterParam_.stepsValue( env ) ) {
                public StarTable getInputTable() {
                    return inTable;
                }
            };
            final StarTable pixTable = pixTableParam_.tableValue( env );
            InputTableSpec pixSpec =
                    new InputTableSpec( pixTableParam_.stringValue( env ),
                                        pixFilterParam_.stepsValue( env ) ) {
                public StarTable getInputTable() throws TaskException {
                    try {
                        return Tables.randomTable( pixTable );
                    }
                    catch ( IOException e ) {
                        throw new TaskException( "Read error for "
                                               + getLocation(), e );
                    }
                }
            };
            return new InputTableSpec[] { inSpec, pixSpec };
        }
    }

    /**
     * Creates a table containing pixel samples corresponding to the rows
     * of a base table in accordance with supplied parameters.
     *
     * @param   base  base table
     * @param   pixSampler   characterises pixel sampling
     * @param   coordReader  turns input coordinate pairs into
     *                       lon/lat coords in the HEALPix coordinate system
     * @param   lonExpr  JEL expression for first input coordinate
     * @param   latExpr  JEL expression for second input coordinate
     * @param   radExpr  JEL expression for averaging radius
     * @return   table containing sampled columns
     */
    public static StarTable
            createSampleTable( StarTable base, final PixSampler pixSampler,
                               final PixSampler.StatMode statMode,
                               final CoordReader coordReader,
                               String lonExpr, String latExpr, String radExpr )
            throws IOException {

        /* Put together a table containing just the input lon, lat, radius. */
        StarTable calcInputTable =
            new JELColumnTable( base,
                                new String[] { lonExpr, latExpr, radExpr },
                                null );

        /* Feed it to a calculator table that turns those inputs into the
         * required pixel samples. */
        return new CalculatorTable( calcInputTable,
                                    pixSampler.getValueInfos( statMode ) ) {
            protected Object[] calculate( Object[] inRow ) throws IOException {
                double[] coords =
                    coordReader.getCoords( getDouble( inRow[ 0 ] ),
                                           getDouble( inRow[ 1 ] ) );
                double lon = coords[ 0 ];
                double lat = coords[ 1 ];
                double radius = getDouble( inRow[ 2 ] );
                return pixSampler.sampleValues( lon, lat, radius, statMode );
            }
        };
    }

    /**
     * Returns a coordinate reader which converts between a given input
     * and output coordinate system.
     * If no conversion is required, use <code>null</code> for in/out systems.
     *
     * @param   inSys  input sky coordinate system
     * @param  outSys  output sky coordinate system
     * @return  coordinate reader that converts
     */
    public static CoordReader createCoordReader( final SkySystem inSys,
                                                 final SkySystem outSys ) {
        if ( inSys == null && outSys == null ) {
            return new CoordReader() {
                public double[] getCoords( double lonDeg, double latDeg ) {
                    return new double[] { lonDeg, latDeg };
                }
            };
        }
        else if ( inSys != null && outSys != null ) {
           final double epoch = 2000.0;
            return new CoordReader() {
                public double[] getCoords( double lonDegIn, double latDegIn ) {
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
     * Possible values for HEALPix ordering parameter.
     */
    private enum HealpixScheme {
        NESTED( "nested" ), RING( "ring" ), AUTO( "(auto)" );
        private final String name_;
        HealpixScheme( String name ) {
            name_ = name;
        }
        public String toString() {
            return name_;
        }
    }

    /**
     * Interface to turn input coordinate values into coordinate values
     * suitable for pixel sampling.
     */
    public interface CoordReader {

        /**
         * Gets sampling lon/lat coordinates given some input coordinates.
         *
         * @param   lonDeg  first input coordinate
         * @param   latDeg  second input coordinate
         * @return   (lon,lat) array of coordinates giving sampling position
         */
        abstract double[] getCoords( double lonDeg, double latDeg );
    }
}
