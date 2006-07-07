package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * Task which maps one or more input tables to an output table.
 * This class provides methods to acquire the table sources and sink;
 * any actual transformation work is done by a separate 
 * {@link TableMapper} object.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class MapperTask implements Task {

    private final TableMapper mapper_;
    private final int nIn_;
    private final InputTableParameter[] inTableParams_;
    private final FilterParameter[] inFilterParams_;
    private final FilterParameter outFilterParam_;
    private final ProcessingMode outMode_;
    private final Parameter[] params_;

    /**
     * Constructor.
     *
     * @param   mapper   object which defines mapping transformation
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useInFilters  allow specification of filters for input tables
     * @param   useOutFilters allow specification of filters for output tables
     */
    public MapperTask( TableMapper mapper, ProcessingMode outMode,
                       boolean useInFilters, boolean useOutFilters ) {
        mapper_ = mapper;
        nIn_ = mapper.getInCount();
        List paramList = new ArrayList();

        /* Input parameters. */
        inTableParams_ = new InputTableParameter[ nIn_ ];
        if ( nIn_ == 1 ) {
            inTableParams_[ 0 ] = new InputTableParameter( "in" );
            inTableParams_[ 0 ].setUsage( "<table>" );
            inTableParams_[ 0 ].setPrompt( "Location of input table" );
            paramList.add( inTableParams_[ 0 ].getFormatParameter() );
            paramList.add( inTableParams_[ 0 ].getStreamParameter() );
            paramList.add( inTableParams_[ 0 ] );
        }
        else {
            for ( int i = 0; i < nIn_; i++ ) {
                int i1 = i + 1;
                String ord = getOrdinal( i1 );
                InputTableParameter inParam =
                    new InputTableParameter( "in" + i1 );
                inTableParams_[ i ] = inParam;
                inParam.setUsage( "<table" + i1 + ">" );
                inParam.setPrompt( "Location of " + ord + " input table" );
                inParam.setDescription( inParam.getDescription()
                                       .replaceFirst( "the input table",
                                                      "the " + ord + 
                                                      " input table" ) );
                InputFormatParameter fmtParam = 
                    inTableParams_[ i ].getFormatParameter();
                fmtParam.setDescription( fmtParam.getDescription()
                                        .replaceFirst( "the input table",
                                                       "the " + ord + 
                                                       " input table" ) );
                paramList.add( fmtParam );
                paramList.add( inParam );
            }
        }
        for ( int i = 0; i < nIn_; i++ ) {
            inTableParams_[ i ].setPosition( i + 1 );
        }

        /* Input filters. */
        if ( useInFilters ) {
            inFilterParams_ = new FilterParameter[ nIn_ ];
            if ( nIn_ == 1 ) {
                FilterParameter fp = new FilterParameter( "icmd" );
                inFilterParams_[ 0 ] = fp;
                fp.setPrompt( "Processing command(s) for input table" );
                fp.setDescription( new String[] {
                    "Commands to operate on the input table,",
                    "before any other processing takes place.",
                    fp.getDescription(),
                } );
            }
            else {
                for ( int i = 0; i < nIn_; i++ ) {
                    int i1 = i + 1;
                    FilterParameter fp = new FilterParameter( "icmd" + i1 );
                    inFilterParams_[ i ] = fp;
                    fp.setPrompt( "Processing command(s) for input table "
                                + i1 );
                    fp.setDescription( new String[] {
                        "Commands to operate on the",
                        getOrdinal( i1 ) + " input table, before any other",
                        "processing takes place.", 
                        fp.getDescription(),
                    } );
                }
            }
            for ( int i = 0; i < nIn_; i++ ) {
                paramList.add( inFilterParams_[ i ] );
            }
        }
        else {
            inFilterParams_ = null;
        }

        /* Processing parameters. */
        paramList.addAll( Arrays.asList( mapper.getParameters() ) );

        /* Output filter. */
        if ( useOutFilters ) {
            outFilterParam_ = new FilterParameter( "ocmd" );
            outFilterParam_.setPrompt( "Processing command(s) " 
                                     + "for output table" );
            outFilterParam_.setDescription( new String[] {
                "Commands to operate on the output table,",
                "after all other processing has taken place.",
                outFilterParam_.getDescription(),
            } );
            paramList.add( outFilterParam_ );
        }
        else {
            outFilterParam_ = null;
        }

        /* Output parameters. */
        outMode_ = outMode;
        paramList.addAll( Arrays.asList( outMode.getAssociatedParameters() ) );

        /* Fix output parameter list. */
        params_ = (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {

        /* Get raw input tables. */
        final StarTable[] inTables = new StarTable[ nIn_ ];
        for ( int i = 0; i < nIn_; i++ ) {
            inTables[ i ] = inTableParams_[ i ].tableValue( env );
        }

        /* Get a sequence of pre-processing steps for each input table. */
        final ProcessingStep[][] inSteps = new ProcessingStep[ nIn_ ][];
        for ( int i = 0; i < nIn_; i++ ) {
            inSteps[ i ] = inFilterParams_ != null
                         ? inFilterParams_[ i ].stepsValue( env )
                         : new ProcessingStep[ 0 ];
        }

        /* Get the mapping which defines the actual processing done by
         * this task. */
        final TableMapping mapping = mapper_.createMapping( env );

        /* Get a sequence of post-processing steps for the output table. */
        final ProcessingStep[] outSteps = outFilterParam_ != null
                                        ? outFilterParam_.stepsValue( env )
                                        : new ProcessingStep[ 0 ];

        /* Get the table consumer, which defines the output table's final
         * destination. */
        final TableConsumer baseConsumer = outMode_.createConsumer( env );

        /* Construct a consumer which will combine the post-processing
         * and the final disposal. */
        final TableConsumer consumer = new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                for ( int i = 0; i < outSteps.length; i++ ) {
                    table = outSteps[ i ].wrap( table );
                }
                baseConsumer.consume( table );
            }
        };

        /* Check unused arguments for things we can write helpful messages
         * about. */
        checkUnused( env );

        /* Construct and return an executable which will do all the work. */
        return new Executable() {
            public void execute() throws IOException, TaskException {

                /* Perform any required pre-filtering of input tables. */
                for ( int i = 0; i < nIn_; i++ ) {
                    for ( int j = 0; j < inSteps[ i ].length; j++ ) {
                        inTables[ i ] = inSteps[ i ][ j ].wrap( inTables[ i ] );
                    }
                }

                /* Finally, execute the pipeline. */
                mapping.mapTables( inTables, new TableConsumer[] { consumer } );
            }
        };
    }

    /**
     * Returns this task's Mapper object.
     *
     * @return  mapper
     */
    public TableMapper getMapper() {
        return mapper_;
    }

    /**
     * Returnst this task's output mode.
     *
     * @return  output mode
     */
    public ProcessingMode getOutputMode() {
        return outMode_;
    }

    /**
     * Checks the unused words in the environment in case we can write any
     * useful messages.
     *
     * @param  env  execution environment
     * @throws  TaskException   if there's trouble
     */
    private void checkUnused( Environment env ) throws TaskException {
        if ( env instanceof LineEnvironment ) {
            String[] unused = ((LineEnvironment) env).getUnused();
            for ( int i = 0; i < unused.length; i++ ) {
                String word = unused[ i ];
                if ( word.startsWith( "out=" ) || word.startsWith( "ofmt=" ) ) {
                    throw new UsageException(
                        word + ": out and ofmt parameters can only be used " +
                        "when omode=out" );
                }
            }
        }
    }

    /**
     * Returns the string representation of the ordinal number corresponding
     * to a given integer.
     *
     * @param  i  number
     * @return   ordinal
     */
    private static String getOrdinal( int i ) {
        switch ( i ) {
            case 1: return "first";
            case 2: return "second";
            case 3: return "third";
            case 4: return "fourth";
            case 5: return "fifth";
            default: return "next";
        }
    }
}
