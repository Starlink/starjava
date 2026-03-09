package uk.ac.starlink.ttools.mode;

import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.TimeDomain;
import uk.ac.starlink.table.TimeMapper;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Area;
import uk.ac.starlink.ttools.AreaDomain;
import uk.ac.starlink.ttools.AreaMapper;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.func.Coverage;
import uk.ac.starlink.ttools.jel.JELQuantity;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.ttools.jel.StarTableJELRowReader;
import uk.ac.starlink.ttools.moc.CdsMocBuilder;
import uk.ac.starlink.ttools.moc.MocBuilder;
import uk.ac.starlink.ttools.moc.MocImpl;
import uk.ac.starlink.ttools.moc.MocStreamFormat;
import uk.ac.starlink.util.Destination;
import uk.ac.starlink.util.IOSupplier;

/**
 * Turns a table into a multi-order coverage map by interpreting a
 * given quantity (column or expression) as an Area specification.
 *
 * @author   Mark Taylor
 * @since    30 Jan 2025
 */
public class MocShapeMode implements ProcessingMode {

    private final IntegerParameter orderParam_;
    private final StringParameter coordsParam_;
    private final ChoiceParameter<AreaMapper> shapeParam_;
    private final IntegerParameter torderParam_;
    private final StringParameter t0Param_;
    private final StringParameter t1Param_;
    private final ChoiceParameter<TimeIntervalType> tshapeParam_;
    private final ChoiceParameter<TimeMapper> tmapperParam_;
    private final ChoiceParameter<MocType> moctypeParam_;
    private final ChoiceParameter<MocStreamFormat> mocfmtParam_;
    private final ChoiceParameter<MocImpl> mocimplParam_;
    private final OutputStreamParameter outParam_;

