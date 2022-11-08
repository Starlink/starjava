package uk.ac.starlink.gbin;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataSource;

/**
 * TableBuilder implementation for GBIN files.
 *
 * @author   Mark Taylor
 * @since    14 Aug 2014
 */
public class GbinTableBuilder extends DocumentedTableBuilder {

    private final MutableGbinTableProfile profile_;

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
        super( new String[] { "gbin" } );
        profile_ = new MutableGbinTableProfile( profile );
    }

    /**
     * Returns the object configuring how GBIN files are mapped to tables.
     * Note this may be a modified version of the object supplied at
     * construction time.
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
                GbinObjectReader.logError( Level.WARNING,
                                           "Couldn't read GBIN metadata", e );
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

    public boolean canStream() {
        return true;
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "GbinTableBuilder.xml" );
    }

    /**
     * Configures value of {@link GbinTableProfile#isReadMeta}.
     *
     * @param  isReadMeta  true to read metadata up front
     */
    @ConfigMethod(
        property = "readMeta",
        doc = "<p>Configures whether the GBIN metadata will be read "
            + "prior to reading the data. "
            + "This may slow things down slightly, "
            + "but means the row count can be determined up front, "
            + "which may have benefits for downstream processing."
            + "</p>\n",
        example = "false"
    )
    public void setReadMeta( boolean isReadMeta ) {
        profile_.isReadMeta_ = isReadMeta;
    }

    /**
     * Configures value of {@link GbinTableProfile#isHierarchicalNames}
     *
     * @param  isHierarchicalNames  whether to name columns hierarchically
     */
    @ConfigMethod(
        property = "hierarchicalNames",
        doc = "<p>Configures whether column names in the output table "
            + "should be forced to reflect the compositional hierarchy "
            + "of their position in the element objects.\n"
            + "If set true, columns will have names like "
            + "\"<code>Astrometry_Alpha</code>\", "
            + "if false they may just be called \"<code>Alpha</code>\".\n"
            + "In case of name duplication however, "
            + "the hierarchical form is always used."
            + "</p>\n",
        example = "true"
    )
    public void setHierarchicalNames( boolean isHierarchicalNames ) {
        profile_.isHierarchicalNames_ = isHierarchicalNames;
    }

    /**
     * Configures value of {@link GbinTableProfile#isTestMagic}.
     *
     * @param  isTestMagic  whether to read magic number from GBIN files
     */
    public void setTestMagic( boolean isTestMagic ) {
        profile_.isTestMagic_ = isTestMagic;
    }

    /**
     * Configures value of {@link GbinTableProfile#isSortedMethods}.
     *
     * @param  isSortedMethods  whether to sort method names alphabetically
     */
    public void isSortedMethods( boolean isSortedMethods ) {
        profile_.isSortedMethods_ = isSortedMethods;
    }

    /**
     * Mutable GbinTableProfile implementation.
     * It is based on a supplied instance, but at least some of its
     * behaviour can be customised by modifying its members.
     */
    private static class MutableGbinTableProfile implements GbinTableProfile {

        private final GbinTableProfile base_;
        boolean isReadMeta_;
        boolean isTestMagic_;
        boolean isHierarchicalNames_;
        boolean isSortedMethods_;

        /**
         * Constructor.
         *
         * @param  base  instance on which this object's behaviour is based
         */
        public MutableGbinTableProfile( GbinTableProfile base ) {
            base_ = base;
            isReadMeta_ = base.isReadMeta();
            isTestMagic_ = base.isTestMagic();
            isHierarchicalNames_ = base.isHierarchicalNames();
            isSortedMethods_ = base.isSortedMethods();
        }

        public boolean isReadMeta() {
            return isReadMeta_;
        }

        public boolean isTestMagic() {
            return isTestMagic_;
        }

        public boolean isHierarchicalNames() {
            return isHierarchicalNames_;
        }

        public boolean isSortedMethods() {
            return isSortedMethods_;
        }

        public String getNameSeparator() {
            return base_.getNameSeparator();
        }

        public String[] getIgnoreMethodNames() {
            return base_.getIgnoreMethodNames();
        }

        public String[] getIgnoreMethodDeclaringClasses() {
            return base_.getIgnoreMethodDeclaringClasses();
        }

        public Representation<?> createRepresentation( Class<?> clazz ) {
            return base_.createRepresentation( clazz );
        }
    }
}
