package uk.ac.starlink.treeview.votable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.w3c.dom.Element;
import uk.ac.starlink.util.DOMUtils;

/**
 * Handler for a VOTable STREAM element.  This is capable of supplying
 * the data contained by the Stream as an InputStream object.
 */
public class Stream {

    private String actuate;
    private String href;
    private String encoding;
    private Element streamEl;

    /**
     * Construct a Stream object from the DOM node representing the STREAM
     * element.
     *
     * @param   streamEl  the STREAM Element
     * @throws  IllegalArgumentException   if streamEl is not a STREAM element.
     */
    public Stream( Element streamEl ) {
        this.streamEl = streamEl;
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
    public InputStream getInputStream()
            throws MalformedURLException, IOException {
        return obtainInputStream();
    }

    /**
     * Does the work of obtaining input stream which returns the bytes 
     * contained by this object.  Any required decoding is done here.
     */
    private InputStream obtainInputStream() 
            throws MalformedURLException, IOException {

        /* Get the source of bytes. */
        InputStream baseStrm;
        if ( href != null ) {
            URL url = new URL( href );
            baseStrm = url.openStream();
        }
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
