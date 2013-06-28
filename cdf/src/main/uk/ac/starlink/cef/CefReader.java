package uk.ac.starlink.cef;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;

class CefReader implements RowSequence {

    private String cefName_;
    private String cefVersion_;
    private final DataLineReader dataReader_;
    private final DescribedValue[] params_;
    private final Variable[] vars_;
    private final int[] startFields_;
    private boolean done_;
    private String[] fields_;
    private static final Logger logger_ =
        Logger.getLogger( CefReader.class.getName() );

    public CefReader( InputStream in ) throws IOException {
        List<DescribedValue> gList = new ArrayList<DescribedValue>();
        List<Variable> vList = new ArrayList<Variable>();
        boolean startData = false;
        String eod = null;
        char eor = '\n';
        while ( ! startData ) {
            Pair pair = readPair( in );
            String pName = pair.name_;
            String pValue = pair.value_;
            if ( "FILE_NAME".equals( pName ) ) {
                cefName_ = getText( pValue );
            }
            else if ( "FILE_FORMAT_VERSION".equals( pName ) ) {
                cefVersion_ = getText( pValue );
            }
            else if ( "END_OF_RECORD_MARKER".equals( pName ) ) {
                eor = toAsciiChar( getText( pValue ).charAt( 0 ) );
            }
            else if ( "START_META".equals( pName ) ) {
                gList.add( readGlobal( in, pValue ) );
            }
            else if ( "START_VARIABLE".equals( pName ) ) {
                Map<String,String> varMeta = readVariableMeta( in, pValue );
                String data = varMeta.remove( "DATA" );
                if ( data == null ) {
                    vList.add( createVariable( pValue, varMeta ) );
                }
                else {
                    gList.add( createParameter( pValue, splitValues( data ),
                                                varMeta ) );
                }
            }
            else if ( "DATA_UNTIL".equals( pName ) ) {
                eod = getText( pValue );
                startData = true;
            }
        }
        if ( cefVersion_ == null ) {
            throw new CefFormatException( "No CEF version string" );
        }
        else if ( ! cefVersion_.toUpperCase().startsWith( "CEF" ) ) {
            throw new CefFormatException( "Unknown CEF version string \""
                                        + cefVersion_ + "\"" );
        }
        else if ( ! cefVersion_.toUpperCase().equals( "CEF-2.0" ) ) {
            logger_.warning( "Non-standard CEF version string \""
                           + cefVersion_ + "\"" );
        }
        params_ = gList.toArray( new DescribedValue[ 0 ] );
        vars_ = vList.toArray( new Variable[ 0 ] );
        int nvar = vars_.length;
        startFields_ = new int[ nvar ];
        int istart = 0;
        for ( int iv = 0; iv < nvar; iv++ ) {
            startFields_[ iv ] = istart;
            istart += vars_[ iv ].getItemCount();
        }
        dataReader_ = createDataLineReader( in, eod, eor );
    }

    public String getName() {
        return cefName_;
    }

    public String getVersionString() {
        return cefVersion_;
    }

    public DescribedValue[] getParameters() {
        return params_;
    }

    public ColumnInfo[] getColumnInfos() {
        int nvar = vars_.length;
        ColumnInfo[] infos = new ColumnInfo[ nvar ];
        for ( int iv = 0; iv < nvar; iv++ ) {
            infos[ iv ] = vars_[ iv ].getColumnInfo();
        }
        return infos;
    }

    public boolean next() throws IOException {
        if ( ! done_ ) {
            String dataLine = dataReader_.readDataLine();
            if ( dataLine != null ) {
                fields_ = splitValues( dataLine );
            }
            else {
                fields_ = null;
                done_ = true;
            }
        }
        return ! done_;
    }

    public Object getCell( int icol ) {
        return vars_[ icol ].readValue( fields_, startFields_[ icol ] );
    }

    public Object[] getRow() {
        int nvar = vars_.length;
        Object[] row = new Object[ vars_.length ];
        for ( int ic = 0; ic < nvar; ic++ ) {
            row[ ic ] = getCell( ic );
        }
        return row;
    }

