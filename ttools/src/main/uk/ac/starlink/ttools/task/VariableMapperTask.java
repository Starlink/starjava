package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * MapperTask which allows a variable number of input tables, fixed at
 * runtime using an integer parameter.  Each input table has its own
 * input format and filter parameters and so on.
 *
 * <p>This task constructs its list of parameters on the fly within its
 * {@link #createExecutable} method.  The {@link #getParameters} method 
 * returns a list which is suitable for documentation purposes only.
 * Execution environments which need the <code>getParameters</code>
 * call to return the actual list of parameters to be used may not
 * therefore be able to work with instances of this class.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2006
 */
public class VariableMapperTask extends MapperTask {

    private final IntegerParameter ninParam_;
    private boolean useInFilters_;

    /**
     * Constructor.
     *
     * @param   purpose  one-line description of the purpose of the task
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useOutFilter allow specification of filters for output table
     * @param   mapper   object which defines mapping transformation
     * @param   useInFilters  whether to use input filter parameters
     */
    public VariableMapperTask( String purpose, ProcessingMode outMode,
                               boolean useOutFilter, TableMapper mapper,
                               boolean useInFilters ) {
        super( purpose, outMode, useOutFilter, mapper );
        useInFilters_ = useInFilters;
        List paramList = new ArrayList();

        /* Prepare list of nominal per-table input parameters.  Note that 
         * these parameters are not used for actually getting values, they
         * serve only a documentation purpose - the parameters used for
         * getting values is constructed in getInputSpecs() when the
         * execution environment is available, and hence we know how
         * many of them there will be. */
        String numLabel = "N";
        InputTableParameter inParam = getInputParameter( numLabel );
        paramList.add( inParam.getFormatParameter() );
        paramList.add( inParam );
        FilterParameter filterParam;
        if ( useInFilters ) {
            filterParam = getFilterParameter( numLabel );
            paramList.add( filterParam );
        }
        else {
            filterParam = null;
        }

        /* Document the nin parameter. */
        Parameter[] inParams =
            (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < inParams.length; i++ ) {
            String pName = inParams[ i ].getName();
            assert pName.endsWith( numLabel );
            if ( i > 0 ) {
                sbuf.append( i == inParams.length - 1 ? " and " : ", " );
            }
            sbuf.append( "<code>" )
                .append( pName )
                .append( "</code>" );
        }
        String inParamListing = sbuf.toString();
        ninParam_ = new IntegerParameter( "nin" );
        ninParam_.setMinimum( 1 );
        ninParam_.setUsage( "<count>" );
        ninParam_.setPrompt( "Number of input tables" );
        ninParam_.setDescription( new String[] {
            "<p>The number of input tables for this task.",
            "For each of the input tables " + numLabel,
            "there will be associated parameters",
            inParamListing + ".",
            "</p>",
        } );
        paramList.add( 0, ninParam_ );

        /* Update task parameter list. */
        getParameterList().addAll( 0, paramList );
    }

    protected InputTableSpec[] getInputSpecs( Environment env )
            throws TaskException {
        int nin = ninParam_.intValue( env );
        InputTableSpec[] inSpecs = new InputTableSpec[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            String label = String.valueOf( i + 1 );
            InputTableParameter inParam = getInputParameter( label );
            StarTable table = inParam.tableValue( env );
            String tName = inParam.stringValue( env );
            ProcessingStep[] steps =
                useInFilters_ ? getFilterParameter( label ).stepsValue( env )
                              : null;
            inSpecs[ i ] = InputTableSpec.createSpec( tName, steps, table );
        }
        return inSpecs;
    }

    /**
     * Constructs an input table parameter with a given distinguishing label.
     *
     * @param  label  input identifier - typically "1", "2", etc
     * @return  new input parameter
     */
    private static InputTableParameter getInputParameter( String label ) {
        InputTableParameter inParam = new InputTableParameter( "in" + label );
        inParam.setUsage( "<table" + label + ">" );
        inParam.setPrompt( "Location of input table " + label );
        inParam.setDescription( inParam.getDescription()
                               .replaceFirst( "the input table",
                                              "input table #" + label ) );
        InputFormatParameter fmtParam = inParam.getFormatParameter();
        fmtParam.setDescription( fmtParam.getDescription()
                                .replaceFirst( "the input table",
                                               "input table #" + label ) );
        return inParam;
    }

    /**
     * Constructs an input filter parameter with a given distinguishing label.
     *
     * @param  label  input identifier - typically "1", "2", etc
     * @return  new filter parameter
     */
    private static FilterParameter getFilterParameter( String label ) {
        FilterParameter filterParam = new FilterParameter( "icmd" + label );
        filterParam.setPrompt( "Processing command(s) for input table "
                             + label );
        filterParam.setDescription( new String[] {
            "<p>Commands to operate on input table #" + label + ",",
            "before any other processing takes place.",
            "</p>",
            filterParam.getDescription(),
        } );
        return filterParam;
    }
}