    private static final int MAX_ORDER = SMoc.MAXORD_S;
    private static final int MAX_TORDER = TMoc.MAXORD_T;
    private static final int CDSMOC_BUFSIZ = CdsMocBuilder.CDSMOC_BUFSIZ;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    /**
     * Constructor.
     */
    public MocShapeMode() {

        moctypeParam_ =
                new ChoiceParameter<MocType>( "moctype", MocType.values() ) {
            @Override
            public MocType stringToChoice( String sval )
                    throws ParameterValueException {
                MocType type = MocType.fromString( sval );
                if ( type == null ) {
                    throw new ParameterValueException( this,
                                                       "Unknown value " + sval);
                }
                return type;
            }
        };
        moctypeParam_.setPrompt( "Type of MOC to create" );
        moctypeParam_.setDescription( new String[] {
            "<p>Identifies whether the output will describe coverage",
            "in Space, Time or both.",
            "This value will determine what other",
            "parameters need to be specified.",
            DocUtils.describedList( MocType.values(),
                                    m -> m.name_, m -> m.description_, false ),
            "</p>",
        } );
        moctypeParam_.setDefaultOption( MocType.SMOC );

        String timeOnlyNote = String.join( "\n", new String[] {
            "<p>This parameter is only used if there is a Time component",
            "of the output MOC",
            "(<code>" + moctypeParam_.getName() + "=</code>"
                      + "<code>tmoc</code> or <code>stmoc</code>).",
            "</p>",
        } );
        String spaceOnlyNote = String.join( "\n", new String[] {
            "<p>This parameter is only used if there is a Space component",
            "of the output MOC",
            "(<code>" + moctypeParam_.getName() + "=</code>"
                      + "<code>smoc</code> or <code>stmoc</code>).",
            "</p>",
        } );

        orderParam_ = new IntegerParameter( "order" );
        orderParam_.setPrompt( "Space MOC Healpix maximum order" );
        orderParam_.setUsage( "0.." + MAX_ORDER );
        orderParam_.setMinimum( 0 );
        orderParam_.setMaximum( MAX_ORDER );
        int orderDflt = 10;
        int dfltResArcmin =
            (int) Math.round( 3520 * Math.pow( 2, -orderDflt ) );
        assert dfltResArcmin > 0 && dfltResArcmin < 5000;
        orderParam_.setIntDefault( orderDflt );
        orderParam_.setDescription( new String[] {
            "<p>Maximum HEALPix order for the spatial MOC.",
            "This defines the maximum resolution of the output coverage map.",
            "The angular resolution corresponding to order <em>k</em>",
            "is approximately 180/sqrt(3.Pi)/2^<em>k</em> degrees",
            "(3520*2^<em>-k</em> arcmin).",
            "Permitted values are 0.." + MAX_ORDER + " inclusive.",
            "The default value is " + orderDflt + ", which corresponds to",
            "about " + dfltResArcmin + " arcmin.",
            "</p>",
            spaceOnlyNote,
        } );

        String coordsName = "coords";
        String shapeName = "shape";
 
        coordsParam_ = new StringParameter( coordsName );
        coordsParam_.setUsage( "<expr>" );
        coordsParam_.setPrompt( "Coordinate array column or expression" );
        coordsParam_.setDescription( new String[] {
            "<p>Name of the column or an array expression",
            "giving the coordinates of the shape in each row to add",
            "to the MOC.",
            "The type and semantics of this value",
            "(the type of shape represented)",
            "are defined by the <code>" + shapeName + "</code> parameter.",
            "</p>",
            "<p>Note the value of this parameter is a single expression",
            "not a list of expressions, so if you need to supply an",
            "array value from a list of scalar values you may need to use",
            "array construction functions",
            "from the <ref id='Arrays'>Arrays</ref> class,",
            "e.g. write",
            "\"<code>" + coordsParam_.getName() + "=array(ra,dec)</code>\"",
            "and not",
            "\"<code>" + coordsParam_.getName() + "=ra,dec</code>\".",
            "</p>",
            spaceOnlyNote,
        } );

        List<AreaMapper> areaMappers =
            new ArrayList<AreaMapper>( Arrays.asList( AreaDomain.INSTANCE
                                                     .getMappers() ) );
        areaMappers.remove( AreaDomain.TFCAT_MAPPER );
        shapeParam_ =
            new ChoiceParameter<AreaMapper>( shapeName, AreaMapper.class,
                                             areaMappers
                                            .toArray( new AreaMapper[ 0 ] ) );
        shapeParam_.setPrompt( "Coordinate shape type" );
        shapeParam_.setDescription( new String[] {
            "<p>Defines the interpretation of the",
            "<code>" + coordsName + "</code> parameter,",
            "i.e. the type of shape defined by the supplied coordinates.",
            "</p>",
            "<p>The options are:",
            DocUtils.describedList( areaMappers,
                                    AreaMapper::toString,
                                    AreaMapper::getSkySourceDescription, true ),
            "If a blank value is supplied (the default)",
            "an attempt will be made to guess the shape type given the",
            "supplied coordinate column; if no good guess can be made,",
            "an error will result.",
            "</p>",
            spaceOnlyNote,
        } );
        shapeParam_.setNullPermitted( true );

        torderParam_ = new IntegerParameter( "torder" );
        torderParam_.setPrompt( "Time MOC maximum order" );
        torderParam_.setUsage( "0.." + MAX_TORDER );
        torderParam_.setMinimum( 0 );
        torderParam_.setMaximum( MAX_TORDER );
        int torderDflt = 31; // ~1000 second - see MOC 2.0 sec 4.2
        int dfltResSec =
            (int) Math.round( ( 1 << ( MAX_TORDER - torderDflt ) ) / 1e6 );
        assert dfltResSec > 0;
        torderParam_.setIntDefault( torderDflt );
        torderParam_.setDescription( new String[] {
            "<p>Maximum order for the time MOC.",
            "This defines the maximum temporal resolution for the output map.",
            "The time resolution corresponding is",
            "2**(" + MAX_TORDER + "-<em>torder</em>) microseconds.",
            "Permitted values are 0.." + MAX_TORDER + " inclusive.",
            "The default value is " + torderDflt + ", which corresponds to",
            "about " + dfltResSec + "s.",
            "Note however that because of floating point precision issues",
            "the limit of precision near the current epoch is around 0.1sec,",
            "corresponding to a value of this parameter of about 43.",
            "</p>",
            timeOnlyNote,
        } );

        String tmapperParamName = "ttype";
        String tshapeParamName = "tshape";
        String t0ParamName = "t0";
        String t1ParamName = "t1";

        tshapeParam_ =
            new ChoiceParameter<TimeIntervalType>( tshapeParamName,
                                                   TimeIntervalType.class,
                                                   TimeIntervalType.getTypes());
        tshapeParam_.setPrompt( "Time interval type" );
        tshapeParam_.setDescription( new String[] {
            "<p>Defines how the <code>" + t0ParamName + "</code> and perhaps",
            "<code>" + t1ParamName + "</code> parameters are interpreted",
            "to define a time interval.",
            "</p>",
            "<p>The options are:",
            DocUtils.describedList( TimeIntervalType.getTypes(),
                                    TimeIntervalType::toString,
                                    t -> t.getDescription( t0ParamName,
                                                           t1ParamName ),
                                    true ),
            "</p>",
            timeOnlyNote,
        } );
        tshapeParam_.setDefaultOption( TimeIntervalType.POINT );

        t0Param_ = new StringParameter( t0ParamName );
        t0Param_.setPrompt( "Primary time coordinate" );
        t0Param_.setUsage( "<time-expr>" );
        t0Param_.setDescription( new String[] {
            "<p>Column or expression giving the primary time coordinate",
            "for the Time MOC element at this row.",
            "The exact meaning of this epoch,",
            "perhaps in combination with the <code>" + t1ParamName + "</code>",
            "parameter, is determined by the value of the",
            "<code>" + tshapeParamName + "</code> parameter.",
            "The specified epoch is interpreted according to the value",
            "of the <code>" + tmapperParamName + "</code> parameter.",
            "</p>",
            timeOnlyNote,
        } );

        t1Param_ = new StringParameter( t1ParamName );
        t1Param_.setPrompt( "Secondary time coordinate" );
        t1Param_.setUsage( "<expr>" );
        t1Param_.setDescription( new String[] {
            "<p>Column or expression giving the secondary time coordinate",
            "for the Time MOC element at this row.",
            "The meaning of this quantity, in combination with the",
            "<code>" + t0ParamName + "</code> parameter,",
            "is determined by the value of the",
            "<code>" + tshapeParamName + "</code> parameter;",
            "for some values of <code>" + tshapeParamName + "</code>",
            "it is not required.",
            "If this quantity represents an epoch",
            "(for instance the end of a time range rather than a duration)",
            "it is interpreted according to the value of the",
            "<code>" + tmapperParamName + "</code> parameter.",
            "</p>",
            timeOnlyNote,
        } );

        tmapperParam_ =
            new ChoiceParameter<TimeMapper>( tmapperParamName, TimeMapper.class,
                                             TimeMapper.getTimeMappers() );
        tmapperParam_.setPrompt( "Value type for time coordinates" );
        tmapperParam_.setDescription( new String[] {
            "<p>Selects the form in which epoch values are specified.",
            "This applies to the <code>" + t0ParamName + "</code>",
            "and possibly the <code>" + t1ParamName + "</code> parameters.",
            "</p>",
            "<p>The options are:",
            DocUtils.describedList( TimeMapper.getTimeMappers(),
                                    TimeMapper::getSourceName,
                                    TimeMapper::getSourceDescription, false ),
            "If left blank, a guess will be attempted depending on the value",
            "supplied for the <code>" + t0ParamName + "</code> parameter.",
            "</p>",
            timeOnlyNote,
        } );
        tmapperParam_.setNullPermitted( true );

        mocfmtParam_ =
            new ChoiceParameter<MocStreamFormat>( "mocfmt",
                                                  MocStreamFormat.FORMATS );
        mocfmtParam_.setPrompt( "Output format for MOC file" );
        mocfmtParam_.setDescription( new String[] {
            "<p>Determines the output format for the MOC file.",
            "</p>",
        } );
        mocfmtParam_.setDefaultOption( MocStreamFormat.ASCII );

        MocImpl[] mocImpls = {
            MocImpl.AUTO,
            MocImpl.CDS,
            MocImpl.CDS_BATCH,
            MocImpl.BITSET,
            MocImpl.LIST,
        };
        mocimplParam_ = new ChoiceParameter<MocImpl>( "mocimpl", mocImpls );
        mocimplParam_.setPrompt( "MOC builder implementation" );
        mocimplParam_.setDescription( new String[] {
            "<p>Controls how the MOC is built.",
            "You can generally leave this alone, but if you find performance",
            "is slow, or you are running out of memory, it may be worth",
            "experimenting with the options.",
            DocUtils.describedList( mocImpls, MocImpl::getName,
                                    MocImpl::getDescription, true ),
            "</p>",
        } );
        mocimplParam_.setDefaultOption( MocImpl.AUTO );

        outParam_ = new OutputStreamParameter( "out" );
        outParam_.setPrompt( "Location of output MOC file" );
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[] {
            moctypeParam_,
            orderParam_,
            shapeParam_,
            coordsParam_,
            torderParam_,
            tshapeParam_,
            tmapperParam_,
            t0Param_,
            t1Param_,
            mocfmtParam_,
            mocimplParam_,
            outParam_,
        };
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Generates and outputs a Multi-Order Coverage map from",
            "sky shapes associated with the rows of the input table.",
            "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        MocType mocType = moctypeParam_.objectValue( env );
        MocImpl mocimpl = mocimplParam_.objectValue( env );
        if ( mocType.useCdsImpl_ && !mocimpl.allowCdsImplementation() ) {
            String msg = mocimpl + " cannot be used with " + moctypeParam_
                       + "=" + mocType;
            throw new ParameterValueException( mocimplParam_, msg );
        }
        final int order;
        final String coordsExpr;
        final AreaMapper shapeMapper;
        if ( mocType.hasSpace_ ) {
            order = orderParam_.intValue( env );
            coordsExpr = coordsParam_.stringValue( env );
            shapeMapper = shapeParam_.objectValue( env );
        }
        else {
            order = -1;
            coordsExpr = null;
            shapeMapper = null;
        }
        final int torder;
        final String t0expr;
        final String t1expr;
        final TimeMapper tmapper;
        final TimeIntervalType tshape;
        if ( mocType.hasTime_ ) {
            torder = torderParam_.intValue( env );
            tmapper = tmapperParam_.objectValue( env );
            tshape = tshapeParam_.objectValue( env );
            t0expr = t0Param_.stringValue( env );
            t1expr = tshape.hasT1() ? t1Param_.stringValue( env ) : null;
        }
        else {
            torder = -1;
            t0expr = null;
            t1expr = null;
            tmapper = null;
            tshape = null;
        }
        MocStreamFormat mocfmt = mocfmtParam_.objectValue( env );
        Destination dest = outParam_.objectValue( env );
        return table -> {
            SequentialJELRowReader rdr = new SequentialJELRowReader( table );
            IOSupplier<Area> areaRdr =
                  mocType.hasSpace_
                ? createAreaReader( rdr, coordsExpr, shapeMapper, shapeParam_ )
                : null;
            IOSupplier<double[]> jdIntervalRdr =
                  mocType.hasTime_
                ? createJdIntervalReader( rdr, t0expr, t1expr, tshape, tmapper,
                                          tmapperParam_ )
                : null;

            /* SMOC. */
            if ( mocType.hasSpace_ && ! mocType.hasTime_ ) {
                assert !mocType.useCdsImpl_;
                MocBuilder mocBuilder = mocimpl.createMocBuilder( order );
                while ( rdr.next() ) {
                    Area area = areaRdr.get();
                    if ( area != null ) {
                        for ( long uniq : area.toMocUniqs( order ) ) {
                            mocBuilder.addTile( Coverage.uniqToOrder( uniq ),
                                                Coverage.uniqToIndex( uniq ) );
                        }
                    }
                }
                rdr.close();
                mocBuilder.endTiles();
                long[] orderCounts = mocBuilder.getOrderCounts();
                long ntile = 0;
                double cov = 0;
                for ( int io = 0; io < orderCounts.length; io++ ) {
                    ntile += orderCounts[ io ];
                    cov += orderCounts[ io ] * 1. / ( 12L << 2 * io );
                }
                if ( logger_.isLoggable( Level.INFO ) ) {
                    logger_.info( "MOC: size=" + ntile
                                + ", coverage=" + cov );
                }
                int maxOrder = orderCounts.length - 1;
                try ( OutputStream out = dest.createStream() ) {
                    mocfmt.writeMoc( mocBuilder.createOrderedUniqIterator(),
                                     ntile, maxOrder, out );
                }
            }

            /* TMOC. */
            else if ( mocType.hasTime_ && ! mocType.hasSpace_ ) {
                // The current implementation uses the cds.moc.TMoc class to
                // build TMOCs.  It shouldn't be too hard to replace this
                // with use of home-grown MocBuilder classes if required,
                // but since TMoc construction is rather niche I haven't
                // bothered so far.
                assert mocType.useCdsImpl_;
                TMoc tmoc = new TMoc( torder );
                tmoc.bufferOn( CDSMOC_BUFSIZ );
                while ( rdr.next() ) {
                    double[] jdInterval = jdIntervalRdr.get();
                    if ( jdInterval != null ) {
                        try {
                            tmoc.add( jdInterval[ 0 ], jdInterval[ 1 ] );
                        }
                        catch ( IOException e ) {
                            throw e;
                        }
                        catch ( Exception e ) {
                            throw new IOException( "MOC error", e );
                        }
                    }
                }
                rdr.close();
                tmoc.bufferOff();
                try ( OutputStream out = dest.createStream() ) {
                    mocfmt.writeCdsMoc( tmoc, out );
                }
            }

            /* STMOC. */
            else if ( mocType.hasTime_ && mocType.hasSpace_ ) {
                // Home-grown MocImpl classes so far don't support 2D MOCs,
                // so at present we have to give up and use cds.moc.STMoc
                // instead.
                assert mocType.useCdsImpl_;
                STMoc stmoc;
                try {
                    stmoc = new STMoc( torder, order );
                }
                catch ( Exception e ) {
                    assert false;
                    throw new IOException( "MOC error", e );
                }
                while ( rdr.next() ) {
                    Area area = areaRdr.get();
                    double[] jdInterval = jdIntervalRdr.get();
                    if ( area != null && jdInterval != null ) {
                        for ( long uniq : area.toMocUniqs( order ) ) {
                            try {
                                stmoc.add( Coverage.uniqToOrder( uniq ),
                                           Coverage.uniqToIndex( uniq ),
                                           jdInterval[ 0 ], jdInterval[ 1 ] );
                            }
                            catch ( IOException e ) {
                                throw e;
                            }
                            catch ( Exception e ) {
                                throw new IOException( "MOC error", e );
                            }
                        }
                    }
                }
                rdr.close();
                try ( OutputStream out = dest.createStream() ) {
                    mocfmt.writeCdsMoc( stmoc, out );
                }
            }

            else {
                assert false;
            }
        };
    }

