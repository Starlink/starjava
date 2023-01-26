package uk.ac.starlink.ttools.build;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Type;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Generates HTML pages for display in a function browser.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Sep 2004
 */
public class HtmlDoclet extends MemberDoclet {

    private Writer out_;
    private File baseDir_;

    /**
     * Begin processing document.
     * This method is part of the Doclet public interface.
     */
    public static boolean start( RootDoc root ) throws IOException {
        return new HtmlDoclet( root ).process();
    }

    /**
     * Define permitted command-line flags. 
     * This method is part of the Doclet public interface.
     */
    public static int optionLength( String option ) {
        if ( option.equals( "-d" ) ) {
            return 2;
        }
        else if ( option.equals( "-headings" ) ) {
            return 1;
        }
        else {
            return 0;
        }
    }

    /** 
     * Constructor.
     */
    public HtmlDoclet( RootDoc root ) throws IOException {
        super( root );
        String[][] options = root.options();
        boolean headings = false;
        for ( int i = 0; i < options.length; i++ ) {
            if ( options[ i ][ 0 ].equals( "-d" ) ) {
                baseDir_ = new File( options[ i ][ 1 ] );
            }
            if ( options[ i ][ 0 ].equals( "-headings" ) ) {
                headings = true;
            }
        }
        if ( baseDir_ == null ) {
            baseDir_ = new File( "." );
        }
        if ( headings ) {
            writeHeadingFiles();
        }
    }

    protected void startClass( ClassDoc clazz ) throws IOException {
        startOutFile( classDocFile( baseDir_, clazz ) );
        outHeader( "Class", clazz.name() );
        outDescription( clazz.commentText() );
        outFooter();
        endOutFile();
    }

    protected void endClass() throws IOException {
    }

    protected void startMember( MemberDoc mem, String memType, String memName )
            throws IOException {
        File file;
        if ( mem instanceof FieldDoc ) {
            file = fieldDocFile( baseDir_, (FieldDoc) mem );
        }
        else if ( mem instanceof MethodDoc ) {
            file = methodDocFile( baseDir_, (MethodDoc) mem );
        }
        else {
            throw new AssertionError();
        }
        startOutFile( file );
        outHeader( memType, memName );
    }

    protected void endMember() throws IOException {
        outFooter();
        endOutFile();
    }

    protected void outItem( String name, String val ) throws IOException {
        out( new String[] {
            "<dl>",
            "<dt><strong>" + name + ":</strong></dt>",
            "<dd>" + val + "</dd>",
            "</dl>",
        } );
    }

    protected void outParameters( Parameter[] params, String[] comments,
                                  boolean isVararg )
            throws IOException {
        if ( params.length > 0 ) {
            out( new String[] { 
                     "<dl>",
                     "<dt><strong>Parameters:</strong></dt>",
                     "<dd>",
                     "<dl>",
            } );
            for ( int i = 0; i < params.length; i++ ) {
                Parameter param = params[ i ];
                out( new String[] {
                    new StringBuffer()
                       .append( "<dt><strong><font color='blue'><tt>" )
                       .append( param.name() )
                       .append( "</tt></font></strong> " )
                       .append( "<em>(" )
                       .append( varargTypeString( param.type(),
                                                  isVararg
                                                  && i == params.length - 1 ) )
                       .append( ")</em>" )
                       .append( "</dt>" )
                       .toString(),
                } );
                if ( comments[ i ] != null ) {
                    out( new String[] {
                        "<dd>" + comments[ i ] + "</dd>",
                    } );
                }
            }
            out( "</dl></dd></dl>" );
        }
    }

    protected void outReturn( Type rtype, String rdesc ) throws IOException {
        if ( rdesc != null ) {
            outItem( "Return Value (" + typeString( rtype ) + ")", rdesc );
        }
        else {
            outItem( "Return Type", typeString( rtype ) );
        }
    }

