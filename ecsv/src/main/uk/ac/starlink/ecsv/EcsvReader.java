package uk.ac.starlink.ecsv;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Parser for the metadata and data of an ECSV file.
 * The format currently supported is ECSV 1.0, as documented at
 * <a href="https://github.com/astropy/astropy-APEs/blob/master/APE6.rst"
 *    >Astropy APE6</a>.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public class EcsvReader implements Closeable {

    private final LineReader lineRdr_;
    private final EcsvMeta meta_;
    private final char delim_;
    private final int ncol_;
    private final EcsvDecoder<?>[] decoders_;
    private final List<String> words_;
    private final StringBuilder wbuf_;
    private List<String> cellWords_;
    private long irow_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ecsv" );

    /**
     * Constructor.
     *
     * @param  in  input stream; doesn't need to be buffered
     * @param  yamlParser   knows how to extrace ECSV metadata from YAML
     * @param  colCheck  what to do on CSV/YAML column name mismatches
     */
    public EcsvReader( InputStream in, YamlParser yamlParser,
                       MessagePolicy colCheck )
            throws IOException, EcsvFormatException {

        /* Initialise workspace. */
        words_ = new ArrayList<String>();
        wbuf_ = new StringBuilder();

        /* Prepare line reader.  ECSV 1.0 is documented as using ASCII
         * rather than, for instance, UTF-8. */
        lineRdr_ = LineReader.createAsciiLineReader( in );

        /* Parse YAML header to acquire metadata. */
        EcsvHeader header = EcsvHeader.readHeader( lineRdr_ );
        meta_ = yamlParser.parseMeta( header.getYamlLines() );
        delim_ = meta_.getDelimiter();
        EcsvColumn<?>[] columns = meta_.getColumns();
        ncol_ = columns.length;
        decoders_ = new EcsvDecoder<?>[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            decoders_[ ic ] = columns[ ic ].getDecoder();
        }

        /* Check some requirements. */
        if ( delim_ != ' ' && delim_ != ',' ) {
            logger_.warning( "Illegal delimiter character '"
                           + Character.toString( delim_ ) + "'" 
                           + " - may cause trouble" );
        }
        LineReader namesLineReader =
            LineReader
           .createArrayLineReader( new String[] { header.getNamesLine() } );
        List<String> nameWords = readNextRowWords( namesLineReader );
        int nName = nameWords == null ? -1 : nameWords.size();
        if ( nName != ncol_ ) {
            throw new EcsvFormatException( "Names line/YAML column count "
                                         + "mismatch: "
                                         + nName + " != " + ncol_ );
        }
        List<String> colMismatches = new ArrayList<>();
        for ( int ic = 0; ic < ncol_; ic++ ) {
            String csvName = nameWords.get( ic );
            String yamlName = columns[ ic ].getName();
            if ( ! yamlName.equalsIgnoreCase( csvName ) ) {
                colMismatches.add( csvName + "!=" + yamlName );
            }
        }
        int nMis = colMismatches.size();
        if ( nMis > 0 ) {
            int nMax = 5;
            StringBuffer sbuf = new StringBuffer()
                .append( "YAML/CSV column name " )
                .append( nMis > 1 ? "mismatches: " : "mismatch: " )
                .append( colMismatches.stream()
                                      .limit( nMax )
                                      .collect( Collectors.joining( ", " ) ) );
            if ( nMis > nMax ) {
                sbuf.append( ", ..." );
            }
            colCheck.deliverMessage( sbuf.toString() );
        }
    }

    /**
     * Returns the ECSV metadata object used by this reader.
     *
     * @return  parsed metadata
     */
    public EcsvMeta getMeta() {
        return meta_;
    }

    /**
     * Attempts to advance the current row to the next one.
     * This method <b>must</b> be called before current row data
     * can be accessed using the
     * {@link #getCell(int)} or {@link #getRow()} methods.
     *
     * @return  true if this sequence has been advanced to the next row,
     *          false if there are no more rows
     */
    public boolean next() throws IOException, EcsvFormatException {
        cellWords_ = readNextRowWords( lineRdr_ );
        if ( cellWords_ != null ) {
            int nword = cellWords_.size();
            if ( nword == ncol_ ) {
                irow_++;
                return true;
            }
            else {
                throw new EcsvFormatException( "Wrong number of fields at line "
                                             + irow_ + " (" + nword
                                             + " != " + ncol_ + ")" );
            }
        }
        else {
            return false;
        }
    }

    /**
     * Returns the contents of a cell in the current row.
     *
     * @return  the contents of cell <tt>icol</tt> in the current row
     */
    public Object getCell( int icol ) {
        if ( cellWords_ == null ) {
            throw new IllegalStateException( "No current row" );
        }
        String word = cellWords_.get( icol );
        if ( word == null || word.length() == 0 ) {
            return null;
        }
        else {
            try {
                return decoders_[ icol ].decode( word );
            }
            catch ( NumberFormatException e ) {
                return null;
            }
        }
    }

    /**
     * Returns the contents of the current row, as an array
     * with one element per column.
     *
     * @return  an array of the objects in each cell
     */
    public Object[] getRow() {
        Object[] values = new Object[ ncol_ ];
        for ( int ic = 0; ic < ncol_; ic++ ) {
            values[ ic ] = getCell( ic );
        }
        return values;
    }

    public void close() throws IOException {
        lineRdr_.close();
    }

    /**
     * Reads the next row from the given LineReader using the
     * syntax appropriate to this reader.
     * We have to be prepared to span lines in the case that a quoted
     * word contains a newline character.
     * 
     * @param   lineReader  supplies raw input lines
     * @return  list of words read from reader,
     *          or null if there is no more input
     */
    private List<String> readNextRowWords( LineReader lineReader )
            throws IOException {
        words_.clear();
        wbuf_.setLength( 0 );
        boolean inQuote = false;
        for ( String line; ( ( line = lineReader.readLine() ) != null ); ) {
            int nc = line.length();
            for ( int i = 0; i < nc; i++ ) {
                char c = line.charAt( i );
                switch ( c ) {
                    case ' ':
                    case ',':
                        if ( c == delim_ && ! inQuote ) {
                            words_.add( wbuf_.toString() );
                            wbuf_.setLength( 0 );
                        }
                        else {
                            wbuf_.append( c );
                        }
                        break;
                    case '"':
                        if ( ! inQuote ) {
                            inQuote = true;
                        }
                        else if ( line.length() > i + 1 &&
                                  line.charAt( i + 1 ) == '"' ) {
                            i++;
                            wbuf_.append( '"' );
                        }
                        else {  
                            inQuote = false;
                        }
                        break;
                    default:
                        wbuf_.append( c );
                }
            }
            if ( inQuote ) {
                wbuf_.append( '\n' );
            }
            else {
                if ( words_.size() == 0 && wbuf_.length() == 0 ) {
                    return null;
                }
                else {
                    words_.add( wbuf_.toString() );
                    wbuf_.setLength( 0 );
                    return words_;
                }
            }
        }
        return null;
    }
}
