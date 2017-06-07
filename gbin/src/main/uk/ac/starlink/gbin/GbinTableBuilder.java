package uk.ac.starlink.gbin;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;
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

    /** ValueInfo for well-known Gaia table name. */
    public static final ValueInfo GAIATABLENAME_INFO =
        new DefaultValueInfo( "GaiaTableName", String.class,
                              "Well-known name of Gaia table" );

    /** ValueInfo for Gaia table description string. */
    public static final ValueInfo DESCRIP_INFO =
        new DefaultValueInfo( "Description", String.class,
                              "Table description from Gaia data model" );

    /** ValueInfo for Gaia column detailed description string. */
    public static final ValueInfo COLDETAIL_INFO =
        new DefaultValueInfo( "GaiaDetail", String.class,
                              "Detailed column description"
                            + " from Gaia data model" );

    /** ValueInfo for GBIN build description string. */
    public static final ValueInfo BUILDDESCRIP_INFO =
        new DefaultValueInfo( "GbinDescription", String.class,
                              "GBIN build description" );

    /**
     * Constructs a builder with default options.
     */
    public GbinTableBuilder() {
        this( new DefaultGbinTableProfile() );
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
        try {
            GbinObjectReader reader = GbinObjectReader.createReader( in );
            if ( ! reader.hasNext() ) {
                throw new IOException( "No objects in GBIN file" );
            }
            Object gobj0 = reader.next();
            GbinStarTable table =
                    new GbinStarTable( profile_, gobj0.getClass() ) {
                public long getRowCount() {
                    return -1;
                }
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
            };
            sink.acceptMetadata( table );
            RowSequence rseq = table.createRowSequence( reader, gobj0 );
            while ( rseq.next() ) {
                sink.acceptRow( rseq.getRow() );
            }
            sink.endRows();
            rseq.close();
        }
        finally {
            in.close();
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

        /* Read an object out of the file to find out the data type. */
        InputStream in = new BufferedInputStream( datsrc.getInputStream() );
        GbinObjectReader rdr = GbinObjectReader.createReader( in );
        if ( ! rdr.hasNext() ) {
            throw new IOException( "No objects in GBIN file" );
        }
        Class<?> gobjClazz = rdr.next().getClass();
        in.close();

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
                     new DescribedValue( BUILDDESCRIP_INFO,
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
        final long nrow0 = nrow;
        StarTable table = new GbinStarTable( profile_, gobjClazz ) {
            public RowSequence getRowSequence() throws IOException {
                final InputStream in =
                    new BufferedInputStream( datsrc.getInputStream() );
                GbinObjectReader reader = GbinObjectReader.createReader( in );
                if ( ! reader.hasNext() ) {
                    throw new IOException( "No objects in GBIN file" );
                }
                Object gobj0 = reader.next();
                return new WrapperRowSequence( createRowSequence( reader,
                                                                  gobj0 ) ) {
                    @Override
                    public void close() throws IOException {
                        in.close();
                    }
                };
            }
            public long getRowCount() {
                return nrow0;
            }
        };
        for ( DescribedValue param : params ) {
            table.setParameter( param );
        }
        return table;
    }
}
