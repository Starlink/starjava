package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayFactory;
import uk.ac.starlink.array.ArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Type;

/**
 * Turns URLs which reference HDS array resources into NDArray objects. 
 * <p>
 * URLs are given in the format:
 * <blockquote>
 *    <i>container</i><tt>.sdf</tt>
 * </blockquote>
 * or
 * <blockquote>
 *    <i>container</i><tt>.sdf#</tt><i>path</i>
 * </blockquote>
 * where the <i>container</i><tt>.sdf</tt> part is a full absolute or
 * relative URL referring
 * to the HDS container file, and the optional fragment identifier
 * gives the HDS path within that container file in the traditional
 * dot-separated format.  If there is no fragment identifier 
 * (no <tt>#</tt>), the object at the top level of the HDS container
 * file is understood.
 * <p>
 * This is a singleton class; use {@link getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 * @see  HDSReference
 */
public class HDSArrayFactory implements ArrayFactory {

    /** Sole instance of the class. */
    private static HDSArrayFactory instance = new HDSArrayFactory();

    /**
     * Private sole constructor.
     */
    private HDSArrayFactory() {}

    /**
     * Returns an HDSArrayFactory.
     *
     * @return   the sole instance of this class
     */
    public static HDSArrayFactory getInstance() {
        return instance;
    }

    public NDArray makeNDArray( URL url, AccessMode mode ) throws IOException {

        /* Parse the URL as a reference to an HDS resource, or bail out
         * if this is not possible. */
        ParsedURL purl = ParsedURL.parseURL( url );
        if ( purl == null ) {
            return null;
        }
        URL container = purl.container;
        String[] hpath = purl.hdsPath;

        /* Get a file from which to form the HDSReference. */
        File file = null;
        boolean isTemporary = false;

        /* If the URL represents a local file, check it's readable and
         * proceed. */
        if ( container.getProtocol().equals( "file" ) ) {
            isTemporary = false;
            file = new File( container.getPath() );
            if ( ! file.exists() ) {
                throw new FileNotFoundException( 
                    "No such file '" + file + "'" );
            }
            if ( ! file.canRead() ) {
                throw new IOException(
                    "File not readable '" + file + "'" );
            }
        }

        /* If not, we will have to create a temporary file and copy the
         * contents of the URL into it. */
        else {
            try {
                isTemporary = true;
                file = File.createTempFile( "HDS", ".sdf" );
                file.deleteOnExit();
                copyStreams( container.openStream(),
                             new FileOutputStream( file ) );
            }
            catch ( IOException e ) {
                if ( isTemporary && file != null ) {
                    file.delete();
                }
                throw e;
            }
        }

        try {

            /* Get a readable HDSObject now. */
            HDSReference href = new HDSReference( file, hpath );
            HDSObject aryObj = href.getObject( "READ" );
            ArrayStructure ary = new ArrayStructure( aryObj );

            /* Make the array's data array the primary locator. */
            ary.getData().datPrmry( true );

            /* Construct an ArrayImpl, which will remove any temporary file
             * when it is finished with. */
            final boolean isTemp = isTemporary;
            final File fil = file;
            ArrayImpl impl = new HDSArrayImpl( ary, AccessMode.READ ) {
                public void close() throws IOException {
                    super.close();
                    if ( isTemp ) {
                        fil.delete();
                    }
                }
            };

            /* Return an NDArray. */
            return new BridgeNDArray( impl, url );
        }

        /* Tidy up the temporary file if we failed. */
        catch ( HDSException e ) {
            if ( isTemporary ) {
                file.delete();
            }
            throw (IOException) new IOException().initCause( e );
        }
    }


    public NDArray makeNewNDArray( URL url, NDShape shape, Type type )
            throws IOException {

        /* Parse the URL as a reference to an HDS resource, or bail out
         * if this is not possible. */
        ParsedURL purl = ParsedURL.parseURL( url );
        if ( purl == null ) {
            return null;
        }
        URL container = purl.container;
        String[] hpath = purl.hdsPath;

        /* If the URL represents a local file, all well and good. */
        File file;
        if ( container.getProtocol().equals( "file" ) ) {
            file = new File( container.getPath() );
        }

        /* Nope?  Should write a local copy and ensure this gets copied
         * across the net at a later date, but this is not yet implemented. */
        else {
            throw new UnsupportedOperationException(
                "Remote writable HDS files not yet supported" );
        }

        try {
            HDSObject aryObj;

            /* If the writable object is to exist at the top level of a
             * container file, create a new container file to hold it
             * (erasing the old one first if necessary) */
            if ( hpath.length == 0 ) {
                if ( file.exists() ) {
                    file.delete();
                }
                String cname = file.getName();
                cname = cname.substring( cname.length() - 4 );
                aryObj = HDSObject.hdsNew( file.getPath(), cname, "ARRAY",
                                           new long[ 0 ] );
            }

            /* Otherwise, create a new HDS structure to hold the writable
             * object, erasing any item by that name already existing in
             * its parent. */
            else {
                HDSReference href = new HDSReference( file, hpath );
                href.pop();
                HDSObject parent = href.getObject( "WRITE" );

                /* If an object with the name of the new one exists,
                 * erase it. */
                String aryName = hpath[ hpath.length - 1 ];
                if ( parent.datThere( aryName ) ) {
                    parent.datErase( aryName );
                }

                /* Create and locate a new HDSObject in which the array 
                 * will be stored. */
                parent.datNew( aryName, "ARRAY", new long[ 0 ] );
                aryObj = parent.datFind( aryName );
            }

            /* Make a new ArrayStructure in this object. */
            HDSType htype = HDSType.fromJavaType( type );
            ArrayStructure ary = new ArrayStructure( aryObj, shape, htype );

            /* Make its data array the primary locator. */
            ary.getData().datPrmry( true );

            /* Make an NDArray from it and return it. */
            ArrayImpl impl = new HDSArrayImpl( ary, AccessMode.WRITE );
            return new BridgeNDArray( impl, url );
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
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


    /**
     * Private helper class to parse the URL as a reference to an HDS object.
     * Gives you the URL of the HDS container file and the HDS path of
     * the target object within that container file.
     */
    private static class ParsedURL {
        final URL container;
        final String[] hdsPath;

        private ParsedURL( URL container, String[] hdsPath ) {
            this.container = container;
            this.hdsPath = hdsPath;
        }

        /**
         * Make a new ParsedURL object from a URL.  Return null if the
         * URL doesn't look like an HDS one.
         */
        static ParsedURL parseURL( URL url ) {

            /* Check it looks like an HDS URL. */
            if ( ! url.getPath().endsWith( ".sdf" ) ) {
                return null;
            }

            /* Parse the fragment, which should be an HDS path. */
            String frag = url.getRef();
            if ( frag == null ) {
                return new ParsedURL( url, new String[ 0 ] );
            }
            else {
                String base = url.toExternalForm();
                URL container;
                try {
                    container = 
                        new URL( base.substring( 0, base.indexOf( '#' ) ) );
                }
                catch ( MalformedURLException e ) {
                    throw new AssertionError( e );
                }
                StringTokenizer st = new StringTokenizer( frag, "." );
                String[] hdsPath = new String[ st.countTokens() ];
                for ( int i = 0; i < hdsPath.length; i++ ) {
                    hdsPath[ i ] = st.nextToken();
                }
                return new ParsedURL( container, hdsPath );
            }
        }
    }

}
