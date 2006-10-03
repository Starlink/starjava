package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * MapperTask which has a variable number of input tables,
 * treating them all the same as each other.
 * A single, possibly repeated, parameter is used to specify any 
 * number of input tables, and a single (not repeated) filter 
 * parameter is used to specify a processing pipeline which operates
 * identically on all the inputs.
 *
 * @author   Mark Taylor
 * @since    14 Sep 2006
 */
public class HomogeneousMapperTask extends MapperTask {

    public InputTablesParameter inTablesParam_;
    public FilterParameter inFilterParam_;

    /**
     * Constructor.
     *
     * @param   purpose  one-line description of the purpose of the task
     * @param   outMode  output mode
     * @param   useOutFilter  whether a postprocessing filter is permitted
     * @param   mapper   table mapper
     * @param   useInFilter  whether preprocessing filters are permitted
     */
    public HomogeneousMapperTask( String purpose, ProcessingMode outMode,
                                  boolean useOutFilter, TableMapper mapper,
                                  boolean useInFilter) {
        super( purpose, outMode, useOutFilter, mapper );
        List paramList = new ArrayList();

        /* Input tables parameter. */
        inTablesParam_ = new InputTablesParameter( "in" );
        inTablesParam_.setUsage( "<table> [<table> ...]" );
        inTablesParam_.setPrompt( "Location of input table(s)" );
        paramList.add( inTablesParam_ );
        paramList.add( inTablesParam_.getFormatParameter() );
        paramList.add( inTablesParam_.getStreamParameter() );

        /* Input filter parameter. */
        if ( useInFilter ) {
            inFilterParam_ = new FilterParameter( "icmd" );
            inFilterParam_.setPrompt( "Processing command(s) "
                                    + "for each input table" );
            inFilterParam_.setDescription( new String[] {
                "Commands which will operate on each of the input tables,",
                "before any other processing takes place.",
                inFilterParam_.getDescription(),
            } );
            paramList.add( inFilterParam_ );
        }
        else {
            inFilterParam_ = null;
        }

        /* Store full parameter list. */
        getParameterList().addAll( 0, paramList );
    }

    public InputTableSpec[] getInputSpecs( final Environment env )
            throws TaskException {
        ProcessingStep[] steps = inFilterParam_ == null
                               ? null
                               : inFilterParam_.stepsValue( env );
        String[] locs = inTablesParam_.stringsValue( env );
        TableProducer[] tprods = inTablesParam_.tablesValue( env );
        int nIn = tprods.length;
        InputTableSpec[] specs = new InputTableSpec[ nIn ];
        for ( int i = 0; i < nIn; i++ ) {
            final TableProducer tprod = tprods[ i ];
            specs[ i ] = new InputTableSpec( locs[ i ], steps ) {
                public StarTable getInputTable() throws TaskException {
                    try {
                        return tprod.getTable();
                    }
                    catch ( IOException e ) {
                        throw new TaskException( e.getMessage(), e );
                    }
                }
            };
        }
        return specs;
    }
}
