/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JUN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.util.prefs.Preferences;

/**
 * A singleton class for controlling the configuration of the web
 * proxy system properties. The values are stored as Preferences
 * associated with this class and can be restored to the related
 * System properties "http.proxySet", "http.proxyHost" and
 * "http.proxyPort" and saved back again.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ProxySetup
{
    /**
     * The instance.
     */
    private static ProxySetup instance = null;

    /**
     * Preferences instance for this class.
     */
    private static Preferences prefs =
        Preferences.userNodeForPackage( ProxySetup.class );

    // Name of proxy properties.
    private final static String PROXYSET = "http.proxySet";
    private final static String PROXYHOST = "http.proxyHost";
    private final static String PROXYPORT = "http.proxyPort";

    /**
     * Get a reference to the only instance of this class.
     */
    public static ProxySetup getInstance()
    {
        if ( instance == null ) {
            instance = new ProxySetup();
        }
        return instance;
    }

    /**
     * Create an instance.
     */
    private ProxySetup()
    {
        restore();
    }

    /**
     * Get if proxy use is enabled.
     */
    public boolean isProxySet()
    {
        String proxySet = System.getProperty( PROXYSET );
        if ( proxySet != null && proxySet.compareToIgnoreCase("true") == 0 ) {
            return true;
        }
        return false;
    }

    /**
     * Set if proxy use is enabled.
     */
    public void setProxySet( boolean set )
    {
        // Set backing store and system property for safety.
        prefs.putBoolean( PROXYSET, set );
        System.setProperty( PROXYSET, "" + set );
    }

    /**
     * Get the proxy host.
     */
    public String getProxyHost()
    {
        return System.getProperty( PROXYHOST );
    }

    /**
     * Set the proxy host.
     */
    public void setProxyHost( String host )
    {
        prefs.put( PROXYHOST, host );
        System.setProperty( PROXYHOST, host );
    }

    /**
     * Get the proxy port.
     */
    public String getProxyPort()
    {
        return System.getProperty( PROXYPORT );
    }

    /**
     * Set the proxy port.
     */
    public void setProxyPort( String port )
    {
        prefs.put( PROXYPORT, port );
        System.setProperty( PROXYPORT, port );
    }

    /**
     * Restore from backing store, updating the system properties.
     */
    public void restore()
    {
        String proxySet = prefs.get( PROXYSET, null );
        if ( proxySet != null ) {
            System.setProperty( PROXYSET, proxySet );
        }
        String proxyHost = prefs.get( PROXYHOST, null );
        if ( proxyHost != null ) {
            System.setProperty( PROXYHOST, proxyHost );
        }
        String proxyPort = prefs.get( PROXYPORT, null );
        if ( proxyPort != null ) {
            System.setProperty( PROXYPORT, proxyPort );
        }
    }

    /**
     * Save state of system properties to backing store.
     */
    public void store()
    {
        String proxySet = System.getProperty( PROXYSET );
        if ( proxySet != null ) {
            prefs.put( PROXYSET, proxySet );
        }
        String proxyHost = getProxyHost();
        if ( proxyHost != null ) {
            prefs.put( PROXYHOST, proxyHost );
        }
        String proxyPort = getProxyPort();
        if ( proxyPort != null ) {
            prefs.put( PROXYPORT, proxyPort );
        }
        try {
            prefs.flush();
        }
        catch (Exception e) {
            // Do nothing.
            e.printStackTrace();
        }
    }
}
