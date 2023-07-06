package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Defines the details of a registry access protocol.
 *
 * @author   Mark Taylor
 * @since    9 Apr 2014
 */
public abstract class RegistryProtocol {

    private final String shortName_;
    private final String fullName_;
    private final String[] dfltUrls_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Protocol instance for Registry Interface 1.0. */
    public static final RegistryProtocol RI1 = new Ri1RegistryProtocol();

    /** Protocol instance for Relational Registry 1.0. */
    public static final RegistryProtocol REGTAP = new RegTapRegistryProtocol();

    /** Known protocols. */
    public static final RegistryProtocol[] PROTOCOLS = { REGTAP, RI1 };

    /**
     * Constructor.
     *
     * @param  shortName  short name
     * @param  fullName   full name
     * @param  dfltUrls  strings giving some default registry endpoints for 
     *                   this access protocol
     */
    protected RegistryProtocol( String shortName, String fullName,
                                String[] dfltUrls ) {
        shortName_ = shortName;
        fullName_ = fullName;
        dfltUrls_ = dfltUrls.clone();
    }

    /**
     * Returns a short name for this protocol.
     *
     * @return   short name
     */
    public String getShortName() {
        return shortName_;
    }

    /**
     * Returns the full name for this protocol.
     *
     * @return  full name
     */
    public String getFullName() {
        return fullName_;
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
     * Constructs a registry query that gets results for a list of
     * given IVO ID strings, optionally restricted by a given capability.
     * The resulting query supplies results for each resource which is
     * all of: 
     * (a) in the registry,
     * (b) in the <code>ivoids</code> list, and
     * (c) has the given capability
     * If <code>capability</code> is null, then restriction (c) does not apply.
     * If the input list of IDs is null or empty, the return value will be null.
     *
     * @param  ivoids  ID values for the required resources
     * @param  capability  service capability type, or null
     * @param  regUrl   endpoint URL for a registry service implementing
     *                  this protocol
     * @return  registry query, or null for empty ID list
     */
    public abstract RegistryQuery createIdListQuery( String[] ivoids,
                                                     Capability capability,
                                                     URL regUrl );

    /**
     * Constructs a registry query that gets results for resources with
     * a match for one or all of a given set of keywords found in
     * a selection of resource fields.
     * 
     * @param  keywords  single-word keywords to match independently
     * @param  rfs   resource fields against which keywords are to match
     * @param  isOr  if false all keywords must match,
     *               if true at least one keyword must match
     * @param  capability   if non-null, restricts the resources to those
     *                      with that capability
     * @param  regUrl   endpoint URL for a registry service implementing
     *                  this protocol
     * @return  registry query
     */
    public abstract RegistryQuery createKeywordQuery( String[] keywords,
                                                      ResourceField[] rfs,
                                                      boolean isOr,
                                                      Capability capability,
                                                      URL regUrl );

    /**
     * Indicates whether a given RegCapabilityInterface object is an
     * instance of a given capability.  This is typically used to weed
     * out RegCapabilityInterface objects returned from a query that
     * might have returned some items different than those queried.
     *
     * <p>Really, the implementation of this ought not to be a function
     * of the registry protocol in use.  However, it's probably the case
     * that the different registry implementations have different quirks
     * in this respect, so take the opportunity to parameterise it by
     * registry protocol in case that's required.
     *
     * @param  stdCap  standard capability definition
     * @param  resCap  capability interface object representing part of
     *                 a registry resource
     * @return  true iff <code>resCap</code> represents a capability
     *          of the type <code>stdCap</code>
     */
    public abstract boolean hasCapability( Capability stdCap,
                                           RegCapabilityInterface resCap );

    /**
     * RegistryProtocol implementation for Registry Interface 1.0.
     */
    private static class Ri1RegistryProtocol extends RegistryProtocol {

        /**
         * Constructor.
         */
        Ri1RegistryProtocol() {
            super( "RI1.0", "Registry Interface 1.0",
                   Ri1RegistryQuery.REGISTRIES );
        }

        public String[] discoverRegistryUrls( String regUrl0 )
                throws IOException {
            return Ri1RegistryQuery.getSearchableRegistries( regUrl0 );
        }

        public RegistryQuery createIdListQuery( String[] ivoids,
                                                Capability capability,
                                                URL regUrl ) {
            if ( ivoids == null || ivoids.length == 0 ) {
                return null;
            }
            StringBuffer sbuf = new StringBuffer();
            if ( capability != null ) {
                sbuf.append( "(" )
                    .append( Ri1RegistryQuery.getAdqlWhere( capability ) )
                    .append( ")" );
            }
            if ( sbuf.length() > 0 ) {
                sbuf.append( " AND " );
            }
            sbuf.append( "(" );
            for ( int i = 0; i < ivoids.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( " OR " );
                }
                sbuf.append( "identifier = '" )
                    .append( ivoids[ i ] )
                    .append( "'" );
            }
            sbuf.append( ")" );
            String adql = sbuf.toString();
            return new Ri1RegistryQuery( regUrl.toString(), adql );
        }

        public RegistryQuery createKeywordQuery( String[] keywords,
                                                 ResourceField[] fields,
                                                 boolean isOr,
                                                 Capability capability,
                                                 URL regUrl ) {
            String conjunction = isOr ? " OR " : " AND ";
            StringBuffer sbuf = new StringBuffer();
            if ( capability != null ) {
                sbuf.append( Ri1RegistryQuery.getAdqlWhere( capability ) );
            }
            if ( keywords.length > 0 ) {
                if ( sbuf.length() > 0 ) {
                    sbuf.append( " AND ( " );
                }
                for ( int iw = 0; iw < keywords.length; iw++ ) {
                    if ( iw > 0 ) {
                        sbuf.append( conjunction );
                    }
                    sbuf.append( "(" );
                    for ( int ip = 0; ip < fields.length; ip++ ) {
                        if ( ip > 0 ) {
                            sbuf.append( " OR " );
                        }
                        sbuf.append( fields[ ip ].getXpath() )
                            .append( " LIKE " )
                            .append( "'%" )
                            .append( keywords[ iw ] )
                            .append( "%'" );
                    }
                    sbuf.append( ")" );
                }
                sbuf.append( " )" );
            }
            String adql = sbuf.toString();
            return new Ri1RegistryQuery( regUrl.toString(), adql );
        }

        public boolean hasCapability( Capability stdCap,
                                      RegCapabilityInterface resCap ) {
            String resType = resCap.getXsiType();
            String stdTypeTail = stdCap.getXsiTypeTail();
            Ivoid resId = new Ivoid( resCap.getStandardId() );
            Ivoid[] stdIds = stdCap.getStandardIds();
            return Arrays.asList( stdIds ).contains( resId ) 
                || ( resType != null && stdTypeTail != null
                                     && resType.endsWith( stdTypeTail ) );
        }
    };

