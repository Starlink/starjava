package uk.ac.starlink.ttools.build;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Doclet which documents public static members of classes in SUN-type XML.
 * This abstract superclass provides basic XML-type doclet functionality.
 *
 * <p>Optional doclet flags beyond the standard ones are:
 * <dl>
 * <dt>-o file</dt>
 * <dd>Specify output file</dd>
 * </dl>
 * 
 * @author   Mark Taylor (Starlink)
 * @since    6 Sep 2004
 */
public abstract class XmlDoclet extends MemberDoclet {

    private final BufferedWriter out_;

    /**
     * Define permitted command-line flags.
     * This method is part of the Doclet public interface.
     */
    public static int optionLength( String option ) {
        if ( option.equals( "-o" ) ) {
            return 2;
        }
        else {
            return 0;
        }
    }

    protected XmlDoclet( RootDoc root ) throws IOException {
        super( root );
        String[][] options = root.options();
        String outloc = null;
        for ( int i = 0; i < options.length; i++ ) {
            String opt = options[ i ][ 0 ];
            if ( opt.equals( "-o" ) ) {
                outloc = options[ i ][ 1 ];
            }
        }
        OutputStream ostrm = ( outloc == null || outloc.equals( "-" ) )
                           ? (OutputStream) System.out
                           : (OutputStream) new FileOutputStream( outloc );
        out_ = new BufferedWriter( new OutputStreamWriter( ostrm ) );
    }

    protected void startClass( ClassDoc clazz ) throws IOException {
    }

    protected void endClass() throws IOException {
    }

    protected void startMember( MemberDoc mem, String memType, 
                                String memName ) throws IOException {
    }

    protected void endMember() throws IOException {
    }

    protected void outDescription( String descrip ) throws IOException {
    }

    protected void outItem( String name, String val ) {
    }

    protected void outParameters( Parameter[] param, String[] comments )
            throws IOException {
    }

    protected void outReturn( Type rtype, String rdesc ) throws IOException {
    }

    protected void outExamples( String[] examples ) {
    }

    /**
     * Outputs some lines of text to the current output stream.
     * Implemented in terms of {@link out(java.lang.String)}.
     *
     * @param  lines text for output
     */
    public void out( String[] lines ) throws IOException {
        for ( int i = 0; i < lines.length; i++ ) {
            out( lines[ i ] );
        }
    }

    /**
     * Outputs a single line of output to the current output stream.
     *
     * @param   line  text for output
     */
    public void out( String line ) throws IOException {
        out_.write( line );
        out_.write( '\n' );
    }

    public void flush() throws IOException {
        out_.flush();
    }

    /**
     * Attempts to turn HTML text into XML.  It's pretty ad-hoc, and many
     * things can go wrong with it - using this relies on the various
     * document tests picking up anything that goes wrong.
     *
     * @param  text  HTML-type text
     * @return  XML-type text
     */
    public static String doctorText( String text ) {
        text = text.replaceAll( "<a href=", "<webref plaintextref='yes' url=" )
                   .replaceAll( "</a>", "</webref>" );
        return pWrap( text );
    }

}
