package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * Parameter for table output mode.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class OutputModeParameter extends Parameter
                                 implements TableConsumerParameter,
                                            ExtraParameter {

    private ProcessingMode mode_;
    private TableConsumer consumer_;

    public OutputModeParameter( String name ) {
        super( name );
        setPrompt( "Output mode" );
        setDefault( "out" );
        setUsage( "<out-mode> <mode-args>" );

        setDescription( new String[] {
            "The mode in which the result table will be output.",
            "The default mode is <code>out</code>, which means that it",
            "will be written as a new table to disk or elsewhere.",
            "However, there are other possibilities, which correspond",
            "to uses to which a table can be put other than outputting it,",
            "such as displaying metadata, calculating statistics,",
            "or populating a table in an SQL database.",
            "For some values of this parameter, additional parameters",
            "are required to determine the exact behaviour.",
            "Use the <code>-help=" + getName() + "</code> flag",
            "or see <ref id=\"outModes\"/> for more information.",
        } );
    }

    public String getExtraUsage( TableEnvironment env ) {
        ObjectFactory modeFactory = Stilts.getModeFactory();
        String[] names = modeFactory.getNickNames();
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "   Available modes, with associated arguments:\n" );
        for ( int i = 0; i < names.length; i++ ) {
            String name = names[ i ];
            try {
                ProcessingMode mode = (ProcessingMode)
                                      modeFactory.createObject( name );
                StringBuffer line = new StringBuffer()
                    .append( "      " )
                    .append( "-mode=" )
                    .append( name );
                String pad = line.toString().replaceAll( ".", " " );
                Parameter[] params = mode.getAssociatedParameters();
                for ( int j = 0; j < params.length; j++ ) {
                    Parameter param = params[ j ];
                    String word = 
                        " -" + param.getName() + "=" + param.getUsage();
                    if ( line.length() + word.length() > 78 ) {
                        sbuf.append( line )
                            .append( '\n' );
                        line = new StringBuffer( pad );
                    }
                    line.append( word );
                }
                sbuf.append( line )
                    .append( '\n' );
            }
            catch ( LoadException e ) {
                if ( env.isDebug() ) {
                    sbuf.append( "    ( " )
                        .append( "mode=" )
                        .append( name )
                        .append( " - not available: " )
                        .append( e )
                        .append( " )" )
                        .append( '\n' );
                }
            }
        }
        sbuf.append( '\n' )
            .append( new OutputFormatParameter( "ofmt" )
                    .getExtraUsage( env ) );
        return sbuf.toString();
    }

    public void setValueFromString( Environment env, String stringval ) 
            throws TaskException {
        ObjectFactory modeFactory = Stilts.getModeFactory();
        if ( ! modeFactory.isRegistered( stringval ) ) {
            throw new ParameterValueException( this, "No such mode: " 
                                                    + stringval );
        }
        try {
            ProcessingMode mode = (ProcessingMode)
                                  modeFactory.createObject( stringval );
            consumer_ = mode.createConsumer( env );
            mode_ = mode;
        }
        catch ( LoadException e ) {
            throw new ParameterValueException( this, "Mode " + stringval +
                                               " unavailable - " + e, e );
        }
        super.setValueFromString( env, stringval );
    }

    /**
     * Returns a TableConsumer which corresponds to the value of this
     * parameter.
     *
     * @param  env  execution environment
     */
    public TableConsumer consumerValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return consumer_;
    }
}
