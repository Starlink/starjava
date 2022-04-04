package uk.ac.starlink.ttools.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.util.BeanConfig;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.URLDataSource;

/**
 * Writes auto-generated documentation XML files for STIL I/O handlers.
 *
 * @author   Mark Taylor
 * @since    21 Sep 2020
 */
public class HandlerDoc {

    private final StarTableFactory tfact_;
    private final StarTableOutput tout_;
    private final StarTable exampleTable_;
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    /**
     * Constructor.
     *
     * @param  tfact   table factory
     * @param  tout    table output controller
     * @param  exampleTable   example table for optional inclusion
     *                        in serialized form in output XML
     */
    public HandlerDoc( StarTableFactory tfact, StarTableOutput tout,
                       StarTable exampleTable ) {
        tfact_ = tfact;
        tout_ = tout;
        exampleTable_ = exampleTable;
    }

    /**
     * Returns the XML documentation for a given STIL input handler.
     *
     * @param   builder  input handler
     * @return  XML documentation string
     */
    public String getBuilderDoc( TableBuilder builder ) throws IOException {
        String fname = builder.getFormatName();
        StringBuffer sbuf = new StringBuffer();
        if ( builder instanceof Documented ) {
            sbuf.append( DocUtils.getXmlDescription( ((Documented) builder) ) );
        }
        tfact_.getTableBuilder( getConfigExample( fname, builder ) );  // test
        sbuf.append( getConfigOptionXml( fname, builder ) );
        if ( builder instanceof MultiTableBuilder ) {
            sbuf.append( String.join( "\n",
                "<p>Files in this format may contain multiple tables;",
                "depending on the context, either one or all tables",
                "will be read.",
                "Where only one table is required,",
                "either the first one in the file is used,",
                "or the required one can be specified after the",
                "\"<code>#</code>\" character at the end of the filename.",
                "</p>",
            "" ) );
        }
        if ( isAutoDetect( builder ) ) {
            sbuf.append( String.join( "\n",
                "<p>This format can be automatically identified by its content",
                "so you do not need to specify the format explicitly",
                "when reading",
                fname,
                "tables, regardless of the filename.",
                "</p>",
            "" ) );
        }
        else {
            sbuf.append( String.join( "\n",
                "<p>This format cannot be automatically identified",
                "by its content, so in general it is necessary",
                "to specify that a table is in",
                fname,
                "format when reading it.",
            "" ) );
            String[] extensions = builder instanceof DocumentedIOHandler
                ? ((DocumentedIOHandler) builder).getExtensions()
                : new String[ 0 ];
            if ( extensions.length > 0 ) {
                sbuf.append( String.join( "\n",
                    "However, if the input file has",
                    getExtensionListXml( extensions ),
                    "an attempt will be made to read it using this format.",
                "" ) );
            }
            sbuf.append( "</p>\n" );
        }
        if ( builder instanceof DocumentedIOHandler &&
             ((DocumentedIOHandler) builder).docIncludesExample() ) {
            sbuf.append( getExampleOutputXml( tout_.getHandler( fname ) ) );
        }
        return sbuf.toString();
    }

    /**
     * Returns the XML documentation for a given STIL output handler.
     *
     * @param  writer  output handler
     * @return  XML documentation string
     */
    public String getWriterDoc( StarTableWriter writer ) throws IOException {
        String fname = writer.getFormatName();
        StringBuffer sbuf = new StringBuffer();
        if ( writer instanceof Documented ) {
            sbuf.append( DocUtils.getXmlDescription( ((Documented) writer ) ) );
        }
        tout_.getHandler( getConfigExample( fname, writer ) );  // test
        sbuf.append( getConfigOptionXml( fname, writer ) );
        if ( writer instanceof MultiStarTableWriter ) {
            sbuf.append( "<p>Multiple tables may be written to a single\n" )
                .append( "output file using this format.\n" )
                .append( "</p>\n" );
        }
        String[] extensions = writer instanceof DocumentedIOHandler
                            ? ((DocumentedIOHandler) writer).getExtensions()
                            : new String[ 0 ];
        if ( extensions.length > 0 ) {
            sbuf.append( String.join( "\n",
                "<p>If no output format is explicitly chosen,",
                "writing to a filename with",
                getExtensionListXml( extensions ),
                "will select <code>" + fname + "</code> format for output.",
                "</p>",
            "" ) );
        }
        if ( writer instanceof DocumentedIOHandler &&
             ((DocumentedIOHandler) writer).docIncludesExample() ) {
            sbuf.append( getExampleOutputXml( writer ) );
        }
        return sbuf.toString();
    }

