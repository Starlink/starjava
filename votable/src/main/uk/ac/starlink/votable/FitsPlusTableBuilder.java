package uk.ac.starlink.votable;

import java.awt.datatransfer.DataFlavor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.RandomAccess;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.FitsConstants;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.QueueTableSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.IOUtils;

/**
 * Table builder which can read files in 'fits-plus' format (as written
 * by {@link FitsPlusTableWriter}).  This looks for a primary header
 * in the FITS file which contains the VOTMETA header (in fact it is
 * quite inflexible about what it recognises as this format - 
 * see {@link #isMagic}) and tries to interpret the data array as a 
 * 1-d array of bytes representing the XML of a VOTable document.
 * This VOTable document should have one or more TABLE elements with no DATA
 * content - the table data is got from the extension extension HDUs,
 * one per table, and they must be BINTABLE extensions matching the
 * metadata described by the VOTable.
 *
 * <p>The point of all this is so that you can store VOTables in
 * the efficient FITS format (it can be mapped if it's on local disk,
 * which makes table creation practically instantaneous, even for
 * random access) without sacrificing any of the metadata that you
 * want to encode in VOTable format.
 *
 * @author   Mark Taylor (Starlink)
 * @since    27 Aug 2004
 * @see      FitsPlusTableWriter
 */
public class FitsPlusTableBuilder implements TableBuilder, MultiTableBuilder {

    private static Logger logger = Logger.getLogger( "uk.ac.starlink.votable" );

    public String getFormatName() {
        return "FITS-plus";
    }

