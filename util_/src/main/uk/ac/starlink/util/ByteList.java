package uk.ac.starlink.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Extendable array of bytes.
 *
 * <p>The {@link #decodeString} and {@link #decodeUtf8} convenience methods
 * provide an efficient and Unicode-compliant way to build
 * a String from bytes.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2006
 */
public class ByteList extends PrimitiveList {

    /**
     * Constructs a new list.
     */
    public ByteList() {
        this( DEFAULT_SIZE );
    }

    /**
     * Constructs a new list with a given initial capacity.
     *
     * @param   size  initial capacity
     */
    public ByteList( int size ) {
        super( new byte[ size ], 0 );
    }

    /**
     * Constructs a new list initialised to the contents of a given array.
     *
     * @param   array   array whose contents form initial contents of list
     */
    public ByteList( byte[] array ) {
        super( array.clone(), array.length );
    }

    /**
     * Returns the element at a given position.
     *
     * @param   i   index
     * @return   element at <code>i</code>
     */ 
    public byte get( int i ) {
        checkIndex( i );
        return ((byte[]) array_)[ i ];
    }

    /**
     * Sets the element at a given position.
     *
     * @param   i  index
     * @param   value   new value for element <code>i</code>
     */
    public void set( int i, byte value ) {
        checkIndex( i );
        ((byte[]) array_)[ i ] = value;
    }

    /**
     * Appends a value to the end of this list.
     *
     * @param   value  value to append
     */
    public void add( byte value ) {
        expandSize( 1 );
        set( size() - 1, value );
    }

    /**
     * Appends all the elements of a second list to this list.
     *
     * @param  other  other list
     * @return   true iff this collection changed as a result of the call
     */
    public boolean addAll( ByteList other ) {
        return super.addAll( other );
    }

    /**
     * Appends all the elements of a primitive array to this list.
     *
     * @param   array  array to append
     * @return   true iff this collection changed as a result of the call
     */ 
    public boolean addAll( byte[] array ) {
        return super.addArrayElements( array, array.length );
    }

    /**
     * Returns the contents of this list as an array.
     *
     * @return   copy of list contents
     */
    public byte[] toByteArray() {
        return (byte[]) toArray();
    }

    /**
     * Returns the array currently used to store the contents of this list.
     * Its length will be greater than or equal to the length of this list.
     * The identity of the returned array may change as this list is mutated.
     *
     * @return  storage array
     */
    public byte[] getByteBuffer() {
        return (byte[]) array_;
    }

    /**
     * Returns a string with the current byte content of this byte list,
     * decoded using the supplied encoding.
     *
     * @param  charset  character set
     * @return   new string
     */
    public String decodeString( Charset charset ) {
        return new String( (byte[]) array_, 0, size(), charset );
    }

    /**
     * Returns a string with the current byte content of this byte list,
     * decoded using the UTF-8 encoding.
     *
     * @return  new string
     */
    public String decodeUtf8() {
        return decodeString( StandardCharsets.UTF_8 );
    }
}
