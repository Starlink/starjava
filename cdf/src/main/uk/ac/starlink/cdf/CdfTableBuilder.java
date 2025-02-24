package uk.ac.starlink.cdf;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.record.Buf;
import uk.ac.bristol.star.cdf.record.Bufs;
import uk.ac.bristol.star.cdf.record.WrapperBuf;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.table.formats.DocumentedTableBuilder;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Table input handler for NASA CDF (Common Data Format) files.
 *
 * @author   Mark Taylor
 * @since    24 Jun 2013
 */
public class CdfTableBuilder extends DocumentedTableBuilder {

    /**
     * Default CDF-StarTable translation profile.
     * This is based on the ISTP Metadata Guidelines at
     * <a href="https://spdf.gsfc.nasa.gov/sp_use_of_cdf.html"
     *         >https://spdf.gsfc.nasa.gov/sp_use_of_cdf.html</a>
     */
    public static final CdfTableProfile DEFAULT_PROFILE = createProfile(
        true,
        new String[] { "CATDESC", "FIELDNAM", "DESCRIP", "DESCRIPTION", },
        new String[] { "UNITS", "UNIT", "UNITSTRING", },
        new String[] { "FILLVAL", },
        new String[] { "DEPEND_0", }
    );

    private final CdfTableProfile profile_;

    /**
     * Constructs a default Cdf table builder.
     */
    public CdfTableBuilder() {
        this( DEFAULT_PROFILE );
    }

    /**
     * Constructs a Cdf table builder with a custom translation profile.
     *
     * @param   profile  CDF-Startable translation profile
     */
    public CdfTableBuilder( CdfTableProfile profile ) {
        super( new String[] { "cdf" } );
        profile_ = profile;
    }

    /**
     * Returns "CDF".
     */
    public String getFormatName() {
        return "CDF";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        if ( ! CdfReader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not a CDF file" );
        }

        /* Get a buf containing the byte data from the input source. */
        final Buf nbuf;
        if ( datsrc instanceof FileDataSource &&
             datsrc.getCompression() == Compression.NONE ) {
            File file = ((FileDataSource) datsrc).getFile();
            nbuf = Bufs.createBuf( file, true, true );
        }
        else {
            ByteStore byteStore = storagePolicy.makeByteStore();
            BufferedOutputStream storeOut =
                new BufferedOutputStream( byteStore.getOutputStream() );
            InputStream dataIn = datsrc.getInputStream();
            IOUtils.copy( dataIn, storeOut );
            dataIn.close();
            storeOut.flush();
            ByteBuffer[] bbufs = byteStore.toByteBuffers();
            storeOut.close();
            byteStore.close();
            nbuf = Bufs.createBuf( bbufs, true, true );
        }

        /* Fix the Buf implementation so that it uses the supplied
         * storage policy for allocating any more required storage. */
        Buf buf = new StoragePolicyBuf( nbuf, storagePolicy );

        /* Turn the buf into a CdfContent and thence into a StarTable. */
        CdfContent content = new CdfContent( new CdfReader( buf ) );
        return new CdfStarTable( content, profile_, null ) {
            @Override
            public void close() {
                // this should really do cleanup on file descriptors etc
                // held by the buf.
            }
        };
    }

