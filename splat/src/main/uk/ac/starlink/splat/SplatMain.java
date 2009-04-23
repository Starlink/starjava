/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     31-OCT-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat;

import java.awt.Frame;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;
import uk.ac.starlink.splat.iface.SplashWindow;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Main class for the SPLAT application. Use this to start SPLAT with
 * a splash screen as this class is optimised to produce a quick
 * startup, displaying an image in a {@link SplashWindow}. The more
 * usual entry point (i.e. without a splash screen) is the
 * {@link uk.ac.starlink.splat.iface.SplatBrowserMain} class.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SplatMain
{
    public static void main( String[] args )
    {
        //  Options that must be established before the UI is started.
        Loader.tweakGuiForMac();
	System.setProperty( "com.apple.mrj.application.apple.menu.about.name",
                            Utilities.getReleaseName() );

        //  Set User-Agent field so VO servers can identify SPLAT.
        Loader.setHttpAgent( Utilities.getReleaseName()
                                      .replaceAll( "\\s+", "_" )
                             + "/" + Utilities.getReleaseVersion() );

        //  Create and populate the SplashWindow.
        Frame splashFrame = null;
        URL imageURL = ImageHolder.class.getResource( "splash.gif" );
        if ( imageURL != null ) {
            splashFrame = SplashWindow.splash
                ( Toolkit.getDefaultToolkit().createImage( imageURL ) );
        }
        else {
            System.err.println( "Failed to locate splash image" );
        }

        //  Use reflection to load the real SPLAT entry point so that
        //  we do not pull in Swing until really necessary. Note do
        //  not use Class.forName(String) as we want to start from webstart.
        try {
            Class browserClass =
                Class.forName( "uk.ac.starlink.splat.iface.SplatBrowserMain",
                               true,
                               Thread.currentThread().getContextClassLoader());

            //  Find the "public static void main( String[] args )" method.
            Method browserMain = 
                browserClass.getMethod( "main", new Class[]{String[].class} );

            //  Invoke it with our args (maybe some spectra). Note the
            //  way that SPLAT is activated means that clicking on the
            //  image will not be seen until the main window is
            //  created, which is too late anyway (SPLAT is created on
            //  the event thread to avoid realization issues).
            browserMain.invoke( null, new Object[] { args } );
        }
        catch ( Throwable e ) {
            e.printStackTrace();
            System.err.flush();
            System.exit( 10 );
        }

        // All done so get rid of the SplashWindow.
        if ( splashFrame != null ) {
            splashFrame.dispose();
        }
    }
}