    protected void outDescription( String descrip ) throws IOException {
        out( new String[] {
            "<dl>",
            "<dt><strong>Description:</strong></dt>",
            "<dd>",
            descrip,
            "</dd>",
            "</dl>",
        } );
    }

    protected void outExamples( String[] examples ) throws IOException {
        if ( examples.length == 1 ) {
            out( new String[] {
                "<dl>",
                "<dt><strong>Example:</strong></dt>",
                "<dd>" + examples[ 0 ] + "</dd>",
                "</dl>",
            } );
        }
        else if ( examples.length > 1 ) {
            out( new String[] {
                "<dl>",
                "<dt><strong>Examples:</strong></dt>",
                "<dd><ul>",
            } );
            for ( int i = 0; i < examples.length; i++ ) {
                out( "<li>" + examples[ i ] + "</li>" );
            }
            out( new String[] {
                "</ul></dd>",
                "</dl>",
            } );
        }
    }

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
            out( new String[] {
                "<dl>",
                "<dt><strong>See Also:</strong></dt>",
            } );
            if ( ns == 1 ) {
                out( "<dd>" + fsees.get( 0 ) + "</dd>" );
            }
            else {
                out( "<dd><ul>" );
                for ( String fsee : fsees ) {
                    out( "<li>" + fsee + "</li>" );
                }
                out( "</ul></dd>" );
            }
            out( "</dl>" );
        }
    }

    /**
     * Attempts to convert the content of a @see tag to
     * HTML suitable for output.
     *
     * @param  stag  @see tag
     * @return   XML version of tag, or null
     */
    private String formatSeeTag( SeeTag stag ) {

        /* This implementation is not complete,
         * it only copes with HTML-style references (&lt;a&gt; tags)
         * not references to other classes/members. */
        String txt = stag.text();
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
     * @param  clazz    class specification
     * @param  suffix   member specification (unique within class)
     * @return  location for storing documentation text
     */
    private static File docFile( File baseDir, ClassDoc clazz, String suffix ) {
        return docFile( baseDir, clazz.qualifiedName(), suffix );
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
                                 String suffix ) {
        File file = baseDir;
        for ( StringTokenizer st = new StringTokenizer( className, "." );
              st.hasMoreTokens(); ) {
            file = new File( file, st.nextToken() );
        }
        return new File( file.getPath() + suffix );
    }

    /**
     * Returns the file used to store documentation about a class.
     *
     * @param  baseDir  root of output fileset
     * @param  clazz    class specification
     * @return  location for storing documentation text
     */
    public static File classDocFile( File baseDir, ClassDoc clazz ) {
        return docFile( baseDir, clazz, ".html" );
    }

    /**
     * Returns the file used to store documentation about a field.
     *
     * @param  baseDir  root of output fileset
     * @param  field    field specification
     * @return  location for storing documentation text
     */
    public static File fieldDocFile( File baseDir, FieldDoc field ) {
        return docFile( baseDir, field.containingClass(), 
                        "-" + field.name() + ".html" );
    }

    /**
     * Returns the file used to store documentation about a method.
     *
     * @param  baseDir  root of output fileset
     * @param  method    method specification
     * @return  location for storing documentation text
     */
    public static File methodDocFile( File baseDir, MethodDoc method ) {
        StringBuffer mangle = new StringBuffer();
        Parameter[] params = method.parameters();
        for ( int i = 0; i < params.length; i++ ) {
            mangle.append( "-" );
            Type type = params[ i ].type();
            mangle.append( type.typeName() );
            String dim = type.dimension();
            if ( dim != null ) {
                mangle.append( dim.replaceAll( "\\[\\]", "," ) );
            }
        }
        return docFile( baseDir, method.containingClass(), 
                        "-" + method.name() + mangle.toString() + ".html" );
    }

    /**
     * Writes documentation associated with known Heading values.
     */
    private void writeHeadingFiles() throws IOException {
        Heading[] headings = Heading.ALL_HEADINGS;
        for ( int i = 0; i < headings.length; i++ ) {
            Heading heading = headings[ i ];
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

}
