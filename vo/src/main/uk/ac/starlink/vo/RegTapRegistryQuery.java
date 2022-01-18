package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.ContentCoding;

/**
 * Registry Query implementation that uses TAP to access a Relational Registry.
 *
 * @author   Mark Taylor
 * @since    11 Apr 2014
 * @see   <a href="http://www.ivoa.net/documents/RegTAP/"
 *           >IVOA Registry Relational Schema</a>
 */
public class RegTapRegistryQuery implements RegistryQuery {

    private final TapService tapService_;
    private final String adql_;
    private final ContentCoding coding_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Used for string aggregation of subjects.
     * Doesn't have to be human-readable, but it does have to be recognised
     * as a word delimiter by ivo_hasword() and should not be something
     * that will crop up in an actual subject value. */
    private static final String SUBJECT_DELIM = ", ";

    /** TAP base URL for high-availablity GAVO registry (DNS pointer). */
    public static final String GAVO_REG = "http://reg.g-vo.org/tap";

    /** TAP base URL for GAVO registry hosted at ARI Heidelberg. */
    public static final String ARI_REG = "http://dc.zah.uni-heidelberg.de/tap";

    /** TAP base URL for GAVO registry hosted at AIP. */
    public static final String AIP_REG = "http://gavo.aip.de/tap";

    /** TAP base URL for registry hosted at ObsPM, synced with GAVO. */
    public static final String PARIS_REG = "http://voparis-rr.obspm.fr/tap";

    /** TAP base URL for Euro-VO registry, currently hosted at ESAC. */
    public static final String EUROVO_REG =
        "http://registry.euro-vo.org/regtap/tap";

    /** TAP base URL for STScI/VAO registry. */
    public static final String STSCI_REG =
        "http://vao.stsci.edu/RegTAP/TapService.aspx";

    /** TAP base URL for INAF registry (not sure if this is permanent). */
    public static final String INAF_REG =
        "http://ia2-vo.oats.inaf.it:8080/registry";

    /** List of known registry TAP base URLs. */
    public static final String[] REGISTRIES = new String[] {
        GAVO_REG,
        STSCI_REG,
        EUROVO_REG,
        ARI_REG,
        AIP_REG,
        PARIS_REG,
    };

    /** Description of metadata item describing registry location. */
    public final static ValueInfo REGISTRY_INFO =
         new DefaultValueInfo( "Registry Location", URL.class,
                               "TAP base URL of registry queried" );

    /** Description of metadata item describing query text. */
    public final static ValueInfo ADQL_INFO =
        new DefaultValueInfo( "Registry Query", String.class,
                              "ADQL text of query made to the registry" );

    /**
     * Query restriction restricting results to standard interfaces.
     * This string is suitable for use in a sequence of ANDed conditions
     * (but not at the start of it) without additional brackets.
     * You want to make this restriction somehow, otherwise you can get
     * interfaces with an intf_type of vs:webbrowser
     * (e.g. for TAP web pages alongside the programmatic interface),
     * which you don't want to pick up.
     *
     * <p>This used to test "<code>intf_type='vs:paramhttp'</code>",
     * following RegTAP 1.0 section 10.
     * However, following RegTAP 1.1, and associated cleanup of the
     * contents of all the running searchable registries,
     * it is now considered safe and preferable to use the more obvious
     * form "<code>intf_role='std'</code>".
     */
    private static final String AND_IS_STANDARD = " AND intf_role='std'";

