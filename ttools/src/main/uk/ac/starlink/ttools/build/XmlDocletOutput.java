package uk.ac.starlink.ttools.build;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DocletOutput implementation for writing SUN-friendly XML.
 * The output is intended to be inserted into TOPCAT/STILTS user
 * documents to document the user-visible expressions in the JEL
 * expression language.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2023
 */
public class XmlDocletOutput implements DocletOutput {

    private final BufferedWriter out_;
    private final boolean headOnly_;
    private final Function<String,String> clazzToId_;
    private MemberIdSet memberIdSet_;
    private String clazzId_;
    private boolean skipMembers_;
    private boolean discardOutput_;

    private static final Pattern P_PATTERN =
        Pattern.compile( "\\s*(</*[Pp]>)?\\s+(<[Pp]>)\\s*" );

    /**
     * Constructor.
     *
     * @param  out  destination stream
     * @param  headOnly   if true, only a short summary of each class
     *                    will be written
     * @param  clazzToId  maps fully-qualified classname to the
     *                    (base) XML ID that will be used for that class
     *                    in the XML output
     */
    public XmlDocletOutput( OutputStream out, boolean headOnly,
                            Function<String,String> clazzToId ) {
        out_ = new BufferedWriter( new OutputStreamWriter( out ) );
        headOnly_ = headOnly;
        clazzToId_ = clazzToId;
    }

    public void startOutput() throws IOException {
        if ( headOnly_ ) {
            out( "<dl>" );
        }
    }

    public void endOutput() throws IOException {
        if ( headOnly_ ) {
            out( "</dl>" );
        }
        out_.flush();
    }

    public void startClass( String className, String firstSentence,
                            String fullDescription ) throws IOException {
        String shortName = className.replaceFirst( "^.*[.]", "" );
        clazzId_ = clazzToId_.apply( className );
        memberIdSet_ = new MemberIdSet();
        if ( headOnly_ ) {
            out( "<dt>"
               + "<ref id='"
               + clazzId_
               + "'>"
               + shortName
               + "</ref>"
               + "</dt>" );
            out( "<dd>" );
        }
        else {
            out( "<subsubsect id='" + clazzId_ + "'>" );
            out( "<subhead><title>" + shortName + "</title></subhead>" );
        }
        String comment = headOnly_ ? firstSentence : fullDescription;
        if ( comment != null ) {
            out( doctorText( comment ) );
        }
        if ( headOnly_ && ! discardOutput_ ) {
            skipMembers_ = true;
            discardOutput_ = true;
        }
        out( "<p><dl>" );
    }

    public void endClass() throws IOException {
        clazzId_ = null;
        memberIdSet_ = null;
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

    public void startMember( String memberName, String memberType,
                             String memberId, String description )
            throws IOException {
        StringBuffer sbuf = new StringBuffer( "<dt" );

        /* Write an ID attribute identifying this member, but do it on a
         * best-efforts basis.  These must be unique (or invalidate the XML),
         * but it should also preferably be easy to read, which the
         * guaranteed unique memberId may not be.
         * So use a simple variant of the member name as long
         * as it hasn't already been used.  If that's not present we could
         * fall back to the guaranteed unique string memberId, but at present
         * don't do that for simplicity of referencing, compatibility
         * with previous behaviour and because these overloaded methods
         * are not often referred to.  guaranteed unique string. */
        String xmlId = memberIdSet_.getUniqueId( memberName );
        if ( xmlId != null ) {
            sbuf.append( " id='" )
                .append( clazzId_ )
                .append( "-" )
                .append( xmlId )
                .append( "'" );
        }
        sbuf.append( "><code>" )
            .append( memberName )
            .append( "</code></dt>" );
        out( sbuf.toString() );
        out( "<dd>" );
        out( doctorText( description ) );
        out( "<p><ul>" );
    }

    public void endMember() throws IOException {
        out( "</ul></p>" );
        out( "</dd>" );
    }

    /**
     * This information is discarded;
     * the XML output is too terse to include it.
     */
    public void outMemberItem( String name, String val ) throws IOException {
    }

    public void outParameters( DocVariable[] params ) throws IOException {
        out( "<li>Parameters:" );
        out( "<ul>" );
        for ( DocVariable param : params ) {
            StringBuffer buf = new StringBuffer();
            buf.append( "<li><code>" )
               .append( param.getName() )
               .append( "</code> " )
               .append( "<em>(" )
               .append( param.getType() )
               .append( ")</em>" );
            String comment = param.getCommentText();
            if ( comment != null ) {
                buf.append( ": " + comment );
            }
            buf.append( "</li>" );
            out( buf.toString() );
        }
        out( "</ul>" );
        out( "</li>" );
    }

    public void outReturn( String type, String comment ) throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append( "<li>Return value" )
           .append( "<ul><li>" )
           .append( "<em>(" )
           .append( type )
           .append( ")</em>" );
        if ( comment != null ) {
            buf.append( ": " )
               .append( comment );
        }
        buf.append( "</li></ul>" );
        buf.append( "</li>" );
        out( buf.toString() );
    }