    /**
     * Returns false.  I don't think there is a MIME type associated with
     * the CDF format.  References to application/x-cdf and application/cdf
     * appear on the web, but neither is IANA registered, and I <em>think</em>
     * they refer to some other format.
     */
    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    /**
     * Throws a TableFormatException.
     * CDF is not suitable for streaming.
     */
    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        throw new TableFormatException( "Can't stream from CDF format" );
    }

    public String getXmlDescription() {
        return String.join( "\n",
            "<p>NASA's Common Data Format, described at",
            DocumentedIOHandler.toLink( "https://cdf.gsfc.nasa.gov/" ) + ",",
            "is a binary format for storing self-described data.",
            "It is typically used to store tabular data for subject areas",
            "like space and solar physics.",
            "</p>",
        "" );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public boolean canStream() {
        return false;
    }

    /**
     * Buf implementation that delegates most methods to a base instance,
     * but when creating new Buf objects it does so in accordance with
     * a give STIL StoragePolicy.
     */
    private static class StoragePolicyBuf extends WrapperBuf {
        private final Buf baseBuf_;
        private final StoragePolicy storagePolicy_;

        /**
         * Constructor.
         *
         * @param   baseBuf  buf supplying base behaviour
         * @param  storagePolicy  policy for managing new buffer creation
         */
        StoragePolicyBuf( Buf baseBuf, StoragePolicy storagePolicy ) {
            super( baseBuf );
            baseBuf_ = baseBuf;
            storagePolicy_ = storagePolicy;
        }

        @Override
        public Buf fillNewBuf( long count, InputStream in ) throws IOException {

            /* Copy the input stream data to a byte store obtained from
             * the storage policy. */
            ByteStore byteStore = storagePolicy_.makeByteStore();
            OutputStream out = byteStore.getOutputStream();
            int bufsiz = 16384;
            byte[] a = new byte[ bufsiz ];
            for ( int ntot = 0; ntot < count; ) {
                int n = in.read( a, 0,
                                 Math.min( bufsiz, (int) ( count - ntot ) ) );
                if ( n < 0 ) {
                    throw new IOException( "Stream ended after " + ntot + "/"
                                         + count + " bytes" );
                }
                else {
                    out.write( a, 0, n );
                    ntot += n;
                }
            }
            out.flush();

            /* Turn the byte store into NIO buffers. */
            ByteBuffer[] bbufs = byteStore.toByteBuffers();
            byteStore.close();

            /* Turn the NIO buffers into a Buf. */
            return Bufs.createBuf( bbufs,
                                   super.isBit64(), super.isBigendian() );
        }
    }

    /**
     * Constructs an instance of CdfTableProfile with some suggestions
     * for attribute names with known semantics.
     *
     * @param  invarParams  true for turning non-row-varying variables into
     *                      table parameters, false for turning them into
     *                      variables
     * @param  descripAttNames  ordered list of names of attributes
     *                          that might supply description metadata
     * @param  unitAttNames     ordered list of names of attributes
     *                          that might supply units metadata
     * @param  blankvalAttNames ordered list of names of attributes
     *                          that might supply magic blank values
     * @param  depend0AttNames  ordered list of names of attributes
     *                          that might supply a dependent variable name
     * @return  new profile instance
     */
    public static CdfTableProfile createProfile( boolean invarParams,
                                                 String[] descripAttNames,
                                                 String[] unitAttNames,
                                                 String[] blankvalAttNames,
                                                 String[] depend0AttNames ) {
        return new ListCdfTableProfile( invarParams, descripAttNames,
                                        unitAttNames, blankvalAttNames,
                                        depend0AttNames );
    }

    /**
     * CdfTableProfile implementation based on lists of names.
     */
    private static class ListCdfTableProfile implements CdfTableProfile {
        private final boolean invarParams_;
        private final String[] descAttNames_;
        private final String[] unitAttNames_;
        private final String[] blankvalAttNames_;
        private final String[] depend0AttNames_;

        /**
         * Constructor.
         *
         * @param  invarParams  true for turning non-row-varying variables into
         *                      table parameters, false for turning them into
         *                      variables
         * @param  descripAttNames  ordered list of names of attributes
         *                          that might supply description metadata
         * @param  unitAttNames     ordered list of names of attributes
         *                          that might supply units metadata
         * @param  blankvalAttNames ordered list of names of attributes
         *                          that might supply magic blank values
         * @param  depend0AttNames  ordered list of names of attributes
         *                          that might supply a dependent variable name
         */
        ListCdfTableProfile( boolean invarParams, String[] descripAttNames,
                             String[] unitAttNames, String[] blankvalAttNames,
                             String[] depend0AttNames ) {
            invarParams_ = invarParams;
            descAttNames_ = descripAttNames;
            unitAttNames_ = unitAttNames;
            blankvalAttNames_ = blankvalAttNames;
            depend0AttNames_ = depend0AttNames;
        }

        public boolean invariantVariablesToParameters() {
            return invarParams_;
        }

        public String getDescriptionAttribute( String[] attNames ) {
            return match( descAttNames_, attNames );
        }

        public String getUnitAttribute( String[] attNames ) {
            return match( unitAttNames_, attNames );
        }

        public String getBlankValueAttribute( String[] attNames ) {
            return match( blankvalAttNames_, attNames );
        }

        public String getDepend0Attribute( String[] attNames ) {
            return match( depend0AttNames_, attNames );
        }

        /**
         * Returns the first element of a list of options that matches
         * any element of a list of targets.  Matching is normalised.
         *
         * @param  opts  options
         * @param  targets  targets
         * @return  first matching option, or null if none match
         */
        private String match( String[] opts, String[] targets ) {
            Set<String> targetSet = new HashSet<String>();
            for ( int i = 0; i < targets.length; i++ ) {
                targetSet.add( normalise( targets[ i ] ) );
            }
            for ( int i = 0; i < opts.length; i++ ) {
                String opt = opts[ i ];
                if ( targetSet.contains( normalise( opt ) ) ) {
                    return opt;
                }
            }
            return null;
        }

        /**
         * Normalises a token by folding case.
         *
         * @param  txt  token
         * @return  normalised token
         */
        private String normalise( String txt ) {
            return txt.trim().toLowerCase();
        }
    }
}
