package uk.ac.starlink.ttools.build;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import java.io.IOException;

/**
 * Doclet which documents public static members of classes in XML
 * for insertion into the TTOOLS user document.
 * Unlike the TOPCAT version, this gives farily complete information,
 * since it is not supplemented by an online help window.
 *
 * @author   Mark Taylor (Starlink)
 * @since    22 Apr 2005
 */
public class FullXmlDoclet extends XmlDoclet {

    /**
     * Begin processing document.
     * This method is part of the Doclet public interface.
     */
    public static boolean start( RootDoc root ) throws IOException {
        return new FullXmlDoclet( root ).process();
    }

    /**
     * Define permitted command-line flags.
     * This method is part of the Doclet public interface.
     */
    public static int optionLength( String option ) {
        return XmlDoclet.optionLength( option );
    }

    private FullXmlDoclet( RootDoc root ) throws IOException {
        super( root );
    }

    protected boolean process() throws IOException {
        boolean ret = super.process();
        flush();
        return ret;
    }

    protected void startClass( ClassDoc clazz ) throws IOException {
        out( "<subsubsect id='" + clazz.qualifiedName() + "'>" );
        out( "<subhead><title>" + clazz.name() + "</title></subhead>" );
        out( doctorText( clazz.commentText() ) );
        out( "<p><dl>" );
    }

    protected void endClass() throws IOException {
        out( "</dl></p>" );
        out( "</subsubsect>" );
    }

    protected void startMember( MemberDoc mem, String memType, String memName )
            throws IOException {
        out( "<dt><code>" + memName + "</code></dt>" );
        out( "<dd>" );
    }

    protected void endMember() throws IOException {
        out( "</ul></p></dd>" );
    }

    protected void outDescription( String desc ) throws IOException {
 
        /* Remove the final </p> since we will close the para at the end of
         * the member (to permit any parameters to be output in the same
         * paragraph). */
        out( doctorText( desc ).replaceFirst( "</p> *\\Z", "<ul>" ) );
    }

    protected void outParameters( Parameter[] params, String[] comments )
            throws IOException {
        if ( params.length > 0 ) {
            for ( int i = 0; i < params.length; i++ ) {
                Parameter param = params[ i ];
                StringBuffer buf = new StringBuffer();
                buf.append( "<li><code>" )
                   .append( param.name() )
                   .append( "</code> " )
                   .append( "<em>(" )
                   .append( typeString( param.type() ) )
                   .append( ")</em>" );
                if ( comments[ i ] != null ) {
                    buf.append( ": " + comments[ i ] );
                }
                buf.append( "</li>" );
                out( buf.toString() );
            }
        }
    }

    protected void outReturn( Type rtype, String rdesc ) throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append( "<li>return value <em>(" )
           .append( typeString( rtype ) )
           .append( ")</em>" );
        if ( rdesc != null ) {
            buf.append( ": " + rdesc );
        }
        buf.append( "</li>" );
        out( buf.toString() );
    }

}
