package uk.ac.starlink.ttools.moc;

import cds.moc.SMoc;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.PrimitiveIterator;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.ttools.func.Coverage;
import uk.ac.starlink.util.DataBufferedOutputStream;

/**
 * Defines a MOC serialization format.
 *
 * @author   Mark Taylor
 * @since    8 Dec 2016
 */
public abstract class MocStreamFormat {

    private final String name_;

    /** Writes MOC 2.0 ASCII output. */
    public static final MocStreamFormat ASCII;

    /** Writes JSON format. */
    public static final MocStreamFormat JSON;

    /** Writes MOC 1.0-compliant FITS files. */
    public static final MocStreamFormat FITS;

    /** Writes a list of UNIQ values. */
    public static final MocStreamFormat RAW;

    /** Writes a text summary. */
    public static final MocStreamFormat SUMMARY;

    /** Writes MOC 2.0 ASCII format using CDS SMoc serialization. */
    public static final MocStreamFormat CDS_ASCII;

    /** Writes JSON format using CDS SMoc serialization. */
    public static final MocStreamFormat CDS_JSON;

    /** Writes MOC 1.0-compliant FITS files using CDS SMoc serialization. */
    public static final MocStreamFormat CDS_FITS;

    /** Known format instances. */
    public static final MocStreamFormat[] FORMATS = {
        ASCII = new AsciiFormat( "ascii" ),
        FITS = new FitsFormat( "fits" ),
        JSON = new JsonFormat( "json" ),
        RAW = new RawFormat( "raw" ),
        SUMMARY = new SummaryFormat( "summary" ),
        CDS_ASCII = new CdsFormat( "cds_ascii", (smoc, out) -> {
                                      smoc.writeASCII( out );
                                      out.write( (int) '\n' );
                                   } ),
        CDS_JSON = new CdsFormat( "cds_json", SMoc::writeJSON ),
        CDS_FITS = new CdsFormat( "cds_fits", SMoc::writeFITS ),
    };

    /**
     * Constructor.
     *
     * @param  name  format name
     */
    protected MocStreamFormat( String name ) {
        name_ = name;
    }

