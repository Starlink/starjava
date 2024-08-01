package uk.ac.starlink.ttools.task;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.jel.JELTable;

/**
 * Task for performing aggregation operations on groups of rows of an
 * input table.
 *
 * @author   Mark Taylor
 * @since    17 Nov 2022
 */
public class TableGroup extends SingleMapperTask {

    private final StringMultiParameter keysParam_;
    private final StringMultiParameter aggcolsParam_;
    private final RowRunnerParameter runnerParam_;
    private final BooleanParameter sortParam_;
    private final BooleanParameter cacheParam_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /** Delimiter character for aggcol parameter entries. */
    public static final char AGGCOL_DELIM = ';';

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public TableGroup() {
        super( "Calculates aggregate functions on groups of rows",
               new ChoiceMode(), true, true );

        keysParam_ = new StringMultiParameter( "keys", ' ' );
        keysParam_.setUsage( "<expr> ..." );
        keysParam_.setPrompt( "Expressions for grouping key values" );
        keysParam_.setDescription( new String[] {
            "<p>List of one or more space-separated words",
            "defining the groups within which aggregation should be done.",
            "Each word can be a column name or an expression using the",
            "<ref id='jel'>expression language</ref>.",
            "Each expression will appear as one of the columns",
            "in the output table.",
            "This list corresponds to the contents of an ADQL/SQL",
            "<code>GROUP BY</code> clause.",
            "</p>",
        } );

        aggcolsParam_ = new StringMultiParameter( "aggcols", ' ' );
        aggcolsParam_.setPrompt( "Aggregate column definitions" );
        aggcolsParam_.setUsage( "<expr>" + AGGCOL_DELIM
                              + "<aggregator>[" + AGGCOL_DELIM
                              + "<name>] ..." );
        aggcolsParam_.setDescription( new String[] {
            "<p>Defines the aggregate quantities to be calculated",
            "for each group of input rows.",
            "Each quantity is defined by one entry in this list;",
            "entries are space-separated, or can be given by multiple",
            "instances of this parameter on the command line.",
            "</p>",
            "<p>Each entry is composed of two or three tokens,",
            "separated by semicolon (\"<code>;</code>\") characters:",
            "<ul>",
            "<li><code>&lt;expr&gt;</code>: <em>(required)</em>",
            "    column name, or expression using the",
            "    <ref id='jel'>expression language</ref>,",
            "    for the quantity to be aggregated",
            "    </li>",
            "<li><code>&lt;aggregator&gt;</code>: <em>(required)</em>",
            "    aggregation method",
            "    </li>",
            "<li><code>&lt;name&gt;</code>: <em>(optional)</em>",
            "    name of output column; if omitted,",
            "    a name based on the <code>&lt;expr&gt;</code> value",
            "    will be used",
            "    </li>",
            "</ul>",
            "</p>",
            "<p>The available <code>&lt;aggregator&gt;</code> values",
            "are as follows:",
            Aggregators.getOptionsDescription(),
            "</p>",
        } );
        aggcolsParam_.setNullPermitted( true );

        runnerParam_ = RowRunnerParameter.createScanRunnerParameter( "runner" );

        sortParam_ = new BooleanParameter( "sort" );
        sortParam_.setBooleanDefault( true );
        sortParam_.setPrompt( "Sort results by keys?" );
        sortParam_.setDescription( new String[] {
            "<p>Determines whether an attempt is made to sort the output table",
            "by the values of the <code>" + keysParam_.getName() + "</code>",
            "expressions.",
            "This may not be possible if no sort order is defined on the keys.",
            "</p>",
            "<p>In most cases such sorting will be a small overhead",
            "on the rest of the work done by this task,",
            "so the default is <code>true</code>",
            "but if ordering by key is not useful",
            "you may save some resources by setting it <code>false</code>.",
            "If no sorting is done, the output row order is undefined.",
            "</p>",
        } );

        cacheParam_ = new BooleanParameter( "cache" );
        cacheParam_.setBooleanDefault( true );
        cacheParam_.setPrompt( "Cache results?" );
        cacheParam_.setDescription( new String[] {
            "<p>Determines whether the results of the aggregation operation",
            "will be cached in random-access storage before output.",
            "This is set true by default, since accessing rows of",
            "the calculated table may be somewhat expensive,",
            "and most uses of the results will need all of the cells.",
            "But if you anticipate making only a small number of",
            "accesses to the output table cells,",
            "it could be more efficient to set this false.",
            "</p>",
        } );

        getParameterList().addAll( Arrays.asList( new Parameter<?>[] {
            keysParam_,
            aggcolsParam_,
            runnerParam_,
            sortParam_,
            cacheParam_,
        } ) );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        String[] keyExprs = keysParam_.stringsValue( env );
        String[] aggcols = aggcolsParam_.stringsValue( env );
        RowRunner runner = runnerParam_.objectValue( env );
        boolean isCache = cacheParam_.booleanValue( env );
        boolean isSort = sortParam_.booleanValue( env );
        int nagg = aggcols.length;
        AggSpec[] aggSpecs = new AggSpec[ nagg ];
        try {
            for ( int iagg = 0; iagg < nagg; iagg++ ) {
                aggSpecs[ iagg ] =
                    parseAggSpec( aggcols[ iagg ], AGGCOL_DELIM );
            }
        }
        catch ( UsageException e ) {
            throw new ParameterValueException( aggcolsParam_, e.getMessage() );
        }
        final TableProducer inProd = createInputProducer( env );
        return new TableProducer() {
            public StarTable getTable() throws IOException, TaskException {
                return aggregateRows( inProd.getTable(), keyExprs, aggSpecs,
                                      runner, isSort, isCache );
            }
        };
    }

