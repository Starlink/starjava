package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.Arrays;
import uk.ac.starlink.ttools.LoadException;
import uk.ac.starlink.ttools.ObjectFactory;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.StepFactory;

/**
 * Writes a section of XML text documenting all the known ProcessingFilters.
 * Output is to standard output.  This class is designed to be used
 * from its {@link #main} method.
 * 
 * @author   Mark Taylor
 * @since    23 Aug 2005
 */
public class FilterDoc {

    private final PrintStream out_;

    private FilterDoc( PrintStream out ) {
        out_ = out;
    }

    private void write() throws LoadException {
        ObjectFactory stepFact = StepFactory.getInstance().getFilterFactory();
        String[] fnames = stepFact.getNickNames();
        Arrays.sort( fnames );
        out_.println( "<dl>" );
        for ( int i = 0; i < fnames.length; i++ ) {
            String name = fnames[ i ];
            ProcessingFilter filter = (ProcessingFilter)
                                      stepFact.createObject( name );
            out_.print( "<dt><code>" + name );
            String usage = filter.getUsage();
            if ( usage != null ) {
                out_.print( " " + usage.replaceAll( "<", "&lt;" )
                                       .replaceAll( ">", "&gt;" ) );
            }
            out_.println( "</code></dt>" );
            out_.println( "<dd><p>" );
            String descrip = filter.getDescription();
            if ( descrip == null ) {
                throw new IllegalArgumentException(
                    "No description for filter " + name );
            }
            out_.print( descrip );
            out_.println( "</p></dd>" );
        }
        out_.println( "</dl>" );
    }

    public static void main( String[] args ) throws LoadException {
        new FilterDoc( System.out ).write();
    }
}