    /**
     * Returns an object that can read Area values from table rows.
     *
     * @param  rdr  row reader
     * @param  coordExpr   supplied expression for shape coordinates
     * @param  mapper0    explictly supplied mapping from coordinates to shape,
     *                    or null to attempt a guess
     * @param  mapperParam  parameter from which mapper0 is supplied
     *                      (used in advisory user messages)
     * @return   reads shape objects from current row of supplied reader
     */
    private static IOSupplier<Area>
            createAreaReader( StarTableJELRowReader rdr, String coordExpr,
                              AreaMapper mapper0,
                              Parameter<AreaMapper> mapperParam )
            throws IOException {
        JELQuantity coordQuantity;
        try {
            coordQuantity =
                JELUtils.compileQuantity( JELUtils.getLibrary( rdr ), rdr,
                                          coordExpr, Object.class );
        }
        catch ( CompilationException e ) {
            throw new IOException( "Bad expression: \"" + coordExpr + "\": "
                                 + e.getMessage(), e );
        }
        CompiledExpression coordCompex = coordQuantity.getCompiledExpression();
        ValueInfo coordInfo = coordQuantity.getValueInfo();
        final AreaMapper mapper;
        if ( mapper0 != null ) {
            mapper = mapper0;
        }
        else {
            mapper = AreaDomain.INSTANCE.getProbableMapper( coordInfo );
        }
        if ( mapper == null ) {
            throw new IOException( "Can't guess shape type; "
                                 + "please supply value for "
                                 + mapperParam.getName() + " parameter" );
        }
        Class<?> coordClazz = coordInfo.getContentClass();
        Function<Object,Area> areaFunc =
            mapper.areaFunction( coordClazz );
        if ( areaFunc == null ) {
                throw new IOException( "Quantity " + coordExpr
                                     + "(" + coordClazz.getSimpleName() + ") "
                                     + "is not compatible with type "
                                     + mapper.getSourceName() );
        }
        return () -> {
            Object coords; 
            try {
                coords = rdr.evaluate( coordCompex );
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error for " + coordExpr, e );
            }
            return areaFunc.apply( coords );
        };
    }

