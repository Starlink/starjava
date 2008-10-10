package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import uk.ac.starlink.ttools.plottask.PaintMode;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;

/**
 * Writes a section of XML text documenting all the known 
 * {@link uk.ac.starlink.ttools.plottask.PaintMode}s.
 * This class is designed to be used from its {@link #main} method.
 *
 * @author   Mark Taylor
 * @since    10 Oct 2008
 */
public class PaintModeDoc {

    private final PrintStream out_;

    /**
     * Constructor.
     */
    private PaintModeDoc( PrintStream out ) {
        out_ = out;
    }

    /**
     * Writes description to this object's output stream.
     */
    private void write() {
        PaintModeParameter modeParam = new PaintModeParameter( "omode" );
        PaintMode[] modes = PaintMode.getKnownModes();
        for ( int i = 0; i < modes.length; i++ ) {
            writeMode( modes[ i ], modeParam );
        }
    }

    /**
     * Writes a subsection for a given mode.
     *
     * @param  mode  mode to describe
     * @param  modeParam  mode parameter for context
     */
    private void writeMode( PaintMode mode, PaintModeParameter modeParam ) {
        String name = mode.getName();
        out_.println( "<subsubsect id='paintmode-" + name + "'>" );
        out_.println( "<subhead><title><code>" + name
                    + "</code></title></subhead>" );
        out_.print( "<p>" );
        out_.println( "<strong>Usage:</strong>" );
        out_.print( "<verbatim>" );
        out_.print( "<![CDATA[" );
        out_.print( modeParam.getName() + "=" + mode.getName()
                  + mode.getModeUsage( modeParam ) );
        out_.print( "]]>" );
        out_.print( "</verbatim>" );
        out_.print( "</p>" );
        out_.println();
        out_.println( "<p>" + mode.getDescription( modeParam ) + "</p>" );
        out_.println( "</subsubsect>" );
    }

    /**
     * Writes subsection to standard output.
     */
    public static void main( String[] args ) {
        new PaintModeDoc( System.out ).write();
    }
}
