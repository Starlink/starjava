package uk.ac.starlink.cef;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class CefValueType<S,A> {

    private static final Map<String,CefValueType> vtMap = createValueTypeMap();
    private static final CefValueType STRING =
        new StringValueType( "string", null );
    private static final Logger logger_ =
        Logger.getLogger( CefValueType.class.getName() );
    private static final Level substLevel_ = Level.WARNING;

    private final String name_;
    private final Class<S> scalarClazz_;
    private final Class<A> arrayClazz_;
    private final String ucd_;

    private CefValueType( String name, Class<S> scalarClazz,
                          Class<A> arrayClazz, String ucd ) {
        name_ = name;
        scalarClazz_ = scalarClazz;
        arrayClazz_ = arrayClazz;
        ucd_ = ucd;
    }

    private CefValueType( String name, Class<S> scalarClazz,
                          Class<A> arrayClazz ) {
        this( name, scalarClazz, arrayClazz, null );
    }

    public String getName() {
        return name_;
    }

    public Class<S> getScalarClass() {
        return scalarClazz_;
    }

    public Class<A> getArrayClass() {
        return arrayClazz_;
    }

    public String getUcd() {
        return ucd_;
    }

    public abstract S parseScalarValue( String entry );
    public abstract A parseArrayValues( String[] entries,
                                        int start, int count );

    /**
     * Substitute a blank value for every occurrence of a given magic
     * value in an array, if possible.
     *
     * @param  array   array whose elements are to be blanked
     * @param  magic1  1-element array containing the magic value
     */
    public abstract void substituteBlanks( A array, A magic1 );

    void warnFail( String txt ) {
        logger_.warning( "Failed to parse " + name_ + " value "
                       + "\"" + txt + "\"" );
    }

    /** does not return null. */
    public static CefValueType getValueType( String vt ) {
        CefValueType type = vtMap.get( vt );
        return type == null ? STRING : type;
    }

    private static Map<String,CefValueType> createValueTypeMap() {
        Map<String,CefValueType> map = new HashMap<String,CefValueType>();
        map.put( "FLOAT", new FloatValueType( "FLOAT" ) );
        map.put( "DOUBLE", new DoubleValueType( "DOUBLE" ) );
        map.put( "INT", new IntegerValueType( "INT" ) );
        map.put( "BYTE", new ByteValueType( "BYTE" ) );
        map.put( "ISO_TIME", new StringValueType( "ISO_TIME", "time.epoch" ) );
        return map;
    }

    private static class FloatValueType extends CefValueType<Float,float[]> {
        FloatValueType( String name ) {
            super( name, Float.class, float[].class );
        }
        public Float parseScalarValue( String item ) {
            try {
                return Float.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public float[] parseArrayValues( String[] items, int start,
                                         int count ) {
            float[] results = new float[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                float value;
                try {
                    value = Float.parseFloat( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = Float.NaN;
                }
                results[ i ] = value;
            }
            return results;
        }
        public void substituteBlanks( float[] array, float[] magic1 ) {
            float magic = magic1[ 0 ];
            int count = array.length;
            for ( int i = 0; i < count; i++ ) {
                if ( array[ i ] == magic ) {
                    array[ i ] = Float.NaN;
                }
            }
        }
    }

    private static class DoubleValueType extends CefValueType<Double,double[]> {
        DoubleValueType( String name ) {
            super( name, Double.class, double[].class );
        }
        public Double parseScalarValue( String item ) {
            try {
                return Double.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public double[] parseArrayValues( String[] items, int start,
                                          int count ) {
            double[] results = new double[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                double value;
                try {
                    value = Double.parseDouble( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = Double.NaN;
                }
                results[ i ] = value;
            }
            return results;
        }
        public void substituteBlanks( double[] array, double[] magic1 ) {
            double magic = magic1[ 0 ];
            int count = array.length;
            for ( int i = 0; i < count; i++ ) {
                if ( array[ i ] == magic ) {
                    array[ i ] = Double.NaN;
                }
            }
        }
    }

    private static class IntegerValueType extends CefValueType<Integer,int[]> {
        IntegerValueType( String name ) {
            super( name, Integer.class, int[].class );
        }
        public Integer parseScalarValue( String item ) {
            try {
                return Integer.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public int[] parseArrayValues( String[] items, int start, int count ) {
            int[] results = new int[ count ];
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                int value;
                try {
                    value = Integer.parseInt( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    value = 0;
                }
                results[ i ] = value;
            }
            return results;
        }
        public void substituteBlanks( int[] array, int[] magic1 ) {
            if ( logger_.isLoggable( substLevel_ ) ) {
                int magic = magic1[ 0 ];
                int count = array.length;
                int nsub = 0;
                for ( int i = 0; i < count; i++ ) {
                    if ( array[ i ] == magic ) {
                        nsub++;
                    }
                }
                if ( nsub > 0 ) {
                    logger_.log( substLevel_,
                                 nsub + " fill values not substitued in "
                               + getName() + " column" );
                }
            }
        }
    }

    // CEF specification does not say whether byte is signed.  Assume so.
    private static class ByteValueType extends CefValueType<Byte,byte[]> {
        ByteValueType( String name ) {
            super( name, Byte.class, byte[].class );
        }
        public Byte parseScalarValue( String item ) {
            try {
                return Byte.valueOf( item );
            }
            catch ( NumberFormatException e ) {
                warnFail( item );
                return null;
            }
        }
        public byte[] parseArrayValues( String[] items, int start, int count ) {
            byte[] results = new byte[ count ];
            int ntrunc = 0;
            for ( int i = 0; i < count; i++ ) {
                String item = items[ start++ ];
                int ivalue;
                try {
                    ivalue = Integer.parseInt( item );
                }
                catch ( NumberFormatException e ) {
                    warnFail( item );
                    ivalue = 0;
                }
                byte bvalue = (byte) ivalue;
                if ( bvalue != ivalue ) {
                    ntrunc++;
                    logger_.info( "truncated byte value "
                                 + ivalue + " -> " + bvalue );
                }
                results[ i ] = bvalue;
            }
            if ( ntrunc > 0 ) {
                logger_.warning( "Truncated " + ntrunc + " values in "
                               + getName() + " column" );
            }
            return results;
        }
        public void substituteBlanks( byte[] array, byte[] magic1 ) {
            if ( logger_.isLoggable( substLevel_ ) ) {
                byte magic = magic1[ 0 ];
                int count = array.length;
                int nsub = 0;
                for ( int i = 0; i < count; i++ ) {
                    if ( array[ i ] == magic ) {
                        nsub++;
                    }
                }
                if ( nsub > 0 ) {
                    logger_.log( substLevel_,
                                 nsub + " fill values not substituted in "
                               + getName() + " column" );
                }
            }
        }
    }

    private static class StringValueType extends CefValueType<String,String[]> {
        StringValueType( String name, String ucd ) {
            super( name, String.class, String[].class, ucd );
        }
        public String parseScalarValue( String item ) {
            return item;
        }
        public String[] parseArrayValues( String[] items, int start,
                                          int count ) {
            String[] results = new String[ count ];
            System.arraycopy( items, start, results, 0, count );
            return results;
        }
        public void substituteBlanks( String[] array, String[] magic1 ) {
            String magic = magic1[ 0 ];
            int count = array.length;
            for ( int i = 0; i < count; i++ ) {
                if ( array[ i ].equals( magic ) ) {
                    array[ i ] = null;
                }
            }
        }
    }
}
