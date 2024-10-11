package uk.ac.starlink.ttools.task;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.ttools.filter.AddColumnsTable;
import uk.ac.starlink.ttools.filter.CalculatorColumnSupplement;
import uk.ac.starlink.ttools.filter.ColumnSupplement;
import uk.ac.starlink.ttools.filter.JELColumnSupplement;

/**
 * Samples data from a HEALPix pixel file.
 *
 * @author   Mark Taylor
 * @since    6 Dec 2011
 */
public class PixSample extends MapperTask {

    private static final String pixdataName_ = "pixdata";

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
        private final StringParameter lonParam_;
        private final StringParameter latParam_;
        private final ChoiceParameter<SkySystem> insysParam_;
        private final ChoiceParameter<SkySystem> outsysParam_;
        private final StringParameter radiusParam_;
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
            radiusParam_ = new StringParameter( "radius" );
            insysParam_ =
                new ChoiceParameter<SkySystem>( "insys",
                                                SkySystem.getKnownSystems() );
            outsysParam_ =
                new ChoiceParameter<SkySystem>( "pixsys",
                                                SkySystem.getKnownSystems() );

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

            String postxt = new StringBuffer()
                .append( "This will usually be the name or ID of a column\n" )
                .append( "in the input table,\n" )
                .append( "or an expression involving one.\n" )
                .append( "If this coordinate does not match the coordinate\n" )
                .append( "system used by the pixel data table,\n" )
                .append( "both coordinate systems must be set using the\n" )
                .append( "<code>" )
                .append( insysParam_.getName() )
                .append( "</code>" )
                .append( " and " )
                .append( "<code>" )
                .append( outsysParam_.getName() )
                .append( "</code>" )
                .append( " parameters.\n" )
                .toString();

            lonParam_ = new StringParameter( "lon" );
            lonParam_.setPrompt( "Input table longitude in degrees" );
            lonParam_.setUsage( "<expr>" );
            lonParam_.setDescription( new String[] {
                "<p>Expression which evaluates to the longitude coordinate",
                "in degrees in the input table at which",
                "positions are to be sampled from the pixel data table.",
                postxt,
                "</p>",
            } );