    /**
     * Returns a table reader that can supply the time in Julian Days
     * for the current row.
     *
     * @param  rdr  row reader
     * @param  t0expr   expression giving primary time coordinate (epoch)
     * @param  t1expr   expression giving secondary time coordinate
     * @param  tshape   time interval type
     * @param  mapper0   time mapper for timeExpr value, or null to guess
     * @param  mapperParam   parameter supplying mapper value,
     *                       used in user communication
     * @return   reader for time as JD
     */
    private static IOSupplier<double[]> 
            createJdIntervalReader( StarTableJELRowReader rdr,
                                    String t0expr, String t1expr,
                                    TimeIntervalType tshape, TimeMapper mapper0,
                                    Parameter<TimeMapper> mapperParam )
            throws IOException {
        Library lib = JELUtils.getLibrary( rdr );
        JELQuantity t0quantity;
        try {
            t0quantity =
                JELUtils.compileQuantity( lib, rdr, t0expr, Object.class );
        }
        catch ( CompilationException e ) {
            throw new IOException( "Bad expression: \"" + t0expr + "\": "
                                 + e.getMessage(), e );
        }
        CompiledExpression t0compex = t0quantity.getCompiledExpression();
        ValueInfo t0info = t0quantity.getValueInfo();
        TimeMapper mapper = mapper0 == null
                          ? TimeDomain.INSTANCE.getProbableMapper( t0info )
                          : mapper0;
        if ( mapper == null ) {
            throw new IOException( "Can't guess time coordinate type; "
                                 + "please supply value for "
                                 + mapperParam.getName() + " parameter" );
        }
        boolean hasT1 = tshape.hasT1();
        CompiledExpression t1compex;
        if ( hasT1 ) {
            try {
                t1compex = JELUtils.compile( lib, rdr.getTable(), t1expr );
            }
            catch ( CompilationException e ) {
                throw new IOException( "Bad expression: \"" + t1expr + "\": "
                                     + e.getMessage(), e );
            }
        }
        else {
            t1compex = null;
        }
        return () -> {
            Object t0obj;
            Object t1obj;
            try {
                t0obj = rdr.evaluate( t0compex );
                t1obj = hasT1 ? rdr.evaluate( t1compex ) : null;
            }
            catch ( Throwable e ) {
                throw new IOException( "Evaluation error for " + t0expr, e );
            }
            double jd0 = mapper.toJd( t0obj );
            return Double.isNaN( jd0 )
                 ? null
                 : tshape.toJdInterval( jd0, t1obj, mapper );
        };
    }

