// Testing: WCS access

import uk.ac.starlink.hdx.*;
import uk.ac.starlink.hdx.array.NDArray;
import uk.ac.starlink.ast.*;

import java.net.URL;
import java.util.List;

class t4 
{
    public static void main( String[] args ) 
    {
	if ( args.length < 1 ) {
	    System.err.println( "Too few arguments (need input files)" );
	    System.exit( 1 );
	}

	HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
	HdxContainer hdx;
	int exitstatus = 0;

	for ( int i = 0; i < args.length; i++ ) {
	    try {
		URL url = new URL( new URL( "file:." ), args[i] );
		hdx = hdxf.readHdx( url );
		List ndxlist = hdx.getNdxList();

		System.out.println( "===" );
		System.out.println( "File " + args[i] + " (URL " + url + ")" );
		for ( int ndxno = 0; ndxno < ndxlist.size(); ndxno++ ) {
		    System.out.println( "--- NDX " + ndxno );
		    Ndx ndx = (Ndx) ndxlist.get( ndxno );
		    showArray( "Data", ndx.getImage(), System.out );
		    showArray( "Variance", ndx.getVariance(), System.out );
		    showArray( "Quality", ndx.getQuality(), System.out );
                    showWCS( ndx.getWCS(), System.out );
		}
	    } 
            catch (java.net.MalformedURLException e) {
		System.err.println ("Malformed URL: " + e);
		exitstatus = 1;
	    } 
            catch (uk.ac.starlink.hdx.HdxException e) {
		System.err.println ("HDX error: " + e);
		exitstatus = 1;
	    }
	}
	System.exit( exitstatus );
    }

    private static void showArray( String name, NDArray a,
				   java.io.PrintStream o ) 
    {
	if ( a == null ) {
	    o.println( "  " + name + ": <null>" );
        }
	else {
	    o.println( "  " + name + " [" + a.getURL().toString()
                       + "]: " + a.toString() );
        }
    }

    private static void showWCS( FrameSet wcs, java.io.PrintStream o )
    {
	if ( wcs == null ) {
	    o.println( "  WCS: <null>");
        }
        else {
            o.println( "  FrameSet: " + wcs );
            o.println( "    Base domain: " + 
                       wcs.getFrame(FrameSet.AST__BASE).getDomain());
            o.println( "    Current domain: " + 
                       wcs.getFrame(FrameSet.AST__CURRENT).getDomain());
        }
    }
}
