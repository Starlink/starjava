package uk.ac.starlink.votable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.URLUtils;

/**
 * Handler for a VOTable STREAM element.  This is capable of supplying
 * the data contained by the Stream as an InputStream object.
 *
 * @author   Mark Taylor (Starlink)
 */
public class Stream extends VOElement {

    private String actuate;
    private String href;
    private String encoding;
    private String systemId;
    private URL url;

    /**
     * Construct a Stream object from an XML Source representing the STREAM
     * element.  Note that the systemId of the Source will be used to 
     * resolve any URL in the STREAM's href attribute.
     *
     * @param   el  the DOM STREAM element
     * @param   systemId  the system ID used for resolving relative URIs
     * @throws  IllegalArgumentException   if xsrc does not contain 
     *          a STREAM element.
     */
    public Stream( Source xsrc ) {
        super( xsrc, "STREAM" );
        systemId = getSystemId();
        actuate = getAttribute( "actuate", "onRequest" );
        encoding = getAttribute( "encoding", "none" );
        href = getAttribute( "href" );
        if ( href != null ) {
            url = URLUtils.makeURL( systemId, href );
        }
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
     * Returns a <tt>DataSource</tt> corresponding to this stream.
     *
     * @return  a data source which can supply the data from this stream
     */
    public DataSource getDataSource() throws IOException {

        /* Return a FileDataSource if we can, since this is the only sort
         * which can be used without loading it all into memory. */
        if ( url != null && url.getProtocol().equals( "file" ) &&
             encoding.equals( "none" ) ) {
            DataSource datsrc = new FileDataSource( new File( url.getFile() ) );
            datsrc.setCompression( Compression.NONE );
            return datsrc;
        }

        /* Otherwise just supply the stream from here. */
        else {
            DataSource datsrc = new DataSource() {
                protected InputStream getRawInputStream() throws IOException {
                    return obtainInputStream();
                }
                public URL getURL() {
                    return url;
                }
            };
            datsrc.setCompression( Compression.NONE );
            return datsrc;
        }
    }

    /**
     * Does the work of obtaining input stream which returns the bytes 
     * contained by this object.  Any required decoding is done here.
     */
    private InputStream obtainInputStream() throws IOException {

        /* Get the source of bytes. */
        InputStream baseStrm;

        /* If there is an href attribute, stream data from the URL. */
        if ( url != null ) {
            baseStrm = url.openStream();
        }

        /* Otherwise, use the text content of the element itself. */
        else {
            baseStrm = getTextContentInputStream( getElement() );
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
