package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;
import uk.ac.starlink.ttools.jel.SequentialJELRowReader;
import uk.ac.starlink.util.IOFunction;

/**
 * Task to add the contents of an external table for each row of
 * an input table as array-valued columns.
 *
 * @author   Mark Taylor
 * @since    17 Jun 2022
 */
public class ArrayJoin extends SingleMapperTask {

    private final ExpressionInputTableParameter atableParam_;
    private final InputFormatParameter afmtParam_;
    private final FilterParameter acmdParam_;
    private final BooleanParameter astreamParam_;
    private final BooleanParameter cacheParam_;
    private final JoinFixActionParameter fixcolsParam_;
    private final StringParameter asuffixParam_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public ArrayJoin() {
        super( "Adds table-per-row data as array-valued columns",
               new ChoiceMode(), true, true );

        acmdParam_ = new FilterParameter( "acmd" );

        atableParam_ = new ExpressionInputTableParameter( "atable" );
        atableParam_.setTableDescription( "array tables" );
        atableParam_.setPrompt( "Per-row location of tables with array data" );
        atableParam_.setUsage( "<loc-expr>" );
        atableParam_.setDescription( new String[] {
            "<p>Gives the location of the table whose rows will be turned into",
            "an array-valued column.",
            "This will generally be an <ref id='jel'>expression</ref>",
            "giving a URL or filename that is different",
            "for each row of the input table.",
            "If table loading fails for the given location,",
            "for instance becase the file is not found or an HTTP 404",
            "response is received,",
            "the array cells in the corresponding row will be blank.",
            "</p>",
            "<p>The first non-blank table loaded defines the array columns",
            "to be added.",
            "If subsequent tables have a different structure",
            "(do not contain similar columns in a similar sequence)",
            "an error may result.",
            "If the external array tables are not all homogenous in this way,",
            "the <code>" + acmdParam_.getName() + "</code> parameter",
            "can be used to filter them so that they are.",
            "</p>",
        } );
        afmtParam_ = atableParam_.getFormatParameter();
        astreamParam_ = atableParam_.getStreamParameter();
        acmdParam_.setTableDescription( "array tables", atableParam_, true );

        cacheParam_ = new BooleanParameter( "cache" );
        cacheParam_.setPrompt( "Cache array values?" );
        cacheParam_.setBooleanDefault( true );
        cacheParam_.setDescription( new String[] {
            "<p>Determines whether the array data will be cached",
            "the first time an array table is read (true)",
            "or re-read from the array table every time",
            "the row is accessed (false).",
            "Since the row construction may be an expensive step,",
            "especially if the tables are downloaded,",
            "it usually makes sense to set this true (the default).",
            "When true it also enables the metadata to be adjusted",
            "to report constant array length where applicable,",
            "which cannot be done before all the rows have been scanned,",
            "and which may enable more efficient file output.",
            "However, if you want to stream the data you can set it false.",
            "</p>",
        } );

        fixcolsParam_ = new JoinFixActionParameter( "fixcols" );
        asuffixParam_ =
            fixcolsParam_
           .createSuffixParameter( "suffixarray", "the array tables", "_a" );
    }

