package uk.ac.starlink.vo.datalink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceDescriptorFactory;

/**
 * Represents the result of a DataLink Links service response.
 * Such documents are usually:
 * <ul>
 * <li>returned from a service with standardID
 *     "<code>ivo://ivoa.net/std/DataLink#links-1.0</code>"</li>
 * <li>described by a MIME type
 *     "<code>application/x-votable+xml;content=datalink</code>"</li>
 * </ul>
 *
 * <p>An instance of this class gives you what you need to work with
 * a Links service response.
 *
 * <p>This class simply aggregates three items: the table, column map,
 * and service descriptors.  However it also has some static utility
 * methods that are useful for creating and working with datalink tables.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2017
 */
public abstract class LinksDoc {

    /**
     * Returns the results table.  This is the data table present in
     * the RESOURCE element with @type="results".
     * According to DALI there is only one such results resource is present,
     * and according to DataLink it contains only one table.
     *
     * @return  sole results table
     */
    public abstract StarTable getResultTable();

    /**
     * Returns an object that knows where the DataLink-defined columns
     * are in this document's table.
     *
     * @return  column map
     */
    public abstract LinkColMap getColumnMap();

    /**
     * Returns a list of the ServiceDescriptor objects defined by
     * RESOURCES with @type="meta" and @utype="adhoc:service".
     *
     * @return   service descriptor objects
     */
    public abstract ServiceDescriptor[] getServiceDescriptors();

    /**
     * Creates a LinksDoc with fixed members.
     *
     * @param   resultTable   results table
     * @param   colMap  object that understands which columns mean what
     *                  in the <code>resultTable</code>
     * @param   servDescriptors  list of service descriptor objects
     *                           associated with the table
     */
    public static LinksDoc
            createLinksDoc( final StarTable resultTable,
                            final LinkColMap colMap,
                            final ServiceDescriptor[] servDescriptors ) {
        return new LinksDoc() {
            public StarTable getResultTable() {
                return resultTable;
            }
            public LinkColMap getColumnMap() {
                return colMap;
            }
            public ServiceDescriptor[] getServiceDescriptors() {
                return servDescriptors;
            }
        };
    }

    /**
     * Returns a LinksDoc based on a supplied table.
     *
     * <p>There is no guarantee that the result will be represent a useful
     * DataLink document, for instance it may have none of the required
     * DataLink columns.
     *
     * @param   table  assumed DataLink results table
     * @return   LinksDoc based on table
     */
    public static LinksDoc createLinksDoc( StarTable table ) {
        LinkColMap colMap = LinkColMap.getMap( table );
        ServiceDescriptor[] sds = getServiceDescriptors( table );
        return createLinksDoc( table, colMap, sds );
    }

    /**
     * Parses a VOElement as a LinksDoc.
     * The supplied element will normally be a top-level VOTABLE element.
     *
     * <p>There is no guarantee that the result will be represent a useful
     * DataLink document, for instance it may have none of the required
     * DataLink columns.
     *
     * @param  el  VOTable DOM element that contains RESOURCES corresponding
     *             to DataLink data and metadata
     * @return  datalink document object
     * @throws  IOException if the element structure does not contain a
     *          unique results table
     */
    public static LinksDoc createLinksDoc( VOElement el ) throws IOException {

        /* Locate table(s) and service descriptors that are descendants
         * of the supplied element.
         * Note this does not require either the results or service descriptor
         * RESOURCE elements to be immediate children of the supplied element.
         * This is deliberate: in some circumstances it might be necessary
         * to group multiple DataLink-type tables within the same VOTable
         * document. */
        StarTable[] tables = readResultTables( el );
        ServiceDescriptor[] descriptors =
            new ServiceDescriptorFactory().readAllServiceDescriptors( el );

        /* We need exactly one result table. */
        if ( tables.length == 0 ) {
            throw new IOException( "No results table found" );
        }
        else if ( tables.length > 1 ) {
            throw new IOException( "Multiple (" + tables.length + ") "
                                 + "result tables in Datalink document" );
        }

        /* Create and return a LinksDoc object from the results. */
        StarTable resultTable = tables[ 0 ];
        LinkColMap colMap = LinkColMap.getMap( resultTable );
        return createLinksDoc( resultTable, colMap, descriptors );
    }

