package uk.ac.starlink.splat.imagedata;
/**
 * Class for accessing NDFs.
 *
 */

import java.util.Vector;
import java.io.File;

public class NDF implements ImageData 
{
    /**
     *  Create an object for opening and accessing components
     *  of an NDF.
     *
     */
    public NDF( ) {
    }

    /**
     *  Open a named NDF.
     *
     * @param name The name of the NDF.
     */
    public boolean open( String name ) {
	boolean ok = true;
	File f = null;
	try {
	    f = new File( name );
	} 
	catch (Exception e ) {
	    System.out.println( e );
	    ok = false;
	}
	
	if ( ok ) {
	    ok = f.exists();
	}
	return ok;
    }

    /**
     *  References to the various elements of the NDF.
     */
    protected Vector refs;

    /**
     *  NDF identifier.
     */
    protected int ident = 0;
}
