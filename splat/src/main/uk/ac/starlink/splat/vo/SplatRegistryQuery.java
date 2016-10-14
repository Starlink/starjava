package uk.ac.starlink.splat.vo;

import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import java.util.Iterator;

import java.util.LinkedHashMap;
import java.util.List;

//import java.net.URL;
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
/*
import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegistryQuery;
import uk.ac.starlink.vo.ResourceField;
import uk.ac.starlink.vo.Ri1RegistryQuery;
*/
import uk.ac.starlink.vo.TapQuery;


import uk.ac.starlink.vo.RegCapabilityInterface;
import uk.ac.starlink.vo.RegResource;
import uk.ac.starlink.vo.RegTapRegistryQuery;
import uk.ac.starlink.vo.RegistryProtocol;
import uk.ac.starlink.vo.RegistryQuery;

import uk.ac.starlink.util.ContentCoding;


/**
 * Registry Query implementation that uses TAP to access a Relational Registry.
 *
 * @author   Margarida Castro Neves
 * @since    11 Apr 2014
 * @see   <a href="http://www.ivoa.net/documents/RegTAP/"
 *           >IVOA Registry Relational Schema</a>
 */
public class SplatRegistryQuery implements RegistryQuery {

    
    private final URL regUrl_;
    private String adql_=null;
    
    // The possible kinds of query
    
