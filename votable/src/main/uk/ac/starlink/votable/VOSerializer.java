package uk.ac.starlink.votable;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableSerializer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;

/**
 * Class which encapsulates information about how fields and data are
 * serialized to a VOTable.  There are different subclasses for 
 * each VOTableWriter.Format instance.
 * <p>
 * Conceptually, this class and its concrete subclasses are private to
 * the VOTableWriter class, but they are in a separate file for readability.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class VOSerializer {
    final StarTable table;

    final static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructs a new serializer which can write a given StarTable.
     *
     * @param  table  the table to write
     */
    VOSerializer( StarTable table ) {
        this.table = table;
    }

    /**
     * Writes the FIELD headers corresponding to this table on a given writer.
     *
     * @param  writer  destination stream
     */
    abstract void writeFields( BufferedWriter writer ) throws IOException;
}


/**
 * Interface for VOSerializers which write their data as binary output 
 * (bytes rather than characters) to a STREAM element.
 */
interface VOStreamable {

    /**
     * Writes raw binary data representing the table data cells 
     * to an output stream.  This is the data which are contained in the
     * STREAM element of a VOTable document.  
     * No markup (e.g. the STREAM start/end tags) should be included.
     * 
     * @param  stream  destination stream
     */
    abstract void streamData( OutputStream stream ) throws IOException;
}


/**
 * Abstract class handling output for native-type VOTables.
 * There are concrete subclasses for TABLEDATA and BINARY serializers.
 */
abstract class NativeVOSerializer extends VOSerializer {
    final Encoder[] encoders;

    NativeVOSerializer( StarTable table ) {
        super( table );
        int ncol = table.getColumnCount();
        encoders = new Encoder[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            encoders[ icol ] = Encoder.getEncoder( info );
            if ( encoders[ icol ] == null ) {
                logger.warning( "Can't serialize column " + info + 
                                " of type " + 
                                info.getContentClass().getName() );
            }
        }
    }

    void writeFields( BufferedWriter writer ) throws IOException {
        int ncol = encoders.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            Encoder encoder = encoders[ icol ];
            if ( encoder != null ) {
                String content = encoder.getFieldContent();
                Map atts = encoder.getFieldAttributes();
                VOTableWriter.writeFieldElement( writer, content, atts );
            }
            else {
                writer.write( "<!-- Omitted column " + 
                              table.getColumnInfo( icol ) + " -->" );
                writer.newLine();
            }
        }
    }
}

class TabledataVOSerializer extends NativeVOSerializer {

    TabledataVOSerializer( StarTable table ) {
        super( table );
    }
 
    /**
     * Writes the table data cells into a TABLEDATA element.  This is 
     * pure XML and includes markup, but the TABLEDATA start/end tags
     * are not written.
     *
     * @param   writer  destination stream
     */
    void writeData( BufferedWriter writer ) throws IOException {
        int ncol = encoders.length;
        for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            writer.write( "  <TR>" );
            writer.newLine();
            Object[] rowdata = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Encoder encoder = encoders[ icol ];
                if ( encoder != null ) {
                    String text = encoder.encodeAsText( rowdata[ icol ] );
                    writer.write( "    <TD>" );
                    writer.write( VOTableWriter.formatText( text ) );
                    writer.write( "</TD>" );
                    writer.newLine();
                }
            }
            writer.write( "  </TR>" );
            writer.newLine();
        }
        writer.flush();
    }
}

class BinaryVOSerializer extends NativeVOSerializer implements VOStreamable {

    BinaryVOSerializer( StarTable table ) {
        super( table );
    }

    public void streamData( OutputStream stream ) throws IOException {
        DataOutputStream out = new DataOutputStream( stream );
        int ncol = encoders.length;
        for ( RowSequence rseq = table.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            Object[] row = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Encoder encoder = encoders[ icol ];
                if ( encoder != null ) {
                    encoder.encodeToStream( row[ icol ], (DataOutput) out );
                }
            }
        }
        out.flush();
    }
}

class FITSVOSerializer extends VOSerializer implements VOStreamable {

    private final FitsTableSerializer fitser;

    FITSVOSerializer( StarTable table ) throws IOException {
        super( table );
        fitser = new FitsTableSerializer( table );
    }

    void writeFields( BufferedWriter writer ) throws IOException {
        int ncol = table.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {

            /* Get information about how this column is going to be 
             * written by the FITS serializer. */
            char tform = fitser.getFormatChar( icol );
            int[] dims = fitser.getDimensions( icol );

            /* Only write a FIELD element if the FITS serializer is going
             * to serialize it. */
            if ( dims != null ) {

                /* Get the basic information for this column. */
                Encoder encoder =
                    Encoder.getEncoder( table.getColumnInfo( icol ) );
                String content = encoder.getFieldContent();
                Map atts = encoder.getFieldAttributes();

                /* Modify the datatype attribute to match what the FITS
                 * serializer will write. */
                String datatype;
                switch ( tform ) {
                    case 'L': datatype = "boolean";       break;
                    case 'X': datatype = "bit";           break;
                    case 'B': datatype = "unsignedByte";  break;
                    case 'I': datatype = "short";         break;
                    case 'J': datatype = "int";           break;
                    case 'K': datatype = "long";          break;
                    case 'A': datatype = "char";          break;
                    case 'E': datatype = "float";         break;
                    case 'D': datatype = "double";        break;
                    case 'C': datatype = "floatComplex";  break;
                    case 'M': datatype = "doubleComplex"; break;
                    default:
                        throw new AssertionError( "Unknown format letter " 
                                                + tform );
                }
                atts.put( "datatype", datatype );

                /* Modify the arraysize attribute to match what the FITS
                 * serializer will write. */
                if ( dims.length == 0 ) {
                    atts.remove( "arraysize" );
                }
                else {
                    StringBuffer arraysize = new StringBuffer();
                    for ( int i = 0; i < dims.length; i++ ) {
                        if ( i > 0 ) {
                            arraysize.append( 'x' );
                        }
                        arraysize.append( dims[ i ] );
                    }
                    atts.put( "arraysize", arraysize.toString() );
                }

                /* Write out the FIELD element with attributes which match
                 * the way the FITS serializer will write the table. */
                VOTableWriter.writeFieldElement( writer, content, atts );
            }
            else {
                writer.write( "<!-- Omitted column " + 
                              table.getColumnInfo( icol ) + " -->" );
                writer.newLine();
            }
        }
    }

    public void streamData( OutputStream stream ) throws IOException {
        DataOutputStream out = new DataOutputStream( stream );
        FitsConstants.writeEmptyPrimary( out );
        fitser.writeHeader( out );
        fitser.writeData( out );
        out.flush();
    }
}
