package uk.ac.starlink.vo.datalink;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceParam;
import uk.ac.starlink.util.CgiQuery;
import uk.ac.starlink.util.URLUtils;

/**
 * Provides functionality related to invoking a service defined by
 * a DataLink document.
 * It is based on a {@link uk.ac.starlink.votable.datalink.ServiceDescriptor}.
 *
 * <p>This class divides the input
 * {@link uk.ac.starlink.votable.datalink.ServiceParam}s defined by the
 * <code>ServiceDescriptor</code> into three different categories
 * according to how their values are acquired at service invocation time:
 * <ul>
 * <li><em>row</em> parameters:
 *     the value is acquired from a cell of the result table;
 *     a table row object must be supplied to obtain the value
 *     </li>
 * <li><em>fixed</em> parameters:
 *     the value is provided by the parameter definition
 *     and fixed for all invocations
 *     </li>
 * <li><em>user</em> parameters:
 *     the value must be supplied for each invocation by the user
 *     </li>
 * </ul>
 *
 * <p>The DataLink-1.0 standard is not very explicit about this distinction,
 * but the above division appears(?) to be the consensus of the standard
 * authors.  This does not however prevent service invocations filling
 * these parameter values differently than according to the above outline.
 * For instance, users may be given the option to change the values of
 * fixed or row parameters for a given invocation.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2017
 * @see      <a href="http://www.ivoa.net/documents/DataLink/"
 *              >DataLink-1.0, sec 4</a>
 */
public class ServiceInvoker {

    private final ServiceDescriptor descriptor_;
    private final URL urlBase_;
    private final Map<ServiceParam,RowValuer> rowParams_;
    private final Map<ServiceParam,String> fixedParams_;
    private final List<ServiceParam> userParams_;

    /**
     * Attempts to construct an invoker based on
     * a service descriptor and an associated results table.
     *
     * @param  descriptor  service descriptor
     * @param   resultTable  result table corresponding to the descriptor
     * @throws  MalformedURLException  if the descriptor's accessUrl is no good
     * @throws  IOException  if the descriptor references a column that is
     *                       not present in the result table
     */
    public ServiceInvoker( ServiceDescriptor descriptor, StarTable resultTable )
            throws MalformedURLException, IOException {
        descriptor_ = descriptor;
        urlBase_ = URLUtils.newURL( descriptor.getAccessUrl() );
        rowParams_ = new LinkedHashMap<ServiceParam,RowValuer>();
        fixedParams_ = new LinkedHashMap<ServiceParam,String>();
        userParams_ = new ArrayList<ServiceParam>();

        /* Go through the service's input parameters and record each one
         * as a row, fixed or user parameter. */
        for ( ServiceParam p : descriptor.getInputParams() ) {
            String ref = p.getRef();
            String value = p.getValue();
            if ( ref != null ) {
                rowParams_.put( p, createRowValuer( resultTable, ref ) );
            }
            else if ( value != null && value.length() > 0 ) {
                fixedParams_.put( p, value );
            }
            else {
                userParams_.add( p );
            }
        }
    }

    /**
     * Returns this invoker's service descriptor.
     *
     * @return   service descriptor
     */
    public ServiceDescriptor getServiceDescriptor() {
        return descriptor_;
    }

    /**
     * Returns the 'row' parameters of this service.
     * These take their values from one of the columns in the results table.
     *
     * @return  list of 'row' parameters
     */
    public ServiceParam[] getRowParams() {
        return rowParams_.keySet().toArray( new ServiceParam[ 0 ] );
    }

    /**
     * Returns the 'fixed' parameters of this service.
     * These have fixed values supplied in the parameter definitions.
     *
     * @return  list of 'fixed' parameters
     */
    public ServiceParam[] getFixedParams() {
        return fixedParams_.keySet().toArray( new ServiceParam[ 0 ] );
    }

    /**
     * Returns the 'user' parameters of this service.
     * The values for these must to be supplied externally
     * in some unspecified way (for instance by user action).
     * They may have some hints in the form of options or min/max values.
     *
     * @return  list of 'user' parameters
     */
    public ServiceParam[] getUserParams() {
        return userParams_.toArray( new ServiceParam[ 0 ] );
    }

