package uk.ac.starlink.treeview.votable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * Handler for a VOTable STREAM element.  This is capable of supplying
 * the data contained by the Stream as an InputStream object.
 */
public class Stream {

    private String actuate;
    private String href;
    private String encoding;
    private Element streamEl;
    private String systemId;

    /**
     * Construct a Stream object from an XML Source representing the STREAM
     * element.  Note that the systemId of the Source will be used to 
     * resolve any URL in the STREAM's href attribute.
     *
     * @param   xsrc  an XML Source containing the STREAM Element
     * @throws  IllegalArgumentException   if xsrc does not contain 
     *          a STREAM element.
     */
    public Stream( Source xsrc ) {
        Node node;
        try {
            node = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw new IllegalArgumentException( "Not an element" );
        }
        if ( ! ( node instanceof Element ) ) {
            throw new IllegalArgumentException( "Not an element" );
        }
        this.streamEl = (Element) node;
        this.systemId = xsrc.getSystemId();
 
        if ( ! streamEl.getTagName().equals( "STREAM" ) ) {
            throw new IllegalArgumentException(
                "Element has tagname <" + streamEl.getTagName() + 
                "> not <STREAM>" );
        }

        actuate = streamEl.hasAttribute( "actuate" ) 
                ? streamEl.getAttribute( "actuate" ) : "onRequest";
        encoding = streamEl.hasAttribute( "encoding" ) 
                 ? streamEl.getAttribute( "encoding" ) : "none";
        href = streamEl.hasAttribute( "href" ) 
             ? streamEl.getAttribute( "href" ) : null;
    }

    /**
     * Returns an input stream containing the data of this Stream.
     * It will have been decoded if necessary.
     * 
     * @return  the input stream holding the Stream content
     */
    public InputStream getInputStream() throws IOException {
        return obtainInputStream();
    }

    /**
     * Does the work of obtaining input stream which returns the bytes 
     * contained by this object.  Any required decoding is done here.
     */
    private InputStream obtainInputStream() throws IOException {

        /* Get the source of bytes. */
        InputStream baseStrm;

        /* If there is an href attribute, stream data from the URL. */
        if ( href != null ) {
            URL url = URLUtils.makeURL( systemId, href );
            baseStrm = url.openStream();
        }

        /* Otherwise, use the text content of the element itself. */
        else {
            baseStrm = getTextContentInputStream( streamEl );
        }

        /* Decode this if necessary. */
        InputStream usableStrm;
        if ( encoding.equals( "gzip" ) ) {
            usableStrm = new GZIPInputStream( baseStrm );
        }
        else if ( encoding.equals( "base64" ) ) {
            usableStrm = new Base64InputStream( baseStrm );
        }
        else {
            usableStrm = baseStrm;
        }
 
        /* Return ready to use stream. */
        return usableStrm;
    }


    /* not very efficient - should recode. */
    private static InputStream getTextContentInputStream( Element el ) {
        String allText = DOMUtils.getTextContent( el );
        int leng = allText.length();
        byte[] bytes = new byte[ leng ];
        for ( int i = 0; i < leng; i++ ) {
            bytes[ i ] = (byte) allText.charAt( i );
        }
        return new ByteArrayInputStream( bytes );
    }
}
