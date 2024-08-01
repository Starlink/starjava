package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * Table representing the flattened results of a registry query.
 * Each row represents an Interface of a Capability of a VOResource,
 * or, in the case that a VOResource has no Capabilities, just a VOResource.
 * Only selected attributes (the most useful?) of these structures are
 * represented here as columns.
 * If you want a more detailed representation of a VOResource object,
 * which is hierarchical, you will need to examine the VOResource objects
 * themselves.
 * 
 * @author   Mark Taylor
 * @since    19 Dec 2008
 */
public class RegistryStarTable extends ColumnStarTable {

    private final Record[] records_;

    private static final ValueInfo IDENTIFIER_INFO =
        new DefaultValueInfo( "ID", String.class,
                              "Registry identifier URI for resource" );
    private static final ValueInfo SHORTNAME_INFO =
        new DefaultValueInfo( "ShortName", String.class,
                              "Short name for resource" );
    private static final ValueInfo TITLE_INFO =
        new DefaultValueInfo( "Title", String.class,
                              "Title for resource" );
    private static final ValueInfo PUBLISHER_INFO =
        new DefaultValueInfo( "Publisher", String.class,
                              "Person or organisation responsible for "
                            + "publishing the resource" );
    private static final ValueInfo SUBJECTS_INFO =
        new DefaultValueInfo( "Subjects", String[].class,
                              "Topic, object type, or other descriptive "
                            + "keywords about the resource" );
    private static final ValueInfo REFURL_INFO =
        new DefaultValueInfo( "ReferenceURL", String.class,
                              "URL describing the resource" );
    private static final ValueInfo CONTACT_INFO =
        new DefaultValueInfo( "Contact", String.class,
                              "Person to contact concerning this resource" );
    private static final ValueInfo CAP_DESCRIPTION_INFO =
        new DefaultValueInfo( "CapDesc", String.class,
                              "Description of the service capability" );
    private static final ValueInfo CAP_STDID_INFO =
        new DefaultValueInfo( "CapStdId", String.class,
                              "Standard ID URI describing the type of "
                            + "service capability provided" );
    private static final ValueInfo CAP_ACURL_INFO = 
        new DefaultValueInfo( "AccessURL", String.class,
                              "Access URL for the service capability" );
    private static final ValueInfo CAP_VERSION_INFO =
        new DefaultValueInfo( "CapVersion", String.class,
                              "Version of the service capability provided" );
    private static final RegCapabilityInterface EMPTY_CAPABILITY =
        new RegCapabilityInterface() {
            public String getAccessUrl() {
                return null;
            }
            public String getDescription() {
                return null;
            }
            public String getStandardId() {
                return null;
            }
            public String getXsiType() {
                return null;
            }
            public String getVersion() {
                return null;
            }
        };

    /**
     * Constructor.
     *
     * @param   query   the query whose results are to be represented
     */
    @SuppressWarnings("this-escape")
    public RegistryStarTable( RegistryQuery query ) throws IOException {
        records_ = getRecords( query );
        setParameters( new ArrayList<DescribedValue>
                                    ( Arrays.asList( query.getMetadata() ) ) );
        addColumn( new ColumnData( IDENTIFIER_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getIdentifier();
            }
        } );
        addColumn( new ColumnData( SHORTNAME_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getShortName();
            }
        } );
        addColumn( new ColumnData( TITLE_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getTitle();
            }
        } );
        addColumn( new ColumnData( PUBLISHER_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getPublisher();
            }
        } );
        addColumn( new ColumnData( SUBJECTS_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getSubjects();
            }
        } );
        addColumn( new ColumnData( REFURL_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getReferenceUrl();
            }
        } );
        addColumn( new ColumnData( CONTACT_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).resource_.getContact();
            }
        } );
        addColumn( new ColumnData( CAP_DESCRIPTION_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).capability_.getDescription();
            }
        } );
        addColumn( new ColumnData( CAP_STDID_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).capability_.getStandardId();
            }
        } );
        addColumn( new ColumnData( CAP_ACURL_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).capability_.getAccessUrl();
            }
        } );
        addColumn( new ColumnData( CAP_VERSION_INFO ) {
            public Object readValue( long irow ) {
                return getRecord( irow ).capability_.getVersion();
            }
        } );
    }

    public long getRowCount() {
        return records_.length;
    }

    /**
     * Returns the Record object corresponding to a given row.
     *
     * @param  irow  row index
     * @return   record
     */
    private Record getRecord( long irow ) {
        return records_[ Tables.checkedLongToInt( irow ) ];
    }

    /**
     * Performs a query and returns the result as an array of flattened
     * Record objects.
     *
     * @param  query   query specification
     * @return   record list
     */
    private static Record[] getRecords( RegistryQuery query )
            throws IOException {
        List<Record> recList = new ArrayList<Record>();
        for ( Iterator<RegResource> it = query.getQueryIterator();
              it.hasNext(); ) {
            RegResource resource = it.next();
            RegCapabilityInterface[] caps = resource.getCapabilities();
            if ( caps.length == 0 ) {
                recList.add( new Record( resource, EMPTY_CAPABILITY ) );
            }
            else {
                for ( int ic = 0; ic < caps.length; ic++ ) {
                    recList.add( new Record( resource, caps[ ic ] ) );
                }
            }
        }
        return recList.toArray( new Record[ 0 ] );
    }

    /**
     * Struct-type class which aggregates a resource and one of its
     * capability/interfaces.
     */
    private static class Record {
        final RegResource resource_;
        final RegCapabilityInterface capability_;
        Record( RegResource resource, RegCapabilityInterface capability ) {
            resource_ = resource;
            capability_ = capability;
        }
    }
}
