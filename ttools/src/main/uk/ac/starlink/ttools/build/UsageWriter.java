package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.LoadException;

/**
 * Write usage paragraphs specific to the STILTS tasks.
 * This class is designed to be used from its <code>main</code> method.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class UsageWriter {

    private final OutputStream out_;
    private final String taskName_;
    private final Task task_;

    private UsageWriter( String taskName, OutputStream out ) 
            throws LoadException {
        out_ = out;
        taskName_ = taskName;
        task_ = Stilts.getTaskFactory().createObject( taskName_ );
    }

    private void writeXml() throws IOException {
        String prefix = "   stilts <stilts-flags> " + taskName_;
        outln( "<subsubsect id=\"" + taskName_ + "-usage\">" );
        outln( "<subhead><title>Usage</title></subhead>" );
        outln( "<p>The usage of <code>" + taskName_ + "</code> is" );
        outln( "<verbatim><![CDATA[" );
        outln( LineInvoker
              .getPrefixedParameterUsage( task_.getParameters(), prefix )
              .replaceFirst( "\n+$", "" ) );
        outln( "]]></verbatim>" );
        outln( "If you don't have the <code>stilts</code> script installed," );
        outln( "write \"<code>java -jar stilts.jar</code>\" instead of" );
        outln( "\"<code>stilts</code>\" - see <ref id=\"invoke\"/>." );
        outln( "The available <code>&lt;stilts-flags&gt;</code> are listed" );
        outln( "in <ref id=\"stilts-flags\"/>." );
        outln( "For <ref id=\"taskApi\">programmatic invocation</ref>," );
        outln( "the Task class for this" );
        outln( "command is <code>" + task_.getClass().getName() + "</code>." );
        outln( "</p>" );
        outln();

        Parameter<?>[] params = task_.getParameters();
        if ( params.length > 0 ) {
            outln( "<p>Parameter values are assigned on the command line" );
            outln( "as explained in <ref id=\"task-args\"/>." );
            outln( "They are as follows:" );
            outln( "</p>" );
            outln( "<p>" );
            outln( "<dl>" );
            Arrays.sort( params, Parameter.BY_NAME );
            for ( int i = 0; i < params.length; i++ ) {
                outln( xmlItem( params[ i ], false ) );
            }
            outln( "</dl>" );
            outln( "</p>" );
        }
        else {
            outln( "<p>This task has no parameters." );
            outln( "</p>" );
        }
        outln( "</subsubsect>" );
    }

    /**
     * Returns a list item (dt/dd pair) for a parameter giving its usage
     * and description.
     * 
     * @param  param  parameter
     * @param  isBasic  if true, avoid adding XML constructs which won't be
     *                  evident (and may cause parsing trouble)
     *                  in plain text output
     * @return   XML snippet for <code>param</code>
     */
    public static String xmlItem( Parameter<?> param, boolean isBasic ) {
        String descrip = param.getDescription();
        if ( descrip == null ) {
            throw new NullPointerException( "No description for parameter "
                                          + param );
        }
        return new StringBuffer()
            .append( "<dt>" )
            .append( "<code>" )
            .append( getUsageXml( param ) )
            .append( "</code>" )
            .append( " " )
            .append( nbsps( 6 ) )
            .append( "<em>(" )
            .append( getTypeXml( param, isBasic ) )
            .append( ")</em>" )
            .append( "</dt>\n" )
            .append( "<dd>" )
            .append( descrip )
            .append( getDefaultXml( param ) )
            .append( "</dd>" )
            .toString();
    }

    /**
     * Returns XML text giving the basic usage text for a parameter.
     *
     * @param  param  parameter to describe
     * @return  XML snippet giving name=value
     */
    private static String getUsageXml( Parameter<?> param ) {
        return ( param.getName() + " = " + param.getUsage() )
              .replaceAll( "<", "&lt;" )
              .replaceAll( ">", "&gt;" );
    }

    /**
     * Returns XML text describing the value class of a parameter. 
     *
     * @param  param   parameter to describe
     * @param  isBasic  if true, avoid adding XML constructs which won't be
     *                  evident (and may cause parsing trouble)
     *                  in plain text output
     * @return  XML snippet
     */
    private static String getTypeXml( Parameter<?> param, boolean isBasic ) {
        Class<?> vClazz = param.getValueClass();
        boolean isArray = vClazz.getComponentType() != null;
        Class<?> clazz = isArray ? vClazz.getComponentType() : vClazz;
        String arraySuffix = isArray ? "[]" : "";
        String clazzName = clazz.getName();
        int pkgLeng = clazzName.lastIndexOf( "." );
        String pkgName = pkgLeng >= 0 ? clazzName.substring( 0, pkgLeng ) : "";
        String unqName = clazz.getSimpleName();
        final String docset;
        if ( pkgName.startsWith( "java" ) ) {
            docset = "&corejavadocs;";
        }
        else if ( pkgName.startsWith( "uk.ac.starlink.ttools" ) ||
                  pkgName.startsWith( "uk.ac.starlink.table.join" ) ) {
            docset = "&stiltsjavadocs;";
        }
        else if ( pkgName.startsWith( "uk.ac.starlink.table" ) ) {
            docset = "&stiljavadocs;";
        }
        else {
            docset = null;
        }
        boolean isPublic = Modifier.isPublic( clazz.getModifiers() );
        StringBuffer sbuf = new StringBuffer();
        if ( pkgName.length() == 0 ||
             pkgName.startsWith( "java.lang" ) ||
             ! isPublic ) {
            sbuf.append( unqName )
                .append( arraySuffix );
        }
        else if ( docset == null || isBasic ) {
            sbuf.append( clazzName )
                .append( arraySuffix );
        }
        else {
            sbuf.append( "<javadoc docset='" )
                .append( docset )
                .append( "'" )
                .append( " class='" )
                .append( clazzName )
                .append( "'" )
                .append( ">" )
                .append( unqName )
                .append( arraySuffix )
                .append( "</javadoc>" );
        }
        return sbuf.toString();
    }

    /**
     * Returns XML text giving the default value for a parameter.
     *
     * @param  param  parameter to describe
     * @return  XML snippet giving default string (may be empty)
     */
    private static String getDefaultXml( Parameter<?> param ) {
        String dflt = param.getStringDefault();
        StringBuffer sbuf = new StringBuffer();
        if ( dflt != null && dflt.length() > 0 ) {
            sbuf.append( "<p>[Default: <code>" )
                .append( dflt.replaceAll( "&", "&amp;" )
                             .replaceAll( "<", "&lt;" )
                             .replaceAll( ">", "&gt;" ) )
                .append( "</code>]</p>" );
        }
        return sbuf.toString();
    }

    /**
     * Returns a padding string containing non-breaking spaces.
     *
     * @param  count  number of spaces
     * @return  count-char padding string
     */
    private static String nbsps( int count ) {
        StringBuffer sbuf = new StringBuffer( count );
        for ( int i = 0; i < count; i++ ) {
            sbuf.append( "&#xA0;" );
        }
        return sbuf.toString();
    }

    private void out( String text ) throws IOException {
        out_.write( text.getBytes() );
    }

    private void outln( String line ) throws IOException {
        out( line + "\n" );
    }

    private void outln() throws IOException {
        out( "\n" );
    }

    /**
     * Writes a file called <i>taskname</i>-summary.xml for each of the
     * tasks in STILTS.  This contains a basic usage summary with some
     * surrounding XML boilerplate.
     */
    public static void main( String[] args ) throws IOException, LoadException {
        Logger.getLogger( "uk.ac.starlink.ttools" ).setLevel( Level.WARNING );
        if ( args.length == 0 ) {
            String[] taskNames = Stilts.getTaskFactory().getNickNames();
            File dir = new File( "." );
            for ( int i = 0; i < taskNames.length; i++ ) {
                String taskName = taskNames[ i ];
                String fname = taskName + "-summary.xml";
                File file = new File( dir, fname );
                System.out.println( "Writing " + fname );
                OutputStream out = new FileOutputStream( file );
                out = new BufferedOutputStream( out );
                new UsageWriter( taskName, out ).writeXml();
                out.close();
            }
        }
        else {
            new UsageWriter( args[ 0 ], System.out ).writeXml();
        }
    }
}
