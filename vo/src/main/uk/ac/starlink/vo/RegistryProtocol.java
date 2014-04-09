package uk.ac.starlink.vo;

import java.io.IOException;

/**
 * Defines the details of a registry access protocol.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2014
 */
public abstract class RegistryProtocol {

    private final String[] dfltUrls_;

    /** Protocol instance for Registry Interface 1.0. */
    public static final RegistryProtocol RI1 = new Ri1RegistryProtocol();

    /**
     * Constructor.
     *
     * @param  dfltUrls  strings giving some default registry endpoints for 
     *                   this access protocol
     */
    protected RegistryProtocol( String[] dfltUrls ) {
        dfltUrls_ = dfltUrls.clone();
    }

    /**
     * Returns default endpoint URLs for this protocol.
     *
     * @return  endpoint URL strings
     */
    public String[] getDefaultRegistryUrls() {
        return dfltUrls_.clone();
    }

    /**
     * Searches a given registry to discover new endpoint URLs serving
     * this registry protocol.
     *
     * @param  regUrl0  bootstrap registry endpoint URL
     * @return   registry endpoint URLs discovered from the registry
     */
    public abstract String[] discoverRegistryUrls( String regUrl0 )
            throws IOException;

    /**
     * RegistryProtocol implementation for Registry Interface 1.0.
     */
    private static class Ri1RegistryProtocol extends RegistryProtocol {
        Ri1RegistryProtocol() {
            super( Ri1RegistryQuery.REGISTRIES );
        }
        public String[] discoverRegistryUrls( String regUrl0 )
                throws IOException {
            return Ri1RegistryQuery.getSearchableRegistries( regUrl0 );
        }
    };
}
