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
    }

    public String getUsage() {
        return "<out-mode> <mode-args>";
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
                    .append( "mode=" )
                    .append( name );
                String pad = line.toString().replaceAll( ".", " " );
                Parameter[] params = mode.getAssociatedParameters();
                for ( int j = 0; j < params.length; j++ ) {
                    Parameter param = params[ j ];
                    String word = 
                        " " + param.getName() + "=" + param.getUsage();
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
            .append( new OutputFormatParameter( "outfmt" )
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