            latParam_ = new StringParameter( "lat" );
            latParam_.setPrompt( "Input table latitude in degrees" );
            latParam_.setUsage( "<expr>" );
            latParam_.setDescription( new String[] {
                "<p>Expression which evaluates to the latitude coordinate",
                "in degrees in the input table at which",
                "positions are to be sampled from the pixel data table.",
                postxt,
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

            String systxt = new StringBuffer()
                .append( "If the sample positions are given\n" )
                .append( "in the same coordinate system as that given by\n" )
                .append( "the pixel data table, both the " )
                .append( "<code>" )
                .append( insysParam_.getName() )
                .append( "</code>" )
                .append( " and " )
                .append( "<code>" )
                .append( outsysParam_.getName() )
                .append( "</code>" )
                .append( " parameters may be set <code>null</code>.\n" )
                .append( "</p>\n" )
                .append( "<p>The available coordinate systems are:\n" )
                .append( SkySystem.getSystemUsage() )
                .toString();

            insysParam_.setPrompt( "Sky coordinate system"
                                 + " for sample positions" );
            insysParam_.setDescription( new String[] {
                "<p>Specifies the sky coordinate system in which",
                "sample positions are provided by the",
                "<code>" + lonParam_.getName() + "</code>" + "/" +
                "<code>" + latParam_.getName() + "</code>",
                "parameters.",
                systxt,
                "</p>",
            } );
            insysParam_.setNullPermitted( true );

            outsysParam_.setPrompt( "Sky coordinate system"
                                  + " for pixel positions" );
            outsysParam_.setDescription( new String[] {
                "<p>Specifies the sky coordinate system",
                "used for the HEALPix data in the " + pixdataName_ + " file.",
                systxt,
                "</p>",
            } );
            outsysParam_.setNullPermitted( true );
        }

        public Parameter<?>[] getParameters() {
            return new Parameter<?>[] {
                schemeParam_,
                modeParam_,
                lonParam_,
                latParam_,
                insysParam_,
                outsysParam_,
                radiusParam_,
            };
        }

        public TableMapping createMapping( Environment env, int nin )
                throws TaskException {
            if ( nin != 2 ) {
                throw new TaskException( "Wrong number of tables" );
            }
            final PixSampler.StatMode statMode = modeParam_.objectValue( env );
            final CoordReader coordReader =
                createCoordReader( insysParam_, outsysParam_, env );
            final String lonExpr = lonParam_.stringValue( env );
            final String latExpr = latParam_.stringValue( env );
            final String radiusExpr = statMode.isPoint()
                                    ? "0"
                                    : radiusParam_.stringValue( env );
            final HealpixScheme scheme = schemeParam_.objectValue( env );
            return new TableMapping() {
                public StarTable mapTables( InputTableSpec[] ins )
                        throws TaskException, IOException {
                    StarTable baseTable = ins[ 0 ].getWrappedTable();
                    StarTable pixTable = ins[ 1 ].getWrappedTable();
                    int order = PixSampler.inferOrder( pixTable );
                    final boolean isNested;
                    if ( scheme == HealpixScheme.RING ) {
                        isNested = false;
                    }
                    else if ( scheme == HealpixScheme.NESTED ) {
                        isNested = true;
                    }
                    else {
                        assert scheme == HealpixScheme.AUTO;
                        Boolean guessNest = PixSampler.inferNested( pixTable );
                        if ( guessNest == null ) {
                            String msg = new StringBuffer()
                                .append( "Can't determine ordering scheme" )
                                .append( "; please supply " )
                                .append( schemeParam_.getName() )
                                .append( " parameter" )
                                .toString();
                            throw new ExecutionException( msg );
                        }
                        else {
                            isNested = guessNest.booleanValue();
                        }
                    }
                    PixSampler pixSampler =
                        new PixSampler( pixTable, isNested, order );
                    ColumnSupplement sampleSup =
                        createSampleSupplement( baseTable, pixSampler, statMode,
                                                coordReader, lonExpr, latExpr,
                                                radiusExpr );
                    return new AddColumnsTable( baseTable, sampleSup );
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
            inFilterParam_.setTableDescription( "the input table",
                                                inTableParam_, Boolean.TRUE );

            pixTableParam_ = new InputTableParameter( pixdataName_ );
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
                "<p>Some HEALPix format FITS tables seem to have rows",
                "which contain 1024-element arrays of pixels",
                "instead of single pixel values.",
                "This (rather perverse?) format is not currently supported",
                "here, but if there is demand support could be added.",
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

            pixFilterParam_ = new FilterParameter( "pcmd" );
            pixFilterParam_.setTableDescription( "pixel data table", 
                                                 pixTableParam_, Boolean.TRUE );
        }

        public Parameter<?>[] getParameters() {
            return new Parameter<?>[] {
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

        public InputTableParameter getInputTableParameter( int i ) {
            return (new InputTableParameter[] { inTableParam_,
                                                pixTableParam_ })[ i ];
        }

        public FilterParameter getFilterParameter( int i ) {
            return (new FilterParameter[] { inFilterParam_,
                                            pixFilterParam_ })[ i ];
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
    public static ColumnSupplement
            createSampleSupplement( StarTable base, final PixSampler pixSampler,
                                    final PixSampler.StatMode statMode,
                                    final CoordReader coordReader,
                                    String lonExpr, String latExpr,
                                    String radExpr )
            throws IOException {

        /* Put together a table containing just the input lon, lat, radius. */
        ColumnSupplement calcInputSup =
            new JELColumnSupplement( base,
                                     new String[] { lonExpr, latExpr, radExpr },
                                     null );

        /* Feed it to a calculator table that turns those inputs into the
         * required pixel samples. */
        return new CalculatorColumnSupplement( calcInputSup,
                                               pixSampler
                                              .getValueInfos( statMode ) ) {
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
     * Returns a coordinate reader which converts between coordinate
     * systems specified by two given parameters.
     * Null values are permitted for both, but not just one, of the parameters.
     *
     * @param  insysParam   input coordinate system parameter
     * @param  outsysParam  output coordinate system parameter
     * @return   coordinate reader that converts
     */
    private static CoordReader
                   createCoordReader( ChoiceParameter<SkySystem> insysParam,
                                      ChoiceParameter<SkySystem> outsysParam,
                                      Environment env ) throws TaskException {
        SkySystem insys = insysParam.objectValue( env );
        outsysParam.setNullPermitted( insys == null );
        SkySystem outsys = outsysParam.objectValue( env );
        if ( ( insys == null ) != ( outsys == null ) ) {
            String msg = new StringBuffer()
                .append( "If one of " )
                .append( insysParam.getName() )
                .append( " and " )
                .append( outsysParam.getName() )
                .append( " is null, they must both be" )
                .toString();
            throw new ParameterValueException( outsysParam, msg );
        }
        else {
            return createCoordReader( insys, outsys );
        }
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
