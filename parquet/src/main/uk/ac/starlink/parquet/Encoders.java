package uk.ac.starlink.parquet;

import java.lang.reflect.Array;
import java.util.function.BiConsumer;
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
     * @return   value encoder
     */
    public static Encoder createEncoder( ColumnInfo info ) {
        final Class<?> clazz = info.getContentClass();
        final String cname = info.getName();
        if ( clazz.equals( Boolean.class ) ) {
            return createScalarEncoder(
                       Boolean.class, cname,
                       PrimitiveType.PrimitiveTypeName.BOOLEAN, null,
                       (val, cns) -> cns.addBoolean( val.booleanValue() ) );
        }
        else if ( clazz.equals( Byte.class ) ) {
            return createScalarEncoder(
                       Byte.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 8, true ),
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Short.class ) ) {
            return createScalarEncoder(
                       Short.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 16, true ),
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Integer.class ) ) {
            return createScalarEncoder(
                       Integer.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32, null,
                       (val, cns) -> cns.addInteger( val.intValue() ) );
        }
        else if ( clazz.equals( Long.class ) ) {
            return createScalarEncoder(
                       Long.class, cname,
                       PrimitiveType.PrimitiveTypeName.INT64, null,
                       (val, cns) -> cns.addLong( val.longValue() ) );
        }
        else if ( clazz.equals( Float.class ) ) {
            return createScalarEncoder(
                       Float.class, cname,
                       PrimitiveType.PrimitiveTypeName.FLOAT, null,
                       (val, cns) -> cns.addFloat( val.floatValue() ) );
        }
        else if ( clazz.equals( Double.class ) ) {
            return createScalarEncoder(
                       Double.class, cname,
                       PrimitiveType.PrimitiveTypeName.DOUBLE, null,
                       (val, cns) -> cns.addDouble( val.doubleValue() ) );
        }
        else if ( clazz.equals( String.class ) ) {
            return createScalarEncoder(
                       String.class, cname,
                       PrimitiveType.PrimitiveTypeName.BINARY,
                       LogicalTypeAnnotation.stringType(),
                       (val, cns) -> cns.addBinary( Binary.fromString( val ) ));
        }
        else if ( clazz.equals( byte[].class ) ) {
            return createArrayEncoder(
                       byte[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 8, true ),
                       (val, ix, cns) -> cns.addInteger( val[ ix ] ) );
        }
        else if ( clazz.equals( short[].class ) ) {
            return createArrayEncoder(
                       short[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32,
                       LogicalTypeAnnotation.intType( 16, true ),
                       (val, ix, cns) -> cns.addInteger( val[ ix ] ) );
        }
        else if ( clazz.equals( int[].class ) ) {
            return createArrayEncoder(
                       int[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT32, null,
                       (val, ix, cns) -> cns.addInteger( val[ ix ] ) );
        }
        else if ( clazz.equals( long[].class ) ) {
            return createArrayEncoder(
                       long[].class, cname,
                       PrimitiveType.PrimitiveTypeName.INT64, null,
                       (val, ix, cns) -> cns.addLong( val[ ix ] ) );
        }
        else if ( clazz.equals( float[].class ) ) {
            return createArrayEncoder(
                       float[].class, cname,
                       PrimitiveType.PrimitiveTypeName.FLOAT, null,
                       (val, ix, cns) -> cns.addFloat( val[ ix ] ) );
        }
        else if ( clazz.equals( double[].class ) ) {
            return createArrayEncoder(
                       double[].class, cname,
                       PrimitiveType.PrimitiveTypeName.DOUBLE, null,
                       (val, ix, cns) -> cns.addDouble( val[ ix ] ) );
        }
        else if ( clazz.equals( String[].class ) ) {
            return createArrayEncoder(
                       String[].class, cname,
                       PrimitiveType.PrimitiveTypeName.BINARY,
                       LogicalTypeAnnotation.stringType(),
                       (val, ix, cns) ->
                           cns.addBinary( Binary.fromString( val[ ix ] ) ) );
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
     * @param   consume   passes a typed input value to a record consumer
     * @return   new encoder
     */
    private static <T> Encoder
            createScalarEncoder( Class<T> clazz, String cname,
                                 PrimitiveType.PrimitiveTypeName primType,
                                 LogicalTypeAnnotation logType,
                                 BiConsumer<T,RecordConsumer> consume ) {
        Types.PrimitiveBuilder<PrimitiveType> builder =
            Types.optional( primType );
        if ( logType != null ) {
            builder = builder.as( logType );
        }
        PrimitiveType type = builder.named( cname );
        return new DefaultEncoder<T>( cname, type, consume );
    }

    /**
     * Returns an encoder for array values based on type information.
     *
     * @param   clazz   input array value type for encoding
     * @param   cname    parquet column name
     * @param   primType  primitive output type
     * @param   logType   logical type annotation
     * @param   arrayReader   passes array-typed values to consumer
     */
    private static <T> Encoder
            createArrayEncoder( Class<T> clazz, String cname,
                                PrimitiveType.PrimitiveTypeName primType,
                                LogicalTypeAnnotation logType,
                                ArrayReader<T> arrayReader ) {
        Types.PrimitiveBuilder<PrimitiveType> elBuilder =
            Types.optional( primType );
        if ( logType != null ) {
            elBuilder = elBuilder.as( logType );
        }
        final String elName = "item";
        final String listName = "list";  // this one is magic
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
                cns.startField( elName, 0 );
                arrayReader.consume( val, i, cns );
                cns.endField( elName, 0 );
                cns.endGroup();
            }
            cns.endField( listName, 0 );
            cns.endGroup();
        };
        return new DefaultEncoder<T>( cname, listType, consume );
    }

    /**
     * Typed encoder implementation.  Instances have to supply an object
     * that can actually pass typed values to a record consumer.
     */
    private static class DefaultEncoder<T> implements Encoder {
        final String cname_;
        final Type type_;
        final BiConsumer<T,RecordConsumer> consumeValue_;

        /**
         * Constructor.
         *
         * @param  cname  parquet column name, must observe parquet syntax rules
         * @param  type   type of column, group or primitive
         * @param  consumeValue  passes a typed value to a record consumer
         */
        DefaultEncoder( String cname, Type type,
                        BiConsumer<T,RecordConsumer> consumeValue ) {
            cname_ = cname;
            type_ = type;
            consumeValue_ = consumeValue;
        }
        public String getColumnName() {
            return cname_;
        }
        public Type getColumnType() {
            return type_;
        }
        public void addValue( Object value, RecordConsumer consumer ) {
            @SuppressWarnings("unchecked")
            T tValue = (T) value;
            consumeValue_.accept( tValue, consumer );
        }
    }

    /**
     * Handles typed array values.
     */
    @FunctionalInterface
    private interface ArrayReader<T> {

        /**
         * Passes one element from an array to a given record consumer.
         *
         * @param  value  array value
         * @param  index  index of element to pass on
         * @param  consumer  element value destination
         */
        void consume( T value, int index, RecordConsumer consumer );
    }
}