    /**
     * Returns a parameter-value map for the fixed parameters.
     *
     * @return  map containing fixed parameter-value pairs
     */
    public Map<ServiceParam,String> getFixedParamMap() {
        return fixedParams_;
    }

    /**
     * Constructs a parameter-value map for the row parameters
     * at a given table row.
     *
     * @param  row  a row of cell values from this invoker's table
     * @return   map containing row parameter-value pairs
     */
    public Map<ServiceParam,String> getRowParamMap( Object[] row ) {
        Map<ServiceParam,String> vmap =
            new LinkedHashMap<ServiceParam,String>();
        for ( Map.Entry<ServiceParam,RowValuer> entry :
              rowParams_.entrySet() ) {
            ServiceParam param = entry.getKey();
            String value = entry.getValue().getValueString( row );
            vmap.put( param, value );
        }
        return vmap;
    }

    /**
     * Assembles an invocation URL from the base URL of this service and any
     * name-value pairs supplied in a given map.
     * Values are appended in the form "<code>?n1=v1&amp;n2=v2</code>".
     *
     * @param   paramMap  map representing parameter values to be
     *                    appended to this service's base URL
     * @return  base URL with supplied parameter name-value pairs appended
     */
    public URL completeUrl( Map<ServiceParam,String> paramMap ) {
        CgiQuery query = new CgiQuery( urlBase_.toString() );
        for ( Map.Entry<ServiceParam,String> entry : paramMap.entrySet() ) {
            query.addArgument( entry.getKey().getName(), entry.getValue() );
        }
        return query.toURL();
    }

    /**
     * Assembles an invocation URL from the base URL of this service,
     * the values of any row parameters corresponding to a supplied
     * table row, and any additional parameters supplied explicitly.
     *
     * <p>This utility method calls the methods
     * {@link #getRowParamMap getRowParamMap},
     * {@link #getFixedParamMap getFixedParamMap} and
     * {@link #completeUrl completeUrl}.
     *
     * @param  row   table row
     * @param  userParamMap  additional non-'row' parameter-value pairs,
     *         usually corresponding to the 'user' parameters
     * @return  base URL with row and user name-value pairs appended
     */
    public URL getUrl( Object[] row, Map<ServiceParam,String> userParamMap ) {
        Map<ServiceParam,String> paramMap =
            new LinkedHashMap<ServiceParam,String>();
        paramMap.putAll( getFixedParamMap() );
        paramMap.putAll( getRowParamMap( row ) );
        paramMap.putAll( userParamMap );
        return completeUrl( paramMap );
    }

    /**
     * Returns an object that can extraxt the value corresponding to
     * a VOTable FIELD with a given ID value from a table row.
     *
     * @param  table  contenxt table (from a VOTable)
     * @param  colRef  value of VOTable FIELD @ID attribute
     *                 whose string value is to be extracted from table rows
     * @return  extractor object if possible
     * @throws   IOException  if no FIELD with the right ID can be found
     */
    private static RowValuer createRowValuer( StarTable table, String colRef )
            throws IOException {
        int ncol = table.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            final ColumnInfo info = table.getColumnInfo( ic );
            if ( colRef.equals( info.getAuxDatumValue( VOStarTable.ID_INFO,
                                                       String.class ) ) ) {
                final int icol = ic;
                return new RowValuer() {
                    public String getValueString( Object[] row ) {
                        Object v = row[ icol ];
                        return v == null ? null : info.formatValue( v, 256 );
                    }
                };
            }
        }
        throw new IOException( "No FIELD found with ID=\"" + colRef + "\"" );
    }

    /**
     * Defines an object that can extract a string value from a table row.
     */
    private interface RowValuer {

        /**
         * Returns a string value based on an array of cell values.
         *
         * @param  row  cell values in a table row
         * @return  string value
         */
        String getValueString( Object[] row );
    }
}
