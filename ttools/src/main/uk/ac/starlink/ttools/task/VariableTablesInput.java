package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.filter.ProcessingStep;

/**
 * TablesInput which allows a variable number of input tables, fixed at
 * runtime using an integer parameter.  Each input table has its own
 * input format and filter parameters and so on.
 *
 * <p>This object constructs its list of parameters on the fly when the
 * environment is available (within {@link #getInputSpecs}).
 * The {@link #getParameters} method returns a list which is suitable
 * for documentation purposes only.
 * Execution environments which need the <code>getParameters</code>
 * call to return the actual list of parameters to be used may not
 * therefore be able to work with instances of this class.
 * 
 * @author   Mark Taylor
 * @since    1 Jul 2010
 */
public class VariableTablesInput implements TablesInput {

    private final IntegerParameter ninParam_;
    private final boolean useInFilters_;
    private final Parameter[] params_;
    private final Naming inNaming_;

    /**
     * Suffix applied to parameters to denote a symbolic variable part of
     * the name, where the symbolic part can take integer values.
     */
    public static final String NUM_SUFFIX = "N";

    /**
     * Constructs an input tables parameter with a default base name.
     *
     * @param   useInFilters  whether to use input filter parameters
     */
    public VariableTablesInput( boolean useInFilters ) {
        this( useInFilters, "in", "input" );
    }

    /**
     * Constructs an input tables parameter with a given base name.
     *
     * @param   useInFilters  whether to use input filter parameters
     * @param   inName  base name for parameter
     * @param   inWord  base word describing parameter content for textual
     *                  descriptions
     */
    public VariableTablesInput( boolean useInFilters,
                                final String inName, final String inWord ) {
        inNaming_ = new Naming() {{pName_ = inName; pWord_ = inWord;}};
        useInFilters_ = useInFilters;
        List<Parameter> paramList = new ArrayList<Parameter>();

        /* Prepare list of nominal per-table input parameters.  Note that
         * these parameters are not used for actually getting values, they
         * serve only a documentation purpose - the parameters used for
         * getting values is constructed in getInputSpecs() when the
         * execution environment is available, and hence we know how
         * many of them there will be. */
        String numLabel = NUM_SUFFIX;
        InputTableParameter inParam =
            createInputParameter( numLabel, inNaming_ );
        paramList.add( inParam.getFormatParameter() );
        paramList.add( inParam );
        FilterParameter filterParam;
        if ( useInFilters ) {
            filterParam = getFilterParameter( numLabel, inNaming_ );
            paramList.add( filterParam );
        }
        else {
            filterParam = null;
        }

        /* Document the nin parameter. */
        Parameter[] inParams = paramList.toArray( new Parameter[ 0 ] );
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
        ninParam_ = new IntegerParameter( "n" + inName );
        ninParam_.setMinimum( 1 );
        ninParam_.setUsage( "<count>" );
        ninParam_.setPrompt( "Number of " + inWord + " tables" );
        ninParam_.setDescription( new String[] {
            "<p>The number of " + inWord + " tables for this task.",
            "For each of the " + inWord + " tables " + numLabel,
            "there will be associated parameters",
            inParamListing + ".",
            "</p>",
        } );
        paramList.add( 0, ninParam_ );

        params_ = paramList.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns the parameter which contains the number of input tables that
     * the user wants to use.
     *
     * @return  count parameter
     */
    public IntegerParameter getCountParam() {
        return ninParam_;
    }

    public Parameter[] getParameters() {
        return params_;
    }

    public InputTableSpec[] getInputSpecs( Environment env )
            throws TaskException {
        int nin = ninParam_.intValue( env );
        InputTableSpec[] inSpecs = new InputTableSpec[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            String label = String.valueOf( i + 1 );
            InputTableParameter inParam =
                createInputParameter( label, inNaming_ );
            StarTable table = inParam.tableValue( env );
            String tName = inParam.stringValue( env );
            ProcessingStep[] steps =
                useInFilters_ ? getFilterParameter( label, inNaming_ )
                               .stepsValue( env )
                              : null;
            inSpecs[ i ] = InputTableSpec.createSpec( tName, steps, table );
        }
        return inSpecs;
    }

    /**
     * Constructs an input table parameter with a given distinguishing label.
     *
     * @param  label  input identifier - typically "1", "2", etc
     * @param  naming  parameter naming policy
     * @return  new input parameter
     */
    private static InputTableParameter
            createInputParameter( String label, Naming naming ) {
        InputTableParameter inParam =
            new InputTableParameter( naming.pName_ + label );
        inParam.setUsage( "<table" + label + ">" );
        inParam.setPrompt( "Location of " + naming.pWord_ + " table " + label );
        inParam.setDescription( inParam.getDescription()
                               .replaceFirst( "the input table",
                                              naming.pWord_
                                            + " table #" + label ) );
        InputFormatParameter fmtParam = inParam.getFormatParameter();
        fmtParam.setDescription( fmtParam.getDescription()
                                .replaceFirst( "the input table",
                                               naming.pWord_
                                             + " table #" + label ) );
        return inParam;
    }

    /**
     * Constructs an input filter parameter with a given distinguishing label.
     *
     * @param  label  input identifier - typically "1", "2", etc
     * @param  naming  parameter naming policy
     * @return  new filter parameter
     */
    private static FilterParameter getFilterParameter( String label,
                                                       Naming naming ) {
        FilterParameter filterParam =
            new FilterParameter( naming.pName_.charAt( 0 ) + "cmd" + label );
        filterParam.setPrompt( "Processing command(s) for " + naming.pWord_
                             + " table " + label );
        filterParam.setDescription( new String[] {
            "<p>Commands to operate on " + naming.pWord_,
            "table #" + label + ",",
            "before any other processing takes place.",
            "</p>",
            filterParam.getDescription(),
        } );
        return filterParam;
    }

    /**
     * Parameter naming policy.
     */
    private static class Naming {

        /** Parameter base name. */
        String pName_;

        /** Word for describing the parameter content in textual description. */
        String pWord_;
    }
}
