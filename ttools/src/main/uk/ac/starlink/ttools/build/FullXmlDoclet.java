package uk.ac.starlink.ttools.build;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Doclet which documents public static members of classes in XML
 * for insertion into SUN-style XML user documents.
 *
 * <p>Optional doclet flags beyond the standard ones are:
 * <dl>
 * <dt>-headonly</dt>
 * <dd>Write only the first sentence of the class headers,
 *     and not information about the methods.
 * </dl>
 *
 * @author   Mark Taylor (Starlink)
 * @since    22 Apr 2005
 */
public class FullXmlDoclet extends XmlDoclet {

    private boolean headOnly_;
    private boolean discardOutput_;
    private boolean skipMembers_;
    private String clazzId_;
    private Set<String> memberIds_;

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
        if ( option.equals( "-headonly" ) ) {
            return 1;
        }
        else {
            return XmlDoclet.optionLength( option );
        }
    }

    /**
     * Constructor.
     *
     * @param  root  root document
     */
    protected FullXmlDoclet( RootDoc root ) throws IOException {
        super( root );
        memberIds_ = new HashSet<String>();
        String[][] options = root.options();
        for ( String[] opts : options ) {
            String opt = opts[ 0 ];
            if ( opt.equals( "-headonly" ) ) {
                headOnly_ = true;
            }
        }
    }

    /**
     * Returns the value to use for the XML ID attached to the subsection
     * describing a given class.
     *
     * @param  clazz  class doc
     * @return   XML ID string
     */
    protected String getXmlId( ClassDoc clazz ) {
        return clazz.qualifiedName();
    }

    /**
     * Indicates whether a given class should be documented by this doclet
     * or ignored.
     * The default implementation returns true always, but it may be
     * overridden by subclasses.
     *
     * @param  clazz  class doc
     * @return   true to use class, false to ignore it
     */
    protected boolean useClass( ClassDoc clazz ) {
        return true;
    }

    protected boolean process() throws IOException {
        if ( headOnly_ ) {
            out( "<dl>" );
        }
        boolean ret = super.process();
        if ( headOnly_ ) {
            out( "</dl>" );
        }
        flush();
        return ret;
    }

    @Override
    protected void startClass( ClassDoc clazz ) throws IOException {
        discardOutput_ = !useClass( clazz );
        clazzId_ = getXmlId( clazz );
        memberIds_.clear();
        if ( headOnly_ ) {
            out( "<dt>" 
               + "<ref id='"
               + clazzId_
               + "'>"
               + clazz.name()
               + "</ref>"
               + "</dt>" );
            out( "<dd>" );
        }
        else {
            out( "<subsubsect id='" + clazzId_ + "'>" );
            out( "<subhead><title>" + clazz.name() + "</title></subhead>" );
        }
        String comment = headOnly_ ? firstSentence( clazz )
                                   : clazz.commentText();
        if ( comment != null ) {
            out( doctorText( comment ) );
        }
        if ( headOnly_ && ! discardOutput_ ) {
            skipMembers_ = true;
            discardOutput_ = true;
        }
        out( "<p><dl>" );
    }

    @Override
    protected void endClass() throws IOException {
        clazzId_ = null;
        memberIds_.clear();
        out( "</dl></p>" );
        if ( skipMembers_ ) {
            discardOutput_ = false;
            skipMembers_ = false;
        }
        if ( headOnly_ ) {
            out( "</dd>" );
        }
        else {
            out( "</subsubsect>" );
        }
        discardOutput_ = false;
    }

    @Override
    protected void startMember( MemberDoc mem, String memType, String memName )
            throws IOException {
        StringBuffer sbuf = new StringBuffer( "<dt" );

        /* Write an ID attribute identifying this member, but do it on a
         * best-efforts basis.  These must be unique (or invalidate the XML)
         * so if the ID value has already been used, just don't bother. */
        String memberId = memName.replaceFirst( "[^a-zA-Z_].*", "" );
        if ( memberIds_.add( memberId ) ) {
            sbuf.append( " id='" )
                .append( clazzId_ )
                .append( "-" )
                .append( memberId )
                .append( "'" );
        }
        sbuf.append( "><code>" )
            .append( memName )
            .append( "</code></dt>" );
        out( sbuf.toString() );
        out( "<dd>" );
    }

    @Override
    protected void endMember() throws IOException {
        out( "</ul></p>" );
        out( "</dd>" );
    }

    @Override
    protected void outDescription( String desc ) throws IOException {
        out( doctorText( desc ) );
        out( "<p><ul>" );
    }

    @Override
    protected void outParameters( Parameter[] params, String[] comments,
                                  boolean isVararg )
            throws IOException {
        if ( params.length > 0 ) {
            out( "<li>Parameters:" );
            out( "<ul>" );
            for ( int i = 0; i < params.length; i++ ) {
                Parameter param = params[ i ];
                StringBuffer buf = new StringBuffer();
                buf.append( "<li><code>" )
                   .append( param.name() )
                   .append( "</code> " )
                   .append( "<em>(" )
                   .append( varargTypeString( param.type(),
                                              isVararg
                                              && i == params.length - 1 ) )
                   .append( ")</em>" );
                if ( comments[ i ] != null ) {
                    buf.append( ": " + comments[ i ] );
                }
                buf.append( "</li>" );
                out( buf.toString() );
            }
            out( "</ul>" );
            out( "</li>" );
        }
    }

    @Override
    protected void outReturn( Type rtype, String rdesc ) throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append( "<li>Return value" )
           .append( "<ul><li>" )
           .append( "<em>(" )
           .append( typeString( rtype ) )
           .append( ")</em>" );
        if ( rdesc != null ) {
            buf.append( ": " + rdesc );
        }
        buf.append( "</li></ul>" )
           .append( "</li>" );
        out( buf.toString() );
    }

    @Override
    protected void outExamples( String[] examples ) throws IOException {
        if ( examples.length > 0 ) {
            out( "<li>" + ( examples.length > 1 ? "Examples:" : "Example:" ) );
            out( "<ul>" );
            for ( String ex : examples ) {
                out( "<li>" + ex + "</li>" );
            }
            out( "</ul>" );
            out( "</li>" );
        }
    }

    @Override
    protected void outSees( SeeTag[] seeTags ) throws IOException {
        List<String> fsees = new ArrayList<String>();
        for ( SeeTag tag : seeTags ) {
            String fsee = formatSeeTag( tag );
            if ( fsee != null && fsee.trim().length() > 0 ) {
                fsees.add( fsee );
            }
        }
        int ns = fsees.size();
        if ( ns > 0 ) {
            out( "<li>See Also:" );
            out( "<ul>" );
            for ( String fsee : fsees ) {
                out( "<li>" + fsee + "</li>" );
            }
            out( "</ul>" );
            out( "</li>" );
        }
    }

    /**
     * Attempts to convert the content of a @see tag to
     * XML suitable for output.
     *
     * @param  stag  @see tag
     * @return   XML version of tag, or null
     */
    private String formatSeeTag( SeeTag stag ) {

        /* This implementation is neither complete nor bulletproof.
         * It only copes with HTML-style references (&lt;a&gt; tags)
         * not references to other classes/members.
         * It also might get the translation wrong, potentially into
         * invalid XML, if the input tag is written in a non-standard way.
         * It would be nice to fix the former, but the latter doesn't
         * matter too much - this is only invoked during package build,
         * where the document unit tests are very likely to
         * pick up any mistakes it makes. */
        String txt = stag.text();
        if ( txt == null || txt.trim().length() == 0 ) {
            return null;
        }
        txt = txt.trim().replaceAll( "\\s+", " " );
        if ( txt.startsWith( "<a" ) ) {
            return txt.replaceAll( "^<a ", "<webref " )
                      .replaceAll( "</a>", "</webref>" )
                      .replaceAll( "href=", "url=" );
        }
        else {
            return null;
        }
    }

    /**
     * Outputs a single line of output to the current output stream.
     *
     * @param   line  text for output
     */
    @Override
    public void out( String line ) throws IOException {
        if ( ! discardOutput_ ) {
            super.out( line );
        }
    }
}
