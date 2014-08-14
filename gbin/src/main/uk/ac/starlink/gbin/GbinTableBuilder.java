package uk.ac.starlink.gbin;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;

/**
 * TableBuilder implementation for GBIN files.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2014
 */
public class GbinTableBuilder implements TableBuilder {

    private final GbinTableProfile profile_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.gbin" );

    /** ValueInfo for GBIN row object classname. */
    public static final ValueInfo CLASSNAME_INFO =
        new DefaultValueInfo( "GbinClass", String.class,
                              "Classname of GBIN objects" );

    /** ValueInfo for GBIN description string. */
    public static final ValueInfo DESCRIPTION_INFO =
        new DefaultValueInfo( "GbinDescription", String.class,
                              "GBIN build description" );

    /**
     * Constructs a builder with default options.
     */
    public GbinTableBuilder() {
        this( new GbinTableProfile() {
            public boolean isReadMeta() {
                return true;
            }
            public boolean isTestMagic() {
                return true;
            }
            public boolean isHierarchicalNames() {
                return false;
            }
            public String getNameSeparator() {
                return "_";
            }
            public boolean isSortedMethods() {
                return true;
            }
            public String[] getIgnoreMethodNames() {
                return new String[] {
                    "getClass",
                    "getField",
                    "getStringValue",
                    "getGTDescription",
                    "getParamMaxValues",
                    "getParamMinValues",
                    "getParamOutOfRangeValues",
                    "getFieldNames",
                };
            }
        } );
    }

    /**
     * Constructs a builder with custom options.
     *
     * @param  profile  configures how GBIN files will be mapped to a table
     */
    public GbinTableBuilder( GbinTableProfile profile ) {
        profile_ = profile;
    }

    /**
     * Returns the object configuring how GBIN files are mapped to tables.
     *
     * @return  configuration profile
     */
    public GbinTableProfile getProfile() {
        return profile_;
    }

    /**
     * Returns "GBIN".
     */
    public String getFormatName() {
        return "GBIN";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        GbinTableReader trdr = null;
        try {
            trdr = new GbinTableReader( new BufferedInputStream( in ),
                                        profile_ );
            sink.acceptMetadata( new GbinStarTable( trdr, -1 ) {
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
            } );
            while ( trdr.next() ) {
                sink.acceptRow( trdr.getRow() );
            }
            sink.endRows();
        }
        finally {
            if ( trdr != null ) {
                trdr.close();
            }
        }
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage )
            throws IOException {

        /* If required, check the magic number of the input file. */
        if ( profile_.isTestMagic() &&
             ! GbinObjectReader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not GBIN" );
        }

        /* Read the start of the file to obtain essential metadata. */
        GbinTableReader trdr =
            new GbinTableReader(
                new BufferedInputStream( datsrc.getInputStream() ), profile_ );
        trdr.close();

        /* If required, read the start of the file again to obtain
         * non-essential metadata.  This requires another read, since the
         * way GBIN files and the GaiaTools reader API are arranged,
         * pretty much anything you read from a GBIN file makes it
         * useless for reading anything else. */
        final long nrow;
        final List<DescribedValue> params = new ArrayList<DescribedValue>();
        if ( profile_.isReadMeta() ) {
            Long nrowObj;
            InputStream metaIn =
                new BufferedInputStream( datsrc.getInputStream() );
            try {
                Object gbinRdrObj =
                    GbinObjectReader.createGbinReaderObject( metaIn );
                GbinMeta meta =
                    GbinMetadataReader.attemptReadMetadata( gbinRdrObj );
                nrowObj = meta.getTotalElementCount();
                params.add(
                     new DescribedValue( DESCRIPTION_INFO,
                                         meta.buildDescription( false ) ) );
            }
            catch ( Throwable e ) {
                nrowObj = null;
                logger_.log( Level.WARNING, "Couldn't read GBIN metadata", e );
            }
            finally {
                metaIn.close();
            }
            nrow = nrowObj == null ? -1 : nrowObj.longValue();
        }
        else {
            nrow = -1;
        }

        /* Construct a table with the metadata we have obtained,
         * and return it. */
        StarTable table = new GbinStarTable( trdr, nrow ) {
            public RowSequence getRowSequence() throws IOException {
                return new GbinTableReader(
                           new BufferedInputStream( datsrc.getInputStream() ),
                           profile_ );
            }
        };
        for ( DescribedValue param : params ) {
            table.setParameter( param );
        }
        return table;
    }

    /** 
     * Partial StarTable implementation for use with GBIN files.
     */
    private static abstract class GbinStarTable extends AbstractStarTable {
        private final ColumnInfo[] colInfos_;
        private final long nrow_;

        /**
         * Constructor.
         *
         * @param  trdr  contains metadata about the table;
         *               will not be used to read data (may be closed)
         * @param  nrow  row count (-1 if not known)
         */
        GbinStarTable( GbinTableReader trdr, long nrow ) {
            nrow_ = nrow;
            colInfos_ = trdr.getColumnInfos();
            setParameter( new DescribedValue( CLASSNAME_INFO,
                                              trdr.getItemClass().getName() ) );
        }

        public int getColumnCount() {
            return colInfos_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public long getRowCount() {
            return nrow_;
        }
    }
}
