package uk.ac.starlink.array;

import java.io.IOException;

/**
 * Default implementation of the ArrayAccess interface, based on an
 * AccessImpl.  The underlying AccessImpl must be private to this
 * object, and not used by any other clients during the lifetime of
 * this object.
 * <p>
 * Exemplifies the Bridge Pattern.
 *
 * @author   Mark Taylor (Starlink)
 */
public class DefaultArrayAccess extends DefaultArrayDescription 
                                implements ArrayAccess {

    /** 
     * The offset variable is package private because it makes life much 
     * easier if subclasses defined in this package (MultiplexArrayAccess)
     * can see it, but it is in general dangerous to set, so we don't
     * want it exposed to outside subclass implementations.
     */
    long offset = 0L;

    private boolean closed = false;
    private AccessImpl impl;
    private Object mappedArray;

    /**
     * Constructs an ArrayAccess object from a description of the array's
     * characteristics and a basic implementation of pixel access
     * functionality.
     *
     * @param  adesc     array characteristics description
     * @param  impl      array access service provider, available for 
     *                   exclusive use by this object
     * @param  mappedArray  a java primitve array containing all the pixel
     *                      data of the accessed array.  May be null if
     *                      mapped access is not provided
     */
    public DefaultArrayAccess( ArrayDescription adesc, AccessImpl impl,
                               Object mappedArray ) {
        super( adesc );
        this.impl = impl;
        this.mappedArray = mappedArray;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset( long off ) throws IOException {
        checkOpen();

        /* Check offset is in legal range. */
        if ( off >= 0L && off < arrayNpix ) {

            /* Check we are not attempting to seek backwards without
             * random access. */
            if ( arrayIsRandom || off >= offset ) {
                impl.setOffset( off );
                offset = off;
            }
            else {
                throw new UnsupportedOperationException(
                    "Random access attempted on non-random array" );
            }
        }
        else {
            throw new IndexOutOfBoundsException(
                "Index " + off + " is out of range 0 .. " + arrayNpix );
        }
    }

    public long[] getPosition() {
        return arrayShape.offsetToPosition( offset );
    }

    public void setPosition( long[] pos ) throws IOException {
        setOffset( arrayShape.positionToOffset( pos ) );
    }

    public void read( Object buffer, int start, int size ) throws IOException {
        checkOpen();
        checkReadable();
        arrayType.checkArray( buffer, start + size );
        if ( size > arrayNpix - offset ) {
            throw new IllegalStateException(
                "Not enough pixels remaining to read" +
                " " + ( arrayNpix - offset ) + " < " + size );
        }
        try {
            impl.read( buffer, start, size );
            offset += size;
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }

    public void readTile( Object buffer, NDShape tile ) throws IOException {
        checkOpen();
        checkReadable();

        /* Check that the supplied buffer is suitable. */
        long tileNpix = tile.getNumPixels();
        if ( tileNpix > (long) Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Tile " + tile + " too big" +
                " (" + tileNpix + " > " + Integer.MAX_VALUE + ")" );
        }
        arrayType.checkArray( buffer, (int) tileNpix );

        /* Find the intersection. */
        IntersectionRowIterator rowIt =
            new IntersectionRowIterator( arrayShape, tile, arrayOrder );
        NDShape inter = rowIt.getIntersection();

        /* If there is no intersection, just return an entirely bad
         * array. */
        if ( inter == null ) {
            arrayHandler.putBad( buffer, 0, (int) tileNpix );
            return;
        }

        /* Check that we don't have to seek backwards in a non-random
         * array. */
        if ( ! arrayIsRandom && rowIt.getOffsetA() < offset ) {
            throw new UnsupportedOperationException(
                "Random access attempted on non-random array" );
        }

        /* If the tile is not entirely within the parent array, fill the
         * tile with bad values before we start copying. */
        boolean allWithin = tile.equals( inter );
        if ( ! allWithin ) {
            arrayHandler.putBad( buffer, 0, (int) tileNpix );
        }

        /* Copy rows from this reader into the buffer. */
        int rowLength = (int) rowIt.getRowLength();
        try {
            for ( ; rowIt.hasNext(); rowIt.next() ) {
                long off = rowIt.getOffsetA();
                impl.setOffset( off );
                impl.read( buffer, (int) rowIt.getOffsetB(), rowLength );
                offset = off + rowLength;
            }
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }

    public void write( Object buffer, int start, int size ) throws IOException {
        checkOpen();
        checkWritable();
        arrayType.checkArray( buffer, start + size );
        if ( size > arrayNpix - offset ) {
            throw new IllegalStateException(
                "Not enough pixels remaining to write" +
                " " + ( arrayNpix - offset ) + " < " + size );
        }
        try {
            impl.write( buffer, start, size );
            offset += size;
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }

    public void writeTile( Object buffer, NDShape tile ) throws IOException {
        checkOpen();

        /* Check that the supplied array is appropriate. */
        long tileNpix = tile.getNumPixels();
        if ( tileNpix > (long) Integer.MAX_VALUE ) {
            throw new IllegalArgumentException(
                "Tile " + tile + " too big" +
                " (" + tileNpix + " > " + Integer.MAX_VALUE + ")" );
        }
        arrayType.checkArray( buffer, (int) tileNpix );

        /* Find the intersection. */
        IntersectionRowIterator rowIt =
            new IntersectionRowIterator( arrayShape, tile, arrayOrder );
        NDShape inter = rowIt.getIntersection();

        /* If there is no intersection, don't attempt any copying. */
        if ( inter == null ) {
            return;
        }

        /* Check that we don't have to seek backwards in a non-random
         * array. */
        if ( arrayIsRandom && rowIt.getOffsetA() < offset ) {
            throw new UnsupportedOperationException(
                "Random access attempted on non-random array" );
        }

        /* Write rows from the buffer into this writer. */
        int rowLength = (int) rowIt.getRowLength();
        try {
            for ( ; rowIt.hasNext(); rowIt.next() ) {
                long off = rowIt.getOffsetA();
                impl.setOffset( off );
                impl.write( buffer, (int) rowIt.getOffsetB(), rowLength );
                offset = off + rowLength;
            }
        }
        catch ( IOException e ) {
            close();
            throw e;
        }
    }

    public boolean isMapped() {
        return mappedArray != null;
    }

    public Object getMapped() {
        checkOpen();
        if ( mappedArray == null ) {
            throw new UnsupportedOperationException( 
                "Accessor is not mappable" );
        }
        return mappedArray;
    }

    public void close() throws IOException {
        if ( ! closed ) {
            impl.close();
            doClose();
        }
    }

    protected void doClose() {
        closed = true;
        impl = null;
        mappedArray = null;
        offset = -1L;
    }

    protected void checkOpen() {
        if ( closed ) {
            throw new IllegalStateException( "Accessor has been closed" );
        }
    }

    private void checkReadable() {
        if ( ! arrayIsReadable ) {
            throw new UnsupportedOperationException(
                "Accessor is not readable" );
        }
    }

    private void checkWritable() {
        if ( ! arrayIsWritable ) {
            throw new UnsupportedOperationException(
                "Accessor is not writable" );
        }
    }

    public String toString() {
        return super.toString() + " impl={ " + impl.toString() + " }";
    }

}
