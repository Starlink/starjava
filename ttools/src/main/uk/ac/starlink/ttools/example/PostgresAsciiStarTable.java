package uk.ac.starlink.ttools.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.formats.RowEvaluator;
import uk.ac.starlink.table.formats.StreamStarTable;
import uk.ac.starlink.util.DataSource;

/**
 * StarTable implementation which reads data from a pipe-separated-value
 * file.  This is modelled on the ASCII form of the 2MASS catalogues
 * as distributed on the 2MASS catalogue DVD set, which claims to be
 * easy to ingest into PostgreSQL 7.3.2
 * (see <a href="ftp://ftp.ipac.caltech.edu/pub/2mass/allsky/"
 *              >ftp://ftp.ipac.caltech.edu/pub/2mass/allsky/</a>
 *
 * @author   Mark Taylor
 * @since    12 Sep 2006
 */
public class PostgresAsciiStarTable extends StreamStarTable {

    private final StringBuffer cellBuf_ = new StringBuffer();
    private final URL schemaUrl_;
    private final int ncol_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    /** Decoder for Postgres double precision type. */
    private static RowEvaluator.Decoder<Double> DOUBLE_DECODER =
            new RowEvaluator.Decoder<Double>( Double.class, "double" ) {
        public Double decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : Double.valueOf( value );
        }
        public boolean isValid( String value ) {
            if ( "\\N".equals( value ) ) {
                return true;
            }
            else {
                try {
                    Double.parseDouble( value );
                    return true;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
    };

    /** Decoder for Postgres real type. */
    private static RowEvaluator.Decoder<Float> REAL_DECODER =
            new RowEvaluator.Decoder<Float>( Float.class, "real" ) {
        public Float decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : Float.valueOf( value );
        }
        public boolean isValid( String value ) {
            if ( "\\N".equals( value ) ) {
                return true;
            }
            else {
                try {
                    Float.parseFloat( value );
                    return true;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
    };

    /** Decoder for Postgres smallint type. */
    private static RowEvaluator.Decoder<Short> SMALLINT_DECODER =
            new RowEvaluator.Decoder<Short>( Short.class, "short" ) {
        public Short decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : Short.valueOf( value );
        }
        public boolean isValid( String value ) {
            if ( "\\N".equals( value ) ) {
                return true;
            }
            else {
                try {
                    Short.parseShort( value );
                    return true;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
    };

    /** Decoder for Postgres integer type. */
    private static RowEvaluator.Decoder<Integer> INTEGER_DECODER =
            new RowEvaluator.Decoder<Integer>( Integer.class, "int" ) {
        public Integer decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : Integer.valueOf( value );
        }
        public boolean isValid( String value ) {
            if ( "\\N".equals( value ) ) {
                return true;
            }
            else {
                try {
                    Integer.parseInt( value );
                    return true;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }
    };

    /** Decoder for Postgres date type. */
    private static RowEvaluator.Decoder<String> DATE_DECODER =
            new RowEvaluator.Decoder<String>( String.class, "date" ) {
        public String decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : value;
        }
        public boolean isValid( String value ) {
            return RowEvaluator.ISO8601_REGEX.matcher( value ).matches();
        }
        public ColumnInfo createColumnInfo( String name ) {
            ColumnInfo info = super.createColumnInfo( name );
            info.setElementSize( 10 );
            info.setUnitString( "iso-8601" );
            return info;
        }
    };

    /** Decoder for Postgres character type. */
    private static RowEvaluator.Decoder<String> CHARACTER_DECODER =
            new RowEvaluator.Decoder<String>( String.class, "character" ) {
        public String decode( String value ) {
            return "\\N".equals( value )
                 ? null
                 : value;
        }
        public boolean isValid( String value ) {
            return true;
        }
    };

    /**
     * Constructor.
     *
     * @param   datsrc  input data source
     * @param   schemaUrl  URL for 'schema' document which describes data
     *          types (like twomass_psc_schema)
     */
    @SuppressWarnings("this-escape")
    public PostgresAsciiStarTable( DataSource datsrc, URL schemaUrl )
            throws IOException {
        super();
        schemaUrl_ = schemaUrl;
        init( datsrc );
        ncol_ = getColumnCount();
    }

    protected List<String> readRow( PushbackInputStream in )
            throws TableFormatException, IOException {
        int icol = 0;
        cellBuf_.setLength( 0 );
        String[] row = new String[ ncol_ ];
        while ( true ) {
            char c = (char) in.read();
            switch ( c ) {
                case END:
                    return null;
                case '\n':
                    row[ icol++ ] = cellBuf_.toString();
                    cellBuf_.setLength( 0 );
                    if ( icol != ncol_ ) {
                        throw new TableFormatException( "Wrong num of cols" );
                    }
                    return Arrays.asList( row );
                case '|':
                    row[ icol++ ] = cellBuf_.toString();
                    cellBuf_.setLength( 0 );
                    break;
                default:
                    cellBuf_.append( c );
            }
        }
    }

    /**
     * Obtains metadata for the table by reading the schema provided at
     * construction time.
     */
    protected RowEvaluator.Metadata obtainMetadata() throws IOException {
        InputStream in = schemaUrl_.openStream();
        try {
            return readSchema( in );
        }
        finally {
            in.close();
        }
    }

    /**
     * Given a PostgreSQL/2MASS-type schema file, works out what the
     * column metadata will look like for each column of the table.
     *
     * @param  in  input stream for schema file
     * @return  metadata object
     */
    protected RowEvaluator.Metadata readSchema( InputStream in )
            throws IOException {
        Pattern regex =
            Pattern.compile( "^ +([a-z_]+) ([a-z ]+)(\\(([0-9]+)\\))?,? *$" );
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( in ) );
        List<RowEvaluator.Decoder<?>> decoderList = new ArrayList<>();
        List<ColumnInfo> infoList =
            new ArrayList<ColumnInfo>();
        for ( String line; ( line = rdr.readLine() ) != null; ) {
            Matcher matcher = regex.matcher( line );
            if ( matcher.matches() ) {
                String name = matcher.group( 1 );
                String type = matcher.group( 2 );
                String ssiz = matcher.group( 4 );
                final RowEvaluator.Decoder<?> decoder;
                if ( "double precision".equals( type ) ) {
                    decoder = DOUBLE_DECODER;
                }
                else if ( "real".equals( type ) ) {
                    decoder = REAL_DECODER;
                }
                else if ( "smallint".equals( type ) ) {
                    decoder = SMALLINT_DECODER;
                }
                else if ( "integer".equals( type ) ) {
                    decoder = INTEGER_DECODER;
                }
                else if ( "date".equals( type ) ) {
                    decoder = DATE_DECODER;
                }
                else if ( "character".equals( type ) ) {
                    decoder = CHARACTER_DECODER;
                }
                else {
                    throw new TableFormatException( "Unknown schema type "
                                                  + type );
                }
                decoderList.add( decoder );
                ColumnInfo info = decoder.createColumnInfo( name );
                if ( info.getContentClass().equals( String.class ) &&
                     info.getElementSize() <= 0 ) {
                    try {
                        info.setElementSize( Integer.parseInt( ssiz ) );
                    }
                    catch ( NumberFormatException e ) {
                        logger_.warning( "Can't parse element size " + ssiz
                                       + " for column " + name );
                    }
                }
                infoList.add( info );
            }
        }
        return new RowEvaluator
              .Metadata( infoList.toArray( new ColumnInfo[ 0 ]),
                         decoderList.toArray( new RowEvaluator.Decoder<?>[ 0 ]),
                         -1L );
    }
}