    /**
     * RegistryProtocol implementation for Relational Registry.
     */
    private static class RegTapRegistryProtocol extends RegistryProtocol {

        /**
         * Constructor.
         */
        public RegTapRegistryProtocol() {
            super( "RegTAP", "Relational Registry 1.0",
                   RegTapRegistryQuery.REGISTRIES );
        }

        public String[] discoverRegistryUrls( String regUrl0 )
                throws IOException {
            try {
                TapService service0 =
                    TapServices.createDefaultTapService( new URL( regUrl0 ) );
                return RegTapRegistryQuery
                      .getSearchableRegistries( service0 );
            }
            catch ( MalformedURLException e ) {
                return new String[ 0 ];
            }
        }

        public RegistryQuery createIdListQuery( String[] ivoids,
                                                Capability capability,
                                                URL regUrl ) {
            if ( ivoids == null || ivoids.length == 0 ) {
                return null;
            }
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "ivoid IN (" );
            for ( int i = 0; i < ivoids.length; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( "'" )
                    .append( ivoids[ i ].toLowerCase() )
                    .append( "'" );
            }
            sbuf.append( ")" );
            String idsWhere = sbuf.toString();
            TapService service = TapServices.createDefaultTapService( regUrl );
            return new RegTapRegistryQuery( service,
                                            capability.getStandardIds(),
                                            idsWhere );
        }

        public boolean hasCapability( Capability stdCap,
                                      RegCapabilityInterface resCap ) {
            return Arrays.asList( stdCap.getStandardIds() )
                         .contains( new Ivoid( resCap.getStandardId() ) );
        }

        public RegistryQuery createKeywordQuery( String[] keywords,
                                                 ResourceField[] fields,
                                                 boolean isOr,
                                                 Capability capability,
                                                 URL regUrl ) {
            Set<String> failFields = new TreeSet<String>();
            boolean useUnion =
                RegTapRegistryQuery.isSupportUnion( regUrl.toString() );
            String keywordWhere =
                  useUnion
                ? createKeywordWhereUnion( keywords, fields, isOr, failFields )
                : createKeywordWhereOr( keywords, fields, isOr, failFields );
            if ( ! failFields.isEmpty() ) {
                logger_.warning( "Failed to set constraint for fields " 
                               + failFields );
            }
            TapService service = TapServices.createDefaultTapService( regUrl );
            return new RegTapRegistryQuery( service,
                                            capability.getStandardIds(),
                                            keywordWhere );
        }

