package uk.ac.starlink.pds4;

import gov.nasa.pds.label.object.FieldType;
import gov.nasa.pds.objectAccess.table.FieldAdapter;

/**
 * Adapts a FieldAdapter to return a typed value.
 * The NASA-provided FieldAdapter class does the work of reading bytes
 * or text to turn them into some numeric or string value,
 * but this class augments that to provide an object that knows what
 * type of object should be read.
 *
 * @author   Mark Taylor
 * @since    24 Nov 2021
 */
public abstract class FieldReader<T> {

    private final FieldType ftype_;
    private final FieldAdapter adapter_;
    private final Class<T> clazz_;

    /**
     * Constructor.
     *
     * @param  ftype   field type
     * @param  clazz   field data content class
     */
    private FieldReader( FieldType ftype, Class<T> clazz ) {
        ftype_ = ftype;
        adapter_ = ftype.getAdapter();
        clazz_ = clazz;
    }

    /**
     * Reads a typed object from a buffer in accordance with this field type.
     * The startBit and endBit arguments reflecct their appearence in
     * the corresponding NASA classes, but I don't know what they do.
     *
     * @param   buf   byte buffer containing data
     * @param   offset   index into buf of byte at which data value begins
     * @param   length   number of bytes over which value is represented
     * @param   startBit   ??
     * @param   endBit     ??
     * @return   typed data value
     */
    public abstract T readField( byte[] buf, int offset, int length,
                                 int startBit, int endBit );

    /**
     * Returns the type of object that this reader will read.
     *
     * @return  content class
     */
    public Class<T> getContentClass() {
        return clazz_;
    }

    /**
     * Returns the field type for this reader.
     *
     * @return   field type
     */
    public FieldType getFieldType() {
        return ftype_;
    }

    /**
     * Returns a FieldReader instance for a given FieldType.
     *
     * @param   ftype   field type
     * @return   field reader
     */
    public static FieldReader<?> getInstance( FieldType ftype ) {
        final FieldAdapter adapter = ftype.getAdapter();
        switch ( ftype ) {
            case UTF8_STRING:
            case ASCII_ANYURI:
            case ASCII_BIBCODE:
            case ASCII_DATE:
            case ASCII_DATE_DOY:
            case ASCII_DATE_TIME:
            case ASCII_DATE_TIME_DOY:
            case ASCII_DATE_TIME_DOY_UTC:
            case ASCII_DATE_TIME_UTC:
            case ASCII_DATE_TIME_YMD:
            case ASCII_DATE_TIME_YMD_UTC:
            case ASCII_DATE_YMD:
            case ASCII_DIRECTORY_PATH_NAME:
            case ASCII_DOI:
            case ASCII_FILE_NAME:
            case ASCII_FILE_SPECIFICATION_NAME:
            case ASCII_LID:
            case ASCII_LIDVID:
            case ASCII_LIDVID_LID:
            case ASCII_MD5_CHECKSUM:
            case ASCII_STRING:
            case ASCII_TIME:
            case ASCII_VID:
            case COMPLEXLSB16:
            case COMPLEXLSB8:
            case COMPLEXMSB16:
            case COMPLEXMSB8:
            case SIGNEDBITSTRING:
            case UNSIGNEDBITSTRING:
            case UNKNOWN:
                return new FieldReader<String>( ftype, String.class ) {
                    public String readField( byte[] buf, int off, int leng,
                                             int startBit, int endBit ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        return txt == null ? null : txt.trim();
                    }
                };

            case SIGNEDBYTE:
            case UNSIGNEDBYTE:
            case SIGNEDLSB2:
            case SIGNEDMSB2:
                return new FieldReader<Short>( ftype, Short.class ) { 
                    public Short readField( byte[] buf, int off, int leng,
                                            int startBit, int endBit ) {
                        return Short
                              .valueOf( adapter
                                       .getShort( buf, off, leng,
                                                  startBit, endBit ) );
                    }
                };

            case SIGNEDLSB4:
            case SIGNEDMSB4:
            case UNSIGNEDLSB2:
            case UNSIGNEDMSB2:
                return new FieldReader<Integer>( ftype, Integer.class ) {
                    public Integer readField( byte[] buf, int off, int leng,
                                              int startBit, int endBit ) {
                        return Integer
                              .valueOf( adapter
                                       .getInt( buf, off, leng,
                                                startBit, endBit ) );
                    }
                };

            case SIGNEDLSB8:
            case SIGNEDMSB8:
            case UNSIGNEDLSB4:
            case UNSIGNEDMSB4:
            case ASCII_INTEGER:
            case ASCII_NONNEGATIVE_INTEGER:
            case ASCII_NUMERIC_BASE16:
            case ASCII_NUMERIC_BASE2:
            case ASCII_NUMERIC_BASE8:
                return new FieldReader<Long>( ftype, Long.class ) {
                    public Long readField( byte[] buf, int off, int leng,
                                           int startBit, int endBit ) {
                        return Long
                              .valueOf( adapter
                                       .getLong( buf, off, leng,
                                                 startBit, endBit ) );
                    }
                };

            case IEEE754LSBSINGLE:
            case IEEE754MSBSINGLE:
                return new FieldReader<Float>( ftype, Float.class ) {
                    public Float readField( byte[] buf, int off, int leng,
                                            int startBit, int endBit ) {
                        return Float
                              .valueOf( adapter
                                       .getFloat( buf, off, leng,
                                                  startBit, endBit ) );
                    }
                };

            case ASCII_REAL:
            case IEEE754LSBDOUBLE:
            case IEEE754MSBDOUBLE:
                return new FieldReader<Double>( ftype, Double.class ) {
                    public Double readField( byte[] buf, int off, int leng,
                                             int startBit, int endBit ) {
                        return Double
                              .valueOf( adapter
                                       .getDouble( buf, off, leng,
                                                   startBit, endBit ) );
                    }
                };

            case UNSIGNEDLSB8:
            case UNSIGNEDMSB8:
                return new FieldReader<Long>( ftype, Long.class ) {
                    public Long readField( byte[] buf, int off, int leng,
                                           int startBit, int endBit ) {
                        try {
                            return Long
                                  .valueOf( adapter
                                           .getLong( buf, off, leng,
                                                     startBit, endBit ) );
                        }
                        catch ( NumberFormatException e ) { 
                            return null;
                        }
                    }
                };

            default:
                assert false;
                return new FieldReader<String>( ftype, String.class ) {
                    public String readField( byte[] buf, int off, int leng,
                                             int startBit, int endBit ) {
                        String txt = adapter.getString( buf, off, leng,
                                                        startBit, endBit );
                        return txt == null ? null : txt.trim();
                    }
                };
        }
    }
}
