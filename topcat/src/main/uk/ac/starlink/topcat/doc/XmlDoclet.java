package uk.ac.starlink.topcat.doc;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import uk.ac.starlink.topcat.JELUtils;

/**
 * Doclet which documents public static members of classes in XML 
 * for insertion into the TOPCAT user document.
 * This doclet intentionally only outputs a summary of the available
 * information, to avoid bloat in the user document.
 *
 * <p>Optional doclet flags beyond the standard ones are:
 * <dl>
 * <dt>-o file</dt>
 * <dd>Specify output file</dd>
 *
 * <dt>-act</dt>
 * <dd>Write output only for 'activation' classes - as reported by
 *     {@link uk.ac.starlink.topcat.JELUtils#getActivationStaticClasses}.</dd>
 * 
 * <dt>-gen</dt>
 * <dd>Write output only for 'general' classes - as reported by
 *     {@link uk.ac.starlink.topcat.JELUtils.getGeneralStaticClasses}.</dd>
 * </dl>
 * 
 * @author   Mark Taylor (Starlink)
 * @since    6 Sep 2004
 */
public class XmlDoclet extends MemberDoclet {

    private final BufferedWriter out_;
    private boolean discardOutput_;
    private Class[] classes_;

    /**
     * Begin processing document.
     * This method is part of the Doclet public interface.
     */
    public static boolean start( RootDoc root ) throws IOException {
        return new XmlDoclet( root ).process();
    }

    /**
     * Define permitted command-line flags.
     * This method is part of the Doclet public interface.
     */
    public static int optionLength( String option ) {
        if ( option.equals( "-o" ) ) {
            return 2;
        }
        else if ( option.equals( "-gen" ) ) {
            return 1;
        }
        else if ( option.equals( "-act" ) ) {
            return 1;
        }
        else {
            return 0;
        }
    }

    private XmlDoclet( RootDoc root ) throws IOException {
        super( root );
        String[][] options = root.options();
        String outloc = null;
        for ( int i = 0; i < options.length; i++ ) {
            String opt = options[ i ][ 0 ];
            if ( opt.equals( "-o" ) ) {
                outloc = options[ i ][ 1 ];
            }
            else if ( opt.equals( "-gen" ) ) {
                classes_ = (Class[]) JELUtils.getGeneralStaticClasses()
                                             .toArray( new Class[ 0 ] );
            }
            else if ( opt.equals( "-act" ) ) {
                classes_ = (Class[]) JELUtils.getActivationStaticClasses()
                                             .toArray( new Class[ 0 ] );
            }
        }
        OutputStream ostrm = ( outloc == null || outloc.equals( "-" ) )
                           ? (OutputStream) System.out
                           : (OutputStream) new FileOutputStream( outloc );
        out_ = new BufferedWriter( new OutputStreamWriter( ostrm ) );
    }

    protected boolean process() throws IOException {
        out( "<dl>" );
        boolean ret = super.process();
        out( "</dl>" );
        out_.flush();
        return ret;
    }

    protected void startClass( ClassDoc clazz ) throws IOException {
        discardOutput_ = ! useClass( clazz );
        out( "<dt>" + clazz.name() + "</dt>" );
        out( "<dd>" );
        String comment = clazz.commentText();
        if ( comment != null ) {
            out( pWrap( comment ) );
        }
       out( "<p><dl>" );
    }

    protected void endClass() throws IOException {
        out( "</dl></p>" );
        out( "</dd>" );
        discardOutput_ = false;
    }

    protected void startMember( MemberDoc mem, String memType, 
                                String memName ) throws IOException {
        out( "<dt>" + memName + "</dt>" );
        String comment = mem.commentText();
        if ( comment != null ) {
            out( "<dd>" +  pWrap( comment ) + "</dd>" );
        }
    }

    protected void endMember() {
    }

    protected void outDescription( String descrip ) throws IOException {
    }

    protected void outItem( String name, String val ) {
    }

    protected void outParameters( Parameter[] param, String[] comments ) {
    }

    protected void outExamples( String[] examples ) {
    }

    /**
     * Indicates whether output should be produced for a class or not.
     *
     * @param  classDoc  class
     * @return   true if output should be produced for classDoc
     */
    private boolean useClass( ClassDoc classDoc ) {
        if ( classes_ == null ) {
            return true;
        }
        else {
            String cname = classDoc.qualifiedName();
            for ( int i = 0; i < classes_.length; i++ ) {
                if ( classes_[ i ].getName().equals( cname ) ) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Outputs some lines of text to the current output stream.
     *
     * @param  lines text for output
     */
    private void out( String[] lines ) throws IOException {
        for ( int i = 0; i < lines.length; i++ ) {
            out( lines[ i ] );
        }
    }

    /**
     * Outputs a single line of output to the current output stream.
     *
     * @param   line  text for output
     */
    private void out( String line ) throws IOException {
        if ( ! discardOutput_ ) {
            out_.write( line );
            out_.write( '\n' );
        }
    }

}