    @Override
    public Parameter<?>[] getParameters() {
        List<Parameter<?>> params = new ArrayList<>();
        params.addAll( Arrays.asList( super.getParameters() ) );
        params.addAll( Arrays.asList( new Parameter<?>[] {
            atableParam_,
            afmtParam_,
            astreamParam_,
            acmdParam_,
            cacheParam_,
            fixcolsParam_,
            asuffixParam_,
        } ) );
        return params.toArray( new Parameter<?>[ 0 ] );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        final TableProducer inProd = createInputProducer( env );
        final String atableExpr = atableParam_.stringValue( env );
        String fmt = afmtParam_.stringValue( env );
        boolean stream = astreamParam_.booleanValue( env );
        final ProcessingStep[] asteps = acmdParam_.stepsValue( env );
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env);
        boolean allowMissing = true;
        boolean isCached = cacheParam_.booleanValue( env );
        final IOFunction<String,StarTable> atableReader = loc -> {
            logger_.info( "Loading table: " + loc );
            StarTable atable;
            try {
                atable = atableParam_.makeTable( loc, fmt, stream, tfact );
            }
            catch ( IOException e ) {
                if ( allowMissing ) {
                    return null;
                }
                else {
                    throw e;
                }
            }
            catch ( TaskException e ) {
                throw new IOException( e.getMessage(), e );
            }
            InputTableSpec spec =
                InputTableSpec.createSpec( loc, asteps, atable );
            try {
                return spec.getWrappedTable();
            }
            catch ( TaskException e ) {
                throw new IOException( "Trouble filtering array table " + loc,
                                       e );
            }
        };
        JoinFixAction inFixact = JoinFixAction.NO_ACTION;
        JoinFixAction aFixact =
            fixcolsParam_.getJoinFixAction( env, asuffixParam_ );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                final StarTable inTable = inProd.getTable();
                final Function<Library,CompiledExpression> alocCompiler;
                try {
                    alocCompiler =
                        JELUtils.compiler( inTable, atableExpr, String.class );
                }
                catch ( CompilationException e ) {
                    throw new ParameterValueException( atableParam_,
                                                       "Bad table location",
                                                       e );
                }
                ArrayDataTable seqArrayTable =
                    createSequentialArrayTable( inTable, alocCompiler,
                                                atableReader );
                StarTable arrayTable = isCached
                                     ? cacheArrayTable( seqArrayTable )
                                     : seqArrayTable;
                StarTable[] tables = { inTable, arrayTable };
                JoinFixAction[] fixacts = { inFixact, aFixact };
                return new JoinStarTable( tables, fixacts );
            }
        };
    }

    /**
     * Generates the joined table given required information.
     *
     * @param  inTable  input table
     * @param  alocCompiler  produces location of array table
     *                       for input table row
     * @param  atableLoader  loads an array table given a location
     * @return   non-random table joining input and arrays
     */
    private static ArrayDataTable
            createSequentialArrayTable(
                    StarTable inTable,
                    Function<Library,CompiledExpression> alocCompiler,
                    IOFunction<String,StarTable> atableLoader )
            throws IOException, TaskException {

        /* Read through the input table to find the first array table.
         * We will use this as a template supplying the array column
         * metadata. */
        SequentialJELRowReader inRdr0 = new SequentialJELRowReader( inTable );
        Library lib = JELUtils.getLibrary( inRdr0 );
        CompiledExpression alocCompex = alocCompiler.apply( lib );
        int[] rowIndex = new int[] { -1 };
        StarTable atable0 = readFirstArrayTable( inTable, alocCompiler,
                                                 atableLoader, rowIndex);
        if ( atable0 == null ) {
            throw new TaskException( "No array tables" ); 
        }
        int irow0 = rowIndex[ 0 ];

        /* Prepare typed array handlers for the array columns we
         * have discovered. */
        int nacol = atable0.getColumnCount();
        List<ArrayColumn<?>> acolList = new ArrayList<>( nacol );
        for ( int ic = 0; ic < nacol; ic++ ) {
            ColumnInfo cinfo = atable0.getColumnInfo( ic );
            ArrayColumn<?> acol = createArrayColumn( cinfo, ic );
            if ( acol != null ) {
                acolList.add( acol );
            }
            else {
                logger_.warning( "Array storage not supported for column "
                               + cinfo + " - ignoring" );
            }
        }
        ArrayColumn<?>[] acols = acolList.toArray( new ArrayColumn<?>[ 0 ] );
        if ( acols.length == 0 ) {
            throw new ExecutionException( "No suitable columns "
                                        + "in template array table" );
        }

        /* Create and return a table ready to read the data. */
        return new ArrayDataTable( inTable, acols, alocCompiler, atableLoader,
                                   irow0 );
    }

    /**
     * Takes a sequential result table and returns a random-access
     * version of the same thing by reading the rows and caching them.
     * This also updates the metadata with array length information
     * not initially known by the input table.
     *
     * @param  seqTable  array-joined table
     */
    private static StarTable cacheArrayTable( ArrayDataTable seqTable )
            throws IOException {

        /* Read sequential row data into a cache. */
        RowStore rowStore = StoragePolicy.getDefaultPolicy().makeRowStore();
        rowStore.acceptMetadata( seqTable );
        try ( RowSequence rseq = seqTable.getRowSequence() ) {
            while ( rseq.next() ) {
                rowStore.acceptRow( rseq.getRow() );
            }
        }
        rowStore.endRows();

        /* Store the column metadata and update it with information
         * gathered by the ArrayDataTable during row iteration. */
        int nc = seqTable.getColumnCount();
        final ColumnInfo[] cinfos = new ColumnInfo[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            cinfos[ ic ] = seqTable.getEnhancedColumnInfo( ic );
        }

        /* Return a table based on the cached table but with enhanced
         * column metadata. */
        return new WrapperStarTable( rowStore.getStarTable() ) {
            @Override
            public ColumnInfo getColumnInfo( int ic ) {
                return cinfos[ ic ];
            }
        };
    }

    /**
     * Reads through an input table to find the first non-blank array
     * table associated with one of its rows.
     *
     * @param  inTable  input table
     * @param  alocCompiler  produces location of array table
     *                       for input table row
     * @param  atableLoader  loads an array table given a location
     * @param  rowIndex  1-element array; on exit this will be updated
     *                   to contain the row index in which the first
     *                   table was found
     * @return  first array table associated with input table
     */
    private static StarTable readFirstArrayTable( 
                    StarTable inTable,
                    Function<Library,CompiledExpression> alocCompiler,
                    IOFunction<String,StarTable> atableLoader,
                    int[] rowIndex )
            throws IOException, TaskException {
        try ( SequentialJELRowReader inRdr =
                  new SequentialJELRowReader( inTable ) ) {
            Library lib = JELUtils.getLibrary( inRdr );
            CompiledExpression alocCompex = alocCompiler.apply( lib );
            while ( inRdr.next() ) {
                String aloc = evaluateLocation( alocCompex, inRdr );
                if ( aloc != null ) {
                    StarTable atable = atableLoader.apply( aloc );
                    if ( atable != null ) {
                        return atable;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns the location of an array table associated with the current
     * state of a rowReader.
     *
     * @param   locCompex   compiled expression yielding table location
     * @param   rowReader  table context within which to evaluate locCompex
     * @return   array table location
     */
    private static String evaluateLocation( CompiledExpression locCompex,
                                            JELRowReader rowReader )
            throws IOException {
        try {
            return (String) rowReader.evaluate( locCompex );
        }

        /* The message here is not very informative, but I don't really
         * expect errors here.  If they do show up in practice, think
         * about improving the report. */
        catch ( Throwable e ) {
            throw new IOException( "Error evaluating table location", e );
        }
    }

    /**
     * Reads the data from a table as a set of arrays, one containing
     * all the data from a table column.
     *
     * @param   acols  defines the columns to be read
     * @param   atable  array table to read
     * @param   irow   index of input table for this array table
     * @return   array of typed arrays, one for each element of acols
     */
    private static Object[] readArrayData( ArrayColumn<?>[] acols,
                                           StarTable atable, long irow )
            throws IOException {
        int na = acols.length;

        /* It makes life much easier if we know the number of rows.
         * Since these tables are not expected to be all that large,
         * and we're going to have to read all the data anyway,
         * cache the table if that's the only way to find out. */
        int nrow = Tables.checkedLongToInt( atable.getRowCount() );
        if ( nrow < 0 ) {
            atable = Tables.randomTable( atable );
            nrow = Tables.checkedLongToInt( atable.getRowCount() );
        }
        assert nrow >= 0;

        /* Check the columns look like we are expecting and
         * prepare array storage. */
        Object[] adata = new Object[ na ];
        for ( int ia = 0; ia < na; ia++ ) {
            ArrayColumn<?> acol = acols[ ia ];
            ColumnInfo gotInfo = atable.getColumnInfo( acol.icol_ );
            ColumnInfo templateInfo = acol.scalarInfo_;
            if ( ! gotInfo.getContentClass()
                          .equals( templateInfo.getContentClass() ) ||
                 ! gotInfo.getName().equals( templateInfo.getName() ) ) {
                throw new IOException( "Table data mismatch at row " + irow
                                     + "; " + gotInfo + " does not match "
                                     + templateInfo );
            }
            adata[ ia ] = acol.createArray( nrow );
        }

        /* Write all the values into the arrays, a row at a time. */
        try ( RowSequence rseq = atable.getRowSequence() ) {
            for ( int ir = 0; ir < nrow; ir++ ) {
                rseq.next();
                Object[] row = rseq.getRow();
                for ( int ia = 0; ia < na; ia++ ) {
                    ArrayColumn<?> acol = acols[ ia ];
                    int ic = acol.icol_;
                    acol.setValueUnchecked( row[ ic ], ir, adata[ ia ] );
                }
            }
        }

        /* Return populated arrays. */
        return adata;
    }

    /**
     * Creates an object for managing array data.
     *
     * @param  sInfo  metadata for scalar column to be stored in array
     * @param  icol   column index in input table
     * @return   typed array storage manager
     */
    private static ArrayColumn<?> createArrayColumn( ColumnInfo sInfo,
                                                     final int icol ) {
        Class<?> clazz = sInfo.getContentClass();
        if ( clazz.equals( Boolean.class ) ) {
            return new ArrayColumn<boolean[]>( boolean[].class, icol, sInfo ) {
                boolean[] createArray( int n ) {
                    return new boolean[ n ];
                }
                void setValue( Object value, int ir, boolean[] array ) {
                    if ( value instanceof Boolean ) {
                        array[ ir ] = ((Boolean) value).booleanValue();
                    }
                }
            };
        }
        else if ( clazz.equals( Byte.class ) ) {
            return new ArrayColumn<byte[]>( byte[].class, icol, sInfo ) {
                byte[] createArray( int n ) {
                    return new byte[ n ];
                }
                void setValue( Object value, int ir, byte[] array ) {
                    array[ ir ] = ((Number) value).byteValue();
                }
            };
        }
        else if ( clazz.equals( Short.class ) ) {
            return new ArrayColumn<short[]>( short[].class, icol, sInfo ) {
                short[] createArray( int n ) {
                    return new short[ n ];
                }
                void setValue( Object value, int ir, short[] array ) {
                    if ( value instanceof Number ) {
                        array[ ir ] = ((Number) value).shortValue();
                    }
                }
            };
        }
        else if ( clazz.equals( Integer.class ) ) {
            return new ArrayColumn<int[]>( int[].class, icol, sInfo ) {
                int[] createArray( int n ) {
                    return new int[ n ];
                }
                void setValue( Object value, int ir, int[] array ) {
                    if ( value instanceof Number ) {
                        array[ ir ] = ((Number) value).intValue();
                    }
                }
            };
        }
        else if ( clazz.equals( Long.class ) ) {
            return new ArrayColumn<long[]>( long[].class, icol, sInfo ) {
                long[] createArray( int n ) {
                    return new long[ n ];
                }
                void setValue( Object value, int ir, long[] array ) {
                    if ( value instanceof Number ) {
                        array[ ir ] = ((Number) value).longValue();
                    }
                }
            };
        }
        else if ( clazz.equals( Float.class ) ) {
            return new ArrayColumn<float[]>( float[].class, icol, sInfo ) {
                float[] createArray( int n ) {
                    return new float[ n ];
                }
                void setValue( Object value, int ir, float[] array ) {
                    array[ ir ] = value instanceof Number
                                ? ((Number) value).floatValue()
                                : Float.NaN;
                }
            };
        }
        else if ( clazz.equals( Double.class ) ) {
            return new ArrayColumn<double[]>( double[].class, icol, sInfo ) {
                double[] createArray( int n ) {
                    return new double[ n ];
                }
                void setValue( Object value, int ir, double[] array ) {
                    array[ ir ] = value instanceof Number
                                ? ((Number) value).doubleValue()
                                : Double.NaN;
                }
            };
        }
        else if ( clazz.equals( String.class ) ) {
            return new ArrayColumn<String[]>( String[].class, icol, sInfo ) {
                String[] createArray( int n ) {
                    return new String[ n ];
                }
                void setValue( Object value, int ir, String[] array ) {
                    if ( value instanceof String ) {
                        array[ ir ] = (String) value;
                    }
                }
            };
        }
        else {
            return null;
        }
    }

    /**
     * Class for managing typed array data.
     */
    private static abstract class ArrayColumn<A> {

        final Class<A> aClazz_;
        final int icol_;
        final ColumnInfo scalarInfo_;
        final ColumnInfo arrayInfo_;

        /**
         * Constructor.
         *
         * @param  aClazz   class for array storage
         * @param  icol    index of column in table containing data
         * @param  scalarInfo   metadata for column in table containing data
         */
        ArrayColumn( Class<A> aClazz, int icol, ColumnInfo scalarInfo ) {
            aClazz_ = aClazz;
            icol_ = icol;
            scalarInfo_ = scalarInfo;
            arrayInfo_ = new ColumnInfo( scalarInfo );
            arrayInfo_.setContentClass( aClazz );
        }

        /**
         * Creates an array for storing typed data.
         *
         * @param  n  row count
         * @return  new array
         */
        abstract A createArray( int n );

        /**
         * Stores a single element in an storage array.
         *
         * @param  value  value to store, expected of suitable type
         * @param  ix     element index
         * @param  array  storage array
         */
        abstract void setValue( Object value, int ix, A array );

        /**
         * Convenience untypesafe method for storing data in an array.
         * The generics on this class are really to ensure that they
         * get implemented without mistakes, not for typechecking
         * at usage time which would in any case be a major pain to do.
         *
         * @param  value  value to store, expected of suitable type
         * @param  ix     element index
         * @param  array  storage array, assumed of type &lt;A&gt;
         */
        final void setValueUnchecked( Object value, int ir, Object array ) {
            setValue( value, ir, aClazz_.cast( array ) );
        }
    }

    /**
     * StarTable implementation for use with this class.
     * It is only capable of sequential access.
     */
    private static class ArrayDataTable extends AbstractStarTable {

        final StarTable base_;
        final ArrayColumn<?>[] arrayCols_;
        final Function<Library,CompiledExpression> alocCompiler_;
        final IOFunction<String,StarTable> atableLoader_;
        final int irow0_;
        final int ncol_;
        final static int UNKNOWN_DIM = -1;
        final static int VARIABLE_DIM = -2;
        int[] arrayDims_;

        /**
         * Constructor.
         *
         * @param  base  input table
         * @param  arrayCols   array of objects describing the
         *                     columns coming from external array tables
         * @param  alocCompiler  produces location of array table
         *                       for input table row
         * @param  atableLoader  loads an array table given a location
         * @param  irow0  index of the first row known to contain a
         *                usable array table; array values can be
         *                assumed missing for any earlier rows
         */
        ArrayDataTable( StarTable base, ArrayColumn<?>[] arrayCols,
                        Function<Library,CompiledExpression> alocCompiler,
                        IOFunction<String,StarTable> atableLoader,
                        int irow0 ) {
            base_ = base;
            arrayCols_ = arrayCols;
            alocCompiler_ = alocCompiler;
            atableLoader_ = atableLoader;
            irow0_ = irow0;
            ncol_ = arrayCols.length;
        }

        public boolean isRandom() {
            return false;
        }

        public int getColumnCount() {
            return ncol_;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return arrayCols_[ icol ].arrayInfo_;
        }

        public long getRowCount() {
            return base_.getRowCount();
        }

        public RowSequence getRowSequence() throws IOException {
            SequentialJELRowReader inRdr = new SequentialJELRowReader( base_ );
            Library lib = JELUtils.getLibrary( inRdr );
            final CompiledExpression alocCompex = alocCompiler_.apply( lib );
            final int[] adims = new int[ ncol_ ];
            final Object[] emptyRow = new Object[ ncol_ ];
            Arrays.fill( adims, UNKNOWN_DIM );
            return new RowSequence() {
                Object[] adata_;
                int irow_ = -1;
                public boolean next() throws IOException {
                    adata_ = null;
                    if ( inRdr.next() ) {
                        irow_++;
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                public Object[] getRow() throws IOException {
                    return getArrayData();
                }
                public Object getCell( int icol ) throws IOException {
                    return getArrayData()[ icol ];
                }
                public void close() throws IOException {
                    adata_ = null;
                    inRdr.close();
                    arrayDims_ = adims.clone();
                }

                /**
                 * Returns an array of arrays giving the array-valued cells
                 * at the current row for each of the array columns
                 * managed by this sequence.
                 * The data is acquired lazily.
                 *
                 * @return  ncol-element array of arrays,
                 *          or null if no array data this row
                 */
                Object[] getArrayData() throws IOException {
                    if ( adata_ == null ) {
                        final Object[] adata;
                        String aloc = evaluateLocation( alocCompex, inRdr );
                        if ( aloc != null && irow_ >= irow0_ - 1 ) {
                            try ( StarTable aTable =
                                      atableLoader_.apply( aloc ) ) {
                                adata = aTable == null
                                      ? emptyRow
                                      : readArrayData( arrayCols_, aTable,
                                                       irow_ );
                            }
                            if ( adata != emptyRow ) {
                                for ( int ic = 0; ic < ncol_; ic++ ) {
                                    Object array = adata[ ic ];
                                    if ( array != null ) {
                                        int n = Array.getLength( array );
                                        if ( n > 0 ) {
                                            if ( adims[ ic ] == UNKNOWN_DIM ) {
                                                adims[ ic ] = n;
                                            }
                                            else if ( adims[ ic ] > 0 &&
                                                      n != adims[ ic ] ) {
                                                adims[ ic ] = VARIABLE_DIM;
                                            }
                                            assert adims[ ic ] == VARIABLE_DIM
                                                || adims[ ic ] == n;
                                        }
                                    }
                                }
                            }
                        }
                        else {
                            adata = null;
                        }
                        adata_ = adata;
                    }
                    return adata_;
                }
            };
        }

        /**
         * Special method to return the most informative metadata available
         * for a given column.   This is like getColumnInfo, but
         * if a RowSequence has been acquired and completed,
         * the dimensions of array columns may be filled in
         * (since we've seen the actual lengths of all the array data).
         * If called before any row iteration,
         * it's just the same as getColumnInfo.
         *
         * @param  icol  column index
         * @return   best available metadata for column
         */
        ColumnInfo getEnhancedColumnInfo( int icol ) {
            ColumnInfo info = new ColumnInfo( arrayCols_[ icol ].arrayInfo_ );
            int adim = arrayDims_ == null
                     ? UNKNOWN_DIM
                     : arrayDims_[ icol ];
            if ( adim > 0 ) {
                info.setShape( new int[] { adim } );
            }
            return info;
        }

        // boilerplate 

        public String getName() {
            return base_.getName();
        }
        public List<DescribedValue> getParameters() {
            return base_.getParameters();
        }
        public DescribedValue getParameterByName( String parname ) {
            return base_.getParameterByName( parname );
        }
        public List<ValueInfo> getColumnAuxDataInfos() {
            return base_.getColumnAuxDataInfos();
        }
        public RowSplittable getRowSplittable() throws IOException {
            return Tables.getDefaultRowSplittable( this );
        }
    }

    /**
     * Custom TableInputParameter subclass.
     * This just returns a string value (that can eventually get interpreted
     * as a table location), but it's worth subclassing
     * AbstractInputTableParameter to make use of the associated parameters
     * giving table input format etc.
     */
    private static class ExpressionInputTableParameter
            extends AbstractInputTableParameter<String> {

        /**
         * Constructor.
         *
         * @param  name  parameter name
         */
        ExpressionInputTableParameter( String name ) {
            super( name, String.class );
        }
        public String stringToObject( Environment env, String sval ) {
            return sval;
        }
    }
}
