package uk.ac.starlink.ttools.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.RowEvaluator;
import uk.ac.starlink.table.formats.StreamStarTable;
import uk.ac.starlink.util.DataSource;

/**
 * StarTable implementation which can read data from the AllWise data
 * release.  At time of writing these files are available from
 * <a href="http://irsadist.ipac.caltech.edu/wise-allwise/"
 *         >http://irsadist.ipac.caltech.edu/wise-allwise/</a>.
 * The files are formatted as pipe-separated ASCII, and optionally compressed.
 * The metadata is stored in an external schema table file.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2014
 */
public class AllWiseAsciiStarTable extends StreamStarTable {

    private final StringBuffer cellBuf_ = new StringBuffer();
    private final DataSource datsrc_;
    private final URL schemaUrl_;
    private final long nrow_;
    private final int ncol_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.examples" );

    private static final RowEvaluator.Decoder<Double> DOUBLE_DECODER =
            new WiseDecoder<Double>( Double.class ) {
        Double decodeNonEmpty( String value ) {
            return Double.valueOf( value );
        }
        void attemptDecode( String value ) {
            Double.parseDouble( value );
        }
    };
    private static final RowEvaluator.Decoder<Float> FLOAT_DECODER =
            new WiseDecoder<Float>( Float.class ) {
        Float decodeNonEmpty( String value ) {
            return Float.valueOf( value );
        }
        void attemptDecode( String value ) {
            Float.parseFloat( value );
        }
    };
    private static final RowEvaluator.Decoder<Short> SHORT_DECODER =
            new WiseDecoder<Short>( Short.class ) {
        Short decodeNonEmpty( String value ) {
            return Short.valueOf( value );
        }
        void attemptDecode( String value ) {
            Short.parseShort( value );
        }
    };
    private static final RowEvaluator.Decoder<Integer> INT_DECODER =
            new WiseDecoder<Integer>( Integer.class ) {
        Integer decodeNonEmpty( String value ) {
            return Integer.valueOf( value );
        }
        void attemptDecode( String value ) {
            Integer.parseInt( value );
        }
    };
    private static final RowEvaluator.Decoder<Long> LONG_DECODER =
            new WiseDecoder<Long>( Long.class ) {
        Long decodeNonEmpty( String value ) {
            return Long.valueOf( value );
        }
        void attemptDecode( String value ) {
            Long.parseLong( value );
        }
    };
    private static final RowEvaluator.Decoder<String> CHAR_DECODER =
            new RowEvaluator.Decoder<String>( String.class, "string" ) {
        public String decode( String value ) {
            return value.length() == 0 ? null : value;
        }
        public boolean isValid( String value ) {
            return true;
        }
    };

    /**
     * Constructor.  You have to supply the number of rows that getRowCount
     * will report.
     *
     * @param   datsrc  input data source
     * @param   schemaUrl  URL for 'schema' table which describes data types
     * @param   nrow   reported number of rows in table, -1 if unknown
     */
    @SuppressWarnings("this-escape")
    public AllWiseAsciiStarTable( DataSource datsrc, URL schemaUrl,
                                  long nrow )
            throws IOException {
        super( StandardCharsets.US_ASCII );
        datsrc_ = datsrc;
        schemaUrl_ = schemaUrl;
        nrow_ = nrow;
        init( datsrc );
        ncol_ = getColumnCount();
    }

    protected List<String> readRow( BufferedReader in )
            throws TableFormatException, IOException {
        String line = in.readLine();
        if ( line == null ) {
            return null;
        }
        else {
            String[] fields = line.split( "\\|", 0 );
            if ( fields.length == ncol_ ) {
                return Arrays.asList( fields );
            }
            else {
                throw new TableFormatException( "Wrong num of cols" );
            }
        }
    }

    /**
     * Obtains metadata for the table by reading the schema provided at
     * construction time.
     */
    protected RowEvaluator.Metadata obtainMetadata() throws IOException {
        return readSchema( DataSource.makeDataSource( schemaUrl_ ) );
    }