    /**
     * Constructs a query which will return RegResource lists for
     * registry resource records with two optional restrictions:
     * (a) restricted to a given service type, and
     * (b) restricted by some free-form ADQL.
     * The supplied <code>adqlWhere</code> text has to be written
     * with some knowledge of the internals of this class, for instance
     * what columns are available.
     *
     * @param  tapService  TAP service hosting relational registry
     * @param  standardIds  possible case-insensitive values for RR
     *                      <code>standard_id</code> field,
     *                      or null if not restricted by service
     * @param   adqlWhere  text to be ANDed with existing ADQL WHERE clause,
     *                     or null for no further restriction
     */
    public RegTapRegistryQuery( TapService tapService, String[] standardIds,
                                String adqlWhere ) {
        tapService_ = tapService;
        coding_ = ContentCoding.GZIP;

        /* SELECT clause.  The columns are required both to support the
         * restrictions we need to make and to provide information to
         * populate the RegResources generated from the result table. */
        String[] selCols = new String[] {
            "ivoid",
            "short_name",
            "res_title",
            "reference_url",
            "base_role",
            "role_name",
            "email",
            "intf_index",
            "access_url",
            "standard_id",
            "cap_type",
            "cap_description",
            "std_version",
            "res_subjects",
        };
        StringBuffer abuf = new StringBuffer()
           .append( "SELECT" );
        for ( int i = 0; i < selCols.length; i++ ) {
            abuf.append( i > 0 ? ", " : " " )
                .append( selCols[ i ] );
        }

       /* FROM clause.  Join all the tables we're going to need. */
       abuf.append( " FROM rr.resource AS res" );
       if ( standardIds != null && standardIds.length > 0 ) {
           abuf.append( " NATURAL JOIN rr.interface" )
               .append( " NATURAL JOIN rr.capability" );
       }

       /* Strictly speaking, these two joins do not need to be LEFT OUTER,
        * since VOResource requires at least one curation/contact and
        * at least one content/subject.  However, we put them in to pick
        * up resources which may be invalid in this respect. */
       abuf.append( " NATURAL LEFT OUTER JOIN rr.res_role" )
           .append( " NATURAL LEFT OUTER JOIN" )
           .append( " (SELECT" )
           .append( " ivoid, " )
           .append( " ivo_string_agg(res_subject, " )
           .append( adqlCharLiteral( SUBJECT_DELIM ) )
           .append( ")" )
           .append( " AS res_subjects" )
           .append( " FROM rr.res_subject" )
           .append( " GROUP BY ivoid) AS sbj" );

       /* WHERE clause.  Restrict by standard_id if required,
        * and throw out any roles we are not interested in. */
       abuf.append( " WHERE (base_role='contact'" )
           .append( " OR base_role='publisher'" )
           .append( " OR base_role IS NULL)" );
       if ( standardIds != null && standardIds.length > 0 ) {
           abuf.append( " AND" )
               .append( " standard_id IN (" );
           for ( int is = 0; is < standardIds.length; is++ ) {
               if ( is > 0 ) {
                   abuf.append( ", " );
               }
               abuf.append( "'" )
                   .append( standardIds[ is ].toLowerCase() )
                   .append( "'" );
           }
           abuf.append( ")" )
               .append( AND_IS_STANDARD );
        }

        /* Concatenate user-supplied WHERE text if required. */
        if ( adqlWhere != null && adqlWhere.trim().length() > 0 ) {
            abuf.append( " AND" )
                .append( " (" )
                .append( adqlWhere )
                .append( ")" );
        }
        adql_ = abuf.toString();
    }

    public DescribedValue[] getMetadata() {
        return new DescribedValue[] {
            new DescribedValue( REGISTRY_INFO, getRegistry() ),
            new DescribedValue( ADQL_INFO, getText() ),
        };
    }

    public URL getRegistry() {
        try {
            return new URL( tapService_.getIdentity() );
        }
        catch ( MalformedURLException e ) {
            return null;
        }
    }

    public String getText() {
        return adql_;
    }

