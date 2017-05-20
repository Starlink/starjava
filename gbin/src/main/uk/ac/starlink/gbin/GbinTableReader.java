package uk.ac.starlink.gbin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;

/**
 * Does most of the work for turning a GbinObjectReader into a table.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2014
 */
public class GbinTableReader implements RowSequence {

    private final InputStream in_;
    private final GbinObjectReader reader_;
    private final Object gobj0_;
    private final String gaiaTableName_;
    private final int ncol_;
    private final ItemReader[] itemReaders_;
    private final Map<ItemReader,Object> itemMap_;
    private final ColumnInfo[] colInfos_;
    private boolean started_;

    /**
     * Constructor.
     *
     * @param   in   input stream containing GBIN file
     * @param  profile  configures details of table construction
     */
    public GbinTableReader( InputStream in, GbinTableProfile profile )
            throws IOException {
        in_ = in;
        reader_ = GbinObjectReader.createReader( in );
        if ( ! reader_.hasNext() ) {
            throw new IOException( "No objects in GBIN file" );
        }
        itemMap_ = new HashMap<ItemReader,Object>();

        /* Read the first object.  We need this now so we know the class
         * of all the objects in the file (assumed the same). */
        gobj0_ = reader_.next();

        /* Get the "official" gaia table name from the class of this object,
         * if it has one. */
        gaiaTableName_ = gobj0_ == null
                       ? null
                       : GbinMetadataReader.getGaiaTableName( gobj0_
                                                             .getClass() );

        /* Construct an array of readers, one for each column, based on
         * the object class. */
        itemReaders_ =
            ItemReader.createItemReaders( gobj0_.getClass(), profile );
        ncol_ = itemReaders_.length;

        /* Group readers by the leaf name of the items they read.
         * The main purpose of this is so we can tell if there are
         * duplicate names, which in turn will affect how columns
         * are named. */
        Map<String,List<ItemReader>> nameMap =
            new HashMap<String,List<ItemReader>>();
        boolean forceHier = profile.isHierarchicalNames();
        String separator = profile.getNameSeparator();
        if ( ! forceHier ) {
            for ( int ic = 0; ic < ncol_; ic++ ) {
                ItemReader irdr = itemReaders_[ ic ];
                String name = irdr.getItemName();
                if ( ! nameMap.containsKey( name ) ) {
                    nameMap.put( name, new ArrayList<ItemReader>() );
                }
                nameMap.get( name ).add( irdr );
            }
        }

        /* Construct a list of column infos based on what we have. */
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            ItemReader irdr = itemReaders_[ ic ];
            String cname =
                ItemReader.getColumnName( irdr, nameMap, forceHier, separator );
            colInfos_[ ic ] =
                new ColumnInfo( cname, irdr.getItemContentClass(), null );
        }
    }

    /**
     * Returns the column metadata representing the columns this object
     * will read.
     *
     * @return  array of metadata items, one for each column
     */
    public ColumnInfo[] getColumnInfos() {
        return colInfos_;
    }

    /**
     * Returns the class of the elements contained in the GBIN file.
     * In fact it's just the class of the first one, but it's assumed
     * the same for all.
     *
     * @return  element class
     */
    public Class getItemClass() {
        return gobj0_.getClass();
    }

    /**
     * Returns the name of the table as known to the Gaia data model
     * on the classpath, if any.
     *
     * @return  well-known gaia table name, or null
     */
    public String getGaiaTableName() {
        return gaiaTableName_;
    }

    public boolean next() throws IOException {
        itemMap_.clear();
        if ( started_ ) {
            if ( reader_.hasNext() ) {
                itemMap_.put( ItemReader.ROOT, reader_.next() );
                return true;
            }
            else {
                return false;
            }
        }
        else {
            started_ = true;
            itemMap_.put( ItemReader.ROOT, gobj0_ );
            return true;
        }
    }

    public Object getCell( int icol ) throws IOException {
        return itemReaders_[ icol ].readItem( itemMap_ );
    }

    public Object[] getRow() throws IOException {
        Object[] row = new Object[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            row[ ic ] = itemReaders_[ ic ].readItem( itemMap_ );
        }
        return row;
    }

    public void close() throws IOException {
        in_.close();
    }
}