    /**
     * Type of MOC to create.
     */
    public static enum MocType {

        /** Space MOC. */
        SMOC( "S", true, false, "Space MOC", false ),

        /** Time MOC. */
        TMOC( "T", false, true, "Time MOC", true ),

        /** Space-Time MOC. */
        STMOC( "ST", true, true, "Space-Time MOC", true );

        final String mininame_;
        final String name_;
        final boolean hasSpace_;
        final boolean hasTime_;
        final String description_;
        final boolean useCdsImpl_;

        /**
         * Constructor.
         *
         * @param  mininame  initial letter(s)
         * @param  hasSpace  true iff there is a spatial component
         * @param  hasTime   true iff there is a temporal component
         * @param  description   human-readable summary of type
         * @param  useCdsImpl   whether this type is built by MocShapeMode
         *                      using CDS classes or with MocImpl methods
         */
        MocType( String mininame, boolean hasSpace, boolean hasTime,
                 String description, boolean useCdsImpl ) {
            mininame_ = mininame;
            name_ = mininame + "MOC";
            hasSpace_ = hasSpace;
            hasTime_ = hasTime;
            description_ = description;
            useCdsImpl_ = useCdsImpl;
        }

        /**
         * Returns a MocType instance corresponding to a user-supplied string.
         *
         * @param  txt   moc type designator
         * @return  MocType instance
         */
        public static MocType fromString( String txt ) {
            return Arrays.stream( MocType.values() )
                         .filter( m -> txt.equalsIgnoreCase( m.mininame_ ) ||
                                       txt.equalsIgnoreCase( m.name_ ) )
                         .findFirst()
                         .orElse( null );
        }
    }
}
