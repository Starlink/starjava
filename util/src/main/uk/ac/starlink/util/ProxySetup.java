/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-JUN-2003 (Peter W. Draper):
 *       Original version.
 *     18-JUL-2003 (Peter W. Draper):
 *       Added nonProxyHosts.
 */
package uk.ac.starlink.util;

import java.util.prefs.Preferences;

/**
 * A singleton class for controlling the configuration of the web
 * proxy system properties. The values are stored as Preferences
 * associated with this class and can be restored to the related
 * System properties "http.proxySet", "http.proxyHost",
 * "http.proxyPort" and nonProxyHosts and saved back again.
 * <p>
 * To enable any stored proxy setup just do:
 * <pre>
 *    ProxySetup.getInstance().restore();
 * </pre>
 * Sometime during application startup. Note that this will supercede
 * any system properties already set (but only if any Preferences have
 * been defined).
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
    private final static String NONPROXYHOSTS = "http.nonProxyHosts";

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
     * Get the hosts that should not be proxied.
     */
    public String getNonProxyHosts()
    {
        return System.getProperty( NONPROXYHOSTS );
    }

    /**
     * Set the hosts that shouldn't use the proxy. Note this is a list
     * of names, separated by |, and possibly including a wildcard,
     * e.g. "*.dur.ac.uk|localhost".
     */
    public void setNonProxyHosts( String nohosts )
    {
        prefs.put( NONPROXYHOSTS, nohosts );
        System.setProperty( NONPROXYHOSTS, nohosts );
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
        String nonProxyHosts = prefs.get( NONPROXYHOSTS, null );
        if ( nonProxyHosts != null ) {
            System.setProperty( NONPROXYHOSTS, nonProxyHosts );
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
        String nonProxyHosts = getNonProxyHosts();
        if ( nonProxyHosts != null ) {
            prefs.put( NONPROXYHOSTS, nonProxyHosts );
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