    /**
     * Returns a LinksDoc with the same content as a given one,
     * for which the result table is guaranteed to support random access.
     *
     * @param  ldoc  input links doc
     * @return   the input links doc if it's random access,
     *           or a copy with a random-access-capable result table otherwise
     */
    public static LinksDoc randomAccess( LinksDoc ldoc ) throws IOException {
        return ldoc.getResultTable().isRandom()
             ? ldoc
             : createLinksDoc( Tables.randomTable( ldoc.getResultTable() ),
                               ldoc.getColumnMap(),
                               ldoc.getServiceDescriptors() );
    }

    /**
     * Utility method to extract a list of service descriptors associated
     * with a given StarTable.  It just goes through the table parameters
     * and chooses all those with ServiceDescriptor-typed values.
     *
     * @param  table   input table
     * @return   array of service descriptors associated with the table
     */
    public static ServiceDescriptor[] getServiceDescriptors( StarTable table ) {
        List<ServiceDescriptor> sdList = new ArrayList<ServiceDescriptor>();
        for ( DescribedValue param : table.getParameters() ) {
            Object value = param.getValue();
            if ( value instanceof ServiceDescriptor ) {
                sdList.add( (ServiceDescriptor) value );
            }
        }
        return sdList.toArray( new ServiceDescriptor[ 0 ] );
    }

    /**
     * Indicates whether the table in question looks like a Links-response
     * table.  A check against name, UCD and datatype is made for
     * all the links-response columns
     * (as listed in {@link LinkColMap#COLDEF_MAP}).
     * If the number of missing/incorrect columns does not exceed a given
     * tolerance, and at least one of the columns <code>access_url</code>,
     * <code>error_message</code> and <code>service_def</code> is present
     * and usable, true is returned.
     *
     * @param  table   table to test
     * @param  nMistake   maximum number of incorrect/missing columns tolerated;
     *                    2 might be a reasonable number?
     * @return  true iff table looks like a usable links-response table
     *          within the given tolerance
     */
    public static boolean isLinksResponse( StarTable table, int nMistake ) {
        if ( table == null ) {
            return false;
        }
        int ncol = table.getColumnCount();
        Set<LinkColMap.ColDef<?>> correctDefs =
            new HashSet<LinkColMap.ColDef<?>>();
        Set<LinkColMap.ColDef<?>> presentDefs =
            new HashSet<LinkColMap.ColDef<?>>();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo = table.getColumnInfo( icol );
            LinkColMap.ColDef<?> coldef =
                LinkColMap.COLDEF_MAP.get( cinfo.getName() );
            if ( coldef != null ) {
                if ( ((Class<?>) coldef.getContentClass())
                    .isAssignableFrom( cinfo.getContentClass() ) ) {
                    presentDefs.add( coldef );
                    String stdUcd = coldef.getUcd();
                    if ( stdUcd == null || stdUcd.equals( cinfo.getUCD() ) ) {
                        correctDefs.add( coldef );
                    }
                }
            }
        }
        if ( !( presentDefs.contains( LinkColMap.COL_ACCESSURL ) ||
                presentDefs.contains( LinkColMap.COL_ERRORMESSAGE ) ||
                presentDefs.contains( LinkColMap.COL_SERVICEDEF ) ) ) {
            return false;
        }
        else {
            int nMissing = 0;
            for ( LinkColMap.ColDef<?> stdCol :
                  LinkColMap.COLDEF_MAP.values() ) {
                if ( stdCol.isRequired() && ! correctDefs.contains( stdCol ) ) {
                    nMissing++;
                }
            }
            return nMissing <= nMistake;
        }
    }

    /**
     * Reads result table descendants of a given element.
     * These are constructed from any TABLE child of
     * a RESOURCE with @type="results".
     *
     * @param  el  container element
     * @return   list of zero or more result tables found
     */
    private static VOStarTable[] readResultTables( VOElement el )
            throws IOException {
        List<StarTable> tableList = new ArrayList<StarTable>();
        NodeList resourceList = el.getElementsByVOTagName( "RESOURCE" );
        for ( int i = 0; i < resourceList.getLength(); i++ ) {
            VOElement resEl = (VOElement) resourceList.item( i );
            if ( "results".equals( resEl.getAttribute( "type" ) ) ) {
                for ( VOElement tableEl : resEl.getChildrenByName( "TABLE" ) ) {
                    tableList.add( new VOStarTable( (TableElement) tableEl ) );
                }
            }
        }
        return tableList.toArray( new VOStarTable[ 0 ] );
    }
}