    /**
     * Given a PostgreSQL/2MASS-type schema file, works out what the
     * column metadata will look like for each column of the table.
     *
     * @param  datsrc  data source for schema file
     * @return  metadata object
     */
    protected RowEvaluator.Metadata readSchema( DataSource datsrc )
            throws IOException {

        /* Read the schema table to get field decoders and column metadata
         * for each AllWise column. */
        StarTable metaTable =
            new AsciiTableBuilder()
           .makeStarTable( datsrc, true, StoragePolicy.PREFER_MEMORY );
        RowSequence rseq = metaTable.getRowSequence();
        Pattern typeRegex =
            Pattern.compile( "([a-z0-9]+)(\\(([0-9,]+)\\))?" );
        List<RowEvaluator.Decoder<?>> decoderList = new ArrayList<>();
        List<ColumnInfo> infoList = new ArrayList<>();
        while( rseq.next() ) {
            Object[] row = rseq.getRow();
            String name = row[ 0 ].toString();
            String fulltype = row[ 1 ].toString();
            String unit = row[ 2 ].toString();
            String hasnull = row[ 3 ].toString();
            String descrip = row[ 4 ].toString();
            Matcher typeMatcher = typeRegex.matcher( fulltype );
            String type;
            String ssiz;
            if ( typeMatcher.matches() ) {
                type = typeMatcher.group( 1 );
                ssiz = typeMatcher.group( 3 );
            }
            else {
                throw new IOException( "bad type: " + fulltype );
            }
            final RowEvaluator.Decoder<?> decoder;
            Number badnum = null;;
            if ( "decimal".equals( type ) ) {
                decoder = DOUBLE_DECODER;
            }
            else if ( "smallfloat".equals( type ) ) {
                decoder = FLOAT_DECODER;
            }
            else if ( "smallint".equals( type ) ) {
                decoder = SHORT_DECODER;
                badnum = Short.valueOf( Short.MIN_VALUE );
            }
            else if ( "integer".equals( type ) ) {
                decoder = INT_DECODER;
                badnum = Integer.valueOf( Integer.MIN_VALUE );
            }
            else if ( "int8".equals( type )
                   || "serial8".equals( type ) ) {
                decoder = LONG_DECODER;
                badnum = Long.valueOf( Long.MIN_VALUE );
            }
            else if ( "char".equals( type ) ) {
                decoder = CHAR_DECODER;
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
            if ( unit.length() > 0 && ! unit.matches( "^-+$" ) ) {
                info.setUnitString( unit );
            }
            if ( descrip.length() > 0 ) {
                info.setDescription( descrip );
            }
            if ( "yes".equals( hasnull ) ) {
                info.setNullable( true );
                info.setAuxDatum( new DescribedValue( Tables
                                                     .NULL_VALUE_INFO,
                                                      badnum ) );
            }
            else {
                info.setNullable( false );
            }
            infoList.add( info );
        }
        rseq.close();
        logger_.info( "Got " + infoList.size() + " allwise columns" );

        /* Return the result as a metadata object. */
        return new RowEvaluator
              .Metadata( infoList.toArray( new ColumnInfo[ 0 ]),
                         decoderList.toArray( new RowEvaluator.Decoder<?>[ 0 ]),
                         nrow_ );
    }

    /**
     * Abstract superclass for decoding text strings.
     */
    private static abstract class WiseDecoder<T>
            extends RowEvaluator.Decoder<T> {

        /**
         * Constructor.
         *
         * @param  clazz  output class
         */
        WiseDecoder( Class<T> clazz ) {
            super( clazz, clazz.getSimpleName() );
        }

        public T decode( String value ) {
            return value.length() == 0 ? null 
                                       : decodeNonEmpty( value );
        }

        public boolean isValid( String value ) {
            if ( value.length() == 0 ) {
                return true;
            }
            else {
                try {
                    attemptDecode( value );
                    return true;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }
            }
        }

        /**
         * Decodes a string value known to be non-empty.
         *
         * @param  value  non-empty string
         * @return  typed value
         */
        abstract T decodeNonEmpty( String value );

        /**
         * Attempts to decode a value, throwing an exception on failure.
         *
         * @param  value  string
         * @throws  NumberFormatException  if it can't be done
         */
        abstract void attemptDecode( String value )
            throws NumberFormatException;
    }
}
