// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    XX-XXXX-XXX (Mark Taylor):
//       Original version.
//    18-JUN-2002 (Peter W. Draper):
//       Modelled from HdxTester by Mark.

package uk.ac.starlink.hdx;

import java.io.*;
import java.net.*;
import uk.ac.starlink.hds.*;
import uk.ac.starlink.hdx.array.*;
import uk.ac.starlink.util.Tester;
import uk.ac.starlink.ast.FrameSet;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

/**
 * Simple test class for the HDS WCS access. Just give it an NDF
 * and it should output the AST FrameSet as a text description.
 *
 * @author Mark Taylor
 * @author Peter W. Draper
 * @version $Id$
 */

class HdxWCSTester extends Tester
{
    private URL ndxurl;

    public static void main( String[] args )
    {
        if ( args.length == 0 ) {
            throw new Error( "usage: HdxWCSTester ndf [ndf] ..." );
        }
        for ( int i = 0; i < args.length; i++ ) {
            HdxWCSTester tester = new HdxWCSTester( args[ i ] );
            tester.doTest();
        }
    }

    private HdxWCSTester( String ndxstr )
    {
         try {
             this.ndxurl = makeURL( ndxstr );
         }
         catch ( MalformedURLException e ) {
             throw new Error( e.getMessage() );
         }
    }

    public void testScript() throws Throwable
    {
        testWCS();
    }

    private static URL makeURL( String loc ) throws MalformedURLException 
    {
        URL context = new File( "." ).toURI().toURL();
        return new URL( context, loc );
    }

    private void testWCS() throws Exception
    {
        logMessage( "WCS" );
        NdxImpl impl1 = new HdsNdxImpl( ndxurl, "READ" );
        Ndx ndx1 = new BridgeNdx( impl1 );

        FrameSet frameSet = ndx1.getWCS();
        frameSet.show();
    }
}
