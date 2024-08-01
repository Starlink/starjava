package uk.ac.starlink.gbin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;


/**
 * Partial StarTable implementation for use with GBIN files.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2017
 */
public abstract class GbinStarTable extends AbstractStarTable {

    private final Class<?> gobjClazz_;
    private final int ncol_;
    private final ItemReader[] itemReaders_;
    private final ColumnInfo[] colInfos_;

    /**
     * Constructor.
     *
     * @param  profile  configures how GBIN files will be mapped to a table
     * @param  gobjClazz   class of all objects in GBIN file
     */
    @SuppressWarnings("this-escape")
    protected GbinStarTable( GbinTableProfile profile, Class<?> gobjClazz ) {
        gobjClazz_ = gobjClazz;

        /* Table metadata. */
        setParameter( new DescribedValue( GbinTableBuilder.CLASSNAME_INFO,
                                          gobjClazz.getName() ) );

        /* Get the "official" gaia table name from the class of this object,
         * if it has one. */
        String gaiaTableName = GbinMetadataReader.getGaiaTableName( gobjClazz );

        /* Get additional metadata about this table from the classpath
         * data model classes if available. */
        final GaiaTableMetadata tmeta;
        if ( gaiaTableName != null ) {
            setName( gaiaTableName );
            setParameter( new DescribedValue( GbinTableBuilder
                                             .GAIATABLENAME_INFO,
                                              gaiaTableName ) );
            tmeta = GbinMetadataReader.getTableMetadata( gaiaTableName );
        }
        else {
            tmeta = null;
        }

        /* Table description. */
        if ( tmeta != null ) {
            String descrip = tmeta.getTableDescription();
            if ( descrip != null ) {
                setParameter( new DescribedValue( GbinTableBuilder.DESCRIP_INFO,
                                                  descrip ) );
            }
        }

        /* Construct an array of readers, one for each column, based on
         * the object class. */
        itemReaders_ = ItemReader.createItemReaders( gobjClazz, profile );
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

            /* Basic column info. */
            ItemReader irdr = itemReaders_[ ic ];
            String cname =
                ItemReader.getColumnName( irdr, nameMap, forceHier, separator );
            ColumnInfo cinfo =
                new ColumnInfo( cname, irdr.getItemContentClass(), null );
            colInfos_[ ic ] = cinfo;

            /* Additional column info from GbinMetadataReader if available. */
            String archName = GbinMetadataReader
                             .convertNameToArchiveFormat( cinfo.getName() );
            if ( archName != null ) {
                cinfo.setName( archName );
            }
            if ( tmeta != null ) {

                /* Column description. */
                String descrip = tmeta.getParameterDescription( cname );
                if ( descrip != null && descrip.trim().length() > 0 ) {
                    cinfo.setDescription( descrip );
                }

                /* Column UCDs. */
                List<?> ucdList = tmeta.getUcds( cname );
                if ( ucdList != null && ucdList.size() > 0 ) {
                    StringBuffer ucdBuf = new StringBuffer();
                    for ( Object ucd : ucdList ) {
                        if ( ucdBuf.length() > 0 ) {
                            ucdBuf.append( ";" );
                        }
                        ucdBuf.append( ucd );
                    }
                    if ( ucdBuf.length() > 0 ) {
                        cinfo.setUCD( ucdBuf.toString() );
                    }
                }

                /* Column extra detail (ends up in STIL but probably
                 * not in any serialized formats). */
                String detail =
                    tmeta.getParameterDetailedDescription( cname );
                if ( detail != null && detail.trim().length() > 0 ) {
                    cinfo.setAuxDatum( new DescribedValue( GbinTableBuilder
                                                          .COLDETAIL_INFO,
                                                           detail ) );
                }
            }
        }
    }

    public int getColumnCount() {
        return ncol_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    /**
     * Returns a row sequence for this table based on some required objects.
     *
     * @param   reader  gbin object reader positioned after the first object
     *                  in the stream; must read objects suitable for this table
     * @param   gobj0   first object read from reader
     * @return  new row sequence for this table
     */
    public RowSequence createRowSequence( final GbinObjectReader reader,
                                          final Object gobj0 ) {
        final Map<ItemReader,Object> itemMap = new HashMap<ItemReader,Object>();
        return new RowSequence() {
            boolean started;
            public boolean next() throws IOException {
                itemMap.clear();
                if ( started ) {
                    if ( reader.hasNext() ) {
                        itemMap.put( ItemReader.ROOT, reader.next() );
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                else {
                    started = true;
                    itemMap.put( ItemReader.ROOT, gobj0 );
                    return true;
                }
            }
            public Object getCell( int icol ) throws IOException {
                if ( started ) {
                    return itemReaders_[ icol ].readItem( itemMap );
                }
                else {
                    throw new IllegalStateException( "next() not called" );
                }
            }
            public Object[] getRow() throws IOException {
                if ( started ) {
                    Object[] row = new Object[ ncol_ ];
                    for ( int ic = 0; ic < ncol_; ic++ ) {
                        row[ ic ] = itemReaders_[ ic ].readItem( itemMap );
                    }
                    return row;
                }
                else {
                    throw new IllegalStateException( "next() not called" );
                }
            }
            public void close() {
            }
        };
    }

    /**
     * Returns the class of the elements contained in the GBIN file.
     *
     * @return  element class
     */
    public Class<?> getGaiaObjectClass() {
        return gobjClazz_;
    }

    /**
     * Returns the ItemReaders used by this table.
     *
     * @return   array of ItemReaders, one for each column
     */
    ItemReader[] getItemReaders() {
        return itemReaders_;
    }

    /**
     * Returns a table instance based on a collection of gaia objects.
     * The returned table is not random access.
     *
     * <p>A random-access implementation is possible, but trying to
     * implement <code>getCell</code> efficiently would be a bit fiddly
     * because of the way that column values are extracted.
     *
     * @param  profile  configures how Gaia objects will be mapped to columns
     * @param  gobjClazz   class of all objects representing rows
     * @param  gobjList  collection of typed objects, one for each table row
     * @return   sequential-access table based on array of gaia objects
     */
    public static <T> GbinStarTable
            createCollectionTable( GbinTableProfile profile,
                                   Class<T> gobjClazz,
                                   final Collection<? extends T> gobjList ) {
        return new GbinStarTable( profile, gobjClazz ) {
            public long getRowCount() {
                return gobjList.size();
            }
            public RowSequence getRowSequence() {
                final Iterator<?> it = gobjList.iterator();
                final Map<ItemReader,Object> itemMap =
                    new HashMap<ItemReader,Object>();
                final ItemReader[] itemReaders = getItemReaders();
                final int ncol = itemReaders.length;
                return new RowSequence() {
                    public boolean next() {
                        itemMap.clear();
                        if ( it.hasNext() ) {
                            itemMap.put( ItemReader.ROOT, it.next() );
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                    public Object getCell( int icol ) throws IOException {
                        return itemReaders[ icol ].readItem( itemMap );
                    }
                    public Object[] getRow() throws IOException {
                        Object[] row = new Object[ ncol ];
                        for ( int ic = 0; ic < ncol; ic++ ) {
                            row[ ic ] = itemReaders[ ic ].readItem( itemMap );
                        }
                        return row;
                    }
                    public void close() {
                    }
                };
            }
        };
    }
}
