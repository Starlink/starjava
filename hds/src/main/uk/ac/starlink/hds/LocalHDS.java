package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.StringTokenizer;


/**
 * References a local HDS object addressed using a URL.  
 * Where necessary and possible it is copied across the network to make
 * it look local.
 */
class LocalHDS {

    private final URL container;
    private final String[] hdsPath;
    private HDSReference href;
    private boolean isTemporary;
    
    /**
     * Initialises the container and hdsPath members.
     */
    private LocalHDS( URL url ) {

        /* Parse the fragment, which should be an HDS path. */
        String frag = url.getRef();
        if ( frag == null ) {
            container = url;
            hdsPath = new String[ 0 ];
        }
        else {
            String base = url.toExternalForm();
            try {
                container =
                    new URL( base.substring( 0, base.indexOf( '#' ) ) );
            }
            catch ( MalformedURLException e ) {
                throw new AssertionError( e );
            }
            StringTokenizer st = new StringTokenizer( frag, "." );
            hdsPath = new String[ st.countTokens() ];
            for ( int i = 0; i < hdsPath.length; i++ ) {
                hdsPath[ i ] = st.nextToken();
            }
        }
    }

    public HDSReference getHDSReference() {
        return href;
    }

    public boolean isTemporary() {
        return isTemporary;
    }


    /**
     * Makes a local HDS object from a url representing an existing object.
     */
    public static LocalHDS getReadableHDS( URL url ) throws IOException {

        /* Check it looks like an HDS URL. */
        if ( ! url.getPath().endsWith( ".sdf" ) ) {
            return null;
        }

        LocalHDS lobj = new LocalHDS( url );

        /* If the URL represents a local file, check it's readable and
         * proceed. */
        if ( lobj.container.getProtocol().equals( "file" ) ) {
            boolean isTemp = false;

            //  Deal with embedded %20 space characters. These are
            //  not understood by HDS.
            String fileName = lobj.container.getPath();
            fileName = fileName.replaceAll( "%20", " " );
            File file = new File( fileName );
            if ( ! file.exists() ) {
                throw new FileNotFoundException(
                    "No such file '" + file + "'" );
            }
            if ( ! file.canRead() ) {
                throw new IOException(
                    "File not readable '" + file + "'" );
            }
            lobj.href = new HDSReference( new File( file.getAbsolutePath() ),
                                          lobj.hdsPath );
            lobj.isTemporary = false;
            return lobj;
        }

        /* If not, we will have to create a temporary file and copy the
         * contents of the URL into it. */
        else {
            File file = null;
            try {
                file = File.createTempFile( "HDS", ".sdf" );
                file.deleteOnExit();
                copyStreams( lobj.container.openStream(),
                             new FileOutputStream( file ) );
            }
            catch ( IOException e ) {
                if ( file != null ) {
                    file.delete();
                }
                throw e;
            }
            lobj.href = new HDSReference( new File( file.getAbsolutePath() ),
                                          lobj.hdsPath );
            lobj.isTemporary = true;
            return lobj;
        }
    }

    /**
     * Create a new HDS scalar structure, with a given type, at the URL given.
     */
    public static LocalHDS getNewHDS( URL url, String type ) 
            throws HDSException, IOException {

        /* Check it looks like an HDS URL. */
        if ( ! url.getPath().endsWith( ".sdf" ) ) {
            return null;
        }

        LocalHDS lobj = new LocalHDS( url );
        lobj.isTemporary = false;

        /* If the URL represents a local file, all well and good. */
        File file;
        if ( lobj.container.getProtocol().equals( "file" ) ) {
            file = new File( lobj.container.getPath() );
        }

        /* Nope?  Should write a local copy and ensure this gets copied
         * across the net at a later date, but this is not yet implemented. */
        else {
            throw new UnsupportedOperationException(
                "Remote writable HDS files not yet supported" );
        }

        /* If the writable object is to exist at the top level of a
         * container file, create a new container file to hold it
         * (erasing the old one first if necessary) */
        if ( lobj.hdsPath.length == 0 ) {
            if ( file.exists() ) {
                file.delete();
            }
            String cname = file.getName();
            cname = cname.substring( 0, Math.min( cname.length() - 4, 
                                                  HDSObject.DAT__SZNAM ) );
            HDSObject newobj = HDSObject.hdsNew( file.getPath(), cname, type,
                                                 new long[ 0 ] );
            lobj.href = new HDSReference( newobj );
            return lobj;
         }

        /* Otherwise, create a new HDS structure to hold the writable
         * object, erasing any item by that name already existing in
         * its parent. */
        else {
            HDSReference href = new HDSReference( file, lobj.hdsPath );
            href.pop();
            HDSObject parent = href.getObject( "WRITE" );

            /* If an object with the name of the new one exists,
             * erase it. */
            String newName = lobj.hdsPath[ lobj.hdsPath.length - 1 ];
            if ( parent.datThere( newName ) ) {
                parent.datErase( newName );
            }

            /* Create and locate a new HDSObject in which the array
             * will be stored. */
            parent.datNew( newName, type, new long[ 0 ] );
            lobj.href = href;
            return lobj;
        }
    }

    /**
     * Copies all the data from the input stream to the output stream.
     * Both streams are closed whatever happens.
     */
    private static void copyStreams( InputStream istrm, OutputStream ostrm )
            throws IOException {
        try {
            if ( ! ( istrm instanceof BufferedInputStream ) ) {
                istrm = new BufferedInputStream( istrm );
            }
            if ( ! ( ostrm instanceof BufferedOutputStream ) ) {
                ostrm = new BufferedOutputStream( ostrm );
            }
            for ( int b; ( b = istrm.read() ) >= 0; ostrm.write( b ) ) {}
            ostrm.flush();
        }
        finally {
            istrm.close();
            ostrm.close();
        }
    }


}
