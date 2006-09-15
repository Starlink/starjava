package uk.ac.starlink.ttools.task;

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
     * @param   mapper   table mapper
     * @param   outMode  output mode
     * @param   useInFilter  whether preprocessing filters are permitted
     * @param   useOutFilter  whether a postprocessing filter is permitted
     */
    public HomogeneousMapperTask( TableMapper mapper, ProcessingMode outMode,
                                  boolean useInFilter, boolean useOutFilter ) {
        super( mapper, outMode, useOutFilter );
        List paramList = new ArrayList();

        /* Input tables parameter. */
        inTablesParam_ = new InputTablesParameter( "in" );
        inTablesParam_.setPosition( 1 );
        inTablesParam_.setUsage( "<table> [<table> ...]" );
        inTablesParam_.setPrompt( "Location of input table(s)" );
        paramList.add( inTablesParam_ );

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
        paramList.addAll( Arrays.asList( super.getParameters() ) );
        setParameters( (Parameter[]) paramList.toArray( new Parameter[ 0 ] ) );
    }

    public InputSpec[] getInputSpecs( Environment env )
            throws TaskException {
        ProcessingStep[] steps = inFilterParam_ == null
                               ? null
                               : inFilterParam_.stepsValue( env );
        StarTable[] tables = inTablesParam_.tablesValue( env );
        int nIn = tables.length;
        InputSpec[] specs = new InputSpec[ nIn ];
        for ( int i = 0; i < nIn; i++ ) {
            specs[ i ] = new InputSpec( tables[ i ], steps );
        }
        return specs;
    }
}
