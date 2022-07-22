package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.Arrays;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.mode.ProcessingMode;
import uk.ac.starlink.ttools.task.OutputModeParameter;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Writes a section of XML text documenteding all the known ProcessingModes.
 * Output is to standard output.  This class is designed to be used
 * from its {@link #main} method.
 *
 * @author   Mark Taylor
 * @since    23 Aug 2005
 */
public class ModeDoc {

    private final PrintStream out_;

    private ModeDoc( PrintStream out ) {
        out_ = out;
    }

    private void write() throws LoadException {
        ObjectFactory<ProcessingMode> modeFact = Stilts.getModeFactory();
        String[] mnames = modeFact.getNickNames();
        Arrays.sort( mnames );
        OutputModeParameter omodeParam = new OutputModeParameter( "omode" );
        for ( int i = 0; i < mnames.length; i++ ) {
            String name = mnames[ i ];
            String modeId = "mode-" + name;
            ProcessingMode mode = modeFact.createObject( name );
            out_.println( "<subsubsect id=\"" + modeId + "\">" );
            out_.println( "<subhead><title><code>" + name 
                        + "</code></title></subhead>" );
            out_.print( "<p>" );
            out_.println( "<strong>Usage:</strong>" );
            out_.print( "<verbatim>" );
            out_.print( "<![CDATA[" );
            out_.print( omodeParam.getModeUsage( name, 3 ) );
            out_.print( "]]>" );
            out_.print( "</verbatim>" );
            out_.print( "</p>" );
            out_.println();
            String descrip = mode.getDescription();
            if ( descrip == null ) {
                throw new IllegalArgumentException(
                    "No description for mode " + name );
            }
            out_.print( descrip );
            Parameter<?>[] params = mode.getAssociatedParameters();
            if ( params.length > 0 ) {
                out_.println( "<p>Additional parameters for this output mode "
                            + "are:<dl>" );
                for ( int j = 0; j < params.length; j++ ) {
                    out_.println( UsageWriter
                                 .xmlItem( params[ j ], modeId, false ) );
                }
                out_.println( "</dl></p>" );
            }
            out_.println( "</subsubsect>" );
        }
    }

    public static void main( String[] args ) throws LoadException {
        new ModeDoc( System.out ).write();
    }

}
