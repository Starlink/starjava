package uk.ac.starlink.votable;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.fits.CardFactory;
import uk.ac.starlink.fits.CardImage;
import uk.ac.starlink.fits.ColFitsTableSerializer;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.fits.FitsTableSerializerConfig;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.FitsUtil;
import uk.ac.starlink.fits.StandardFitsTableSerializer;
import uk.ac.starlink.fits.VariableFitsTableSerializer;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.formats.DocumentedIOHandler;
import uk.ac.starlink.util.ConfigMethod;
import uk.ac.starlink.util.DataBufferedOutputStream;
import uk.ac.starlink.util.IOConsumer;

/**
 * FITS output handler that supports multiple different options.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2024
 * @see      uk.ac.starlink.util.BeanConfig
 */
public class UnifiedFitsTableWriter extends FitsTableWriter
                                    implements DocumentedIOHandler {

    /**
     * Primary HDU type containing full metadata for the output table
     * in VOTable format.  This corresponds to "fits-plus" format.
     * The default version of VOTable is used.
     */
    public static final PrimaryType VOTABLE_PRIMARY_TYPE =
        new VOTablePrimaryType( "votable", VOTableVersion.getDefaultVersion() );

    /**
     * Primary HDU type containing a minimal primary HDU.
     * This corresponds to basic FITS format.
     */
    public static final PrimaryType BASIC_PRIMARY_TYPE = PrimaryType.BASIC;

    /**
     * Primary HDU type that does not write a primary HDU.
     */
    public static final PrimaryType NONE_PRIMARY_TYPE = PrimaryType.NONE;

    private static final Charset XML_ENCODING = StandardCharsets.UTF_8;

    /**
     * No-arg constructor.
     */
    @SuppressWarnings("this-escape")
    public UnifiedFitsTableWriter() {
        setFormatName( "fits" );
        setPrimaryType( VOTABLE_PRIMARY_TYPE );
    }

    public String[] getExtensions() {
        return isColfits()
             ? new String[] { "colfits", }
             : new String[] { "fits", "fit", "fts", };
    }

    public boolean looksLikeFile( String location ) {
        return DocumentedIOHandler.matchesExtension( this, location );
    }

    public boolean docIncludesExample() {
        return false;
    }

    public String getXmlDescription() {
        return readText( "/uk/ac/starlink/votable/UnifiedFitsTableWriter.xml" );
    }

    @ConfigMethod(
        property = "primary",
        usage = "basic|votable[n.n]|none",
        example = "basic",
        sequence = 110,
        doc = "<p>Determines what is written into the Primary HDU.\n"
            + "The Primary HDU (PHDU) of a FITS file cannot contain a table;\n"
            + "the following options are available.\n"
            + "<dl>\n"
            + "<dt><code>basic</code></dt>\n"
            + "<dd><p>A minimal PHDU is written with no interesting content\n"
            + "    </p></dd>"
            + "<dt><code>votable[n.n]</code></dt>\n"
            + "<dd><p>The PHDU contains the full table metadata as the text\n"
            + "    of a VOTable document, along with headers to indicate\n"
            + "    that this has been done.\n"
            + "    This corresponds to the \"<strong>fits-plus</strong>\"\n"
            + "    format.\n"
            + "    The \"<code>[n.n]</code>\" part\n"
            + "    is optional, but if included\n"
            + "    (e.g. \"<code>votable1.5</code>\")\n"
            + "    indicates the version of the VOTable format to use.\n"
            + "    </p></dd>\n"
            + "<dt><code>none</code></dt>\n"
            + "<dd><p>No PHDU is written.\n"
            + "    The output is therefore not a legal FITS file,\n"
            + "    but it can be appended to an existing FITS file that\n"
            + "    already has a PHDU and perhaps other extension HDUs.\n"
            + "    </p></dd>\n"
            + "</dl>\n"
            + "</p>"
    )
    @Override
    public void setPrimaryType( PrimaryType primaryType ) {
        super.setPrimaryType( primaryType );
    }

    @ConfigMethod(
        property = "col",
        sequence = 120,
        doc = "<p>If true, writes data in column-oriented format.\n"
            + "In this case, the output is a single-row table in which\n"
            + "each cell is an array value holding the data\n"
            + "for an entire column.\n"
            + "All the arrays in the row have the same length,\n"
            + "which is the row count of the table being represented.\n"
            + "This corresponds to the \"<strong>colfits</strong>\" format.\n"
            + "</p>"
    )
    @Override
    public void setColfits( boolean colfits ) {
        super.setColfits( colfits );
    }

    @ConfigMethod(
        property = "var",
        sequence = 130,
        example = "true",
        doc = "<p>Determines how variable-length array-valued columns\n"
            + "will be stored.\n"
            + "<code>True</code> stores variable-length array values\n"
            + "after the main part of the table in the heap,\n"
            + "while <code>false</code> stores all arrays as fixed-length\n"
            + "(with a length equal to that of the longest array\n"
            + "in the column) in the body of the table."
            + "The options <code>P</code> or <code>Q</code> can be used\n"
            + "to force 32-bit or 64-bit pointers for indexing into the heap,\n"
            + "but it's not usually necessary since a suitable choice\n"
            + "is otherwise made from the data.\n"
            + "</p>"
    )
    @Override
    public void setVarArray( VarArrayMode varArray ) {
        super.setVarArray( varArray );
    }

    /**
     * Returns a PrimaryType that writes a FITS-plus-style VOTable primary HDU,
     * with a specified version of the VOTable standard.
     *
     * @param  vers  VOTable version
     * @return   new PHDU type
     */
    public static PrimaryType createVOTablePrimaryType( VOTableVersion vers ) {
        return new VOTablePrimaryType( "votable" + vers.getVersionNumber(),
                                       vers );
    }

    /**
     * Decodes a string as a PrimaryType.
     *
     * <p>This public static method is used by BeanConfig.
     *
     * @param  txt  string representation of a PrimaryType
     * @return   primary type instance, or null if txt doesn't look like one
     */
    public static PrimaryType toPrimaryTypeInstance( String txt ) {
        if ( PrimaryType.BASIC.toString().equalsIgnoreCase( txt ) ) {
            return PrimaryType.BASIC;
        }
        else if ( PrimaryType.NONE.toString().equalsIgnoreCase( txt ) ) {
            return PrimaryType.NONE;
        }
        else if ( txt.length() >= 7 &&
                  "votable".equalsIgnoreCase( txt.substring( 0, 7 ) ) ) {
            String vtxt = txt.substring( 7 );
            if ( vtxt.length() == 0 ) {
                return VOTABLE_PRIMARY_TYPE;
            }
            else {
                VOTableVersion votvers =
                    VOTableVersion.getKnownVersions().get( vtxt );
                return votvers == null
                     ? null
                     : createVOTablePrimaryType( votvers );
            }
        }
        else {
            return null;
        }
    }

    /**
     * Subclass with default configuration but colfits set true.
     */
    public static class Col extends UnifiedFitsTableWriter {
        @SuppressWarnings("this-escape")
        public Col() {
            setColfits( true );
            setFormatName( "colfits" );
        }
    }

    /**
     * PrimaryType implementation for a VOTable PHDU.
     */
    private static class VOTablePrimaryType extends PrimaryType {

        private final VOTableVersion votVersion_;

        /**
         * Constructor.
         *
         * @param  name  type name
         * @param  version   VOTable version
         */
        VOTablePrimaryType( String name, VOTableVersion version ) {
            super( name );
            votVersion_ = version;
        }

        public boolean allowSignedByte() {
            return false;
        }

        public void writeTables( FitsTableWriter writer, TableSequence tseq,
                                 OutputStream out )
                throws IOException {

            /* Get all the input tables and serializers up front.
             * This does have negative implications for scalability
             * (can't stream one table at a time), but it's necessary
             * to write the header. */
            List<StarTable> tlist = new ArrayList<>();
            for ( StarTable table; ( table = tseq.nextTable() ) != null; ) {
                tlist.add( table );
            }
            StarTable[] tables = tlist.toArray( new StarTable[ 0 ] );
            writeFitsPlusTables( writer, tables, out );
        }

        @Override
        public int hashCode() {
            return votVersion_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof VOTablePrimaryType ) {
                VOTablePrimaryType other = (VOTablePrimaryType) o;
                return this.votVersion_.equals( other.votVersion_ );
            }
            else {
                return false;
            }
        }

        /**
         * Writes an array of tables with VOTable PHDUs
         *
         * @param  writer  FITS writer
         * @param  tables  tables to write
         * @param  out   destination stream
         */
        private void writeFitsPlusTables( FitsTableWriter writer,
                                          StarTable[] tables, OutputStream out )
                throws IOException {
            List<TableWithSerializer> tsList = new ArrayList<>();
            for ( StarTable table : tables ) {
                FitsTableSerializer fitser = writer.createSerializer( table );
                tsList.add( new TableWithSerializer( table, fitser ) );
            }
            TableWithSerializer[] tss =
                tsList.toArray( new TableWithSerializer[ 0 ] );

            /* Prepare destination stream. */
            DataBufferedOutputStream dout = new DataBufferedOutputStream( out );
            out = null;

            /* Write the primary HDU. */
            writeVOTablePrimary( writer, tss, dout );

            /* Write the data. */
            for ( TableWithSerializer ts : tss ) {
                writer.writeTableHDU( ts.table_, ts.fitser_, dout );
            }

            /* Tidy up. */
            dout.flush();
        }

        /**
         * Writes a primary that consists of a byte array holding a
         * UTF8-encoded VOTable which holds the table metadata.
         *
         * @param  tables  tables to write
         * @param  fitsers   FITS serializers
         * @param  out   destination stream
         */
        private void writeVOTablePrimary( FitsTableWriter fitsWriter,
                                          TableWithSerializer[] tss,
                                          OutputStream out )
                throws IOException {
            int ntable = tss.length;

            /* Get a buffer to hold the VOTable character data. */
            StringWriter textWriter = new StringWriter();

            /* Turn it into a BufferedWriter. */
            BufferedWriter writer = new BufferedWriter( textWriter );

            /* Get an object that knows how to write a VOTable document. */
            VOTableWriter votWriter =
                new VOTableWriter( (DataFormat) null, false, votVersion_ );
            votWriter.setWriteDate( fitsWriter.getWriteDate() );

            /* Output preamble. */
            votWriter.writePreTableXML( writer );
            String plusComment = new StringBuffer()
                .append( "<!-- " )
                .append( "Describes BINTABLE extensions in the following " )
                .append( ntable == 1 ? "HDU" : ( ntable + " HDUs" ) )
                .append( "." )
                .append( "-->" )
                .toString();
            writer.write( plusComment );
            writer.newLine();

            /* Output table elements containing the metadata with the help of
             * the VOTable serializer. */
            int i = 0;
            for ( TableWithSerializer ts : tss ) {
                if ( i++ > 0 ) {
                    votWriter.writeBetweenTableXML( writer );
                }
                VOSerializer voser =
                    VOSerializer
                   .makeFitsSerializer( ts.table_, ts.fitser_, votVersion_ );
                voser.writePreDataXML( writer );
                writer.write( "<!-- Dummy VOTable - no DATA element -->" );
                writer.newLine();
                voser.writePostDataXML( writer );
            }

            /* Output trailing tags and flush. */
            votWriter.writePostTableXML( writer );
            writer.flush();

            /* Get a byte array containing the VOTable text. */
            byte[] textBytes = textWriter.getBuffer().toString()
                                         .getBytes( XML_ENCODING );
            int nbyte = textBytes.length;

            /* Prepare and write a FITS header describing the character data. */
            List<CardImage> cards = new ArrayList<>();
            CardFactory cf = CardFactory.STRICT;
            cards.addAll( Arrays.asList( new CardImage[] {
                cf.createLogicalCard( "SIMPLE", true, "Standard FITS format" ),
                cf.createIntegerCard( "BITPIX", 8, "Character data" ),
                cf.createIntegerCard( "NAXIS", 1, "Text string" ),
                cf.createIntegerCard( "NAXIS1", nbyte, "Number of characters" ),
            } ) );
            if ( fitsWriter.isColfits() ) {
                cards.add( cf.createLogicalCard( "COLFITS", true,
                                                 "Table extension stored " +
                                                 "column-oriented" ) );
            }
            cards.add( cf.createLogicalCard( "VOTMETA", true,
                                             "Table metadata in "
                                           + "VOTable format" ) );
            cards.add( cf.createLogicalCard( "EXTEND", true,
                                             "There are standard extensions" ));
            String plural = ntable == 1 ? "" : "s";
            String[] comments = new String[] {
                " ",
                "The data in this primary HDU consists of bytes which",
                "comprise a VOTABLE document.",
                "The VOTable describes the metadata of the table" + plural
                     + " contained",
                "in the following BINTABLE extension" + plural + ".",
                "Such a BINTABLE extension can be used on its own " +
                "as a perfectly",
                "good table, but the information from this HDU may provide " +
                "some",
                "useful additional metadata.",
                ( ntable == 1 ? "There is one following BINTABLE."
                              : "There are " + ntable
                                             + " following BINTABLEs." ),
            };
            for ( String comm : comments ) {
                cards.add( cf.createCommentCard( comm ) );
            }
            cards.add( cf.createIntegerCard( "NTABLE", ntable,
                                             "Number of following "
                                           + "BINTABLE HDUs" ));
            cards.add( CardFactory.END_CARD );
            assert primaryHeaderOK( fitsWriter,
                                    cards.toArray( new CardImage[ 0 ] ) );
            FitsUtil.writeHeader( cards.toArray( new CardImage[ 0 ] ), out );

            /* Write the character data itself. */
            out.write( textBytes );

            /* Write padding to the end of the FITS block. */
            int partial = textBytes.length % FitsUtil.BLOCK_LENG;
            if ( partial > 0 ) {
                int pad = FitsUtil.BLOCK_LENG - partial;
                out.write( new byte[ pad ] );
            }
        }

        /**
         * Indicates whether the primary header of a FITS file looks correct
         * for a writer of the given type.
         *
         * @param  fitsWriter  writer
         * @param  cards   header cards for primary header
         */
        private boolean primaryHeaderOK( FitsTableWriter fitsWriter,
                                         CardImage[] cards ) {
            final byte[] buf;
            try ( ByteArrayOutputStream bout = new ByteArrayOutputStream() ) {
                FitsUtil.writeHeader( cards, bout );
                buf = bout.toByteArray();
            }
            catch ( IOException e ) {
                assert false;
                return false;
            }
            return fitsWriter.isColfits()
                 ? ColFitsPlusTableBuilder.isMagic( buf )
                 : FitsPlusTableBuilder.isMagic( buf );                   
        }
    }

    /**
     * Convenience class that aggregates a table with a FitsTableSerializer
     * that can serialize it.
     */
    private static class TableWithSerializer {
        final StarTable table_;
        final FitsTableSerializer fitser_;
        TableWithSerializer( StarTable table, FitsTableSerializer fitser ) {
            table_ = table;
            fitser_ = fitser;
        }
    }
}