    public void close() throws IOException {
        dataReader_.close();
    }

    /**
     * Reads a line from the input that terminates with an 0x0a.
     * Comments and whitespace are stripped.
     * Returns null for end of file.
     */
    private static String readRawLine( InputStream in ) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        boolean inQuote = false;
        boolean inComment = false;
        while ( true ) {
            int b = (int) in.read();
            if ( b < 0 ) {
                return null;
            }
            char c = toAsciiChar( b );
            if ( c == '\n' ) {
                return sbuf.toString().trim();
            }
            if ( c == '"' && ! inComment ) {
                inQuote = ! inQuote;
            }
            if ( c == '!' && ! inQuote ) {
                inComment = true;
            }
            if ( ! inComment ) {
                sbuf.append( c );
            }
        }
    }

    private static Pair readPair( InputStream in ) throws IOException {
        String line = null;
        do {
            line = readRawLine( in );
        } while ( line != null && line.length() == 0 );
        if ( line == null ) {
            return null;
        }
        int eqPos = line.indexOf( '=' );
        if ( eqPos < 0 ) {
            throw new CefFormatException( "Metadata line does not contain "
                                        + "'=' character" );
        }
        String pName = normalise( line.substring( 0, eqPos ).trim() );
        StringBuffer vbuf =
            new StringBuffer( line.substring( eqPos + 1 ).trim() );
        while ( vbuf.charAt( vbuf.length() - 1 ) == '\\' ) {
            vbuf.setLength( vbuf.length() - 1 );
            vbuf.append( readRawLine( in ).trim() );
        }
        return new Pair( pName, vbuf.toString() );
    }

    private static DescribedValue readGlobal( InputStream in, String gname )
            throws IOException {
        Map<String,String> meta = new LinkedHashMap<String,String>();
        List<String> entryList = new ArrayList<String>();
        while ( true ) {
            Pair pair = readPair( in );
            String pName = pair.name_;
            String pValue = getText( pair.value_ );
            if ( "END_META".equals( pName ) ) {
                if ( gname.equals( pValue ) ) {
                    return createParameter( gname,
                                            entryList
                                           .toArray( new String[ 0 ] ),
                                            meta );
                }
                else {
                    throw new CefFormatException( "Global name mismatch: "
                                                + gname + " != " + pValue );
                }
            }
            else if ( "ENTRY".equals( pName ) ) {
                entryList.add( pValue );
            }
            else if ( "VALUE_TYPE".equals( pName ) ) {
                meta.put( pName, pValue );
            }
            else {
                logger_.warning( "Unknown key \"" + pName + "\""
                               + " in global metadata" );
                meta.put( pName, pValue );
            }
        }
    }

    private static Map<String,String> readVariableMeta( InputStream in,
                                                        String varname )
            throws IOException {
        Map<String,String> map = new LinkedHashMap<String,String>();
        while ( true ) {
            Pair pair = readPair( in );
            String pName = pair.name_;
            String pValue = pair.value_;
            if ( "END_VARIABLE".equals( pName ) ) {
                if ( varname.equals( pair.value_ ) ) {
                    return map;
                }
                else {
                    throw new CefFormatException( "Variable name mismatch: "
                                                + varname + " != " + pValue );
                }
            }
            else {
                map.put( pName, pValue );
            }
        }
    }

    private static String[] splitValues( String txt ) {
        List<String> list = new ArrayList<String>();
        int leng = txt.length();
        StringBuffer sbuf = new StringBuffer();
        boolean inQuote = false;
        for ( int i = 0; i < leng; i++ ) {
            char c = txt.charAt( i );
            if ( c == '"' ) {
                inQuote = ! inQuote;
            }
            if ( c == ',' && ! inQuote ) {
                list.add( getText( sbuf.toString() ) );
                sbuf.setLength( 0 );
            }
            else {
                sbuf.append( c );
            }
        }
        list.add( getText( sbuf.toString() ) );
        return list.toArray( new String[ 0 ] );
    }

    private static String getText( String txt ) {
        if ( txt == null ) {
            return null;
        }
        txt = txt.trim();
        int leng = txt.length();
        return ( txt.charAt( 0 ) == '"' && txt.charAt( leng - 1 ) == '"' )
             ? txt.substring( 1, leng - 1 )
             : txt;
    }

    private static char toAsciiChar( int b ) {
        return (char) b;
    }

    private static String normalise( String str ) {
        return str == null ? null : str.toUpperCase();
    }

    private static DescribedValue createAuxDatum( String key, String svalue ) {
        String name = key;
        String[] svalues = splitValues( svalue );
        final Object value;
        final int[] shape;
        if ( svalues.length == 0 ) {
            value = null;
            shape = null;
        }
        else if ( svalues.length == 1 ) {
            value = svalues[ 0 ];
            shape = null;
        }
        else {
            value = svalues;
            shape = new int[] { svalues.length };
        }
        Class clazz = value == null ? String.class : value.getClass();
        DefaultValueInfo info = new DefaultValueInfo( name, clazz );
        info.setShape( shape );
        return new DescribedValue( info, value );
    }

    private static DescribedValue createParameter( String name,
                                                   String[] entries,
                                                   Map<String,String> meta ) {
        CefValueType valueType =
            CefValueType.getValueType( meta.get( "VALUE_TYPE" ) );
        int nent = entries.length;
        final Class clazz = nent > 1 ? valueType.getArrayClass()
                                     : valueType.getScalarClass();
        final Object value;
        final int[] shape;
        if ( nent == 0 ) {
            value = null;
            shape = null;
        }
        else if ( nent == 1 ) {
            value = valueType.parseScalarValue( entries[ 0 ] );
            shape = null;
        }
        else {
            value = valueType.parseArrayValues( entries, 0, nent );
            shape = new int[] { nent };
        }
        DefaultValueInfo info = new DefaultValueInfo( name, clazz );
        info.setShape( shape );
        info.setUCD( valueType.getUcd() );
        return new DescribedValue( info, value );
    }

    private static Variable createVariable( String vname,
                                            Map<String,String> meta )
            throws CefFormatException {
        final CefValueType<?,?> valueType =
            CefValueType.getValueType( meta.remove( "VALUE_TYPE" ) );
        String unit = getText( meta.remove( "UNITS" ) );
        String descrip = getText( meta.remove( "FIELDNAM" ) );
        String sizes = meta.remove( "SIZES" );
        String name;
        if ( meta.containsKey( "LABLAXIS" ) ) {
            name = getText( meta.remove( "LABLAXIS" ) );
            meta.put( "varName", vname );
        }
        else {
            name = vname;
        }
        final String fillval;
        if ( meta.containsKey( "FILLVAL" ) ) {
            fillval = getText( meta.get( "FILLVAL" ) );
        }
        else {
            fillval = null;
        }
        final int[] shape;
        final int nel;
        if ( sizes != null ) {
            String[] sdims = splitValues( sizes );
            int ndim = sdims.length;
            shape = new int[ ndim ];
            int n = 1;
            for ( int idim = 0; idim < ndim; idim++ ) {
                try {
                    shape[ idim ] = Integer.parseInt( sdims[ idim ] );
                }
                catch ( NumberFormatException e ) {
                    throw new CefFormatException( "Error parsing dimension "
                                                + sdims[ idim ] + " in "
                                                + "\"" + sdims + "\"" );
                }
                n *= shape[ idim ];
            }
            nel = n;
        }
        else {
            shape = null;
            nel = 1;
        }
        String ucd = valueType.getUcd();
        boolean isArray = nel > 1; 
        final ColumnInfo info =
            new ColumnInfo( name,
                            isArray ? valueType.getArrayClass()
                                    : valueType.getScalarClass(),
                            descrip );
        if ( ucd != null ) {
            info.setUCD( ucd );
        }
        if ( shape != null ) {
            info.setShape( shape );
        }
        if ( unit != null ) {
            info.setUnitString( unit );
        }
        List<DescribedValue> auxData = new ArrayList<DescribedValue>();
        for ( Map.Entry<String,String> auxItem : meta.entrySet() ) {
            auxData.add( createAuxDatum( auxItem.getKey(),
                                         auxItem.getValue() ) );
        }
        info.setAuxData( auxData );
        if ( isArray ) {
            if ( fillval == null ) {
                return new Variable( info, nel ) {
                    public Object readValue( String[] fields, int start ) {
                        return valueType.parseArrayValues( fields, start, nel );
                    }
                };
            }
            else {
                return createArrayFillvalVariable( valueType, info, nel,
                                                   fillval );
            }
        }
        else {
            if ( fillval == null ) {
                return new Variable( info, 1 ) {
                    public Object readValue( String[] fields, int start ) {
                        return valueType.parseScalarValue( fields[ start ] );
                    }
                };
            }
            else {
                final Object fillObj = valueType.parseScalarValue( fillval );
                return new Variable( info, 1 ) {
                    public Object readValue( String[] fields, int start ) {
                        Object result =
                            valueType.parseScalarValue( fields[ start ] );
                        return result.equals( fillObj )
                             ? null
                             : result;
                    }
                };
            }
        }
    }

    private static <S,A> Variable
            createArrayFillvalVariable( final CefValueType<S,A> valueType,
                                        ColumnInfo info, final int nel,
                                        String fillval ) {
        final A fv1 =
            valueType.parseArrayValues( new String[] { fillval }, 0, 1 );
        return new Variable( info, nel ) {
            public Object readValue( String[] fields, int start ) {
                A result = valueType.parseArrayValues( fields, start, nel );
                valueType.substituteBlanks( result, fv1 );
                return result;
            }
        };
    }


    private static DataLineReader createDataLineReader( InputStream in,
                                                        String eod,
                                                        final char eor ) {
        if ( eor == '\n' ) {
            return new DataLineReader( in, eod ) {
                public String readDataLine() throws IOException {
                    return readRawLine();
                }
            };
        }
        else {
            return new DataLineReader( in, eod ) {
                final StringBuffer sbuf = new StringBuffer();
                public String readDataLine() throws IOException {
                    sbuf.setLength( 0 );
                    while ( sbuf.length() == 0 ||
                            sbuf.charAt( sbuf.length() - 1 ) != eor ) {
                        String rawLine = readRawLine();
                        if ( rawLine == null ) {
                            return null;
                        }
                        sbuf.append( rawLine );
                    }
                    return sbuf.substring( 0, sbuf.length() - 1 );
                }
            };
        }
    }

    private static abstract class Variable {
        private final ColumnInfo info_;
        private final int itemCount_;
        Variable( ColumnInfo info, int itemCount ) {
            info_ = info;
            itemCount_ = itemCount;
        }
        ColumnInfo getColumnInfo() {
            return info_;
        }
        int getItemCount() {
            return itemCount_;
        }
        abstract Object readValue( String[] fields, int start );
    }

    private static abstract class DataLineReader {
        private final InputStream in_;
        private final String eod_;
        DataLineReader( InputStream in, String eod ) {
            in_ = in;
            eod_ = eod;
        }

        /**
         * Reads a single \n-terminated line, or null if the end of the
         * data rows have been reached.  The trailing \n is stripped.
         */
        String readRawLine() throws IOException {
            String line = CefReader.readRawLine( in_ );
            if ( line == null ) {
                if ( ! "EOF".equals( eod_ ) ) {
                    logger_.warning( "EOF before data end record \""
                                   + eod_ + "\" was encountered" );
                }
                return null;
            }
            else if ( line.equals( eod_ ) ) {
                return null;
            }
            else {
                return line;
            }
        }
        void close() throws IOException {
            in_.close();
        }
        abstract String readDataLine() throws IOException;
    }

    private static class Pair {
        final String name_;
        final String value_;
        Pair( String name, String value ) {
            name_ = name;
            value_ = value;
        }
    }
}
