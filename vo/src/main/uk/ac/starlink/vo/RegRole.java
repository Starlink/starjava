package uk.ac.starlink.vo;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ContentCoding;

/**
 * Characterises a role item from the registry resource model.
 * This corresponds to a row of the RegTAP rr.res_role table.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2015
 * @see    <a href="http://www.ivoa.net/documents/RegTAP/">RegTAP</a>
 */
public abstract class RegRole {

    /**
     * Returns the role played by this entity; should be one of
     * "contact", "publisher", "creator".
     *
     * @return   role type
     */
    public abstract String getBaseRole();

    /**
     * Returns the real-world name or title of the person or organisation.
     *
     * @return  name
     */
    public abstract String getName();

    /**
     * Returns the email address associated with the person or organisation.
     *
     * @return  email
     */
    public abstract String getEmail();

    /**
     * Returns the URL of a logo associated with this entity.
     *
     * @return  logo URL
     */
    public abstract String getLogo();

    /**
     * Performs a RegTAP query to return all the role records corresponding
     * to a given registry resource (ivoid).
     *
     * @param   regTapUrl  service URL for RegTAP service
     * @param   ivoid    identifier for resource
     * @param  coding  configures HTTP compression
     * @return  role records for resource
     */
    public static RegRole[] readRoles( String regTapUrl, String ivoid,
                                       ContentCoding coding )
            throws IOException {
        final String NAME = "role_name";
        final String EMAIL = "email";
        final String BASE_ROLE = "base_role";
        final String LOGO = "logo";
        String[] colNames = { NAME, EMAIL, BASE_ROLE, LOGO };
        StringBuffer sbuf = new StringBuffer()
            .append( "SELECT" );
        for ( String colName : colNames ) {
           if ( colName != colNames[ 0 ] ) {
               sbuf.append( "," );
           }
           sbuf.append( " " )
               .append( colName );
        }
        sbuf.append( " FROM rr.res_role" )
            .append( " WHERE ivoid='" )
            .append( ivoid )
            .append( "'" );
        TapQuery tq =
            new TapQuery( new URL( regTapUrl ), sbuf.toString(), null );
        StarTable result =
            tq.executeSync( StoragePolicy.PREFER_MEMORY, coding );
        int nc = colNames.length;
        List<RegRole> list = new ArrayList<RegRole>();
        RowSequence rseq = result.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                final Map<String,String> map = new HashMap<String,String>();
                for ( int ic = 0; ic < nc; ic++ ) {
                    Object cell = row[ ic ];
                    if ( cell instanceof String ) {
                        map.put( colNames[ ic ], (String) cell );
                    }
                }
                list.add( new RegRole() {
                    public String getName() {
                        return map.get( NAME );
                    }
                    public String getEmail() {
                        return map.get( EMAIL );
                    }
                    public String getBaseRole() {
                        return map.get( BASE_ROLE );
                    }
                    public String getLogo() {
                        return map.get( LOGO );
                    }
                } );
            }
        }
        finally {
            rseq.close();
        }
        return list.toArray( new RegRole[ 0 ] );
    }
}
