package uk.ac.starlink.hds;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import uk.ac.starlink.array.AccessMode;

/**
 * Stores the location of an HDS object. 
 * This class can store information about where an HDSObject is stored,
 * including the container file it lives in and its location within that
 * (its HDS path).  Objects in this class, which are not immutable,
 * can be used to navigate up and down an HDS path in a stack-like
 * fashion, but pushing new elements on to the end of the HDS path
 * and popping them off.
 * <p>
 * It also deals with translating object location information to and 
 * from a URL.  Other classes ought to use this class to translate between
 * URLs and HDS paths so that the HDS URL format can be changed easily
 * in a single place.  The format of a URL representing an HDS location
 * is currently of the form <i>container.sdf#hdspath</i>.  This may
 * get changed.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HDSReference implements Cloneable {

    private File containerFile;
    private String containerName;
    private ArrayList pathList = new ArrayList();
    
    /**
     * Constructs a new HDSReference representing the top level object in an
     * HDS container file.
     *
     * @param  container   a File object giving the container file 
     *                     (including the .sdf extension)
     * @throws  IllegalArgumentException  if the filename does not end '.sdf'
     */
    public HDSReference( File container ) {
        setContainerFile( container );
    }

    private void setContainerFile( File container ) {
        this.containerFile = container;
        String cf = containerFile.toString();
        String exten = cf.substring( cf.length() - 4 );
        if ( ! exten.equalsIgnoreCase( ".sdf" ) ) {
            throw new IllegalArgumentException( 
                "HDS filename does not end in \".sdf\"" );
        }
        String cname = containerFile.getAbsolutePath();
        containerName = cname.substring( 0, cname.length() - 4 );
    }

    /**
     * Constructs an HDSReference given a container file and an HDS path
     * array within it.
     *
     * @param   container   a File object giving the container file 
     *                      (including the .sdf extension)
     * @param   path        an array of Strings each representing the name
     *                      of one level of the HDS path
     * @throws  IllegalArgumentException  if the filename does not
     *                      end in ".sdf"
     */
    public HDSReference( File container, String[] path ) {
        setContainerFile( container );
        for ( int i = 0; i < path.length; i++ ) {
            push( path[ i ] );
        }
    }

    /**
     * Constructs an HDSReference given a container file and an 
     * HDS path string within it.
     *
     * @param   container   a File object giving the container file 
     *                      (including the .sdf extension)
     * @param   path        a dot-separated string giving the HDS path
     * @throws  IllegalArgumentException  if the filename does not end '.sdf'
     */
    public HDSReference( File container, String path ) {
        setContainerFile( container );
        if ( path != null ) {
            StringTokenizer st = new StringTokenizer( path, "." );
            while ( st.hasMoreTokens() ) {
                push( st.nextToken() );
            }
        }
    }

    
    /**
     * Constructs an HDSRefernce from a 'traditional' HDS pathname.
     *
     * @param   path   a dot-separated string in which the container name
     *                 minus its '.sdf' forms the front and the HDS path
     *                 proper forms the back
     */
    public HDSReference( String path ) {
        boolean found = false;
        int lastSlash = path.lastIndexOf( File.pathSeparatorChar );
        for ( int pos = path.length(); 
              pos > lastSlash + 1; 
              pos = path.lastIndexOf( '.', pos - 1 ) ) {
            File file = new File( path.substring( 0, pos ) + ".sdf" );
            if ( file.exists() ) {
                setContainerFile( file );
                String subPath = path.substring( pos );
                StringTokenizer st = new StringTokenizer( subPath, "." );
                while ( st.hasMoreTokens() ) {
                    push( st.nextToken() );
                }
                found = true;
            }
        }

        /* Not found - assume that the first dot after the last slash
         * represents the end of the container file. */
        if ( ! found ) {
            int nextDot = path.indexOf( '.', lastSlash + 1 );
            if ( nextDot > 0 ) {
                File file = new File( path.substring( 0, nextDot ) + ".sdf" );
                setContainerFile( file );
                String subPath = path.substring( nextDot );
                StringTokenizer st = new StringTokenizer( subPath, "." );
                while ( st.hasMoreTokens() ) {
                    push( st.nextToken() );
                }
            }
            else {
                throw new IllegalArgumentException( "Cannot parse HDS path "
                                                    + '"' + path + '"' );
            }
        }
    }

    /**
     * Constructs an HDSReference from a URL.  Only URLs using the 
     * <code>file:</code> protocol are supported.
     *
     * @param   url   a URL describing the location of the HDSObject
     * @throws  UnsupportedOperationException if the protocol
     *          of <code>url</code> is not <code>file</code>
     */
    public HDSReference( URL url ) {
        this( getFileFromURL( url ), url.getRef() );
    }

    private static File getFileFromURL( URL url ) {
        String protocol = url.getProtocol();
        if ( protocol.equals( "file" ) ) {
            //  Deal with any embedded %20 space characters. These
            //  are not understood by HDS.
            String fileName = url.getFile();
            fileName = fileName.replaceAll( "%20", " " );
            return new File( fileName );
        }
        else {
            throw new UnsupportedOperationException(
                "URL protocol '" + protocol + "' not supported" );
        }
    }

    /**
     * Constructs an HDSReference from an existing HDSObject.
     *
     * @param  hobj  the object whose location is to be referenced.
     * @throws HDSException   if an HDS error occurs
     */
    public HDSReference( HDSObject hobj ) throws HDSException {

        /* Find the path and container filename of the HDS object. */
        String[] trace = new String[ 2 ];
        hobj.hdsTrace( trace );
        String path = trace[ 0 ];
        String file = trace[ 1 ];

        /* Now set the HDSReference's filename and path from that. */
        setContainerFile( new File( file ) );
        StringTokenizer st = new StringTokenizer( path, "." );

        /* Skip the first path element since it is implicit. */
        if ( st.hasMoreTokens() ) {
            st.nextToken();
        }

        /* Push the rest on to the stack. */
        while ( st.hasMoreTokens() ) {
            push( st.nextToken() );
        }
    }

    /**
     * Returns the container file associated with this HDSReference
     * (including the '.sdf' extension).
     *
     * @return   the file
     */
    public File getContainerFile() {
        return containerFile;
    }

    /**
     * Returns the HDS-style name of the container file associated with 
     * this HDSReference - this does <i>not</i> include the '.sdf'
     * extension.
     *
     * @return  container name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Returns the HDS path of this HDSReference as an array of Strings.
     *
     * @return   an array in which the first element is the name of the topmost
     *           part of the HDS path etc.
     */
    public String[] getPath() {
        return (String[]) pathList.toArray( new String[ 0 ] );
    }

    /**
     * Returns a URL which describes this HDSReference.
     *
     * @return   a URL
     */
    public URL getURL() {
        URL url;
        StringBuffer frag = new StringBuffer();
        for ( int i = 0; i < pathList.size(); i++ ) {
            frag.append( ( i > 0 ) ? "." : "" )
                .append( (String) pathList.get( i ) );
        }
        try {
           url = containerFile.toURL();
           url = new URL( url, "#" + frag.toString() );
        }
        catch ( MalformedURLException e ) {
            throw new AssertionError( "Unexpected malformed URL for " + this );
        }
        return url;
    }

    /**
     * Opens a new primary HDSObject at the location referenced by this
     * HDSReference.
     *
     * @param  accessMode  the HDS access mode for the hdsOpen call -
     *                     "READ", "WRITE" or "UPDATE"
     */
    public HDSObject getObject( String accessMode ) throws HDSException {
        HDSObject cntnr = HDSObject.hdsOpen( containerName, accessMode );
        HDSObject e1 = cntnr.datClone();
        e1.datPrmry( false );
        HDSObject e2;
        for ( int i = 0; i < pathList.size(); i++ ) {
            e2 = e1.datFind( (String) pathList.get( i ) );
            e1 = e2;
        }
        e1.datPrmry( true );
        return e1;
    }

    public String toString() {
        StringBuffer s = new StringBuffer( containerName );
        for ( int i = 0; i < pathList.size(); i++ ) {
            s.append( "." ).append( pathList.get( i ) );
        }
        return s.toString();
    }

    /**
     * Adds a new path element to the end of the current path, thus 
     * navigating down the HDS hierarchy.
     *
     * @param  pathEl  the new path element to add
     */
    public void push( String pathEl ) {
        pathList.add( pathEl );
    }

    /**
     * Removes an element from the end of the current path, thus
     * navigating up the HDS hierarchy.
     *
     * @return  the element which is removed
     */
    public String pop() {
        return (String) pathList.remove( pathList.size() - 1 );
    }

    /**
     * Returns a copy of this object - modifying the returned object will
     * not affect the original one.
     *
     * @return   a clone of this HDSReference
     */
    public Object clone() {
        try {
            HDSReference copy = (HDSReference) super.clone();
            copy.pathList = (ArrayList) pathList.clone();
            return copy;
        }
        catch ( CloneNotSupportedException e ) {
            // assert false;
            throw new AssertionError();
        }
    }

    /**
     * Gets an HDS access mode string ("READ"/"WRITE"/"UPDATE") from 
     * an AccessMode.
     *
     * @param  mode   the AccessMode object
     * @return   mode string
     */
    static String hdsMode( AccessMode mode ) {
        if ( mode == AccessMode.READ ) {
            return "READ";
        }
        else if ( mode == AccessMode.WRITE ) {
            return "WRITE";
        }
        else if ( mode == AccessMode.UPDATE ) {
            return "UPDATE";
        }
        else {
            return null;
        }
    }


}