    public RegResource[] getQueryResources() throws IOException {
        logger_.info( adql_ );
        TapQuery query = new TapQuery( tapService_, adql_, null );
        QuerySink sink = new QuerySink();
        boolean overflow;
        try {
            overflow = query.executeSync( sink, coding_ );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        RegResource[] resources = sink.getResources();
        int ncap = 0;
        for ( int i = 0; i < resources.length; i++ ) {
            ncap += resources[ i ].getCapabilities().length;
        }
        StringBuffer sbuf = new StringBuffer()
            .append( "RegTAP query: " )
            .append( sink.nrow_ )
            .append( " rows, " )
            .append( resources.length )
            .append( " resources, " )
            .append( ncap )
            .append( " capabilities" );
        if ( overflow ) {
            sbuf.append( " (truncated)" );
        }
        logger_.info( sbuf.toString() );
        return resources;
    }

    public Iterator<RegResource> getQueryIterator() throws IOException {
        return Arrays.asList( getQueryResources() ).iterator();
    }

    /**
     * Returns text that can be used as part of a WHERE clause to supply
     * to this class that tests for a keyword in a given RR field.
     * The nature of the test (=, LIKE etc) depends on the field.
     * If no suitable ADQL can be written, null is returned
     *
     * <p>Note that this code currently works by identifying known
     * ResourceFields, so unknown ResourceFields will return null.
     *
     * @param   field  field whose content is to be tested
     * @param   keyword  value to test against
     * @param   hasSubjectsTable  true iff the rr.res_subject table is
     *                            available for the query
     *                            (hack; if not it assumes the res_subjects
     *                            field is available)
     * @return  ADQL snippet that may be inserted into WHERE clause,
     *          or null if it can't be done
     */
    public static String getAdqlCondition( ResourceField field,
                                           String keyword,
                                           boolean hasSubjectsTable ) {
        String rrName = field.getRelationalName();
        if ( field == ResourceField.ID ||
             field == ResourceField.SHORTNAME ) {
            return new StringBuffer()
                .append( "1=ivo_nocasematch(" )
                .append( rrName )
                .append( ", " )
                .append( adqlCharLiteral( "%" + keyword + "%" ) )
                .append( ")" )
                .toString();
        }
        else if ( field == ResourceField.TITLE ||
                  field == ResourceField.DESCRIPTION ) {
            return new StringBuffer()
                .append( "1=ivo_hasword(" )
                .append( rrName )
                .append( ", " )
                .append( adqlCharLiteral( keyword ) )
                .append( ")" )
                .toString();
        }
        else if ( field == ResourceField.PUBLISHER ) {
            return new StringBuffer()
                .append( "(" )
                .append( "base_role='publisher'" )
                .append( " AND " )
                .append( "1=ivo_nocasematch(" )
                .append( "role_name" )
                .append( ", " )
                .append( adqlCharLiteral( "%" + keyword + "%" ) )
                .append( ")" )
                .append( ")" )
                .toString();
        }
        else if ( field == ResourceField.SUBJECTS ) {
            if ( hasSubjectsTable ) {
                return new StringBuffer()
                      .append( "res_subject=" )
                      .append( adqlCharLiteral( keyword ) )
                      .toString();
            }
            else {
                return new StringBuffer()
                    .append( "1=ivo_hasword(res_subjects, " )
                    .append( adqlCharLiteral( keyword ) )
                    .append( ")" )
                    .toString();
            }
        }
        else {
            return null;
        }
    }

    /**
     * Quotes a string to make it suitable for insertion in ADQL text
     * as a character literal.  Quote characters are doubled as per
     * the ADQL BNF.  This should protect against injection errors.
     *
     * @param  txt  content of the character literal
     * @return  quoted ADQL representing <code>txt</code>
     */
    private static String adqlCharLiteral( String txt ) {
        return "'" + txt.replaceAll( "'", "''" ) + "'";
    }

    /**
     * Queries a given registry for searchable registries suitable for
     * use with this class.
     *
     * @param  regtapService  TAP service description of
     *                        bootstrap relational registry
     * @return   list of TAP base URLs for found relational registries
     */
    public static String[] getSearchableRegistries( TapService regtapService )
            throws IOException {

        /* Copied from RegTAP 1.0 examples. */
        String adql = new StringBuffer()
            .append( "SELECT access_url" )
            .append( " FROM rr.interface" )
            .append( " NATURAL JOIN rr.capability" )
            .append( " NATURAL JOIN rr.res_detail" )
            .append( " WHERE standard_id='ivo://ivoa.net/std/tap'" )
            .append( AND_IS_STANDARD )
            .append( " AND detail_xpath='/capability/dataModel/@ivo-id' " )
            .append( " AND 1=ivo_nocasematch(detail_value, " )
            .append(                        "'ivo://ivoa.net/std/regtap#1.0')" )
            .toString();
        TapQuery query = new TapQuery( regtapService, adql, null );
        logger_.info( adql );
        StarTable table = query.executeSync( StoragePolicy.PREFER_MEMORY,
                                             ContentCoding.NONE );
        List<String> urlList = new ArrayList<String>();
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            urlList.add( (String) rseq.getCell( 0 ) );
        }
        rseq.close();
        return urlList.toArray( new String[ 0 ] );
    }

    /**
     * RegResource implementation for use with this class.
     */
    private static class RegTapResource implements RegResource {
        final String ivoid_;
        final String shortName_;
        final String title_;
        final String refUrl_;
        String contactName_;
        String contactEmail_;
        String publisherName_;
        String[] subjects_;
        Map<Object,RegCapabilityInterface> capMap_;

        /**
         * Constructor.
         *
         * @param  ivoid  resource identifier
         * @param  shortName  resource short name
         * @param  title  resource title
         * @param  refUrl  resource reference URL
         * @param  subjects  array of subject strings
         */
        RegTapResource( String ivoid, String shortName, String title,
                        String refUrl, String[] subjects ) {
            ivoid_ = ivoid;
            shortName_ = shortName;
            title_ = title;
            refUrl_ = refUrl;
            subjects_ = subjects;
            capMap_ = new LinkedHashMap<Object,RegCapabilityInterface>();
        }

        public String getIdentifier() {
            return ivoid_;
        }

        public String getShortName() {
            return shortName_;
        }

        public String getTitle() {
            return title_;
        }

        public String getReferenceUrl() {
            return refUrl_;
        }

