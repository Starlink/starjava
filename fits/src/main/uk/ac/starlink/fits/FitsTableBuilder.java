package uk.ac.starlink.fits;

import java.awt.datatransfer.DataFlavor;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import nom.tam.fits.AsciiTable;
import nom.tam.fits.AsciiTableHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.RandomAccess;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Implementation of the <tt>TableBuilder</tt> interface which 
 * gets <tt>StarTable</tt>s from FITS files.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FitsTableBuilder implements TableBuilder {

    /**
     * Creates a StarTable from a DataSource which refers to a FITS
     * file or stream.  If the source has a position attribute, it
     * will be interpreted as an integer indicating which HDU the
     * table is in.  The first HDU is number 0 (though being a primary
     * HDU this one can't hold a table).  If there is no position,
     * the first HDU which does hold a table is used.
     *
     * @param  datsrc  the source of the FITS table data
     * @param  wantRandom  whether a random-access table is preferred
     * @return  a new StarTable based on <tt>datsrc</tt>
     */
    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom ) 
            throws IOException {

        /* Check if this looks like a FITS file. */
        if ( ! FitsConstants.isMagic( datsrc.getIntro() ) ) {
            return null;
        }

        ArrayDataInput strm = null;
        StarTable table = null;
        try {

            /* Get a FITS data stream. */
            strm = FitsConstants.getInputStream( datsrc );

            /* Keep track of the position in the stream. */
            long[] pos = new long[] { 0L };

            /* If an HDU was specified explicitly, try to pick up that one
             * as a table. */
            if ( datsrc.getPosition() != null ) {
                try {
                    table = attemptReadTable( strm, wantRandom, datsrc, pos );
                }
                catch ( EOFException e ) {
                    throw new IOException( "Fell off end of file looking for "
                                         + datsrc );
                }
                if ( table != null ) {
                    return table;
                }
                else {
                    throw new IOException( datsrc + " not a Table HDU" );
                }
            }

            /* Otherwise starting from where we are, find the first
             * table HDU. */
            else {
                try {
                    while ( true ) {
                        table = attemptReadTable( strm, wantRandom,
                                                  datsrc, pos );
                        if ( table != null ) {
                            return table;
                        }
                    }
                }
                catch ( EOFException e ) {
                    throw new IOException( "No table HDUs in " + datsrc );
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException().initCause( e );
        }
        finally {
            if ( strm != null && table == null ) {
                strm.close();
            }
        }
    }

    /**
     * Returns <tt>true</tt> for a flavor with the MIME type "application/fits".
     */
    public boolean canImport( DataFlavor flavor ) {
        if ( flavor.getPrimaryType().equals( "application" ) &&
             flavor.getSubType().equals( "fits" ) ) {
            return true;
        }
        return false;
    }

    /**
     * Reads a FITS table from an input stream and writes it to a sink.
     *
     * @param  istrm  input stream containing the FITS data
     * @param  sink  destination for table data
     * @param  extnum  may contain a string representation of the HDU
     *         number in which the required table is found (otherwise the
     *         first table HDU will be used)
     */
    public void streamStarTable( InputStream istrm, TableSink sink,
                                 String extnum ) throws IOException {
        ArrayDataInput in = new BufferedDataInputStream( istrm );
        
        try {
            if ( extnum != null && extnum.matches( "[1-9][0-9]*" ) ) {
                int ihdu = Integer.parseInt( extnum );
                FitsConstants.skipHDUs( in, ihdu );
                if ( ! attemptStreamStarTable( in, sink, false ) ) {
                    throw new IOException( "No table HDU at extension " 
                                         + ihdu );
                }
            }
            else {
                boolean done = false;
                while ( ! done ) {
                    done = attemptStreamStarTable( in, sink, true );
                }
                if ( ! done ) {
                    throw new IOException( "No table extensions found" );
                }
            }
        }
        catch ( FitsException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Attempts to convert the HDU starting at the current position in
     * an input stream into a table, writing it to a given sink.
     * If the HDU does not contain a table, no table is written.
     *
     * @param  in  input stream positioned at the start of a FITS HDU
     * @param  sink  destination for the table data if one is found
     * @param  readAnyway  whether to skip forward to the end of the HDU
     *         even if it does not contain a table
     * @return  <tt>true</tt> if the table was successfully copied
     */
    private boolean attemptStreamStarTable( ArrayDataInput in, TableSink sink,
                                            boolean readAnyway )
            throws IOException, FitsException {
        Header hdr = new Header();
        FitsConstants.readHeader( hdr, in );
        String xtension = hdr.getStringValue( "XTENSION" );
        if ( "BINTABLE".equals( xtension ) ) {
            BintableStarTable.streamStarTable( hdr, in, sink );
            return true;
        }
        else if ( "TABLE".equals( xtension ) ) {
            AsciiTable tdata = new AsciiTable( hdr );
            tdata.read( in );
            tdata.getData();
            TableHDU thdu = new AsciiTableHDU( hdr, (Data) tdata );
            Tables.streamStarTable( new FitsStarTable( thdu ), sink );
            return true;
        }
        else {
            if ( readAnyway ) {
                long datasize = FitsConstants.getDataSize( hdr );
                IOUtils.skipBytes( in, datasize );
            }
            return false;
        }
    }

    /**
     * Reads the next header, and if it represents a table HDU, makes a
     * StarTable out of it and returns.  If it is some other kind of HDU, 
     * <tt>null</tt> is returned.  In either case, the stream is advanced
     * the end of that HDU.
     * 
     * @param  strm  stream to read from, positioned at the start of an HDU
     *         (before the header)
     * @param  wantRandom  whether a random-access table is preferred
     * @param  datsrc  a DataSource which can supply the data 
     *         in <tt>strm</tt>
     * @param  pos  a 1-element array holding the position in <tt>datsrc</tt>
     *         at which <tt>strm</tt> is positioned -
     *         it's an array so it can be updated by this routine (sorry)
     * @return   a StarTable made from the HDU at the start of <tt>strm</tt>
     *           or null
     */
    public static StarTable attemptReadTable( ArrayDataInput strm,
                                              boolean wantRandom, 
                                              DataSource datsrc, long[] pos )
            throws FitsException, IOException {

        /* Read the header. */
        Header hdr = new Header();
        int headsize = FitsConstants.readHeader( hdr, strm );
        pos[ 0 ] += headsize;
        String xtension = hdr.getStringValue( "XTENSION" );
          
        /* If it's a BINTABLE HDU, make a BintableStarTable out of it. */ 
        if ( "BINTABLE".equals( xtension ) ) {
            if ( strm instanceof RandomAccess ) {
                return BintableStarTable
                      .makeRandomStarTable( hdr, (RandomAccess) strm );
            }
            else if ( wantRandom ) {
                return BintableStarTable
                      .makeRandomStarTable( hdr, (DataInput) strm );
            }
            else {
                return BintableStarTable
                      .makeSequentialStarTable( hdr, datsrc, pos[ 0 ] );
            }

            // BinaryTable tdata = new BinaryTable( hdr );
            // tdata.read( strm );
            // TableHDU thdu = new BinaryTableHDU( hdr, (Data) tdata );
            // return new FitsStarTable( thdu );
        }

        /* If it's a TABLE HDU (ASCII table), make a FitsStarTable. */
        else if ( "TABLE".equals( xtension ) ) {
            AsciiTable tdata = new AsciiTable( hdr );
            tdata.read( strm );
            tdata.getData();
            TableHDU thdu = new AsciiTableHDU( hdr, (Data) tdata );
            return new FitsStarTable( thdu );
        }

        /* It's not a table HDU - just skip over it and return null. */
        else {
            long datasize = FitsConstants.getDataSize( hdr );
            IOUtils.skipBytes( strm, datasize );
            pos[ 0 ] += datasize;
            return null;
        }
    }
}
