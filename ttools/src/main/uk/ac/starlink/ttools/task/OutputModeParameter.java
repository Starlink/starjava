package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

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

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public OutputModeParameter( String name ) {
        super( name );
        setPrompt( "Output mode" );
        setDefault( "out" );
        setUsage( "<out-mode> <mode-args>" );

        StringBuffer sbuf = new StringBuffer();
        String[] modeNames = Stilts.getModeFactory().getNickNames();
        sbuf.append( "<ul>\n" );
        for ( int i = 0; i < modeNames.length; i++ ) {
            sbuf.append( "<li><code>" )
                .append( modeNames[ i ] )
                .append( "</code></li>" )
                .append( '\n' );
        }
        sbuf.append( "</ul>\n" );
        String modeList = sbuf.toString();
        setDescription( new String[] {
            "<p>The mode in which the result table will be output.",
            "The default mode is <code>out</code>, which means that",
            "the result will be written as a new table to disk or elsewhere,",
            "as determined by the <code>out</code> and <code>ofmt</code>",
            "parameters.",
            "However, there are other possibilities, which correspond",
            "to uses to which a table can be put other than outputting it,",
            "such as displaying metadata, calculating statistics,",
            "or populating a table in an SQL database.",
            "For some values of this parameter, additional parameters",
            "(<code>&lt;mode-args&gt;</code>)",
            "are required to determine the exact behaviour.",
            "</p>",
            "<p>Possible values are",
            modeList,
            "Use the <code>help=" + getName() + "</code> flag",
            "or see <ref id=\"outModes\"/> for more information.",
            "</p>",
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
                sbuf.append( getModeUsage( names[ i ], "      " ) );
            }
            catch ( LoadException e ) {
                if ( env.isDebug() ) {
                    sbuf.append( "    ( " )
                        .append( getName() )
                        .append( '=' )
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

    /**
     * Returns a usage message for a given processing mode.
     *
     * @param  modeName  name of the mode
     * @param  prefix  prefix for each line of output (e.g. padding spaces)
     * @return   usage message
     */
    public String getModeUsage( String modeName, String prefix )
            throws LoadException {
        ProcessingMode mode = (ProcessingMode)
                              Stilts.getModeFactory().createObject( modeName );
        StringBuffer sbuf = new StringBuffer();
        StringBuffer line = new StringBuffer()
            .append( prefix )
            .append( getName() )
            .append( '=' )
            .append( modeName );
        String pad = prefix + line.substring( prefix.length() )
                                  .toString().replaceAll( ".", " " );
        Parameter[] params = mode.getAssociatedParameters();
        for ( int i = 0; i < params.length; i++ ) {
            Parameter param = params[ i ];
            String word = " " + param.getName() + "=" + param.getUsage();
            if ( line.length() + word.length() > 78 ) {
                sbuf.append( line )
                    .append( '\n' );
                line = new StringBuffer( pad );
            }
            line.append( word );
        }
        sbuf.append( line )
            .append( '\n' );
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

    /**
     * Sets the value directly from a given TableConsumer.
     *
     * @param  consumer  table consumer
     */
    public void setValueFromConsumer( TableConsumer consumer ) {
        consumer_ = consumer;
        setStringValue( consumer.toString() );
        setGotValue( true );
    }
}