        /**
         * Returns a where clause that matches a given set of fields against
         * a given set of keywords, using straightforward OR constraints.
         *
         * <p>This should work on any ADQL implementation, but is known to
         * be slow in some cases for a Postgres back end, which is inefficient
         * when combining OR constraints against different tables.
         * It can result in timeouts for DaCHS.
         *
         * @param  keywords  single-word keywords to match independently
         * @param  fields   resource fields against which keywords are to match
         * @param  isOr  if false all keywords must match,
         *               if true at least one keyword must match
         * @param  failFields  writable collection to receive names of fields
         *                     whose constraints cannot be applied
         * @return  ADQL condition text
         */
        private String
                createKeywordWhereOr( String[] keywords,
                                      ResourceField[] fields, boolean isOr,
                                      Collection<String> failFields ) {
            String conjunction = isOr ? " OR " : " AND ";
            StringBuffer sbuf = new StringBuffer();
            for ( int iw = 0; iw < keywords.length; iw++ ) {
                String word = keywords[ iw ];
                if ( iw > 0 ) {
                    sbuf.append( conjunction );
                }
                sbuf.append( "(" );
                boolean hasField = false;
                for ( int ip = 0; ip < fields.length; ip++ ) {
                    ResourceField field = fields[ ip ];
                    String keyCond = RegTapRegistryQuery
                                    .getAdqlCondition( field, word, false );
                    if ( keyCond != null ) {
                        if ( hasField ) {
                            sbuf.append( " OR " );
                        }
                        hasField = true;
                        sbuf.append( keyCond );
                    }
                    else {
                        failFields.add( field.getLabel() );
                    }
                }
                if ( ! hasField ) {
                    sbuf.append( "1=1" );
                }
                sbuf.append( ")" );
            }
            return sbuf.toString();
        }

        /**
         * Returns a where clause that matches a given set of fields against
         * a given set of keywords, using the optional ADQL 2.1 UNION
         * construction.
         *
         * <p>This is supposed to work for any ADQL 2.1 implementation that
         * declares UNION support; it is known to work, and can be much faster
         * than the OR-based implementation, for recent DaCHS services.
         *
         * @param  keywords  single-word keywords to match independently
         * @param  fields   resource fields against which keywords are to match
         * @param  isOr  if false all keywords must match,
         *               if true at least one keyword must match
         * @param  failFields  writable collection to receive names of fields
         *                     whose constraints cannot be applied
         * @return  ADQL condition text
         */
        private String
                createKeywordWhereUnion( String[] keywords,
                                         ResourceField[] fields, boolean isOr,
                                         Collection<String> failFields ) {
            Map<String,List<ResourceField>> tFields =
                Arrays.stream( fields )
                      .collect( Collectors.groupingBy( ResourceField::
                                                       getRelationalTable ) );
            List<String> kClauses = new ArrayList<>();
            for ( String kw : keywords ) {
                List<String> fClauses = new ArrayList<>();
                for ( Map.Entry<String,List<ResourceField>> entry :
                      tFields.entrySet() ) {
                    String rrTable = entry.getKey();
                    List<ResourceField> fieldList = entry.getValue();
                    List<String> condList = new ArrayList<>();
                    for ( ResourceField field : fieldList ) {
                        String keyCond = RegTapRegistryQuery
                                        .getAdqlCondition( field, kw, true );
                        if ( keyCond != null ) {
                            condList.add( keyCond );
                        }
                        else {
                            failFields.add( field.getLabel() );
                        }
                    }
                    if ( condList.size() > 0 ) {
                        String fClause = new StringBuffer()
                           .append( "SELECT ivoid FROM " )
                           .append( rrTable )
                           .append( " WHERE " )
                           .append( String.join( " OR ", condList ) )
                           .toString();
                        fClauses.add( fClause );
                    }
                }
                if ( fClauses.size() > 0 ) {
                    String kClause = new StringBuffer()
                       .append( "(ivoid IN (" )
                       .append( String.join( " UNION ", fClauses ) )
                       .append( "))" )
                       .toString();
                    kClauses.add( kClause );
                }
            }
            return kClauses.size() == 0
                 ? "1=1"
                 : String.join( isOr ? " OR " : " AND ", kClauses );
        }
    }
}
