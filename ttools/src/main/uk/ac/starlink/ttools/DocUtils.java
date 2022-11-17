package uk.ac.starlink.ttools;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import uk.ac.starlink.table.Documented;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.filter.BasicFilter;
import uk.ac.starlink.util.XmlWriter;

/**
 * Utilities used for automatically-generated documentation.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2006
 */
public class DocUtils {

    /**
     * Private sole constructor prevents instantiation.
     */
    private DocUtils() {
    }

    /**
     * Concatenates an array of strings, appending a carriage return
     * to each one.
     *
     * @param   lines  array of input strings
     * @return  one long output string
     */
    public static String join( String[] lines ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < lines.length; i++ ) {
            sbuf.append( lines[ i ] )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Provides a snippet of XML which references a processing filter.
     *
     * @param  filter  processing filter
     * @return  filter reference
     */
    public static String filterRef( BasicFilter filter ) {
        String name = filter.getName();
        return new StringBuffer()
            .append( "<code>" )
            .append( "<ref id=\"" )
            .append( name )
            .append( "\">" )
            .append( name )
            .append( "</ref>" )
            .append( "</code>" )
            .toString();
    }

    /**
     * Provides a snippet of XML which references a named
     * {@link uk.ac.starlink.ttools.mode.ProcessingMode}.
     *
     * @param name  mode name
     * @return  mode reference
     */
    public static String modeRef( String name ) {
        return new StringBuffer()
            .append( "<code>" )
            .append( "<ref id=\"mode-" )
            .append( name )
            .append( "\">" )
            .append( name )
            .append( "</ref>" )
            .append( "</code>" )
            .toString();
    }

    /**
     * Returns a string listing the supplied array of metadata objects.
     * The returned string should be suitable for inserting into XML text.
     *
     * @param  infos  array of infos
     * @return  string listing <code>infos</code> by name
     */
    public static String listInfos( ValueInfo[] infos ) {
        return describedList( infos, ValueInfo::getName,
                              ValueInfo::getDescription, false );
    }

    /**
     * Returns a XML string listing an array of objects
     * with names and descriptions.
     * The output is a <code>&lt;ul&gt;</code> element.
     *
     * <p>This is a convenience wrapper for the overloaded method
     * that takes a <code>Collection&lt;E&gt;</code>.
     *
     * @param   items   objects to list
     * @param   namer   maps object to object name
     * @param   describer  maps object to object description
     * @param   isDescriptionXml  if true, the description text may contain
     *                            XML formatting, and should be inserted
     *                            into the output as is;
     *                            if false, any magic characters will be
     *                            escaped as required for XML
     * @return  xml list
     */
    public static <E> String describedList( E[] items,
                                            Function<E,String> namer,
                                            Function<E,String> describer,
                                            boolean isDescriptionXml ) {
        return describedList( Arrays.asList( items ), namer, describer,
                              isDescriptionXml );
    }

    /**
     * Returns a XML string listing an array of objects
     * with names and descriptions.
     * The output is a <code>&lt;ul&gt;</code> element.
     *
     * @param   items   objects to list
     * @param   namer   maps object to object name
     * @param   describer  maps object to object description
     * @param   isDescriptionXml  if true, the description text may contain
     *                            XML formatting, and should be inserted
     *                            into the output as is;
     *                            if false, any magic characters will be
     *                            escaped as required for XML
     * @return  xml list
     */
    public static <E> String describedList( Collection<E> items,
                                            Function<E,String> namer,
                                            Function<E,String> describer,
                                            boolean isDescriptionXml ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<ul>\n" );
        for ( E item : items ) {
            String name = namer.apply( item );
            String descrip = describer.apply( item );
            sbuf.append( "<li><code>" )
                .append( name )
                .append( "</code>: " )
                .append( isDescriptionXml ? descrip
                                          : XmlWriter.formatText( descrip ) )
                .append( "\n</li>" )
                .append( "\n" );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }

    /**
     * Returns the XML description provided by an item implementing the
     * Documented interface, in a form suitable for insertion into
     * the STILTS user document.
     *
     * <p>Note the implementation of this is currently somewhat scrappy;
     * it works on the things that it's called upon to transform during
     * the STILTS user document build, but it may need extra work if
     * some less constrained XHTML gets fed to it.  If that becomes the case,
     * problems should show up at TTOOLS package build/test time.
     *
     * @param   item  supplier of (XHTML-like) documentation XML
     * @return   SUN-compliant XML
     */
    public static String getXmlDescription( Documented item )
            throws IOException {
        try {
            return fromXhtml( item.getXmlDescription() );
        }
        catch ( TransformerException e ) {
            throw new IOException( "XSLT trouble", e );
        }
    }

    /**
     * Does minimal conversion from XHTML-like XML to SUN-friendly XML.
     * Since this works on strings, it's not intended for huge documents.
     *
     * @param  xhtml  input XHTML-like XML string, expected to be a sequence
     *                of P elements
     * @return  output SUN-friendly XML string
     */
    public static String fromXhtml( String xhtml )
            throws IOException, TransformerException {

        /* Need to wrap the content in a top-level document, since it may
         * consist of a sequence of siblings rather than a single element. */
        String wrapTag = "wrap-doc";
        String wrapXhtml = "<" + wrapTag + ">" + xhtml + "</" + wrapTag + ">";

        /* Transform using custom XSLT. */
        URL xsltUrl = DocUtils.class.getResource( "fromXhtml.xslt" );
        Source xsltSrc = new StreamSource( xsltUrl.openStream() );
        Source docSrc = new StreamSource( new StringReader( wrapXhtml ) );
        Transformer trans =
            TransformerFactory.newInstance().newTransformer( xsltSrc );
        StringWriter out = new StringWriter();
        Result docRes = new StreamResult( out );
        trans.transform( docSrc, docRes );
        out.close();

        /* Return result. */
        return out.getBuffer().toString();
    }
}