    public StarTable makeStarTable( DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {

        /* See if this looks like a fits-plus table. */
        if ( ! isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( 
                "Doesn't look like a FITS-plus file" );
        }

        /* Get an input stream. */
        ArrayDataInput strm = FitsConstants.getInputStreamStart( datsrc );
        try {

            /* Read the metadata from the primary HDU. */
            long[] pos = new long[ 1 ]; 
            TableElement[] tabels = readMetadata( strm, pos );

            /* Get the metadata for the table we are interested in. */
            int iTable = getTableIndex( datsrc.getPosition(), tabels.length );
            TableElement tabel = tabels[ iTable ];

            /* Skip HDUs if required.  They should all be BINTABLE HDUs
             * corresponding to tables earlier than the one we need. */
            pos[ 0 ] += FitsConstants.skipHDUs( strm, iTable );

            /* Now get the StarTable from the next HDU. */
            StarTable starTable =
                FitsTableBuilder.attemptReadTable( strm, wantRandom,
                                                   datsrc, pos );
            if ( starTable == null ) {
                throw new TableFormatException( "No BINTABLE HDU found" );
            }

            /* Return a StarTable with data from the BINTABLE but metadata
             * from the VOTable header. */
            return createFitsPlusTable( tabel, starTable );
        }
        catch ( FitsException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
        catch ( NullPointerException e ) {  // don't like this
            throw new TableFormatException( "Table not quite in " +
                                            "fits-plus format", e );
        }
    }

    public TableSequence makeStarTables( DataSource datsrc,
                                         StoragePolicy storagePolicy)
            throws IOException {

        /* If there is a position, use makeStarTable.  Otherwise, we want
         * all the tables. */
        String srcpos = datsrc.getPosition();
        if ( srcpos != null && srcpos.trim().length() > 0 ) {
            return Tables
                  .singleTableSequence( makeStarTable( datsrc, false,
                                                       storagePolicy ) );
        }

        /* See if this looks like a fits-plus table. */
        if ( ! isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException(
                "Doesn't look like a FITS-plus file" );
        }

        /* Return an iterator over the tables in the data source. */
        MultiLoadWorker loadWorker = new MultiLoadWorker( datsrc );
        loadWorker.start();
        return loadWorker.getTableSequence();
    }

    public void streamStarTable( InputStream in, final TableSink sink,
                                 String pos )
            throws IOException {
        ArrayDataInput strm = new BufferedDataInputStream( in );
        try {

            /* Read the metadata from the primary HDU. */
            TableElement[] tabels = readMetadata( strm, new long[ 1 ] );

            /* Get the metadata for the table we are interested in. */
            int iTable = getTableIndex( pos, tabels.length );
            TableElement tabel = tabels[ iTable ];

            /* Skip HDUs if required.  They should all be BINTABLE HDUs
             * corresponding to tables earlier than the one we need. */
            FitsConstants.skipHDUs( strm, iTable );

            /* Prepare a modified sink which behaves like the one we were
             * given but will pass on the VOTable metadata rather than that 
             * from the BINTABLE extension. */
            final StarTable voMeta = new VOStarTable( tabel );
            TableSink wsink = new TableSink() {
                public void acceptMetadata( StarTable fitsMeta )
                        throws TableFormatException {
                    sink.acceptMetadata( voMeta );
                }
                public void acceptRow( Object[] row ) throws IOException {
                    sink.acceptRow( row );
                }
                public void endRows() throws IOException {
                    sink.endRows();
                }
            };

            /* Write the table data from the upcoming BINTABLE element to the
             * sink. */
            Header hdr = new Header();
            FitsConstants.readHeader( hdr, strm );
            BintableStarTable.streamStarTable( hdr, strm, wsink );
        }
        catch ( FitsException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
        finally {
            strm.close();
        }
    }

    /**
     * Reads the primary HDU of a FITS stream, checking it is of the 
     * correct FITS-plus format, and returns the VOTable TABLE elements
     * which are encoded in it.  On successful exit, the stream will 
     * be positioned at the start of the first non-primary HDU 
     * (which should contain a BINTABLE).
     *
     * @param   strm  stream holding the data (positioned at the start)
     * @param   pos   1-element array for returning the number of bytes read
     *                into the stream
     * @return  array of TABLE elements in the primary HDU
     */
    private static TableElement[] readMetadata( ArrayDataInput strm,
                                                long[] pos )
            throws IOException {

        /* Read the first FITS block from the stream into a buffer. 
         * This should contain the entire header of the primary HDU. */
        byte[] headBuf = new byte[ 2880 ];
        strm.readFully( headBuf );

        /* Check it seems to have the right form. */
        if ( ! isMagic( headBuf ) ) {
            throw new TableFormatException( "Primary header not FITS-plus" );
        }
        try {

            /* Turn it into a header and find out the length of the 
             * data unit. */
            Header hdr = new Header();
            ArrayDataInput hstrm = 
                new BufferedDataInputStream(
                    new ByteArrayInputStream( headBuf ) );
            int headsize = FitsConstants.readHeader( hdr, hstrm );
            int datasize = (int) FitsConstants.getDataSize( hdr );
            pos[ 0 ] = headsize + datasize;
            assert headsize == 2880;
            assert hdr.getIntValue( "NAXIS" ) == 1;
            assert hdr.getIntValue( "BITPIX" ) == 8;
            int nbyte = hdr.getIntValue( "NAXIS1" );

            /* Read the data from the primary HDU into a byte buffer. */
            byte[] vobuf = new byte[ nbyte ];
            strm.readFully( vobuf );

            /* Advance to the end of the primary HDU. */
            int pad = datasize - nbyte;
            IOUtils.skipBytes( strm, pad );

            /* Read XML from the byte buffer, performing a custom
             * parse to DOM. */
            VOElementFactory vofact = new VOElementFactory();
            DOMSource domsrc =
                vofact.transformToDOM( 
                    new StreamSource( new ByteArrayInputStream( vobuf ) ),
                    false );

            /* Obtain the TABLE elements, which ought to be empty. */
            VODocument doc = (VODocument) domsrc.getNode();
            VOElement topel = (VOElement) doc.getDocumentElement();
            NodeList tlist = topel.getElementsByVOTagName( "TABLE" );
            int nTable = tlist.getLength();
            TableElement[] tabels = new TableElement[ nTable ];
            for ( int i = 0; i < nTable; i++ ) {
                tabels[ i ] = (TableElement) tlist.item( i );
                if ( tabels[ i ].getChildByName( "DATA" ) != null ) {
                    throw new TableFormatException(
                        "TABLE #" + ( i + i ) + " in embedded VOTable document "
                      + "has unexpected DATA element" );
                }
            }
            return tabels;
        }
        catch ( FitsException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
        catch ( SAXException e ) {
            throw new TableFormatException( e.getMessage(), e );
        }
    }

    /**
     * Returns the index of the table requested by a data source position
     * string.  The first table is represented by position string "1".
     * The returned value is the 0-based index of the table,
     * which corresponds to the 1-based HDU number (the primary HDU is
     * excluded).  If the supplied position string does not correspond
     * to a known table, a TableFormatException is thrown.
     *
     * @param   pos  position string (first is "1")
     * @param   nTable  number of tables in the whole file
     */
    private static int getTableIndex( String pos, int nTable )
            throws TableFormatException {
        if ( nTable <= 0 ) {
            throw new TableFormatException( "No tables present "
                                          + "in FITS-plus container" );
        }
        if ( pos == null || pos.trim().length() == 0 ) {
            return 0;
        }
        else {
            try {
                int index = Integer.parseInt( pos.trim() );
                if ( index >= 1 && index <= nTable ) {
                    return index - 1;
                }
                else if ( index == 0 ) {
                    throw new TableFormatException( "No table with position "
                                                  + pos + "; first table is "
                                                  + "#1" );
                }
                else {
                    throw new TableFormatException( "No table with position "
                                                  + pos + "; there are " + 
                                                  + nTable );
                }
            }
            catch ( NumberFormatException e ) {
                throw new TableFormatException( "Can't interpret position "
                                              + pos + " (not a number)", e );
            }
        }
    }

    /**
     * Combines the metadata from a VOTable TABLE element and the data
     * from a corresponding FITS BINTABLE HDU to construct a StarTable.
     * If the two don't seem to match sufficiently, an error may be thrown.
     *
     * @param  tabEl   metadata-bearing TABLE element
     * @param  dataTable  data-bearing StarTable
     * @return  combined table
     */
    private static StarTable createFitsPlusTable( TableElement tabEl,
                                                  StarTable dataTable )
            throws IOException {

        /* Check that the FIELD elements look consistent with the
         * FITS data.  If not, the FITS file has probably been 
         * messed about with somehow, and attempting to interpret this
         * file as FITS-plus is probably a bad idea. */
        /* The implementation of this test is a bit desperate. 
         * It is doing some of the same work as the consistency 
         * adjustment below, but trying to be more stringent.  
         * It tries to avoid the issue noted below concerning strings 
         * and characters - but I don't remember the details of what 
         * that was :-(.  This part was written later. */
        FieldElement[] fields = tabEl.getFields();
        if ( fields.length != dataTable.getColumnCount() ) {
            throw new TableFormatException( "FITS/VOTable metadata mismatch"
                                          + " - column counts differ" );
        }
        for ( int ic = 0; ic < fields.length; ic++ ) {
            Class fclazz = dataTable.getColumnInfo( ic ).getContentClass();
            Class vclazz = fields[ ic ].getDecoder().getContentClass();
            if ( fclazz.equals( vclazz )
                 || ( ( fclazz.equals( String.class ) ||
                        fclazz.equals( Character.class ) ||
                        fclazz.equals( char[].class ) )
                   && ( vclazz.equals( String.class ) ||
                        vclazz.equals( Character.class ) ||
                        vclazz.equals( char[].class ) ) ) ) {
                // ok
            }
            else {
                throw new TableFormatException( "FITS/VOTable metadata "
                                              + "mismatch"
                                              + " - column types differ" );
            }
        }

        /* Turn it into a TabularData element associated it with its
         * TABLE DOM element as if the DOM builder had found the table
         * data in a DATA element within the TABLE element. */
        tabEl.setData( new TableBodies.StarTableTabularData( dataTable ) );

        /* Now create and return a StarTable based on the TABLE element; 
         * its metadata comes from the VOTable, but its data comes from 
         * the FITS table we've just read. */
        VOStarTable outTable = new VOStarTable( tabEl );

        /* Ensure column type consistency.  There can occasionally by 
         * some nasty issues with Character/String types. */
        int ncol = dataTable.getColumnCount();
        assert ncol == outTable.getColumnCount();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo fInfo = dataTable.getColumnInfo( icol );
            ColumnInfo vInfo = outTable.getColumnInfo( icol );
            if ( ! vInfo.getContentClass()
                        .isAssignableFrom( fInfo.getContentClass() ) ) {
                vInfo.setContentClass( fInfo.getContentClass() );
            }
        }
        return outTable;
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
     * Tests whether a given buffer contains bytes which might be the
     * first few bytes of a FitsPlus table.
     * The criterion is that it looks like the start of a FITS header, 
     * and the first few cards look roughly like this:
     * <pre>
     *     SIMPLE  =              T
     *     BITPIX  =              8
     *     NAXIS   =              1
     *     NAXIS1  =            ???
     *     VOTMETA =              T
     * </pre>
     *
     * @param  buffer  byte buffer containing leading few bytes of data
     * @return  true  if it looks like a FitsPlus file
     */
    public static boolean isMagic( byte[] buffer ) {
        final int ntest = 5;
        int pos = 0;
        int ncard = 0;
        boolean ok = true;
        for ( int il = 0; ok && il < ntest; il++ ) {
            if ( buffer.length > pos + 80 ) {
                char[] cbuf = new char[ 80 ];
                for ( int ic = 0; ic < 80; ic++ ) {
                    cbuf[ ic ] = (char) ( buffer[ pos++ ] & 0xff );
                }
                try {
                    HeaderCard card = new HeaderCard( new String( cbuf ) );
                    ok = ok && cardOK( il, card );
                }
                catch ( FitsException e ) {
                    ok = false;
                }
            }
        }
        return ok;
    }

    /**
     * Checks whether the i'th card looks like it should do for the file
     * to be readable by this handler.
     *
     * @param  icard  card index
     * @param  card   header card
     * @return  true  if <tt>card</tt> looks like the <tt>icard</tt>'th
     *          header card of a FitsPlus primary header should do
     */
    private static boolean cardOK( int icard, HeaderCard card )
            throws FitsException {
        String key = card.getKey();
        String value = card.getValue();
        switch ( icard ) {
            case 0:
                return "SIMPLE".equals( key ) && "T".equals( value );
            case 1:
                return "BITPIX".equals( key ) && "8".equals( value );
            case 2:
                return "NAXIS".equals( key ) && "1".equals( value );
            case 3:
                return "NAXIS1".equals( key );
            case 4:
                return "VOTMETA".equals( key ) && "T".equals( value );
            default:
                return true;
        }
    }

    /**
     * Thread which loads the table data from a FITS-plus file.
     */
    private static class MultiLoadWorker extends Thread {
        private final DataSource datsrc_;
        private final QueueTableSequence tqueue_;

        /**
         * Constructor.
         *
         * @param   datsrc  data source
         */
        MultiLoadWorker( DataSource datsrc ) {
            super( "FITS-plus multi table loader" );
            setDaemon( true );
            datsrc_ = datsrc;
            tqueue_ = new QueueTableSequence();
        }

        /**
         * Returns the table sequence populated by this thread.
         * The thread must be started in order for the returned sequence to
         * become populated and eventually terminated.
         *
         * @return   output table sequence
         */
        TableSequence getTableSequence() {
            return tqueue_;
        }

        public void run() {
            try {
                multiLoad();
            }
            catch ( Throwable e ) {
                tqueue_.addError( e );
            }
            finally {
                tqueue_.endSequence();
            }
        }

        /**
         * Do the work for loading tables.
         */
        private void multiLoad() throws IOException, FitsException {

            /* Get an input stream. */
            ArrayDataInput in = FitsConstants.getInputStreamStart( datsrc_ );

            /* Read the metadata from the primary HDU. */
            long[] posptr = new long[ 1 ]; 
            TableElement[] tabEls = readMetadata( in, posptr );
            long pos = posptr[ 0 ];
            int nTable = tabEls.length;

            /* Read each table HDU in turn. */
            for ( int itab = 0; itab < nTable; itab++ ) {

                /* Make sure we have a usable stream positioned at the start
                 * of the right HDU. */
                if ( in == null ) {
                    in = FitsConstants.getInputStreamStart( datsrc_ );
                    if ( pos > 0 ) {
                        if ( in instanceof RandomAccess ) {
                            ((RandomAccess) in).seek( pos );
                        }
                        else {
                            IOUtils.skipBytes( in, pos );
                        }
                    }
                }

                /* Read the HDU header. */
                Header hdr = new Header();
                int headsize = FitsConstants.readHeader( hdr, in );
                long datasize = FitsConstants.getDataSize( hdr );
                long datpos = pos + headsize;
                if ( ! "BINTABLE".equals( hdr.getStringValue( "XTENSION" ) ) ) {
                    throw new TableFormatException( "Non-BINTABLE at ext #"
                                                  + itab + " - not FITS-plus" );
                }

                /* Read the BINTABLE. */
                final StarTable dataTable;
                if ( in instanceof RandomAccess ) {
                    dataTable = BintableStarTable
                               .makeRandomStarTable( hdr, (RandomAccess) in );
                }
                else {
                    dataTable = BintableStarTable
                               .makeSequentialStarTable( hdr, datsrc_, datpos );
                }
                in = null;

                /* Combine the data from the BINTABLE with the header from
                 * the VOTable to create an output table. */
                tqueue_.addTable( createFitsPlusTable( tabEls[ itab ],
                                                       dataTable ) );
                pos += headsize + datasize;
            }
        }
    }
}
