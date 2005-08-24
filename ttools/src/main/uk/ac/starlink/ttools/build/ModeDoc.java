package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.Arrays;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.mode.ProcessingMode;

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
        ObjectFactory modeFact = Stilts.getModeFactory();
        String[] mnames = modeFact.getNickNames();
        Arrays.sort( mnames );
        out_.println( "<dl>" );
        for ( int i = 0; i < mnames.length; i++ ) {
            String name = mnames[ i ];
            ProcessingMode mode = (ProcessingMode)
                                  modeFact.createObject( name );
            out_.println( "<dt><code>-mode=" + name + "</code></dt>" );
            out_.println( "<dd>" );
            out_.println( "<p>" + mode.getDescription() + "</p>" );
            Parameter[] params = mode.getAssociatedParameters();
            if ( params.length > 0 ) {
                out_.println( "<p>Additional parameters for this output mode "
                            + "are:<dl>" );
                for ( int j = 0; j < params.length; j++ ) {
                    out_.println( UsageWriter.xmlItem( params[ j ] ) );
                }
                out_.println( "</dl></p>" );
            }
            out_.println( "</dd>" );
        }
        out_.println( "</dl>" );
    }

    public static void main( String[] args ) throws LoadException {
        new ModeDoc( System.out ).write();
    }

}