    /**
     * Returns XML text serializing this object's example table
     * using a given output handler.
     *
     * @param  writer  output handler
     * @return XML p element presenting serialized example table,
     *         or empty string if there is no example table
     */
    private String getExampleOutputXml( StarTableWriter writer )
            throws IOException {
        StringBuffer sbuf = new StringBuffer();
        if ( exampleTable_ != null ) {
            sbuf.append( "<p>An example looks like this:\n" )
                .append( "<verbatim><![CDATA[\n" )
                .append( serializeTable( writer ) )
                .append( "]]></verbatim>\n" )
                .append( "</p>\n" );
        }
        return sbuf.toString();
    }

    /**
     * Serializes a table to a string using a given output handler.
     *
     * @param   handler  output handler
     * @return   string containing serialized representation of
     *           this documenter's example table
     */
    private String serializeTable( StarTableWriter handler )
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        handler.writeStarTable( exampleTable_, out );
        out.flush();
        return new String( out.toByteArray(), UTF8 );
    }

    /**
     * Returns a string containing an XML sentence listing a disjunction
     * of given file extensions.
     *
     * @param  extensions  one or more file extensions, excluding "."
     * @return  sentence something like "the extension e1, e2 or e3"
     */
    private String getExtensionListXml( String[] extensions ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "the extension" );
        int next = extensions.length;
        for ( int i = 0; i < next; i++ ) {
            sbuf.append( " \"<code>." )
                .append( extensions[ i ] )
                .append( "</code>\"" );
            if ( i < next - 2 ) {
                sbuf.append( ", " );
            }
            else if ( i == next - 2 ) {
                sbuf.append( " or" );
            }
        }
        sbuf.append( " (case insensitive)" );
        return sbuf.toString();
    }

    /**
     * Returns a handler specification string for a given handler.
     * Where possible, it will include some example settings of
     * config methods.  The output should be suitable for feeding
     * to the relevant factory to acquire an I/O handler instance.
     *
     * @param  handlerName  name of handler type, known to I/O controller
     * @param  handler   handler instance
     * @return   handler specification string, maybe including config options
     */
    private String getConfigExample( String handlerName, Object handler ) {
        StringBuffer sbuf = new StringBuffer( handlerName.toLowerCase() );
        List<String> examplePairs = new ArrayList<>();
        List<Method> meths = getConfigMethods( handler.getClass() );
        for ( Method meth : meths ) {
            ConfigMethod ann = meth.getAnnotation( ConfigMethod.class );
            String propName = ann.property();
            String propValue = getExampleValue( handler, meth );
            if ( propValue != null ) {
                examplePairs.add( propName + "=" + propValue );
            }
            else {
                throw new RuntimeException( "No example text for "
                                          + handlerName + " property "
                                          + propName );
            }
        }
        if ( examplePairs.size() > 0 ) {
            sbuf.append( "(" )
                .append( examplePairs.stream()
                                     .limit( 2 )
                                     .collect( Collectors.joining( "," ) ) )
                .append( ")" );
        }
        return sbuf.toString();
    }

    /**
     * Returns p-level XML containing documentation for a given
     * I/O handler.  Content is determined by reflection and use of
     * the {@link uk.ac.starlink.util.ConfigMethod} annotation.
     *
     * @param  handlerName  name of handler type, known to I/O controller
     * @param  handler   handler instance
     * @return   XML text, a p elementn or the empty string
     */
    private String getConfigOptionXml( String handlerName, Object handler ) {
        List<Method> meths = getConfigMethods( handler.getClass() );
        StringBuffer sbuf = new StringBuffer();
        if ( meths.size() > 0 ) {
            sbuf.append( String.join( "\n",
                "<p>The handler behaviour may be modified by specifying",
                "one or more comma-separated name=value configuration options",
                "in parentheses after the handler name, e.g.",
                "\"<code>" + getConfigExample( handlerName, handler )
                           + "</code>\".",
                 "The following options are available:",
                 "<dl>",
            "" ) );
            for ( Method meth : meths ) {
                ConfigMethod ann = meth.getAnnotation( ConfigMethod.class );
                sbuf.append( "<dt><code>" )
                    .append( ann.property() )
                    .append( " = <![CDATA[" )
                    .append( BeanConfig.getMethodUsage( meth ) )
                    .append( "]]></code></dt>\n" )
                    .append( "<dd>" )
                    .append( asXml( ann.doc() ) )
                    .append( "</dd>\n" );
            }
            sbuf.append( "</dl>\n" )
                .append( "</p>\n" );
        }
        return sbuf.toString();
    }

    /**
     * Returns a list of those methods of a class which are decorated
     * with the {@link uk.ac.starlink.util.ConfigMethod} annotation.
     *
     * @param  clazz  class
     * @return  ordered list of configuration methods
     */
    private static List<Method> getConfigMethods( Class<?> clazz ) {
        return Arrays.stream( clazz.getMethods() )
              .filter( m -> m.getAnnotation( ConfigMethod.class ) != null )
              .sorted( Comparator
                      .comparingInt( m -> m.getAnnotation( ConfigMethod.class )
                                           .sequence() ) )
              .collect( Collectors.toList() );
    }

    /**
     * Tries to get a suitable string representation of an example setting
     * for a mutator method.  Uses bean reflection and/or ConfigMethod
     * annotation.
     *
     * @param  handler   handler object
     * @param  mutatorMethod  setXxx method to apply to handler
     * @return   example value string, or null
     */
    private static String getExampleValue( Object handler,
                                           Method mutatorMethod ) {
        ConfigMethod ann = mutatorMethod.getAnnotation( ConfigMethod.class );
        String annExample = ann.example();
        if ( annExample != null && annExample.length() > 0 ) {
            return annExample;
        }
        String mutatorName = mutatorMethod.getName();
        Class<?> hclazz = handler.getClass();
        if ( mutatorName.startsWith( "set" ) ) {
            Object value;
            try {
                value = hclazz
                       .getMethod( "get" + mutatorName.substring( 3 ),
                                   new Class<?>[ 0 ] )
                       .invoke( handler, new Object[ 0 ] );
            }
            catch ( ReflectiveOperationException e ) {
                try {
                    value = hclazz
                           .getMethod( "is" + mutatorName.substring( 3 ),
                                       new Class<?>[ 0 ] )
                           .invoke( handler, new Object[ 0 ] );
                }
                catch ( ReflectiveOperationException e2 ) {
                    return null;
                }
            }
            return value.toString();
        }
        else {
            return null;
        }
    }

    /**
     * Indicates whether a given handler can auto-detect compliant tables
     * using their magic number.
     *
     * @param   handler  input handler
     * @return   true iff it's capable of content-based auto-detection
     */
    private boolean isAutoDetect( TableBuilder handler ) {
        return tfact_.getDefaultBuilders().contains( handler );
    }

    /**
     * Converts text which may or may not be XML to p-level XML.
     *
     * @param  txt  input text
     * @return  txt if it looks like plain text, or wrapped in a p element
     *          if it looks like XML
     */
    private static String asXml( String txt ) {
        return txt.startsWith( "<" )
             ? txt
             : new StringBuffer()
              .append( "<p>" )
              .append( txt.replaceAll( "&", "&amp;" )
                          .replaceAll( "<", "&lt;" )
                          .replaceAll( ">", "&gt;" ) )
              .append( "</p>" )
              .toString();
    }

    /**
     * Utility functional interface; like Function but throws an IOException.
     */
    @FunctionalInterface
    private interface IOFunction<T,R> {
        R apply( T t ) throws IOException;
    }

    /**
     * Outputs generic I/O handler documentation as supplied.
     *
     * @param  filePrefix   prefix for per-handler output filenames,
     *                      or null to send it all to System.out
     * @param  names    handler names, one output item for each
     * @param  docClazz  type of handler for which to generate documentation
     * @param  getHandler  obtains a handler instance from a handler name
     * @param  getXml   obtains XML documentation from a handler instance
     */
    private static <T> void writeDocs( String filePrefix, List<String> names,
                                       Class<?> docClazz,
                                       IOFunction<String,T> getHandler,
                                       IOFunction<T,String> getXml )
            throws IOException {
        boolean isFileOutput = filePrefix != null;
        for ( String name : names ) {
            name = name.toLowerCase();
            T handler = getHandler.apply( name );
            if ( docClazz.isInstance( handler ) ) {
                final OutputStream out;
                if ( isFileOutput ) {
                    File file = new File( filePrefix + name + ".xml" );
                    System.out.println( "Writing " + file );
                    out = new FileOutputStream( file );
                }
                else {
                    out = System.out;
                    System.out.println( "-------------------------------" );
                    System.out.println( "* " + name );
                    System.out.println( "-------------------------------" );
                }
                out.write( getXml.apply( handler ).getBytes( UTF8 ) );
                if ( isFileOutput ) {
                    out.close();
                }
            }
            else {
                System.err.println( "No XML for undocumented handler " + name );
            }
        }
    }

    /**
     * Writes documentation for all handlers.
     * Depending on presence of "-[no]files" flag will write to
     * standard output or to individual named files in the current directory.
     * Run with -help for usage.  
     */
    public static void main( String[] args ) throws IOException {
        String usage = "\n   Usage: " + HandlerDoc.class.getSimpleName()
                     + " [-[no]files]"
                     + " [-in|-out|-inout]"
                     + "\n";

        /* Process command-line arguments. */
        List<String> argList = new ArrayList<>( Arrays.asList( args ) );
        boolean toFiles = false;
        boolean doIn = true;
        boolean doOut = true;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( "-files".equals( arg ) ) {
                it.remove();
                toFiles = true;
            }
            else if ( "-nofiles".equals( arg ) ) {
                it.remove();
                toFiles = false;
            }
            else if ( "-in".equals( arg ) ) {
                it.remove();
                doIn = true;
                doOut = false;
            }
            else if ( "-out".equals( arg ) ) {
                it.remove();
                doOut = true;
                doIn = false;
            }
            else if ( "-inout".equals( arg ) ) {
                it.remove();
                doIn = true;
                doOut = true;
            }
        }
        if ( argList.size() > 0 ) {
            System.err.println( usage );
            return;
        }

        /* Set up documenter instance. */
        URL exampleUrl = HandlerDoc.class.getResource( "animals.vot" );
        StarTableFactory tfact = new StarTableFactory();
        StarTableOutput tout = new StarTableOutput();
        StarTable exTable =
            tfact.makeStarTable( new URLDataSource( exampleUrl ), "votable" );
        HandlerDoc hdoc = new HandlerDoc( tfact, tout, exTable );

        /* Write input handler documentation. */
        if ( doIn ) {
            writeDocs( toFiles ? "in-" : null, tfact.getKnownFormats(),
                       DocumentedTableBuilder.class,
                       tfact::getTableBuilder, hdoc::getBuilderDoc );
        }

        /* Write output handler documentation. */
        if ( doOut ) {
            List<String> ofmts = tout.getKnownFormats();
            ofmts.remove( "jdbc" );
            writeDocs( toFiles ? "out-" : null, ofmts,
                       DocumentedIOHandler.class,
                       tout::getHandler, hdoc::getWriterDoc );
        }
    }
}
