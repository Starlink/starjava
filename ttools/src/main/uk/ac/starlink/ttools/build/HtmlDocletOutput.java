package uk.ac.starlink.ttools.build;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

/**
 * DocletOutput implementation for writing basic HTML,
 * with one file per class, method and field.
 * The output is suitable for display in a
 * {@link uk.ac.starlink.ttools.gui.MethodBrowser}.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2023
 */
public class HtmlDocletOutput implements DocletOutput {

    private final File baseDir_;
    private final boolean isHeadings_;
    private Writer out_;
    private String className_;

    /**
     * Constructor.
     *
     * @param   baseDir  base directory
     * @param   isHeadings  if true, writes a couple of extra files
     */
    public HtmlDocletOutput( File baseDir, boolean isHeadings ) {
        baseDir_ = baseDir;
        isHeadings_ = isHeadings;
    }

    public void startOutput() throws IOException {
        if ( isHeadings_ ) {
            writeHeadingFiles();
        }
    }

    public void endOutput() throws IOException {
    }

    public void startClass( String className, String firstSentence,
                            String fullDescription ) throws IOException {
        className_ = className;
        startOutFile( docFile( baseDir_, className, ".html" ) );
        String shortName = className.replaceFirst( "^.*[.]", "" );
        outHeader( "Class", shortName );
        outDescription( fullDescription );
        outFooter();
        endOutFile();
    }

    public void endClass() {
        className_ = null;
    }

    public void startMember( String memberName, String memberType,
                             String uniqueId, String description )
            throws IOException {
        startOutFile( docFile( baseDir_, className_,
                               "-" + uniqueId + ".html" ) );
        outHeader( memberType, memberName );
        outDescription( description );
    }

    public void endMember() throws IOException {
        outFooter();
        endOutFile();
    }

    public void outMemberItem( String name, String val ) throws IOException {
        out( new String[] {
            "<dl>",
            "<dt><strong>" + name + ":</strong></dt>",
            "<dd>" + val + "</dd>",
            "</dl>",
        } );
    }

    public void outParameters( DocVariable[] params ) throws IOException {
        out( new String[] {
                 "<dl>",
                 "<dt><strong>Parameters:</strong></dt>",
                 "<dd>",
                 "<dl>",
             } );
        for ( DocVariable param : params ) {
            out( new StringBuffer()
                .append( "<dt><strong><font color='blue'><tt>" )
                .append( param.getName() )
                .append( "</tt></font></strong> " )
                .append( "<em>(" )
                .append( param.getType() )
                .append( ")</em>" )
                .append( "</dt>" )
                .toString() );
            String comment = param.getCommentText();
            if ( comment != null ) {
                out( "<dd>" + comment + "</dd>" );
            }
        }
        out( "</dl></dd></dl>" );
    }

    public void outReturn( String type, String comment ) throws IOException {
        if ( comment != null ) {
            outMemberItem( "Return Value (" + type + ")", comment );
        }
        else {
            outMemberItem( "Return Type", type );
        }
    }

    private void outDescription( String descrip ) throws IOException {
        out( new String[] {
            "<dl>",
            "<dt><strong>Description:</strong></dt>",
            "<dd>",
            descrip,
            "</dd>",
            "</dl>",
        } );
    }

    public void outExamples( String heading, String[] examples )
            throws IOException {
        outList( heading, examples );
    }

    public void outSees( String heading, String[] sees ) throws IOException {
        List<String> seeTxts = Arrays.stream( sees )
                                     .map( t -> formatSeeText( t ) )
                                     .filter( t -> t != null )
                                     .collect( Collectors.toList() );
        if ( seeTxts.size() > 0 ) {
            outList( heading, seeTxts.toArray( new String[ 0 ] ) );
        }
    }

    /**
     * Writes a list of items.
     *
     * @param  heading  heading
     * @param  items   strings to list
     */
    private void outList( String heading, String[] items ) throws IOException {
        int nItem = items.length;
        if ( nItem > 0 ) {
            out( new String[] {
                     "<dl>",
                     "<dt><strong>" + heading + ":</strong></dt>",
                 } );
            if ( nItem == 1 ) {
                out( "<dd>" + items[ 0 ] + "</dd>" );
            }
            else {
                 out( "<dd><ul>" );
                 for ( String item : items ) {
                     out( "<li>" + item + "</li>" );
                 }
                 out( "</ul></dd>" );
            }
            out( new String[] {
                     "</dl>",
                 } );
         }
    }

    /**
     * Writes documentation associated with known Heading values.
     */
    private void writeHeadingFiles() throws IOException {
        Heading[] headings = Heading.ALL_HEADINGS;
        for ( Heading heading : Heading.ALL_HEADINGS ) {
            File file = docFile( baseDir_, Heading.class.getName(),
                                 heading.getDocSuffix() );
            startOutFile( file );
            outHeader( "", heading.getUserString() );
            out( heading.getDescription() );
            outFooter();
            endOutFile();
        }
    }

    /**
     * Outputs the start of an HTML file to the current output stream.
     *
     * @param  memType  type of member (short string)
     * @param  memName  name of member (short string)
     */
    private void outHeader( String memType, String memName )
            throws IOException {
        out( new String[] {
            "<html>",
            "<head><title>" + memName + "</title></head>",
            "<body bgcolor='white'>",
            "<h2>" + memType + " <font color='blue'><tt>" + memName
                             + "</tt></font></h2>",
        } );
    }

    /**
     * Outputs the end of an HTML file to the current output stream.
     */
    private void outFooter() throws IOException {
        out( new String[] {
            "</body>",
            "</html>",
        } );
    }

    /**
     * Sets the current output destination to the given file.
     *
     * @param  file  new output destination file
     */
    private void startOutFile( File file ) throws IOException {
        File parent = file.getParentFile();
        if ( parent != null && ! parent.exists() && ! parent.mkdirs() ) {
            throw new IOException( "Can't create directory " + parent );
        }
        out_ = new BufferedWriter( new FileWriter( file ) );
    }

    /**
     * Closes the current output destination (the last one for which
     * {@link #startOutFile} was called).
     */
    private void endOutFile() throws IOException {
        out_.close();
        out_ = null;
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
        out_.write( line );
        out_.write( '\n' );
    }

    /**
     * Attempts to convert the content of a @see tag to
     * HTML suitable for output.
     *
     * @param  txt  content of @see tag
     * @return   HTML version of tag, or null
     */
    private static String formatSeeText( String txt ) {

        /* This implementation is not complete,
         * it only copes with HTML-style references (&lt;a&gt; tags)
         * not references to other classes/members. */
        if ( txt == null || txt.trim().length() == 0 ) {
            return null;
        }
        txt = txt.trim().replaceAll( "\\s+", " " );
        if ( txt.startsWith( "<a " ) ) {
            return txt;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the abstract filename in which the documentation for a
     * class member will be stored.
     *
     * @param  baseDir  root of output fileset
     * @param  className    class specification
     * @param  suffix   member specification (unique within class)
     * @return  location for storing documentation text
     */
    private static File docFile( File baseDir, String className,
                                 String suffix ) throws IOException {
        File file = baseDir;
        for ( StringTokenizer st = new StringTokenizer( className, "." );
              st.hasMoreTokens(); ) {
            file = new File( file, st.nextToken() );
        }
        return new File( file.getPath() + suffix );
    }
}