    public void outExamples( String heading, String[] examples )
            throws IOException {
        out( "<li>" + heading + ":" );
        out( "<ul>" );
        for ( String example : examples ) {
            out( "<li>" + example + "</li>" );
        }
        out( "</ul>" );
        out( "</li>" );
    }

    public void outSees( String heading, String[] sees ) throws IOException {
        List<String> seeTxts = Arrays.stream( sees )
                                     .map( t -> formatSeeText( t ) )
                                     .filter( t -> t != null )
                                     .collect( Collectors.toList() );
        if ( seeTxts.size() > 0 ) {
            out( "<li>" + heading + ":" );
            out( "<ul>" );
            for ( String seeTxt : seeTxts ) {
                out( "<li>" + seeTxt + "</li>" );
            }
            out( "</ul>" );
            out( "</li>" );
        }
    }

    /**
     * Writes a line to this object's destination stream.
     * A standard line-end character is added.
     *
     * @param   line  line to output
     */
    private void out( String line ) throws IOException {
        if ( ! discardOutput_ ) {
            out_.write( line );
            out_.write( '\n' );
        }
    }

    /**
     * Attempts to convert the content of a @see tag to
     * XML suitable for output.
     *
     * @param  txt  content of @see tag
     * @return   XML version of tag, or null
     */
    private static String formatSeeText( String txt ) {

        /* This implementation is neither complete nor bulletproof.
         * It only copes with HTML-style references (&lt;a&gt; tags)
         * not references to other classes/members.
         * It also might get the translation wrong, potentially into
         * invalid XML, if the input tag is written in a non-standard way.
         * It would be nice to fix the former, but the latter doesn't
         * matter too much - this is only invoked during package build,
         * where the document unit tests are very likely to
         * pick up any mistakes it makes. */
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
     * Attempts to turn HTML text into XML.  It's pretty ad-hoc, and many
     * things can go wrong with it - using this relies on the various
     * document tests picking up anything that goes wrong.
     *
     * @param  text  HTML-type text
     * @return  XML-type text
     */
    private static String doctorText( String text ) {
        text = text.replaceAll( "<a href=", "<webref plaintextref='yes' url=" )
                   .replaceAll( "</a>", "</webref>" )
                   .replaceAll( "<pre>", "<verbatim>" )
                   .replaceAll( "</pre>", "</verbatim>" );
        return pWrap( text );
    }

    /**
     * Ensures that a string is a sequence of &lt;p&gt; elements
     * (though it's not foolproof).
     *
     * @param  text  basic text
     * @return  same as <code>text</code> but a sequence of HTML P elements
     */
    private static String pWrap( String text ) {
        String[] params = P_PATTERN.split( text );
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < params.length; i++ ) {
            sbuf.append( "<p>" + params[ i ] + "</p>\n" );
        }
        return sbuf.toString();
    }
}