    public static final int SSAP = 0;
    public static final int OBSCORE = 1;
    public static final int SLAP = 2;
    

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );
    
    public static final String RI1 = "Ri1";
    public static final String REGTAP = "RegTap";
    
    /** possible registry types */
    public static final String[] REGTYPES = new String[] {
        RI1,
        REGTAP,
    };
    
    /** TAP endpoint for high-availablity GAVO registry (DNS pointer). */
    public static final String GAVO_REG = "http://reg.g-vo.org/tap";

    /** TAP endpoint for GAVO registry hosted at ARI Heidelberg. */
    public static final String ARI_REG = "http://dc.zah.uni-heidelberg.de/tap";

    /** TAP endpoint for GAVO registry hosted at AIP. */
    public static final String AIP_REG = "http://gavo.aip.de/tap";

    /** TAP endpoint for INAF registry (not sure if this is permanent). */
    public static final String INAF_REG =
        "http://ia2-vo.oats.inaf.it:8080/registry";

    /** List of known registry TAP endpoints. */
    public static final String[] REGISTRIES = new String[] {
        GAVO_REG,
        ARI_REG,
        AIP_REG,
        INAF_REG,
    };

    
    /** Description of metadata item describing registry location. */
    public final static ValueInfo REGTYPE_INFO =
         new DefaultValueInfo( "Registry Type", String.class,
                               "Type of registry queried" );

    /** Description of metadata item describing registry location. */
    public final static ValueInfo REGISTRY_INFO =
         new DefaultValueInfo( "Registry Location", URL.class,
                               "TAP endpoint of registry queried" );

    /** Description of metadata item describing query text. */
    public final static ValueInfo ADQL_INFO =
        new DefaultValueInfo( "Registry Query", String.class,
                              "ADQL text of query made to the registry" );

    /**
     * Constructs a query which will return RegResource lists for
     * registry resource records with two optional restrictions:
     * (a) restricted to a given service type, and
     * (b) restricted by some free-form ADQL.
     * The supplied <code>adqlWhere</code> text has to be written
     * with some knowledge of the internals of this class, for instance
     * what columns are available.
     *
     * @param  tapurl  TAP endpoint for service hosting relational registry
     * @param  standardId  required value of RR <code>standard_id</code> field,
     *                     or null if not resricted by service
     * @param   adqlWhere  text to be ANDed with existing ADQL WHERE clause,
     *                     or null for no further restriction
     */
    public SplatRegistryQuery( String regurl, int protocol) {
       
       regUrl_ = toUrl( regurl );
       
       if (protocol == OBSCORE)
               adql_ = getObsCoreAdql();
       else  if (protocol == SSAP)
               adql_ = getSSAPAdql();
       // else if (protocol == SLAP)
       //    adql_ = getSLAPAdql();

  }
    
    private String getSSAPAdql() {

        return "SELECT short_name, res_title,  res_description, ivoid, access_url, reference_url, "+
                "waveband, content_type, baseroles, rolenames,  emails, cappaths, capvals, " +
                "standard_id, std_version, res_subjects " +
                "FROM rr.resource AS res NATURAL JOIN rr.interface NATURAL JOIN rr.capability " +
                "NATURAL LEFT OUTER JOIN   (SELECT ivoid, " +
                "ivo_string_agg(detail_xpath, '#') AS cappaths, "+
                "ivo_string_agg(detail_value, '#') AS capvals "+ 
                "FROM rr.res_detail GROUP BY ivoid ) as qq " +
                "NATURAL LEFT OUTER JOIN (SELECT ivoid,  ivo_string_agg(res_subject, ', ') AS res_subjects " +
                "FROM rr.res_subject GROUP BY ivoid) AS sbj " +
                "NATURAL LEFT OUTER JOIN (SELECT  ivoid, " +
                "ivo_string_agg(base_role, '#') as baseroles, "+
                "ivo_string_agg(role_name, '#') as rolenames, "+
                "ivo_string_agg(email, '#') as emails "+
                "FROM rr.res_role GROUP BY ivoid) as q "+
                "WHERE standard_id='ivo://ivoa.net/std/ssa' AND intf_type='vs:paramhttp' " ;
    }
    
    private String getObsCoreAdql() {

        return "SELECT short_name, res_title,  res_description, ivoid, access_url, reference_url, "+
                "waveband, content_type, baseroles, rolenames,  emails, cappaths, capvals, " +
                "standard_id, std_version, res_subjects " +
                "FROM rr.resource AS res NATURAL JOIN rr.interface NATURAL JOIN rr.capability " +
                "NATURAL LEFT OUTER JOIN   (SELECT ivoid, " +
                "detail_xpath, detail_value, " +
                "ivo_string_agg(detail_xpath, '#') AS cappaths, "+
                "ivo_string_agg(detail_value, '#') AS capvals "+ 
                "FROM rr.res_detail GROUP BY ivoid, detail_xpath, detail_value ) as qq " +
                "NATURAL LEFT OUTER JOIN (SELECT ivoid,  ivo_string_agg(res_subject, ', ') AS res_subjects " +
                "FROM rr.res_subject GROUP BY ivoid) AS sbj " +
                "NATURAL LEFT OUTER JOIN (SELECT  ivoid, " +
                "ivo_string_agg(base_role, '#') as baseroles, "+
                "ivo_string_agg(role_name, '#') as rolenames, "+
                "ivo_string_agg(email, '#') as emails "+
                "FROM rr.res_role GROUP BY ivoid) as q "+
                "WHERE standard_id='ivo://ivoa.net/std/tap' AND detail_xpath='/capability/dataModel/@ivo-id' "+                
                "AND (1=ivo_nocasematch(detail_value, 'ivo://ivoa.net/std/obscore%'))";
    }

    public DescribedValue[] getMetadata() {
        return new DescribedValue[] {
            new DescribedValue( REGISTRY_INFO, getRegistry() ),
            new DescribedValue( ADQL_INFO, getText() ),
        };
    }

    public URL getRegistry() {
        return regUrl_;
    }

    public String getText() {
        return adql_;
    }

    public RegResource[] getQueryResources() throws IOException {
        logger_.info( adql_ );
        TapQuery query = new TapQuery( regUrl_, adql_, null );
        QuerySink sink = new QuerySink();
        boolean overflow;
        try {
            overflow = query.executeSync( sink, ContentCoding.NONE );
        }
        catch ( SAXException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        SSAPRegResource[] resources = sink.getResources();
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
        return (RegResource []) resources;
    }

    public Iterator getQueryIterator() throws IOException {
        return Arrays.asList( getQueryResources() ).iterator();
    }

    /**
     * Queries a given registry for searchable registries suitable for
     * use with this class.
     *
     * @param  regUrl  TAP endpoint for bootstrap relational registry
     * @return   list of TAP endpoints for found relational registries
     */
    public static String[] getSearchableRegistries( String regUrl )
            throws IOException {

        /* Copied from RegTAP 1.0 examples. */
        String adql = new StringBuffer()
            .append( "SELECT access_url" )
            .append( " FROM rr.interface" )
            .append( " NATURAL JOIN rr.capability" )
            .append( " NATURAL JOIN rr.res_detail" )
            .append( " WHERE standard_id='ivo://ivoa.net/std/tap'" )
            .append( " AND intf_type='vs:paramhttp'" )
            .append( " AND detail_xpath='/capability/dataModel/@ivo-id' " )
            .append( " AND 1=ivo_nocasematch(detail_value, " )
            .append(                        "'ivo://ivoa.net/std/regtap#1.0')" )
            .toString();
        TapQuery query =
            new TapQuery( toUrl( regUrl ), adql, null );
        logger_.info( adql );
        StarTable table = query.executeSync( StoragePolicy.PREFER_MEMORY, ContentCoding.NONE );
        List<String> urlList = new ArrayList<String>();
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            urlList.add( (String) rseq.getCell( 0 ) );
        }
        rseq.close();
        return urlList.toArray( new String[ 0 ] );
    }
    /**
     * Turns a string into a URL without any pesky checked exceptions.
     *
     * @param  url  URL string
     * @return  URL
     */
    static URL toUrl( String url ) {
        try {
            return new URL( url );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Not a URL: " + url )
                 .initCause( e );
        }
    }


 
    /**
     * Receives table rows to build up a list of RegResource objects that
     * it represents.
     */
    private static class QuerySink implements TableSink {

        private static Map<String,SSAPRegResource> resMap_;
        private static Map<String,Integer> colMap_;
        long nrow_;

        /**
         * Constructor.
         */
        QuerySink() {
            resMap_ = new LinkedHashMap<String,SSAPRegResource>();
            colMap_ = new HashMap<String,Integer>();
        }

        /**
         * Returns the resource list that this sink has received.
         *
         * @return  resource list
         */
        public SSAPRegResource[] getResources() {
            return resMap_.values().toArray( new SSAPRegResource[ 0 ] );
        }

        public SSAPRegResource[] getSSAPRegResource() {
            Collection col = resMap_.values();
            
            return null;
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
           
            final Number intfIndex = (Number) getEntry( row, "intf_index" );
            final String accessUrl = getString( row, "access_url" );
            final String standardId = getString( row, "standard_id" );
            final String contType = getString( row, "content_type" );
            final String resDescription = getString( row, "res_description" );
            final String stdVersion = getString( row, "std_version" );
           
            final String subjectTxt = getString( row, "res_subjects" );
            final String [] waveBand = getString( row, "waveband" ).split("#");
            
            String cappaths = getString( row, "cappaths" );
            String capvals = getString( row, "capvals" );
             
            String baseRoles = getString( row, "baseroles" );
            String roleNames = getString( row, "rolenames" );
            String email = getString( row, "emails" ).replace("<", "&lt;").replace(">", "&gt;"); // replace needed if information is displayed in html
            String contact = "";
            String publisher = "";
           
            if (baseRoles!= null && roleNames!=null) {
                String[] roles = baseRoles.split("#");
                String[] names = roleNames.split("#");
                for (int b=0;b<roles.length;b++) {
                    if (  roles[b].equalsIgnoreCase("publisher") )
                        publisher=names[b];   
                    else if (  roles[b].equalsIgnoreCase("contact") )
                        contact=names[b];   
                }  
                if (email != null)
                    contact += " ("+ email +") ";
            }
            
           String dataSource = "";
           String creationType = "";
           
            if (cappaths != null && capvals != null) {
                String [] paths = cappaths.split("#");
                String [] vals = capvals.split("#");
                for (int k=0;k<paths.length; k++) {
                    if (paths[k].contains("/capability/dataSource")) 
                        dataSource = vals[k];
                    if (paths[k].contains("capability/creationType")) 
                        creationType = vals[k];
                }
            } 
            
           
            /* Update this object's data structures in accordance with the
             * information received from this row. */
            if ( ! resMap_.containsKey( ivoid ) ) {
                String[] subjects = subjectTxt == null
                                  ? new String[ 0 ]
                                  : subjectTxt.split( "," );
                SSAPRegResource res = new SSAPRegResource( shortName, title, resDescription, accessUrl);
                res.setContact(contact);
                res.setPublisher(publisher);
                res.setReferenceUrl(refUrl);
                res.setIdentifier(ivoid);
                res.setVersion(stdVersion);
                res.setSubjects(subjects);
                res.setWaveband(waveBand);
                res.setContentType(contType);
           //     res.capMap = new LinkedHashMap<Integer, SSAPRegCapability>();
                resMap_.put( ivoid, res );
            }
            SSAPRegResource resource = resMap_.get(ivoid);

            if ( intfIndex != null ) {
               
               // Integer ix = new Integer( intfIndex.intValue() );
              //  if ( ! resource.capMap.containsKey( ix ) ) {
                    SSAPRegCapability cap = new SSAPRegCapability("", accessUrl );
              //      cap.setIntfIndex(ix);
                    cap.setCreationType(creationType);
                    cap.setDataSource(dataSource);
                    cap.setStandardId(standardId);
                    
              //      resource.capMap.put( ix, cap ); 
                    SSAPRegCapability [] caps = new SSAPRegCapability[1];
                    caps[0] = cap;
                    resource.setCapabilities(caps);
                //}
            }
        }

        /**
         * Gets a value from a table row by row name.
         *
         * @param  row   array of cells
         * @param  rrName   column name
         * @return   value of cell in column with name <code>rrName</code>
         */
        private static Object getEntry( Object[] row, String rrName ) {
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
        private static String getString( Object[] row, String rrName ) {
            return (String) getEntry( row, rrName );
        }
    }

}
