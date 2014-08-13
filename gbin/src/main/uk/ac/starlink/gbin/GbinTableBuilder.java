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

public class GbinTableBuilder implements TableBuilder {

    private final GbinTableProfile profile_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.gbin" );

    public static final ValueInfo CLASSNAME_INFO =
        new DefaultValueInfo( "GbinClass", String.class,
                              "Classname of GBIN objects" );
    public static final ValueInfo DESCRIPTION_INFO =
        new DefaultValueInfo( "GbinDescription", String.class,
                              "GBIN build description" );

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
            public String[] getIgnoreNames() {
                return new String[] {
                    "Class",
                    "Field",
                    "StringValue",
                    "GTDescription",
                    "ParamMaxValues",
                    "ParamMinValues",
                    "ParamOutOfRangeValues",
                    "FieldNames",
                };
            }
        } );
    }

    public GbinTableBuilder( GbinTableProfile profile ) {
        profile_ = profile;
    }

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
        if ( profile_.isTestMagic() &&
             ! GbinObjectReader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not GBIN" );
        }
        GbinTableReader trdr =
            new GbinTableReader(
                new BufferedInputStream( datsrc.getInputStream() ), profile_ );
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
        trdr.close();
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

    private static abstract class GbinStarTable extends AbstractStarTable {
        private final GbinTableReader trdr_;
        private final long nrow_;

        GbinStarTable( GbinTableReader trdr, long nrow ) {
            trdr_ = trdr;
            nrow_ = nrow;
            @SuppressWarnings("unchecked")
            List<DescribedValue> paramList = getParameters();
            paramList.add( new DescribedValue( CLASSNAME_INFO,
                                               trdr_.getItemClass()
                                                    .getName() ) );
        }

        public int getColumnCount() {
            return trdr_.getColumnCount();
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return trdr_.getColumnInfo( icol );
        }

        public long getRowCount() {
            return nrow_;
        }
    }
}