        public String getContact() {
            boolean hasName =
                contactName_ != null && contactName_.trim().length() > 0;
            boolean hasEmail =
                contactEmail_ != null && contactEmail_.trim().length() > 0;
            if ( hasEmail && hasName ) {
                return contactName_ + " <" + contactEmail_ + ">";
            }
            else if ( hasEmail ) {
                return contactEmail_;
            }
            else if ( hasName ) {
                return contactName_;
            }
            else {
                return null;
            }
        }

        public String getPublisher() {
            return publisherName_;
        }

        public String[] getSubjects() {
            return subjects_;
        }

        public RegCapabilityInterface[] getCapabilities() {
            return capMap_.values().toArray( new RegCapabilityInterface[ 0 ] );
        }
    }

    /**
     * Receives table rows to build up a list of RegResource objects that
     * it represents.
     */
    private static class QuerySink implements TableSink {

        private final Map<String,RegTapResource> resMap_;
        private final Map<String,Integer> colMap_;
        long nrow_;

        /**
         * Constructor.
         */
        QuerySink() {
            resMap_ = new LinkedHashMap<String,RegTapResource>();
            colMap_ = new HashMap<String,Integer>();
        }

        /**
         * Returns the resource list that this sink has received.
         *
         * @return  resource list
         */
        public RegTapResource[] getResources() {
            return resMap_.values().toArray( new RegTapResource[ 0 ] );
        }

        public void acceptMetadata( StarTable meta ) {

            /* Prepare a lookup table of column indices by name. */
            int ncol = meta.getColumnCount();
            for ( int i = 0; i < ncol; i++ ) {
                colMap_.put( meta.getColumnInfo( i ).getName().toLowerCase(),
                             i );
            }
        }

        public void endRows() {
        }

        public void acceptRow( Object[] row ) {

            /* Bump recorded row count. */
            nrow_++;

            /* Get values using the column lookup table.
             * In fact we know what sequence the columns are in so we could
             * hard code the colum indices in here, but doing it like this
             * reduces the chance of programming error. */
            final String ivoid = getString( row, "ivoid" );
            final String shortName = getString( row, "short_name" );
            final String title = getString( row, "res_title" );
            final String refUrl = getString( row, "reference_url" );
            final String baseRole = getString( row, "base_role" );
            final String roleName = getString( row, "role_name" );
            final String email = getString( row, "email" );
            final Object intfIndex = getEntry( row, "intf_index" );
            final String accessUrl = getString( row, "access_url" );
            final String standardId = getString( row, "standard_id" );
            final String capType = getString( row, "cap_type" );
            final String capDescription = getString( row, "cap_description" );
            final String stdVersion = getString( row, "std_version" );
            final String subjectTxt = getString( row, "res_subjects" );

            /* Update this object's data structures in accordance with the
             * information received from this row. */
            if ( ! resMap_.containsKey( ivoid ) ) {
                String[] subjects = subjectTxt == null
                                  ? new String[ 0 ]
                                  : subjectTxt.split( SUBJECT_DELIM );
                resMap_.put( ivoid,
                             new RegTapResource( ivoid, shortName, title,
                                                 refUrl, subjects ) );
            }
            RegTapResource resource = resMap_.get( ivoid );
            if ( "contact".equals( baseRole ) ) {
                resource.contactName_ = roleName;
                resource.contactEmail_ = email;
            }
            else if ( "publisher".equals( baseRole ) ) {
                resource.publisherName_ = roleName;
            }
            if ( intfIndex != null ) {
                if ( ! resource.capMap_.containsKey( intfIndex ) ) {
                    resource.capMap_.put( intfIndex,
                                          new RegCapabilityInterface() {
                        public String getAccessUrl() {
                            return accessUrl;
                        }
                        public String getStandardId() {
                            return standardId;
                        }
                        public String getXsiType() {
                            return capType;
                        }
                        public String getDescription() {
                            return capDescription;
                        }
                        public String getVersion() {
                            return stdVersion;
                        }
                    } );
                }
            }
        }

        /**
         * Gets a value from a table row by row name.
         *
         * @param  row   array of cells
         * @param  rrName   column name
         * @return   value of cell in column with name <code>rrName</code>
         */
        private Object getEntry( Object[] row, String rrName ) {
            Integer icol = colMap_.get( rrName );
            return icol == null ? null : row[ icol.intValue() ];
        }

        /**
         * Gets a string value from a table row by row name.
         *
         * @param  row   array of cells
         * @param  rrName   column name
         * @return   value of cell in column with name <code>rrName</code>
         */
        private String getString( Object[] row, String rrName ) {
            return (String) getEntry( row, rrName );
        }
    }
}