    /**
     * Parses an aggregate specification string to an AggSpec.
     * The returned value can be passed to the
     * {@link #aggregateRows aggregateRows} method.
     * The input string is as documented of the form
     * "<code>&lt;input-expr&gt;&lt;delim&gt;&lt;aggregator-name&gt;[&lt;delim&gt;&lt;output-name&gt;]</code>".
     * If the input string cannot be so parsed, a UsageException with
     * an informative message is thrown.
     *
     * @param  aggSpecTxt   text specifying aggregated output
     * @param  delimChr    delimiter character between the three parts of
     *                     the expression
     * @return   parsed specification
     */
    public static AggSpec parseAggSpec( String aggSpecTxt, char delimChr )
            throws UsageException {
        String delimRegex = "\\Q" + delimChr + "\\E";
        String[] fields = aggSpecTxt.split( delimRegex, 3 );
        int nf = fields.length;
        assert nf > 0 && nf <= 3;
        if ( nf < 2 ) {
            String msg = new StringBuffer()
                .append( "Column specifier \"" )
                .append( aggSpecTxt )
                .append( "\" not of form " )
                .append( "<expr>" )
                .append( delimChr )
                .append( "<aggregator>" )
                .append( "[" )
                .append( delimChr )
                .append( "<name>]" )
                .toString();
            throw new UsageException( msg );
        }
        String expr = fields[ 0 ];
        String aggTxt = fields[ 1 ];
        Aggregator aggregator = Aggregators.getAggregator( aggTxt );
        if ( aggregator == null ) {
            throw new UsageException( "No such aggregation type "
                                    + "\"" + aggTxt + "\"" );
        }
        String outName = nf > 2 ? fields[ 2 ] : null;
        return new AggSpec( expr, aggregator, outName );
    }

