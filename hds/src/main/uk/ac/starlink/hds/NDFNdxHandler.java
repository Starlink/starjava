package uk.ac.starlink.hds;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.hds.HDSObject;
import uk.ac.starlink.hds.HDSPackage;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxImpl;

/**
 * Turns URLs which reference NDF structures into Ndx objects.
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
 * This is a singleton class; use {@link #getInstance} to get an instance.
 *
 * @author    Mark Taylor (Starlink)
 * @see  HDSReference
 */
public class NDFNdxHandler implements NdxHandler {

    /** Sole instance of the class. */
    private static NDFNdxHandler instance = new NDFNdxHandler();

    /**
     * Private sole constructor.
     */
    private NDFNdxHandler() {}

    /**
     * Returns an NDFNdxHandler.
     *
     * @return   the sole instance of this class
     * @throws   LinkageError  if the JNIHDS package is not available
     */
    public static NDFNdxHandler getInstance() {
        if ( HDSPackage.isAvailable() ) {
            return instance;
        }
        else {
            throw (LinkageError) new LinkageError( "Native code for the JNIHDS "
                                                 + "package is not installed" );
        }
    }

    /**
     * Creates an NDX with the given mode from a URL referening an HDS file.
     * Note that the primary locator via which the resulting NDX is 
     * accessed will not be 
     * annulled except under control of the garbage collector at such
     * time as all references are known to have disappeared.
     * This means that a writable NDX may not get flushed until 
     * the GC wakes up or the controlling process ends.
     *
     * @param  url  the location of the HDS object
     * @param  mode  the read/write/update mode for data access 
     */
    public Ndx makeNdx( URL url, AccessMode mode ) throws IOException {
        if ( mode != AccessMode.READ && ! url.getProtocol().equals( "file" ) ) {
            throw new IOException( "Remote " + mode + " access not supported "
                                 + "for HDS files" );
        }
        LocalHDS lobj = LocalHDS.getReadableHDS( url );
        if ( lobj == null ) {
            return null;
        }
        HDSReference href = lobj.getHDSReference();
        final File file = href.getContainerFile();
        final boolean isTemp = lobj.isTemporary();

        try {
            /* Construct an NdxImpl which will remove any temporary file
             * when it is finalized. */
            NdxImpl impl = new NDFNdxImpl( href, url, mode ) {
                public void finalize() throws Throwable {
                    try {
                        super.finalize();
                    }
                    finally {
                        if ( isTemp ) {
                            file.delete();
                        }
                    }
                }
            };

           /* Return an Ndx. */
           return new BridgeNdx( impl );
        }

        /* Tidy up the temporary file if we failed. */
        catch ( HDSException e ) {
            if ( isTemp ) {
                file.delete();
            }
            throw (IOException) new IOException().initCause( e );
        }
    }

    /**
     * Constructs an Ndx based on an existing HDS object.
     *
     * @param  hobj  the HDS object to be viewed as an Ndx
     * @param  persistentURL  the URL at which this NDX persists.
     *         Use <tt>null</tt> if <tt>hobj</tt> resides in a temporary file
     *         or is otherwise transient.
     * @param  mode  the read/write/update mode for the Ndx array data
     * @return  the new Ndx based on <tt>hobj</tt>, or <tt>null</tt> 
     *          if it doesn't look like an NDF
     * @throws  HDSException  if there is an error in HDS
     * @throws  IllegalArgumentException  if <tt>hobj</tt> doesn't look like
     *          and NDF structure
     */
    public Ndx makeNdx( HDSObject hobj, URL persistentURL, AccessMode mode )
            throws HDSException {
        try {
            return new BridgeNdx( new NDFNdxImpl( hobj, persistentURL, mode ) );
        }
        catch ( IllegalArgumentException e ) {
            return null;
        }
    }

    public boolean makeBlankNdx( URL url, Ndx template ) throws IOException {
        try {
            LocalHDS lobj = LocalHDS.getNewHDS( url, "NDF" );
            if ( lobj == null ) {
                return false;
            }
            HDSReference href = lobj.getHDSReference();
            HDSObject place = href.getObject( "WRITE" );
            new NdfMaker().makeBlankNDF( template, place );
            return true;
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public boolean outputNdx( URL url, Ndx orig ) throws IOException {
        try {
            LocalHDS lobj = LocalHDS.getNewHDS( url, "NDF" );
            if ( lobj == null ) {
                return false;
            }
            HDSReference href = lobj.getHDSReference();
            HDSObject place = href.getObject( "WRITE" );
            new NdfMaker().makeNDF( orig, place );
            return true;
        }
        catch ( HDSException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