    /**
     * Outputs a given MOC to a given stream.
     *
     * @param  uniqIt  iterator over sorted list of uniq-encoded tile values
     * @param  count   number of tiles in iterator
     * @param  maxOrder  maximum HEALPix order represented in iterator
     * @param  out  destination stream
     */
    public abstract void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                                   int maxOrder, OutputStream out )
        throws IOException;

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Extracts the order from a UNIQ value.
     *
     * @param  uniq  well-formed UNIQ value
     * @return   order
     */
    private static int uniqToOrder( long uniq ) {
        return Coverage.uniqToOrder( uniq );
    }

    /**
     * Extracts the pixel index from a UNIQ value.
     *
     * @param  uniq  well-formed UNIQ value
     * @return   pixel index
     */
    private static long uniqToIndex( long uniq ) {
        return Coverage.uniqToIndex( uniq );
    }

    /**
     * Implementation that writes UNIQ values to output.
     */
    private static class RawFormat extends MocStreamFormat {

        /**
         * Constructor.
         *
         * @param  name  format name
         */
        RawFormat( String name ) {
            super( name );
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out )
                throws IOException {
            TextWriter writer = new TextWriter( out ) {
                public void writeUniq( long uniq ) throws IOException {
                    write( new StringBuffer()
                          .append( Long.toHexString( uniq ) )
                          .append( ":\t" )
                          .append( uniqToOrder( uniq ) )
                          .append( "\t" )
                          .append( uniqToIndex( uniq ) )
                          .append( "\n" )
                          .toString() );
                }
                public void flush() {
                }
            };
            while ( uniqIt.hasNext() ) {
                writer.writeUniq( uniqIt.nextLong() );
            }
            writer.flush();
        }
    }

    /**
     * Implementation that summarises MOC properties to output.
     */
    private static class SummaryFormat extends MocStreamFormat {

        /**
         * Constructor.
         *
         * @param  name  format name
         */
        SummaryFormat( String name ) {
            super( name );
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out ) {
            long ntile = 0;
            int maxOrd = 0;
            double cov = 0;
            Checksum cksum = new Adler32();
            while ( uniqIt.hasNext() ) {
                long uniq = uniqIt.next();
                ntile++;
                updateChecksum( cksum, uniq );
                int order = uniqToOrder( uniq );
                maxOrd = Math.max( order, maxOrd );
                cov += 1. / ( 12L << 2 * order );
            }
            if ( ntile != count ) {
                throw new IllegalArgumentException( "count mismatch: "
                                                  + ntile + " != " + count );
            }
            if ( maxOrd != maxOrder ) {
                throw new IllegalArgumentException( "Max order mismatch: "
                                                  + maxOrd + " != " + maxOrder);
            }
            PrintStream pout = new PrintStream( out );
            pout.println( "Count: " + count
                        + "\nOrder: " + maxOrder 
                        + "\nCoverage: " + cov
                        + "\nChecksum: " + Long.toHexString(cksum.getValue()) );
            pout.flush();
        }

        /**
         * Updates a supplied checksum object with a supplied long value.
         *
         * @param  cksum  checksum to update
         * @param  lvalue   long value
         */
        private static void updateChecksum( Checksum cksum, long lvalue ) {
            cksum.update( (byte) ( lvalue >>> 56 ) );
            cksum.update( (byte) ( lvalue >>> 48 ) );
            cksum.update( (byte) ( lvalue >>> 40 ) );
            cksum.update( (byte) ( lvalue >>> 32 ) );
            cksum.update( (byte) ( lvalue >>> 24 ) );
            cksum.update( (byte) ( lvalue >>> 16 ) );
            cksum.update( (byte) ( lvalue >>>  8 ) );
            cksum.update( (byte) ( lvalue >>>  0 ) );
        }
    }

    /**
     * Implementation that writes to MOC 2.0 ASCII format.
     */
    private static class AsciiFormat extends MocStreamFormat {

        /**
         * Constructor.
         *
         * @param  name  format name
         */
        AsciiFormat( String name ) {
            super( name );
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out )
                throws IOException {
            AsciiWriter writer = new AsciiWriter( out );
            while ( uniqIt.hasNext() ) {
                writer.writeUniq( uniqIt.nextLong() );
            }
            writer.flush();
        }

        /**
         * Writes a stream of UNIQ values to an ASCII representation.
         * It is kind of fiddly to get it right, which requires
         * writing N-M ranges where applicable.
         */
        private static class AsciiWriter extends TextWriter {

            private int lastOrder_;
            private long i0_;
            private long i1_;
            private long i2_;
            private int o0_;
            private int o1_;
            private int o2_;

            /**
             * Constructor.
             *
             * @param  out  destination stream
             */
            AsciiWriter( OutputStream out ) {
                super( out );
                o0_ = -3;
                o1_ = -1;
                i0_ = -2;
                i1_ = -4;
            }

            void writeUniq( long uniq ) throws IOException {
                i2_ = i1_;
                i1_ = i0_;
                i0_ = uniqToIndex( uniq );
                o2_ = o1_;
                o1_ = o0_;
                o0_ = uniqToOrder( uniq );
                if ( o1_ >= 0 ) {
                    write1();
                }
            }

            void flush() throws IOException {
                i2_ = i1_;
                i1_ = i0_;
                i0_ = Long.MAX_VALUE;
                o2_ = o1_;
                o1_ = o0_;
                o0_ = Integer.MAX_VALUE;
                write1();
                write( "\n" );
            }

            /**
             * Writes the last-but-one submitted value.
             */
            private void write1() throws IOException {

                /* Start of a new order, write order number and first index. */
                if ( o1_ != o2_ ) {
                    write( " " + Integer.toString( o1_ ) + "/"
                               + Long.toString( i1_ ) );
                }

                /* Same order as last time, write the index as appropriate. */
                else {
                    if ( i1_ != i2_ + 1 ) {
                        write( " " + Long.toString( i1_ ) );
                    }
                    else if ( i1_ != i0_ - 1 ) {
                        write( "-" + Long.toString( i1_ ) );
                    }
                    else {
                        // write nothing, we're in the middle of a range
                    }
                }
            }
        }
    }

    /**
     * Implementation that writes to JSON format.
     */
    private static class JsonFormat extends MocStreamFormat {

        /**
         * Constructor.
         *
         * @param  name  format name
         */
        JsonFormat( String name ) {
            super( name );
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out )
                throws IOException {
            JsonWriter writer = new JsonWriter( out );
            writer.write( "{" );
            while ( uniqIt.hasNext() ) {
                writer.writeUniq( uniqIt.nextLong() );
            }
            writer.flush();
        }

        /**
         * Writes a stream of UNIQ values to a JSON representation.
         */
        private static class JsonWriter extends TextWriter {
            private int lastOrder_;

            /**
             * Constructor.
             *
             * @param  out  destination stream
             */
            JsonWriter( OutputStream out ) {
                super( out );
                lastOrder_ = -1;
            }

            /**
             * Accepts the next uniq value for writing.
             *
             * @param  uniq  uniq value
             */
            void writeUniq( long uniq ) throws IOException {
                int order = uniqToOrder( uniq );
                long index = uniqToIndex( uniq );
                if ( order == lastOrder_ ) {
                    write( "," );
                }
                else {
                    if ( lastOrder_ >= 0 ) {
                        write( "]," );
                    }
                    write( "\n  \"" + Integer.toString( order ) + "\": [" );
                    lastOrder_ = order;
                }
                write( Long.toString( index ) );
            }

            /**
             * Must be called after the last uniq value has been submitted.
             */
            void flush() throws IOException {
                if ( lastOrder_ >= 0 ) {
                    write( "]" );
                }
                write( "\n}\n" );
            }
        }
    }

    /**
     * Implementation that writes to MOC-1.0 compatible FITS format.
     */
    private static class FitsFormat extends MocStreamFormat {

        /**
         * Constructor.
         *
         * @param  name  format name
         */
        FitsFormat( String name ) {
            super( name );
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out )
                throws IOException {
            boolean isLong = maxOrder >= 14;
            FitsUtil.writeEmptyPrimary( out );
            CardFactory cfact = CardFactory.DEFAULT;
            CardImage[] cards = new CardImage[] {
                cfact.createStringCard( "XTENSION", "BINTABLE",
                                        "binary table extension" ),
                cfact.createIntegerCard( "BITPIX", 8, "8-bit bytes" ),
                cfact.createIntegerCard( "NAXIS", 2, "2-dimensional table" ),
                cfact.createIntegerCard( "NAXIS1", isLong ? 8 : 4,
                                         "width of table in bytes" ),
                cfact.createIntegerCard( "NAXIS2", count, "row count" ),
                cfact.createIntegerCard( "PCOUNT", 0,
                                         "size of special data area" ),
                cfact.createIntegerCard( "GCOUNT", 1, "one data group" ),
                cfact.createIntegerCard( "TFIELDS", 1, "number of columns" ),
                cfact.createStringCard( "TFORM1", isLong ? "1K" : "1J",
                                        "Type for column 1" ),
                cfact.createStringCard( "MOCVERS", "2.0", "MOC version" ),
                cfact.createStringCard( "TTYPE1", "UNIQ", "UNIQ pixel number" ),
                cfact.createStringCard( "COORDSYS", "C",
                                        "Space reference frame" ),
                cfact.createStringCard( "MOCDIM", "SPACE",
                                        "Physical dimension" ),
                cfact.createIntegerCard( "MOCORD_S", maxOrder,
                                         "Maximum MOC order" ),
                cfact.createIntegerCard( "MOCORDER", maxOrder,
                                         "Maximum MOC order" ),
                cfact.createStringCard( "MOCTOOL", "STIL",
                                        "Name of MOC generator" ),
                CardFactory.END_CARD,
            };
            FitsUtil.writeHeader( cards, out );
            DataBufferedOutputStream dataOut =
                new DataBufferedOutputStream( out );
            if ( isLong ) {
                while ( uniqIt.hasNext() ) {
                    dataOut.writeLong( uniqIt.nextLong() );
                }
            }
            else {
                while ( uniqIt.hasNext() ) {
                    long uniq = uniqIt.nextLong();
                    int iuniq = (int) uniq;
                    assert iuniq == uniq;
                    dataOut.writeInt( iuniq );
                }
            }
            dataOut.flush();
            long nWritten = ( isLong ? 8 : 4 ) * count;
            int extra = (int) ( nWritten % (long) FitsUtil.BLOCK_LENG );
            if ( extra > 0 ) {
                out.write( new byte[ FitsUtil.BLOCK_LENG - extra ] );
            }
        }
    }

    /**
     * Implementation that works by constructing a CDS SMoc from the
     * UNIQ stream and then using one of the SMoc's serialization methods.
     */
    private static class CdsFormat extends MocStreamFormat {

        private final SmocWriter smocWriter_;
       
        /**
         * Constructor.
         *
         * @param  name  format name
         * @param  smocWriter  serializes an SMoc
         */
        CdsFormat( String name, SmocWriter smocWriter ) {
            super( name );
            smocWriter_ = smocWriter;
        }

        public void writeMoc( PrimitiveIterator.OfLong uniqIt, long count,
                              int maxOrder, OutputStream out )
                throws IOException {
            SMoc smoc = new SMoc();

            /* Construct the SMoc.  There may be more efficient ways
             * of doing this. */
            smoc.bufferOn( 500_000 );
            try {
                while ( uniqIt.hasNext() ) {
                    long uniq = uniqIt.nextLong();
                    smoc.add( uniqToOrder( uniq ), uniqToIndex( uniq ) );
                }
            }
            catch ( Exception e ) {
                throw new IOException( e );
            }
            smoc.bufferOff();

            /* Serialize the SMoc. */
            try {
                smocWriter_.writeSmoc( smoc, out );
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Exception e ) {
                throw new IOException( e );
            }
        }

        
        /**
         * Defines SMoc serialization.
         */
        @FunctionalInterface
        static interface SmocWriter {

            /**
             * Serializes an SMoc to a given output stream.
             *
             * @param  smoc  SMoc object 
             * @param  out  destination stream
             */
            void writeSmoc( SMoc smoc, OutputStream out ) throws Exception;
        }
    }

    /**
     * Utility class to help with serialization of text-like output.
     */
    private static abstract class TextWriter {

        private final OutputStream out_;

        /**
         * Constructor.
         *
         * @param  out  destination stream
         */
        TextWriter( OutputStream out ) {
            out_ = out;
        }

        /**
         * Accepts the next uniq value for writing.
         *
         * @param  uniq  uniq value
         */
        abstract void writeUniq( long uniq ) throws IOException;

        /**
         * Must be called after the last uniq value has been submitted.
         */
        abstract void flush() throws IOException;

        /**
         * Outputs a supplied string as UTF-8.
         *
         * @param  txt  text to write
         */
        void write( String txt ) throws IOException {
            out_.write( txt.getBytes( StandardCharsets.UTF_8 ) );
        }
    }
}