    /**
     * Does the aggregation work.
     *
     * @param  inTable  input table
     * @param  keyExprs   input table values that define grouping
     * @param  aggSpecs   specification for output aggregated columns
     * @param  runner    row runner
     * @param  isSort    if true, output table is sorted before return
     *                   (where possible)
     * @param  isCache   if true, output table is cached before return
     * @return  output table containing aggregated values
     */
    public static StarTable aggregateRows( StarTable inTable, String[] keyExprs,
                                           AggSpec[] aggSpecs, RowRunner runner,
                                           boolean isSort, boolean isCache )
            throws IOException, TaskException {
        final int nkey = keyExprs.length;
        final int nagg = aggSpecs.length;
        final int ik0 = 0;
        final int ia0 = nkey;

        /* Create a new table that will contain all the information we need
         * from the input table; first grouping key columns, then input columns
         * for aggregated quantities. */
        String[] exprs = new String[ nkey + nagg ];
        for ( int ik = 0; ik < nkey; ik++ ) {
            exprs[ ik0 + ik ] = keyExprs[ ik ];
        }
        for ( int ia = 0; ia < nagg; ia++ ) {
            exprs[ ia0 + ia ] = aggSpecs[ ia ].getExpression();
        }
        StarTable jelTable;
        try {
            jelTable = JELTable.createJELTable( inTable, exprs );
        }
        catch ( CompilationException e ) {
            throw new ExecutionException( "Bad expression", e );
        }

        /* Prepare aggregation calculations. */
        Aggregator.Aggregation[] aggregations =
            new Aggregator.Aggregation[ nagg ];
        for ( int ia = 0; ia < nagg; ia++ ) {
            Aggregator aggregator = aggSpecs[ ia ].getAggregator();
            ValueInfo inInfo = jelTable.getColumnInfo( ia0 + ia );
            Aggregator.Aggregation aggregation =
                aggregator.createAggregation( inInfo );
            if ( aggregation == null ) {
                throw new ExecutionException( "Aggregator "
                                            + aggregator.getName()
                                            + " cannot be applied to value "
                                            + inInfo );
            }
            aggregations[ ia ] = aggregation;
        }

        /* Prepare metadata for output table.  This has a similar column
         * structure to the JEL table. */
        final ColumnInfo[] outInfos = new ColumnInfo[ nkey + nagg ];
        for ( int ik = 0; ik < nkey; ik++ ) {
            outInfos[ ik0 + ik ] = jelTable.getColumnInfo( ik0 + ik );
        }
        for ( int ia = 0; ia < nagg; ia++ ) {
            ColumnInfo cinfo =
                new ColumnInfo( aggregations[ ia ].getResultInfo() );
            String outName = aggSpecs[ ia ].getOutputName();
            if ( outName != null && outName.trim().length() > 0 ) {
                cinfo.setName( outName );
            }
            outInfos[ ia0 + ia ] = cinfo;
        }

        /* Iterate over the table rows to perform the aggregations. */
        final Map<List<Object>,Aggregator.Accumulator[]> accMap =
            runner.collect( new GroupCollector( nkey, aggregations ),
                            jelTable );

        /* Sort rows if required. */
        final Comparator<List<Object>> keyComparator;
        if ( isSort ) {
            Class<?>[] keyClazzes = new Class<?>[ nkey ];
            for ( int ik = 0; ik < nkey; ik++ ) {
                keyClazzes[ ik ] = outInfos[ ik0 + ik ].getContentClass();
            }
            boolean nullsFirst = true;
            keyComparator = getListComparator( keyClazzes, nullsFirst );
            if ( keyComparator == null ) {
                logger_.warning( "Can't sort keys (not Comparable)" );
            }
        }
        else {
            keyComparator = null;
        }
        Set<List<Object>> keySet = accMap.keySet();
        final Collection<List<Object>> keyCollection;
        if ( keyComparator == null ) {
            keyCollection = keySet;
        }
        else {
            List<List<Object>> keyList = new ArrayList<>( keySet );
            try {
                keyList.sort( keyComparator );
            }
            catch ( RuntimeException e ) {
                logger_.log( Level.WARNING, "Sort failed", e );
            }
            keyCollection = keyList;
        }

        /* Package the results as a StarTable. */
        StarTable outTable = new AbstractStarTable() {
            public int getColumnCount() {
                return outInfos.length;
            }
            public long getRowCount() {
                return accMap.size();
            }
            public ColumnInfo getColumnInfo( int icol ) {
                return outInfos[ icol ];
            }
            public RowSequence getRowSequence() {
                final Iterator<List<Object>> keyIt = keyCollection.iterator();
                return new RowSequence() {
                    List<Object> keyList_;
                    Aggregator.Accumulator[] aggaccs_;
                    public boolean next() {
                        if ( keyIt.hasNext() ) {
                            keyList_ = keyIt.next();
                            aggaccs_ = accMap.get( keyList_ );
                            return true;
                        }
                        else {
                            keyList_ = null;
                            aggaccs_ = null;
                            return false;
                        }
                    }
                    public Object[] getRow() {
                        if ( keyList_ != null ) {
                            Object[] row = new Object[ nkey + nagg ];
                            for ( int ik = 0; ik < nkey; ik++ ) {
                                row[ ik0 + ik ] = keyList_.get( ik );
                            }
                            for ( int ia = 0; ia < nagg; ia++ ) {
                                row[ ia0 + ia ] = aggaccs_[ ia ].getResult();
                            }
                            return row;
                        }
                        else {
                            throw new IllegalStateException( "No current row" );
                        }
                    }
                    public Object getCell( int ic ) {
                        if ( keyList_ != null ) {
                            return ic < nkey
                                 ? keyList_.get( ic )
                                 : aggaccs_[ ic - ia0 ].getResult();
                        }
                        else {
                            throw new IllegalStateException( "No current row" );
                        }
                    }
                    public void close() {
                    }
                };
            }
        };

        /* Return the cached or uncached table contents. */
        return isCache ? StoragePolicy.getDefaultPolicy().copyTable( outTable )
                       : outTable;
    }

