package uk.ac.starlink.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.ivoa.registry.search.ParseException;
import net.ivoa.registry.search.Where2DOM;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Constructs SOAP requests for use with RI 1.0 registries.
 *
 * @author   Mark Taylor
 * @see  <a href="http://www.ivoa.net/Documents/RegistryInterface"
 *          >IVOA Registry Interface</a>
 */
public class RegistryRequestFactory {

    private static final String RS_NS =
        "http://www.ivoa.net/wsdl/RegistrySearch/v1.0";

    /**
     * Returns a SOAP request used for a registry Search query, given
     * an ADQL/S string.  Ray Plante's library is used for the conversion
     * to ADQL/X.
     *
     * @param  adqls  WHERE clause (minus WHERE) in ADQL specifying search
     * @return   SOAP request
     */
    public static SoapRequest adqlsSearch( String adqls ) throws IOException {
        String act = "Search";
        Where2DOM w2d = new Where2DOM( new StringReader( "where " + adqls ) );
        Element whereEl;
        try {
            whereEl = w2d.Where( null );
        }
        catch ( ParseException e ) {
           throw (IOException) new IOException( "ADQL Syntax Error" )
                               .initCause( e );
        }
        whereEl.setPrefix( "rs" );
        String body = new StringBuffer()
            .append( getStartEl( act ) )
            .append( nodeToString( whereEl ) )
            .append( getEndEl( act ) )
            .toString();
        return new DefaultSoapRequest( act, body );
    }

    /**
     * Returns a SOAP request used for a registry KeywordSearch query.
     *
     * @param  keywords  space-separated list of keywords, as required by 
     *         the KeywordSearch operation
     */
    public static SoapRequest keywordSearch( String[] keywords,
                                             boolean orValues )
            throws IOException {
        String act = "KeywordSearch";
        StringBuffer bbuf = new StringBuffer()
            .append( getStartEl( act ) )
            .append( "<orValues>" )
            .append( orValues ? "true" : "false" )
            .append( "</orValues>" )
            .append( "<keywords>" );
        for ( int ik = 0; ik < keywords.length; ik++ ) {
            if ( ik > 0 ) {
                bbuf.append( ' ' );
            }
            bbuf.append( keywords[ ik ].replaceAll( "&", "&amp;" )
                                       .replaceAll( "<", "&lt;" )
                                       .replaceAll( ">", "&gt;" ) );
        }
        bbuf.append( "</keywords>" )
            .append( getEndEl( act ) );
        return new DefaultSoapRequest( act, bbuf.toString() );
    }

    /**
     * Returns syntactically correct, but unsupported, RI operation.
     * Useful for testing purposes only.
     *
     * @return  bad RI SOAP request
     */
    public static SoapRequest illegalOperation() {
        String act = "DoWhat";
        String body = new StringBuffer()
            .append( getStartEl( act ) )
            .append( getEndEl( act ) )
            .toString();
        return new DefaultSoapRequest( act, body );
    }

    /**
     * Returns the start element for a given action.
     *
     * @param  act  unqualified action name
     * @return  start element string
     */
    private static String getStartEl( String act ) {
        return new StringBuffer()
            .append( "<rs:" )
            .append( act )
            .append( " " )
            .append( "xmlns:rs='" )
            .append( RS_NS )
            .append( "'>" )
            .toString();
    }

    /**
     * Returns the end element for a given action.
     *
     * @param  act  unqualified action name
     * @return  end element string
     */
    private static String getEndEl( String act ) {
        return "</rs:" + act + ">";
    }

    /**
     * Utility method to serialize a DOM Node to a String.
     *
     * @param  node  node to serialize
     * @return   string version
     */
    private static String nodeToString( Node node ) throws IOException {
        Source xsrc = new DOMSource( node );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Result xres = new StreamResult( bout );
        try {
            Transformer trans =
                TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty( "omit-xml-declaration", "yes" );
            trans.setOutputProperty( "indent", "yes" );
            trans.setOutputProperty( "encoding", "UTF-8" );
            trans.transform( xsrc, xres );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        bout.flush();
        return new String( bout.toByteArray(), "utf-8" );
    }

    /**
     * Simple implementation of SoapRequest.
     */
    private static class DefaultSoapRequest implements SoapRequest {
        private final String action_;
        private final String body_;

        /**
         * Constructor.
         *
         * @param  act  unqualified action name
         * @param  body  SOAP request body
         */
        DefaultSoapRequest( String act, String body ) {
            action_ = RS_NS + "#" + act;
            body_ = body;
        }

        public String getAction() {
            return action_;
        }

        public String getBody() {
            return body_;
        }
    }
}
