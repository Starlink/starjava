/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: ProxyServerUtil.java,v 1.2 2002/07/09 13:30:37 brighton Exp $
 */

package jsky.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A utility class for managing access to a proxy server. If set, a proxy server
 * is used to access URLs, usually when a firewall is in place. The proxy server,
 * specified as a host name and port number, does the HTTP GET for us and returns
 * the result.
 *
 * @see jsky.util.gui.ProxyServerDialog
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class ProxyServerUtil {

    // Keys to use to save proxy settings
    private static final String PROXY_HOST = "http.proxyHost";
    private static final String PROXY_PORT = "http.proxyPort";
    private static final String NON_PROXY_HOSTS = "http.nonProxyHosts";


    /**
     * This method should be called once at startup, so that any previous
     * proxy settings are restored.
     */
    public static void init() {
        // Check for saved preferences
        String savedHost = Preferences.get(PROXY_HOST);
        String savedPort = Preferences.get(PROXY_PORT);
        String savedNonProxyHosts = Preferences.get(NON_PROXY_HOSTS);

        // Java properties (-D options) override saved preferences
        String hostOption = System.getProperty(PROXY_HOST);
        String portOption = System.getProperty(PROXY_PORT);
        String nonProxyHostsOption = System.getProperty(NON_PROXY_HOSTS);

        String host = hostOption;
        if (host == null)
            host = savedHost;
        if (host != null && host != hostOption)
            System.setProperty(PROXY_HOST, host);

        String port = portOption;
        if (port == null)
            port = savedPort;
        if (port != null && port != portOption)
            System.setProperty(PROXY_PORT, port);

        String nonProxyHosts = nonProxyHostsOption;
        if (nonProxyHosts == null)
            nonProxyHosts = savedNonProxyHosts;
        if (nonProxyHosts != null && nonProxyHosts != nonProxyHostsOption)
            System.setProperty(NON_PROXY_HOSTS, nonProxyHosts);

    }

    /**
     * Set the proxy server information.
     *
     * @param host proxy server host
     * @param port proxy server port number
     * @param nonProxyHosts a list of domains not requiring a proxy server (separated by spaces)
     */
    public static void setProxy(String host, int port, String nonProxyHosts) {
        String portStr = String.valueOf(port);

        Preferences.set(PROXY_HOST, host);
        Preferences.set(PROXY_PORT, portStr);
        Preferences.set(NON_PROXY_HOSTS, nonProxyHosts);

        System.setProperty(PROXY_HOST, host);
        System.setProperty(PROXY_PORT, portStr);
        System.setProperty(NON_PROXY_HOSTS, nonProxyHosts);
    }

    /**
     * Return the proxy server host name.
     */
    public static String getHost() {
        return System.getProperty(PROXY_HOST);
    }

    /**
     * Return the proxy server port.
     */
    public static int getPort() {
        String s = System.getProperty(PROXY_PORT);
        if (s == null)
            return 80;
        return Integer.parseInt(s);
    }

    /**
     * Return the space separated list of domains not requiring a proxy.
     */
    public static String getNonProxyHosts() {
        return System.getProperty(NON_PROXY_HOSTS);
    }
}