    /**
     * Attempts to return a Comparator for sorting Lists.
     *
     * @param  clazzes  array of types, one for each element of lists to be
     *                  sorted
     * @param  nullsFirst  true to sort null values at the head of the list,
     *                     false to sort them at the end
     * @return   comparator, or null if not possible, for instance if
     *           classes are not comparable
     */
    private static Comparator<List<Object>>
            getListComparator( Class<?>[] clazzes, boolean nullsFirst ) {
        final int n = clazzes.length;
        final boolean nullsLast = !nullsFirst;
        for ( Class<?> clazz : clazzes ) {
            if ( ! Comparable.class.isAssignableFrom( clazz ) ) {
                return null;
            }
        }
        return new Comparator<List<Object>>() {
            public int compare( List<Object> list1, List<Object> list2 ) {
                for ( int i = 0; i < n; i++ ) {
                    int cmp = compareValues( list1.get( i ), list2.get( i ) );
                    if ( cmp != 0 ) {
                        return cmp;
                    }
                }
                return list1.equals( list2 )
                     ? 0
                     : Integer.compare( list1.hashCode(), list2.hashCode() );
            }
            @SuppressWarnings("unchecked")
            private int compareValues( Object o1, Object o2 ) {
                boolean null1 = o1 == null;
                boolean null2 = o2 == null;
                if ( null1 && null2 ) {
                    return 0;
                }
                else if ( null1 ) {
                    return nullsLast ? +1 : -1;
                }
                else if ( null2 ) {
                    return nullsLast ? -1 : +1;
                }
                else {
                    return ((Comparable<Object>) o1)
                          .compareTo( (Comparable) o2 );
                }
            }
        };
    }

