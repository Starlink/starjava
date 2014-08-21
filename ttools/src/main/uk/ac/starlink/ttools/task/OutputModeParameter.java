package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ObjectFactoryParameter;
import uk.ac.starlink.task.Parameter;
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
public class OutputModeParameter
             extends ObjectFactoryParameter<ProcessingMode>
             implements TableConsumerParameter, ExtraParameter {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public OutputModeParameter( String name ) {
        super( name, Stilts.getModeFactory() );
        setPrompt( "Output mode" );
        setDefault( "out" );
        setUsage( "<out-mode> <mode-args>" );

        StringBuffer sbuf = new StringBuffer();
        String[] modeNames = getObjectFactory().getNickNames();
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
        String[] names = getObjectFactory().getNickNames();
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
        ProcessingMode mode = getObjectFactory().createObject( modeName );
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

    public TableConsumer consumerValue( Environment env ) throws TaskException {
        ProcessingMode mode = objectValue( env );
        return mode == null ? null : mode.createConsumer( env );
    }

    /**
     * Sets the value directly from a given TableConsumer.
     *
     * @param  env   execution environment
     * @param  consumer  table consumer
     */
    public void setValueFromConsumer( Environment env,
                                      final TableConsumer consumer )
            throws TaskException {
        setValueFromObject( env, new ProcessingMode() {
            public TableConsumer createConsumer( Environment env ) {
                return consumer;
            }
            public Parameter[] getAssociatedParameters() {
                return new Parameter[ 0 ];
            }
            public String getDescription() {
                return "";
            }
            public String toString() {
                return consumer.toString();
            }
        } );
    }
}
