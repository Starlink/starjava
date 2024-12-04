package uk.ac.starlink.parquet;

import java.lang.reflect.Array;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import uk.ac.starlink.table.ColumnInfo;

/**
 * Provides Encoder implementations.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2021
 */
public class Encoders {

    /**
     * Private constructor prevents instantiation.
     */
    private Encoders() {
    }

    /**
     * Returns an encoder for a given ColumnInfo.
     *
     * @param   info  column metadata
     * @param   groupArray   true for group-style arrays,
     *                       false for repeated primitives
     * @return   value encoder
     */
    public static Encoder<?> createEncoder( ColumnInfo info,
                                            boolean groupArray ) {
        final Class<?> clazz = info.getContentClass();
        final String cname = info.getName();
        if ( clazz.equals( Boolean.class ) ) {
            return createScalarEncoder(
                       Boolean.class, cname,
                       PrimitiveType.PrimitiveTypeName.BOOLEAN, null,
                       val -> false,
                       (val, cns) -> cns.addBoolean( val.booleanValue() ) );
        }
        else if ( clazz.equals( Byte.class ) ) {
            return createScalarEncoder(
                       Byte.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 8, true ),
                       val -> false,
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Short.class ) ) {
            return createScalarEncoder(
                       Short.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 16, true ),
                       val -> false,
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Integer.class ) ) {
            return createScalarEncoder(
                       Integer.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32, null,
                       val -> false,
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Long.class ) ) {
            return createScalarEncoder(
                       Long.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT64, null,
                       val -> false,
                       (val, cns) -> cns.addLong( val.longValue() ) );
        }
        else if ( clazz.equals( Float.class ) ) {
            return createScalarEncoder(
                       Float.class, cname,
                       PrimitiveType.PrimitiveTypeName.FLOAT, null,
                       val -> val.isNaN(),
                       (val, cns) -> cns.addFloat( val.floatValue() ) );
        }
        else if ( clazz.equals( Double.class ) ) {
            return createScalarEncoder(
                       Double.class, cname,
                       PrimitiveType.PrimitiveTypeName.DOUBLE, null,
                       val -> val.isNaN(),
                       (val, cns) -> cns.addDouble( val.doubleValue() ) );
        }
        else if ( clazz.equals( String.class ) ) {
            return createScalarEncoder(
                       String.class, cname,
                       PrimitiveType.PrimitiveTypeName.BINARY,
                       LogicalTypeAnnotation.stringType(),
                       val -> val.length() == 0,
                       (val, cns) -> cns.addBinary( Binary.fromString( val ) ));
        }
        else if ( clazz.equals( boolean[].class ) ) {
            return createArrayEncoder(
                       boolean[].class, cname,
                       PrimitiveType.PrimitiveTypeName.BOOLEAN, null,
                       (val, ix) -> new WritableElement() {
                           boolean el = val[ ix ];
                           public boolean isBlank() {
                               return false;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addBoolean( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( byte[].class ) ) {
            return createArrayEncoder(
                       byte[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 8, true ),
                       (val, ix) -> new WritableElement() {
                           byte el = val[ ix ];
                           public boolean isBlank() {
                               return false;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addInteger( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( short[].class ) ) {
            return createArrayEncoder(
                       short[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 16, true ),
                       (val, ix) -> new WritableElement() {
                           short el = val[ ix ];
                           public boolean isBlank() {
                               return false;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addInteger( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( int[].class ) ) {
            return createArrayEncoder(
                       int[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32, null,
                       (val, ix) -> new WritableElement() {
                           int el = val[ ix ];
                           public boolean isBlank() {
                               return false;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addInteger( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( long[].class ) ) {
            return createArrayEncoder(
                       long[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT64, null,
                       (val, ix) -> new WritableElement() {
                           long el = val[ ix ];
                           public boolean isBlank() {
                               return false;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addLong( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( float[].class ) ) {
            return createArrayEncoder(
                       float[].class, cname,
                       PrimitiveType.PrimitiveTypeName.FLOAT, null,
                       (val, ix) -> new WritableElement() {
                           float el = val[ ix ];
                           public boolean isBlank() {
                               return Float.isNaN( el );
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addFloat( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( double[].class ) ) {
            return createArrayEncoder(
                       double[].class, cname,
                       PrimitiveType.PrimitiveTypeName.DOUBLE, null,
                       (val, ix) -> new WritableElement() {
                           double el = val[ ix ];
                           public boolean isBlank() {
                               return Double.isNaN( el );
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addDouble( el );
                           }
                       },
                       groupArray );
        }
        else if ( clazz.equals( String[].class ) ) {
            return createArrayEncoder(
                       String[].class, cname,
                       PrimitiveType.PrimitiveTypeName.BINARY,
                       LogicalTypeAnnotation.stringType(),
                       (val, ix) -> new WritableElement() {
                           String el = val[ ix ];
                           public boolean isBlank() {
                               return el == null || el.length() == 0;
                           }
                           public void writeToRecord( RecordConsumer cns ) {
                               cns.addBinary( Binary.fromString( el ) );
                           }
                       },
                       groupArray );
        }
        else {
            return null;
        }
    }

    /**
     * Returns an encoder for scalar values given type information.
     *
     * @param   clazz   input value type for encoding
     * @param   cname    parquet column name
     * @param   primType  primitive output type
     * @param   logType   logical type annotation
     * @param   isBlank   whether a non-null instance value is to be
     *                    considered equivalent to null
     * @param   consume   passes a typed input value to a record consumer
     * @return   new encoder
     */
    private static <T> Encoder<T>
            createScalarEncoder( Class<T> clazz, String cname,
                                 PrimitiveType.PrimitiveTypeName primType,
                                 LogicalTypeAnnotation logType,
                                 Predicate<T> isBlank,
                                 BiConsumer<T,RecordConsumer> consume ) {
        Types.PrimitiveBuilder<PrimitiveType> builder =
            Types.optional( primType );
        if ( logType != null ) {
            builder = builder.as( logType );
        }
        Function<Object,T> toTyped = value -> {
            if ( clazz.isInstance( value ) ) {
                T tval = clazz.cast( value ); 
                return isBlank.test( tval ) ? null : tval;
            }
            else {
                return null;
            }
        };
        PrimitiveType type = builder.named( cname );
        Runnable checkNull = () -> {};
        return new DefaultEncoder<T>( cname, type, toTyped, consume, checkNull);
    }

    /**
     * Returns an encoder for array values based on type information.
     *
     * @param   clazz   input array value type for encoding
     * @param   cname    parquet column name
     * @param   primType  primitive output type
     * @param   logType   logical type annotation
     * @param   arrayReader   passes array-typed values to consumer
     * @param   groupArray   true for group-style arrays,
     *                       false for repeated primitives
     * @return   new encoder
     */
    private static <T> Encoder<T>
            createArrayEncoder( Class<T> clazz, String cname,
                                PrimitiveType.PrimitiveTypeName primType,
                                LogicalTypeAnnotation logType,
                                ArrayReader<T> arrayReader,
                                boolean groupArray ) {
        Function<Object,T> toTyped =
            value -> clazz.isInstance( value ) && Array.getLength( value ) > 0
                   ? clazz.cast( value )
                   : null;

        /* There are two ways to write array-valued columns,
         * as described by the file LogicalTypes.md from
         * https://github.com/apache/parquet-format/.
         * I'm looking at tag apache-parquet-format-2.10.0.
         * Note that these cannot be mixed in a single file:
         *
         *   "Implementations should use either LIST and MAP annotations
         *    or unannotated repeated fields, but not both. When using the
         *    annotations, no unannotated repeated types are allowed."
         */
        if ( groupArray ) {

            /* Annotated LIST option:
             *
             *   "LIST must always annotate a 3-level structure:
             *
             *       <list-repetition> group <name> (LIST) {
             *          repeated group list {
             *             <element-repetition> <element-type> element;
             *          }
             *       }
             *
             *    The outer-most level must be a group annotated with LIST
             *    that contains a single field named 'list'.
             *    The repetition of this level must be either 'optional' or
             *    'required' and determines whether the list is nullable.
             *    The middle level, named 'list', must be a repeated group
             *    with a single field named 'element'.  The 'element' field
             *    encodes the list's element type and repetition.
             *    Element repetition must be 'required' or 'optional'."
             */
            Types.PrimitiveBuilder<PrimitiveType> elBuilder =
                Types.optional( primType );
            if ( logType != null ) {
                elBuilder = elBuilder.as( logType );
            }
            final String elName = "element";
            final String listName = "list";
            PrimitiveType elType = elBuilder.named( elName );
            GroupType listType =
                Types.optionalList()
                     .element( elType )
                     .named( cname );
            BiConsumer<T,RecordConsumer> consume = (val, cns) -> {
                cns.startGroup();
                cns.startField( listName, 0 );
                int nel = Array.getLength( val );
                for ( int i = 0; i < nel; i++ ) {
                    cns.startGroup();
                    WritableElement writable =
                        arrayReader.getWritable( val, i );
                    if ( ! writable.isBlank() ) {
                        cns.startField( elName, 0 );
                        writable.writeToRecord( cns );
                        cns.endField( elName, 0 );
                    }
                    cns.endGroup();
                }
                cns.endField( listName, 0 );
                cns.endGroup();
            };
            Runnable checkNull = () -> {};
            return new DefaultEncoder<T>( cname, listType, toTyped, consume,
                                          checkNull );
        }
        else {

            /* Repeated field option:
             *
             *   "A repeated field that is neither contained by a LIST- or
             *    MAP-annotated group nor annotated by LIST or MAP should
             *    be interpreted as a required list of required elements
             *    where the element type is the type of the field."
             */
            Types.PrimitiveBuilder<PrimitiveType> elBuilder =
                Types.repeated( primType );
            if ( logType != null ) {
                elBuilder = elBuilder.as( logType );
            }
            PrimitiveType elType = elBuilder.named( cname );
            BiConsumer<T,RecordConsumer> consume = (val, cns) -> {
                int nel = Array.getLength( val );
                for ( int i = 0; i < nel; i++ ) {
                    WritableElement writable =
                        arrayReader.getWritable( val, i );
                    if ( writable.isBlank() ) {
                        String msg = "Null array elements not permitted"
                                   + " for groupArray=false";
                        throw new NullsWithoutGroupArrayException( msg );
                    }
                    writable.writeToRecord( cns );
                }
            };
            Runnable checkNull = () -> {
                String msg =
                    "Null array values not permitted for groupArray=false";
                throw new NullsWithoutGroupArrayException( msg );
            };
            return new DefaultEncoder<T>( cname, elType, toTyped, consume,
                                          checkNull );
        }
    }

    /**
     * Typed encoder implementation.  Instances have to supply an object
     * that can actually pass typed values to a record consumer.
     */
    private static class DefaultEncoder<T> implements Encoder<T> {
        final String cname_;
        final Type type_;
        final Function<Object,T> toTyped_;
        final BiConsumer<T,RecordConsumer> consumeValue_;
        final Runnable checkNull_;

        /**
         * Constructor.
         *
         * @param  cname  parquet column name, must observe parquet syntax rules
         * @param  type   type of column, group or primitive
         * @param  toTyped  converts an untyped object that should be of
         *                  the right class to a typed object that is of
         *                  the right class, or to null
         *                  if it's effectively blank
         * @param  consumeValue  passes a typed value to a record consumer
         * @param  checkNull  called if null will be written
         */
        DefaultEncoder( String cname, Type type,
                        Function<Object,T> toTyped,
                        BiConsumer<T,RecordConsumer> consumeValue,
                        Runnable checkNull ) {
            cname_ = cname;
            type_ = type;
            toTyped_ = toTyped;
            consumeValue_ = consumeValue;
            checkNull_ = checkNull;
        }
        public String getColumnName() {
            return cname_;
        }
        public Type getColumnType() {
            return type_;
        }
        public T typedValue( Object obj ) {
            return toTyped_.apply( obj );
        }
        public void addValue( T tValue, RecordConsumer consumer ) {
            consumeValue_.accept( tValue, consumer );
        }
        public void checkNull() {
            checkNull_.run();
        }
    }

    /**
     * Defines field content for writing.
     */
    private interface WritableElement {

        /**
         * If true, the content corresponds to a null value.
         *
         * @return  true iff element is blank
         */
        boolean isBlank();

        /**
         * Passes this content to a record consumer for writing.
         * This method will only be called if {@link #isBlank} returns false.
         *
         * @param  cns  record consumer
         */
        void writeToRecord( RecordConsumer cns );
    }

    /**
     * Handles typed array values.
     */
    @FunctionalInterface
    private interface ArrayReader<T> {

        /**
         * Returns an object representing the value of an element
         * from an array.
         *
         * @param  value  array object
         * @param  index  index of array element to write
         */
        WritableElement getWritable( T value, int index );
    }
}