    /**
     * RowCollector that performs aggregation.
     * The collected result is a map in which
     * each key is a List of the key column values defining a group of rows,
     * and each value is an array of aggregated results for that group.
     * The key is a {@link java.util.List} rather than an array since
     * <code>List</code> has the equality semantics required for use
     * as a {@link java.util.Map} key, which arrays do not.
     */
    private static class GroupCollector
            extends RowCollector<Map<List<Object>,Aggregator.Accumulator[]>> {
        private final int nkey_;
        private final int nagg_;
        private final int ia0_;
        private final Aggregator.Aggregation[] aggregations_;

        /**
         * Constructor.
         *
         * @param  nkey  number of columns in the input table,
         *               starting at the first one,
         *               which together constitute the grouping key
         * @param  aggregations   list of aggregations to be performed
         *                        for each group
         */
        GroupCollector( int nkey, Aggregator.Aggregation[] aggregations ) {
            nkey_ = nkey;
            nagg_ = aggregations.length;
            aggregations_ = aggregations;
            ia0_ = nkey;
        }

        public Map<List<Object>,Aggregator.Accumulator[]> createAccumulator(){
            return new HashMap<List<Object>,Aggregator.Accumulator[]>();
        }

        public void accumulateRows( RowSplittable rseq,
                                    Map<List<Object>,
                                        Aggregator.Accumulator[]> map )
                throws IOException {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                Object[] keys = new Object[ nkey_ ];
                for ( int ik = 0; ik < nkey_; ik++ ) {
                    keys[ ik ] = row[ ik ];
                }
                List<Object> keyList = Arrays.asList( keys );
                Aggregator.Accumulator[] aggaccs =
                    map.computeIfAbsent( keyList,
                                         k -> createAggregateAccumulators() );
                for ( int ia = 0; ia < nagg_; ia++ ) {
                    aggaccs[ ia ].submit( row[ ia0_ + ia ] );
                }
            }
        }

        public Map<List<Object>,Aggregator.Accumulator[]>
                combine( Map<List<Object>,Aggregator.Accumulator[]> map1,
                         Map<List<Object>,Aggregator.Accumulator[]> map2 ) {
            Map<List<Object>,Aggregator.Accumulator[]> mapA;
            Map<List<Object>,Aggregator.Accumulator[]> mapB;
            if ( map1.size() > map2.size() ) {
                mapA = map1;
                mapB = map2;
            }
            else {
                mapA = map2;
                mapB = map1;
            }
            for ( Map.Entry<List<Object>,Aggregator.Accumulator[]> entry :
                  mapB.entrySet() ){
                List<Object> keyList = entry.getKey();
                Aggregator.Accumulator[] aggaccsB = entry.getValue();
                Aggregator.Accumulator[] aggaccsA = mapA.get( keyList );
                if ( aggaccsA == null ) {
                    mapA.put( keyList, aggaccsB );
                }
                else {
                    for ( int ia = 0; ia < nagg_; ia++ ) {
                        aggaccsA[ ia ].add( aggaccsB[ ia ] );
                    }
                }
            }
            return mapA;
        }

        /**
         * Returns an array of new Aggregator.Accumulators,
         * suitable for accumulating all the aggregated values.
         *
         * @return  new accumulator array
         */
        private Aggregator.Accumulator[] createAggregateAccumulators() {
            Aggregator.Accumulator[] aggaccs =
                new Aggregator.Accumulator[ nagg_ ];
            for ( int ia = 0; ia < nagg_; ia++ ) {
                aggaccs[ ia ] = aggregations_[ ia ].createAccumulator();
            }
            return aggaccs;
        }
    }

    /**
     * Defines the aggregation that should be done to form an output column.
     */
    public static class AggSpec {

        private final String expr_;
        private final Aggregator aggregator_;
        private final String outName_;

        /**
         * Constructor.
         *
         * @param  expr  expression giving quantity to be aggregated
         * @param  aggregator  aggregation method
         * @param  outName   output column name, or null for default
         */
        public AggSpec( String expr, Aggregator aggregator, String outName ) {
            expr_ = expr;
            aggregator_ = aggregator;
            outName_ = outName;
        }

        /**
         * Returns an expression giving the quantity to be aggregated.
         *
         * @return  JEL expression or column name
         */
        public String getExpression() {
            return expr_;
        }

        /**
         * Returns the aggregation method.
         *
         * @return  aggregator
         */
        public Aggregator getAggregator() {
            return aggregator_;
        }

        /**
         * Returns the output column name, if specified.
         *
         * @return  user-specified output column name, or null
         */
        public String getOutputName() {
            return outName_;
        }
    }
}
