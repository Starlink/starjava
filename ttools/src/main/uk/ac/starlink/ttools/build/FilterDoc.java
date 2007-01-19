package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.Arrays;
import uk.ac.starlink.ttools.filter.ProcessingFilter;
import uk.ac.starlink.ttools.filter.StepFactory;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

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
        for ( int i = 0; i < fnames.length; i++ ) {
            String name = fnames[ i ];
            ProcessingFilter filter = (ProcessingFilter)
                                      stepFact.createObject( name );
            out_.println( "<subsubsect id=\"" + name + "\">" );
            out_.println( "<subhead><title><code>" + name
                        + "</code></title></subhead>" );
            out_.print( "<p>" );
            out_.println( "<strong>Usage:</strong>" );
            out_.print( "<verbatim>" + "   " + name );
            String pad = "   " + name.replaceAll( ".", " " );
            String usage = filter.getUsage();
            if ( usage != null ) {
                out_.print( " " + usage.replaceAll( "<", "&lt;" )
                                       .replaceAll( ">", "&gt;" )
                                       .replaceAll( "\n", "\n " + pad ) );
            }
            out_.print( "</verbatim>" );
            out_.print( "</p>" );
            out_.println();
            String descrip = filter.getDescription();
            if ( descrip == null ) {
                throw new IllegalArgumentException(
                    "No description for filter " + name );
            }
            out_.print( descrip );
            out_.println( "</subsubsect>" );
            out_.println();
        }
    }

    public static void main( String[] args ) throws LoadException {
        new FilterDoc( System.out ).write();
    }
}
