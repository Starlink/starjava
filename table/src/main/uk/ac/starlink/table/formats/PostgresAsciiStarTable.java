package uk.ac.starlink.table.formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableFormatException;
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

    /**
     * Constructor.
     *
     * @param   datsrc  input data source
     * @param   schemaUrl  URL for 'schema' document which describes data
     *          types (like twomass_psc_schema)
     */
    public PostgresAsciiStarTable( DataSource datsrc, URL schemaUrl )
            throws IOException {
        super();
        schemaUrl_ = schemaUrl;
        init( datsrc );
        ncol_ = getColumnCount();
    }

    protected List readRow( PushbackInputStream in )
            throws TableFormatException, IOException {
        int icol = 0;
        cellBuf_.setLength( 0 );
        Object[] row = new Object[ ncol_ ];
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
        ColumnInfo[] colInfos;
        try {
            colInfos = readSchema( in );
        }
        finally {
            in.close();
        }
        int ncol = colInfos.length;
        RowEvaluator.Decoder[] decoders = new RowEvaluator.Decoder[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            Class clazz = colInfos[ i ].getContentClass();
            RowEvaluator.Decoder decoder;
            if ( clazz == Double.class ) {
                decoder = new RowEvaluator.Decoder() {
                    public Object decode( String value ) {
                        return "\\N".equals( value )
                             ? null
                             : Double.valueOf( value );
                    }
                };
            }
            else if ( clazz == Float.class ) {
                decoder = new RowEvaluator.Decoder() {
                    public Object decode( String value ) {
                        return "\\N".equals( value )
                             ? null
                             : Float.valueOf( value );
                    }
                };
            }
            else if ( clazz == Integer.class ) {
                decoder = new RowEvaluator.Decoder() {
                    public Object decode( String value ) {
                        return "\\N".equals( value )
                             ? null
                             : Integer.valueOf( value );
                   } 
                };
            }
            else if ( clazz == Short.class ) {
                decoder = new RowEvaluator.Decoder() {
                    public Object decode( String value ) {
                        return "\\N".equals( value )
                             ? null
                             : Short.valueOf( value );
                    }
                };
            }
            else if ( clazz == String.class ) {
                decoder = new RowEvaluator.Decoder() {
                    public Object decode( String value ) {
                        return "\\N".equals( value )
                             ? null
                             : value;
                    }
                };
            }
            else {
                throw new AssertionError( "Unknown class" );
            }
            decoders[ i ] = decoder;
        }
        return new RowEvaluator.Metadata( colInfos, decoders, -1L );
    }

    /**
     * Given a PostgreSQL/2MASS-type schema file, works out what the
     * column metadata will look like for each column of the table.
     *
     * @param  in  input stream for schema file
     * @return  column metadata array
     */
    protected ColumnInfo[] readSchema( InputStream in ) throws IOException {
        Pattern regex =
            Pattern.compile( "^ +([a-z_]+) ([a-z ]+)(\\(([0-9]+)\\))?,? *$" );
        List infoList = new ArrayList();
        BufferedReader rdr =
            new BufferedReader( new InputStreamReader( in ) );
        for ( String line; ( line = rdr.readLine() ) != null; ) {
            Matcher matcher = regex.matcher( line );
            if ( matcher.matches() ) {
                String name = matcher.group( 1 );
                String type = matcher.group( 2 );
                String ssiz = matcher.group( 4 );
                ColumnInfo info;
                if ( "double precision".equals( type ) ) {
                    info = new ColumnInfo( name, Double.class, null );
                }
                else if ( "real".equals( type ) ) {
                    info = new ColumnInfo( name, Float.class, null );
                }
                else if ( "smallint".equals( type ) ) {
                    info = new ColumnInfo( name, Short.class, null );
                }
                else if ( "integer".equals( type ) ) {
                    info = new ColumnInfo( name, Integer.class, null );
                }
                else if ( "date".equals( type ) ) {
                    info = new ColumnInfo( name, String.class, null );
                    info.setElementSize( 10 );
                    info.setUnitString( "iso-8601" );
                }
                else if ( "character".equals( type ) ) {
                    info = new ColumnInfo( name, String.class, null );
                    info.setElementSize( Integer.parseInt( ssiz ) );
                }
                else {
                    throw new TableFormatException( "Unknown schema type "
                                                  + type );
                }
                infoList.add( info );
            }
        }
        return (ColumnInfo[]) infoList.toArray( new ColumnInfo[ 0 ] );
    }
}
