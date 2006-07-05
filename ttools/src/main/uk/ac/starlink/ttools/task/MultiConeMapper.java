package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.ConstantStarTable;
import uk.ac.starlink.table.EmptyStarTable;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.ColumnIdentifier;
import uk.ac.starlink.ttools.JELUtils;
import uk.ac.starlink.ttools.SequentialJELRowReader;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.vo.ConeSearch;

/**
 * Mapper that does the work for the MultiCone task.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class MultiConeMapper implements TableMapper {

    private final Parameter urlParam_;
    private final Parameter raParam_;
    private final Parameter decParam_;
    private final Parameter srParam_;
    private final ChoiceParameter verbParam_;
    private final Parameter copycolsParam_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public MultiConeMapper() {
        urlParam_ = new Parameter( "serviceurl" );
        urlParam_.setPrompt( "Base URL for query returning VOTable" );
        urlParam_.setDescription( new String[] {
            "The base part of a URL which defines the queries to be made.",
            "Additional parameters will be appended to this using CGI syntax",
            "(\"<code>name=value</code>\", separated by '&amp;' characters).",
            "If this value does not end in either a '?' or a '&amp;',",
            "one will be added as appropriate.",
        } );

        raParam_ = new Parameter( "ra" );
        raParam_.setUsage( "<expr>" );
        raParam_.setPrompt( "Right Ascension expression in degrees (J2000)" );
        raParam_.setDescription( new String[] {
            "Expression which evaluates to the right ascension in degrees",
            "in the J2000 coordinate system",
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
        } );

        decParam_ = new Parameter( "dec" );
        decParam_.setUsage( "<expr>" );
        decParam_.setPrompt( "Declination expression in degrees (J2000)" );
        decParam_.setDescription( new String[] {
            "Expression which evaluates to the declination in degrees",
            "in the J2000 coordinate system",
            "for the request at each row of the input table.",
            "This will usually be the name or ID of a column in the",
            "input table, or a function involving one.",
        } );

        srParam_ = new Parameter( "sr" );
        srParam_.setUsage( "<expr>" );
        srParam_.setPrompt( "Search radius in degrees" );
        srParam_.setDescription( new String[] {
            "Expression which evaluates to the search radius in degrees",
            "for the request at each row of the input table.",
            "This will often be a constant numerical value, but may be",
            "the name or ID of a column in the input table,",
            "or a function involving one.",
        } );

        verbParam_ = new ChoiceParameter( "verb",
                                          new String[] { "1", "2", "3", } );
        verbParam_.setNullPermitted( true );
        verbParam_.setPrompt( "Verbosity level of search responses (1..3)" );
        verbParam_.setDescription( new String[] {
            "Verbosity level of the tables returned by the query service.",
            "A value of 1 indicates the bare minimum and",
            "3 indicates all available information.",
        } );

        copycolsParam_ = new Parameter( "copycols" );
        copycolsParam_.setUsage( "<colid-list>" );
        copycolsParam_.setNullPermitted( true );
        copycolsParam_.setPrompt( "Columns to be copied from input table" );
        copycolsParam_.setDescription( new String[] {
            "List of columns from the input table which are to be copied",
            "to the output table.",
            "Each column identified here will be prepended to the",
            "columns of the combined output table,",
            "and its value for each row taken from the input table row",
            "which provided the parameters of the query which produced it.",
            "See <ref id='colid-list'/> for list syntax.",
        } );
    }

    public int getInCount() {
        return 1;
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            raParam_,
            decParam_,
            srParam_,
            verbParam_,
            copycolsParam_,
        };
    }

    public TableMapping createMapping( Environment env )
            throws TaskException {
        final ConeSearch coner;
        try { 
            coner = new ConeSearch( urlParam_.stringValue( env ) );
        }
        catch ( IllegalArgumentException e ) {
            throw new ParameterValueException( urlParam_, e.getMessage(), e );
        }
        String sverb = verbParam_.stringValue( env );
        final int verb = sverb == null ? -1 : Integer.parseInt( sverb );
        final String copyColIdList = copycolsParam_.stringValue( env );
        final StarTableFactory tfact = TableEnvironment.getTableFactory( env );
        final String raString = raParam_.stringValue( env );
        final String decString = decParam_.stringValue( env );
        final String srString = srParam_.stringValue( env );
        return new TableMapping() {
            public void mapTables( StarTable[] in, TableConsumer[] out )
                    throws TaskException, IOException {
                StarTable inTable = in[ 0 ];
                int[] iCopyCols = ( copyColIdList == null ||
                                    copyColIdList.trim().length() == 0 )
                                ? new int[ 0 ]
                                : new ColumnIdentifier( inTable )
                                 .getColumnIndices( copyColIdList );
                SequentialJELRowReader jelReader =
                    new SequentialJELRowReader( inTable );
                try {
                    Library lib = JELUtils.getLibrary( jelReader );
                    StarTable outTable =
                        multiCone( inTable, coner, tfact, jelReader,
                                   compileDouble( raString, lib ),
                                   compileDouble( decString, lib ),
                                   compileDouble( srString, lib ),
                                   verb, iCopyCols );
                    out[ 0 ].consume( outTable );
                }
                finally {
                    jelReader.close();
                }
            }
        };
    }

    /**
     * Performs multiple cone searches directed by exprssions defined
     * relative to an input table, and constructs a concatenated table
     * for the result.
     *
     * @param   in   input table
     * @param   tfact  table factory
     * @param   jelReader  row reader which reads/calculates values from
     *          <code>in</code>
     * @param   raExpr  calculates J2000 right ascension in degrees from
     *          <code>jelReader</code>
     * @param   decExpr calculates J2000 declination in degrees from
     *          <code>jelReader</code>
     * @param   srExpr  calculates search radius in degrees from
     *          <code>jelReader</code>
     * @param   verb  verbosity level 
     * @param   iCopyCols  array of column indices from <code>in</code>
     *          to copy to the output table
     */
    private static StarTable multiCone( StarTable in, final ConeSearch coner,
                                        final StarTableFactory tfact,
                                        final SequentialJELRowReader jelReader,
                                        final CompiledExpression raExpr,
                                        final CompiledExpression decExpr,
                                        final CompiledExpression srExpr,
                                        final int verb, final int[] iCopyCols )
            throws IOException, TaskException {

        /* Create array of column metadata objects for the columns which are
         * to be copied from the input table. */
        final int ncopy = iCopyCols.length;
        final ColumnInfo[] constInfos = new ColumnInfo[ ncopy ];
        for ( int ic = 0; ic < ncopy; ic++ ) {
            constInfos[ ic ] = in.getColumnInfo( iCopyCols[ ic ] );
        }

        /* Get a metadata-only table from the service by specifying a 
         * search radius of zero.  This also acts as a useful check that
         * the service is alive. */
        StarTable coneMeta;
        try {
            coneMeta = coner.performSearch( 0., 0., 0., verb, tfact );
        }
        catch ( IOException e ) {
            throw (IOException)
                  new IOException( "Error response while retrieving metadata: "
                                 + e.getMessage() )
                 .initCause( e );
        }
        final JoinStarTable meta =
             new JoinStarTable( new StarTable[] {
                 new ConstantStarTable( constInfos, new Object[ ncopy ], 0L ),
                 coneMeta,
             } );
        meta.setParameters( coneMeta.getParameters() );

        /* Construct an iterator which will iterate over the rows of the 
         * input table returning StarTable objects representing the result
         * of the cone searches they define. */
        Iterator tableIt = new Iterator() {
            StarTable next_ = nextTable();
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public boolean hasNext() {
                return next_ != null;
            }
            public Object next() {
                StarTable current = next_;
                next_ = nextTable();
                return current;
            }
            private StarTable nextTable() {
                try {
                    if ( ! jelReader.next() ) {
                        return null;
                    }
                }
                catch ( IOException e ) {
                    logger_.warning( "Read error: " + e.getMessage() );
                    return null;
                }
                double ra;
                double dec;
                double sr;
                Object[] copyCells;
                try {
                    Object raObj = jelReader.evaluate( raExpr );
                    Object decObj = jelReader.evaluate( decExpr );
                    Object srObj = jelReader.evaluate( srExpr );
                    ra = raObj instanceof Number
                       ? ((Number) raObj).doubleValue()
                       : Double.NaN;
                    dec = decObj instanceof Number
                        ? ((Number) decObj).doubleValue()
                        : Double.NaN;
                    sr = srObj instanceof Number
                       ? ((Number) srObj).doubleValue()
                       : Double.NaN;
                    copyCells = new Object[ ncopy ];
                    for ( int ic = 0; ic < ncopy; ic++ ) {
                        copyCells[ ic ] = jelReader.getCell( iCopyCols[ ic ] );
                    }
                }
                catch ( IOException e ) {
                    logger_.warning( "Data read error: " + e.getMessage() );
                    return new EmptyStarTable( meta );
                }
                catch ( Throwable e ) {
                    logger_.warning( "Data evaluation error: "
                                   + e.getMessage() );
                    return new EmptyStarTable( meta );
                }
                if ( Double.isNaN( ra ) ||
                     Double.isNaN( dec ) ||
                     Double.isNaN( sr ) ) {
                    logger_.warning( "Search parameters invalid" );
                    return new EmptyStarTable( meta );
                }

                /* Make the request. */
                StarTable coneResult;
                try {
                    coneResult = coner.performSearch( ra, dec, sr, verb,
                                                      tfact );
                    coneResult = tfact.getStoragePolicy()
                                      .copyTable( coneResult );
                }
                catch ( IOException e ) {
                    logger_.warning( "Error response: " + e.getMessage() );
                    return new EmptyStarTable( meta );
                }
 
                /* Combine selected cells from the input table as 
                 * requested with the retrieved data to construct 
                 * a table giving the current section of the result
                 * table. */
                long nr = coneResult.getRowCount();
                assert nr >= 0;
                logger_.info( "Retrieved " + nr + " rows" );
                if ( nr > 0 ) {
                    StarTable constTable =
                        new ConstantStarTable( constInfos, copyCells, nr );
                    StarTable[] pair = new StarTable[] { constTable,
                                                         coneResult };
                    return new JoinStarTable( pair );
                }
                else {
                    return new EmptyStarTable( meta );
                }
            }
        };

        /* Combine all the result tables into one and return. */
        return new ConcatStarTable( meta, tableIt );
    }

    /**
     * Compiles a JEL expression.
     * An informative UsageException is thrown if it won't compile.
     *
     * @param   lib   JEL library
     * @param   sexpr   string expression
     * @return  compiled expression
     */
    private static CompiledExpression compileDouble( String sexpr, Library lib )
            throws UsageException {
        try {
            return Evaluator.compile( sexpr, lib, double.class );
        }
        catch ( CompilationException e ) {
            throw new UsageException( "Bad numeric expression \"" + sexpr + "\""
                                    + " - " + e.getMessage() );
        }
    }
}
