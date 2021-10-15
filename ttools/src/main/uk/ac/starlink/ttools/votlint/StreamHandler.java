package uk.ac.starlink.ttools.votlint;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.xml.sax.Locator;
import uk.ac.starlink.util.Base64InputStream;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.PipeReaderThread;
import uk.ac.starlink.util.URLUtils;

/**
 * Handler for STREAM elements.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Apr 2005
 */
public class StreamHandler extends ElementHandler {

    private PipeReaderThread pipeReader_;
    private OutputStream dataSink_;

    public void startElement() {

        /* Get and check href and encoding attributes. */
        String href = getAttribute( "href" );
        String encoding = getAttribute( "encoding" );
        if ( ( href == null || href.trim().length() == 0 ) && 
             ! "base64".equals( encoding ) ) {
            warning( new VotLintCode( "N64" ),
                     this + " has no href but encoding is not base64" );
        }

        ElementHandler parent = getAncestry().getParent();
        if ( parent instanceof StreamingHandler ) {
            final StreamingHandler streamer = (StreamingHandler) parent;

            /* If we have the data specified by URL, feed the resulting
             * decoded input stream to the stream consumer directly. */
            if ( href != null ) {
                Locator locator = getContext().getLocator();
                String systemId = locator == null ? null 
                                                  : locator.getSystemId();
                URL url = URLUtils.makeURL( systemId, href );
                if ( url != null ) {
                    InputStream in = null;
                    try {
                        in = url.openStream();
                        in = new BufferedInputStream( in );
                        in = decodeStream( in );
                        streamer.feed( in );
                    }
                    catch ( IOException e ) {
                        warning( new VotLintCode( "IOE" ),
                                 "Read error for external stream " + e );
                    }
                    finally {
                        if ( in != null ) {
                            try {
                                in.close();
                            }
                            catch ( IOException e ) {
                                // ignore.
                            }
                        }
                    }
                }
                else {
                    error( new VotLintCode( "HRQ" ),
                           "Can't make sense of href '" + href + "'" );
                }
            }

            /* Otherwise, we must expect the data to appear in the STREAM
             * element's content.  Set up reader which will be fed bytes
             * from this handler's characters() method during the parse. */
            else {
                try {
                    pipeReader_ = new PipeReaderThread() {
                        protected void doReading( InputStream in )
                                throws IOException {
                            in = new BufferedInputStream( in );
                            in = decodeStream( in );
                            streamer.feed( in );
                        }
                    };
                    pipeReader_.start();
                    dataSink_ = new BufferedOutputStream( pipeReader_
                                                         .getOutputStream() );
                }
                catch ( IOException e ) {
                    error( new VotLintCode( "SRQ" ),
                           "Trouble with STREAM content - can't validate" );
                    pipeReader_ = null;
                    dataSink_ = null;
                }
            }
        }
        else {
           error( new VotLintCode( "STS" ),
                  "Illegal parent of STREAM " + parent );
        }
    }

    public void characters( char[] ch, int start, int leng ) {

        /* Deal with any character content of the element by writing them
         * to the consumer stream we've set up. */
        if ( dataSink_ != null ) {
            try {
                for ( int i = 0; i < leng; i++ ) {
                    dataSink_.write( ch[ start++ ] );
                }
            }
            catch ( IOException e ) {
                // Ignore - it will be picked up by finishReading if it's
                // important.
            }
        }
        else {
            error( new VotLintCode( "SX1" ),
                   "Unexpected content of STREAM " +
                   "(stream with href should be empty)" );
        }
    }

    public void endElement() {

        /* Tidy up the consumer stream. */
        if ( dataSink_ != null ) {
            assert pipeReader_ != null;
            try {
                dataSink_.close();
                pipeReader_.finishReading();
            }
            catch ( IOException e ) {
                error( new VotLintCode( "STE" ), "Streaming error " + e );
            }
            dataSink_ = null;
            pipeReader_ = null;
        }
    }

    /**
     * Decode the binary data stream in accordance with the encoding attribute
     * on this element.
     *
     * @param   in  raw input stream
     * @return  decoded input stream
     */
    private InputStream decodeStream( InputStream in ) throws IOException {
        String encoding = getAttribute( "encoding" );
        if ( encoding == null ||
             encoding.equals( "none" ) ||
             encoding.trim().length() == 0 ) {
            return in;
        }
        else if ( encoding.equals( "base64" ) ) {
            return new Base64InputStream( in );
        }
        else if ( encoding.equals( "gzip" ) ) {
            return new GZIPInputStream( in );
        }
        else if ( encoding.equals( "dynamic" ) ) {
            warning( new VotLintCode( "DYF" ),
                     "Parser can't interpret dynamic stream " +
                     "encodings properly - sorry" );

            /* Pass it to the Compression class, which will probably be able
             * to work out the correct compression type based on content.
             * This shirks the validation though. */
            return Compression.decompressStatic( in );
        }
        else {
            error( new VotLintCode( "UKE" ),
                   "Unknown encoding type '" + encoding + "' (assume none)" );
            return in;
        }
    }
}
