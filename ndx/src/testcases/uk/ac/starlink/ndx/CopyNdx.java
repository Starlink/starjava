package uk.ac.starlink.ndx;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.util.SourceReader;

public class CopyNdx {

    private NdxIO ndxio = new NdxIO();
    private SourceReader sr = new SourceReader().setIndent( 2 );

    public static void main( String[] args ) 
            throws IOException, MalformedURLException {
        CopyNdx cndx = new CopyNdx();
        if ( args.length == 1 ) {
            cndx.writeXML( new URL( args[ 0 ] ), System.out );
        }
        else if ( args.length == 2 ) {
            cndx.makeCopy( new URL( args[ 0 ] ), new URL( args[ 1 ] ) );
        }
        else {
            throw new IllegalArgumentException( "Usage: CopyNdx url [url]" );
        }
    }

    public void makeCopy( URL inURL, URL outURL ) throws IOException {
        Ndx indx = ndxio.makeNdx( inURL, AccessMode.READ );
        if ( indx == null ) {
            throw new IOException( "Failed to resolve URL " + inURL );
        }
        ndxio.outputNdx( outURL, indx );
    }

    public void writeXML( URL inURL, OutputStream ostrm ) throws IOException {
        Ndx indx = ndxio.makeNdx( inURL, AccessMode.READ );
        try {
            sr.writeSource( indx.toXML( null ), ostrm );
        }
        catch ( TransformerException e ) {
            throw (IOException) new IOException()
                 .initCause( e );
        }
    }

}
