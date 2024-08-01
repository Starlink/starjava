package uk.ac.starlink.ttools.cea;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.InputStreamParameter;
import uk.ac.starlink.task.MultiParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.join.MatchEngineParameter;
import uk.ac.starlink.ttools.task.AbstractInputTableParameter;
import uk.ac.starlink.ttools.task.InputFormatParameter;
import uk.ac.starlink.ttools.task.OutputFormatParameter;
import uk.ac.starlink.ttools.task.OutputModeParameter;
import uk.ac.starlink.ttools.task.OutputTableParameter;

/**
 * Represents a parameter of a CEA task.
 *
 * @author   Mark Taylor
 * @since    17 Mar 2006
 */
public class CeaParameter {

    private String name_;
    private String description_;
    private String summary_;
    private String type_;
    private String dflt_;
    private boolean isOutput_;
    private boolean isRef_;
    private boolean isMulti_;
    private boolean isNullPermitted_;
    private String[] options_;

    private static StarTableFactory tableFactory_ = new StarTableFactory();
    private static StarTableOutput tableOutput_ = new StarTableOutput();

    /**
     * Constructor.
     *
     * @param   taskParam   parameter within the ttools/task parameter system
     */
    @SuppressWarnings("this-escape")
    public CeaParameter( Parameter<?> taskParam ) {
        name_ = taskParam.getName();
        description_ = taskParam.getDescription();
        summary_ = taskParam.getPrompt();
        dflt_ = taskParam.getStringDefault();
        isNullPermitted_ = taskParam.isNullPermitted();
        type_ = "text";
        if ( taskParam instanceof OutputTableParameter ||
             taskParam instanceof OutputStreamParameter ) {
            setOutput( true );
        }
        if ( taskParam instanceof AbstractInputTableParameter ||
             taskParam instanceof InputStreamParameter ) {
            isRef_ = true;
            dflt_ = null;
            truncateDescription();
        }
        if ( taskParam instanceof MultiParameter ) {
            isMulti_ = true;
        }
        if ( taskParam instanceof BooleanParameter ) {
            type_ = "boolean";
        }
        if ( taskParam instanceof DoubleParameter ) {
            type_ = "double";
        }

        if ( taskParam instanceof ChoiceParameter ) {
            options_ = ((ChoiceParameter<?>) taskParam).getOptionNames();
        }
        else if ( taskParam instanceof InputFormatParameter ) {
            List<String> opts = new ArrayList<String>();
            opts.add( StarTableFactory.AUTO_HANDLER );
            opts.addAll( tableFactory_.getKnownFormats() );
            options_ = opts.toArray( new String[ 0 ] );
        }
        else if ( taskParam instanceof OutputFormatParameter ) {
            List<String> opts =
                new ArrayList<String>( tableOutput_.getKnownFormats() );
            opts.remove( "jdbc" );
            options_ = opts.toArray( new String[ 0 ] );

            /* Auto mode won't work because the output filename is munged
             * by CEA. */
            assert ! opts.contains( StarTableOutput.AUTO_HANDLER );
            dflt_ = null;
        }
        else if ( taskParam instanceof MatchEngineParameter ) {
            options_ = MatchEngineParameter.getExampleValues();
        }
        else if ( taskParam instanceof OutputModeParameter ) {

            /* Ouput modes other than "out" are too complicated to deal
             * with in CEA. */
            assert false;
        }
    }

    /**
     * Returns this parameter's name.
     *
     * @return   parameter name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns this parameter's description.
     *
     * @return  parameter description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns a one-line summary of this parameter's purpose.
     *
     * @return  summary
     */
    public String getSummary() {
        return summary_;
    }

    /**
     * Returns the CEA type (CmdLineParameterDef/type) of this parameter.
     *
     * @return  CEA type
     */
    public String getType() {
        return type_;
    }

    /**
     * Returns whether this parameter is an output parameter in the CEA sense.
     *
     * @return  true iff this parameter is for output
     */
    public boolean isOutput() {
        return isOutput_;
    }

    public void setOutput( boolean isOutput ) {
        isOutput_ = isOutput;
        if ( isOutput ) {
            isRef_ = true;
            dflt_ = null;
            isNullPermitted_ = false;
            truncateDescription();
        }
    }

    public void setRef( boolean isRef ) {
        isRef_ = isRef;
        if ( isRef ) {
            dflt_ = null;
            isNullPermitted_ = false;
            truncateDescription();
        }
    }

    /**
     * Returns whether this parameter is a CEA 'reference' parameter 
     * (points to a file).
     *
     * @return   true iff this is a file reference parameter
     */
    public boolean isRef() {
        return isRef_;
    }

    /**
     * Returns whether this parameter can be specified multiple times on
     * the command line.
     *
     * @return  true iff this parameter is multiple
     */
    public boolean isMulti() {
        return isMulti_;
    }

    /**
     * Returns whether null is a permitted value for this parameter.
     *
     * @return  true iff null is legal
     */
    public boolean isNullPermitted() {
        return isNullPermitted_;
    }

    /**
     * Returns the default value of this parameter if there is one.
     *
     * @return  default value, or null
     */
    public String getDefault() {
        return dflt_;
    }

    /**
     * Returns an array of permitted options if there is one.
     *
     * @return   array of sole permitted values, or null
     */
    public String[] getOptions() {
        return options_;
    }

    /**
     * Truncates this parameter's description to its first sentence.
     * This is a hack which (with luck) has the effect of cutting out
     * bits of the parameter description which are not relevant to CEA use.
     */
    public void truncateDescription() {
        int dot1 = description_.indexOf( '.' );
        description_ = dot1 > 0 ? description_.substring( 0, dot1 + 1 )
                                : description_;
        if ( description_.startsWith( "<p>" ) ) {
            description_ = description_.substring( 3 );
        }
    }
}
