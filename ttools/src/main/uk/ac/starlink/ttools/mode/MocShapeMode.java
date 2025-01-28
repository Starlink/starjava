package uk.ac.starlink.ttools.mode;

import cds.healpix.Healpix;
import cds.healpix.HealpixNested;
import cds.moc.SMoc;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
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
import uk.ac.starlink.ttools.moc.MocBuilder;
import uk.ac.starlink.ttools.moc.MocImpl;
import uk.ac.starlink.ttools.moc.MocStreamFormat;
import uk.ac.starlink.util.Destination;

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
    private final ChoiceParameter<MocStreamFormat> mocfmtParam_;
    private final ChoiceParameter<MocImpl> mocimplParam_;
    private final OutputStreamParameter outParam_;

    private static final int MAX_ORDER = SMoc.MAXORD_S;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.mode" );

    /**
     * Constructor.
     */
    public MocShapeMode() {
        orderParam_ = new IntegerParameter( "order" );
        orderParam_.setPrompt( "MOC Healpix maximum order" );
        orderParam_.setUsage( "0.." + MAX_ORDER );
        orderParam_.setMaximum( MAX_ORDER );
        int orderDflt = 10;
        int dfltResArcmin =
            (int) Math.round( 3520 * Math.pow( 2, -orderDflt ) );
        assert dfltResArcmin > 0 && dfltResArcmin < 5000;
        orderParam_.setIntDefault( orderDflt );
        orderParam_.setDescription( new String[] {
            "<p>Maximum HEALPix order for the MOC.",
            "This defines the maximum resolution of the output coverage map.",
            "The angular resolution corresponding to order <em>k</em>",
            "is approximately 180/sqrt(3.Pi)/2^<em>k</em> degrees",
            "(3520*2^<em>-k</em> arcmin).",
            "Permitted values are 0.." + MAX_ORDER + " inclusive.",
            "The default value is " + orderDflt + ", which corresponds to",
            "about " + dfltResArcmin + " arcmin.",
            "</p>",
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

        } );
        shapeParam_.setNullPermitted( true );

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
            orderParam_,
            coordsParam_,
            shapeParam_,
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
        int order = orderParam_.intValue( env );
        String coordsExpr = coordsParam_.stringValue( env );
        AreaMapper shapeMapper0 = shapeParam_.objectValue( env );
        MocImpl mocimpl = mocimplParam_.objectValue( env );
        MocStreamFormat mocfmt = mocfmtParam_.objectValue( env );
        MocBuilder mocBuilder = mocimpl.createMocBuilder( order );
        HealpixNested hpx = Healpix.getNested( order );
        Destination dest = outParam_.objectValue( env );
        return table -> {
            SequentialJELRowReader rdr = new SequentialJELRowReader( table );
            JELQuantity coordQuantity;
            try {
                coordQuantity =
                    JELUtils.compileQuantity( JELUtils.getLibrary( rdr ), rdr,
                                              coordsExpr, Object.class );
            }
            catch ( CompilationException e ) {
                throw new IOException( "Bad expression " + e.getMessage() );
            }
            CompiledExpression coordCompex =
                coordQuantity.getCompiledExpression();
            ValueInfo coordInfo = coordQuantity.getValueInfo();
            final AreaMapper shapeMapper;
            if ( shapeMapper0 != null ) {
                shapeMapper = shapeMapper0;
            }
            else {
                shapeMapper =
                    AreaDomain.INSTANCE.getProbableMapper( coordInfo );
            }
            if ( shapeMapper == null ) {
                throw new IOException( "Can't guess shape type; "
                                     + "please supply value for "
                                     + shapeParam_.getName() + " parameter" );
            }
            Class<?> coordClazz = coordInfo.getContentClass();
            Function<Object,Area> areaFunc =
                shapeMapper.areaFunction( coordClazz );
            if ( areaFunc == null ) {
                throw new IOException( "Quantity " + coordsExpr
                                     + "(" + coordClazz.getSimpleName() + ") "
                                     + "is not compatible with type "
                                     + shapeMapper.getSourceName() );
            }
            while ( rdr.next() ) {
                Object coords;
                try {
                    coords = rdr.evaluate( coordCompex );
                }
                catch ( Throwable e ) {
                    throw new IOException( "Evaluation error for " + coordsExpr,
                                           e );
                }
                Area area = areaFunc.apply( coords );
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
        };
    }
}
