package uk.ac.starlink.array;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * Utility methods for NDArray manipulation.
 *
 * @author   Mark Taylor (Starlink)
 */
public class NDArrays {

    /* Private sole constructor. */
    private NDArrays() {
    }

    /**
     * Copies the data from one NDArray into another.
     * The two must have the same shape (origin and dimensions), 
     * though may have different pixel ordering schemes.  They may have 
     * different primitive types and/or bad values; out-of-range values 
     * in narrowing conversions result in bad values.
     * An effort is made to perform the copy in the most efficient way
     * possible.
     *
     * @param  src   the source NDArray
     * @param  dest  the destination NDArray
     * @throws  IOException   if an I/O error occurs
     * @throws  IllegalArgumentException   if src and dest do not match
     *              in origin, dimensions or type
     * @throws  UnsupportedOperationException   if src is not readable
     *              or dest is not writable
     * @see  TypeConverter
     */
    public static void copy( NDArray src, NDArray dest ) throws IOException {

        /* Validate arguments. */
        checkSameShape( src, dest );
        checkSameType( src, dest );
        checkReadable( src );
        checkWritable( dest );

        /* Do the copy. */
        copy( src.getAccess(), dest.getAccess() );
    }

    /**
     * Tests two NDArrays for equivalence.
     * Returns true only if the data of both are equivalent; this means
     * that the type and shape must be the same, but not necessarily
     * the pixel ordering scheme, URL, writability etc.  A bad value in
     * one array is considered equivalent to a bad value in the other,
     * even if the two do not have the same magic bad value.
     *
     * @param  nda1  first NDArray
     * @param  nda2  second NDArray
     * @return  true if and only if the data and shape of 
     *               <code>nda1</code> and <code>nda2</code> are equivalent
     */
    public static boolean equals( NDArray nda1, NDArray nda2 ) 
            throws IOException {
        Type type = nda1.getType();
        if ( nda2.getType() != type ) {
            return false;
        }
        OrderedNDShape oshape1 = nda1.getShape();
        OrderedNDShape oshape2 = nda2.getShape();
        if ( ! oshape1.sameShape( oshape2 ) ) {
            return false;
        }
        long npix = oshape1.getNumPixels();

        /* Get accessors for both arrays.  They must have the same ordering
         * scheme. */
        ArrayAccess acc1;
        ArrayAccess acc2;
        Order order1 = oshape1.getOrder();
        Order order2 = oshape2.getOrder();
        NDArray ndatmp = null;
        if ( order1 == order2 ) {
            acc1 = nda1.getAccess();
            acc2 = nda2.getAccess();
        }
        else {
            if ( nda1.isRandom() ) {
                Requirements req = new Requirements().setOrder( order2 );
                ndatmp = toRequiredArray( nda1, req );
                acc1 = ndatmp.getAccess();
                acc2 = nda2.getAccess();
            }
            else {
                Requirements req = new Requirements().setOrder( order1 );
                ndatmp = toRequiredArray( nda2, req );
                acc1 = nda1.getAccess();
                acc2 = ndatmp.getAccess();
            }
        }
    
        /* Check whether the data matches element by element. */
        ChunkStepper cit = new ChunkStepper( npix );
        int size = cit.getSize();
        Object buf1 = type.newArray( size );
        Object buf2 = type.newArray( size );
        BadHandler bh1 = nda1.getBadHandler();
        BadHandler bh2 = nda2.getBadHandler();
        boolean match = true;
        for ( ; cit.hasNext() && match; cit.next() ) {
            size = cit.getSize();
            acc1.read( buf1, 0, size );
            acc2.read( buf2, 0, size );
            if ( type == Type.BYTE ) {
                byte[] b1 = (byte[]) buf1;
                byte[] b2 = (byte[]) buf2;
                for ( int i = 0; i < size; i++ ) {
                    boolean bad1 = bh1.isBad( b1, i );
                    boolean bad2 = bh2.isBad( b2, i );
                    match = match &&
                            ( ( ! bad1 && ! bad2 && b1[ i ] == b2[ i ] ) ||
                              ( bad1 && bad2 ) );
                }
            }
            else if ( type == Type.SHORT ) {
                short[] b1 = (short[]) buf1;
                short[] b2 = (short[]) buf2;
                for ( int i = 0; i < size; i++ ) {
                    boolean bad1 = bh1.isBad( b1, i );
                    boolean bad2 = bh2.isBad( b2, i );
                    match = match &&
                            ( ( ! bad1 && ! bad2 && b1[ i ] == b2[ i ] ) ||
                              ( bad1 && bad2 ) );
                }
            }
            else if ( type == Type.INT ) {
                int[] b1 = (int[]) buf1;
                int[] b2 = (int[]) buf2;
                for ( int i = 0; i < size; i++ ) {
                    boolean bad1 = bh1.isBad( b1, i );
                    boolean bad2 = bh2.isBad( b2, i );
                    match = match &&
                            ( ( ! bad1 && ! bad2 && b1[ i ] == b2[ i ] ) ||
                              ( bad1 && bad2 ) );
                }
            }
            else if ( type == Type.FLOAT ) {
                float[] b1 = (float[]) buf1;
                float[] b2 = (float[]) buf2;
                for ( int i = 0; i < size; i++ ) {
                    boolean bad1 = bh1.isBad( b1, i );
                    boolean bad2 = bh2.isBad( b2, i );
                    match = match &&
                            ( ( ! bad1 && ! bad2 && b1[ i ] == b2[ i ] ) ||
                              ( bad1 && bad2 ) );
                }
            }
            else if ( type == Type.DOUBLE ) {
                double[] b1 = (double[]) buf1;
                double[] b2 = (double[]) buf2;
                for ( int i = 0; i < size; i++ ) {
                    boolean bad1 = bh1.isBad( b1, i );
                    boolean bad2 = bh2.isBad( b2, i );
                    match = match &&
                            ( ( ! bad1 && ! bad2 && b1[ i ] == b2[ i ] ) ||
                              ( bad1 && bad2 ) );
                }
            }
            else {
                assert false;
            } 
        }
        acc1.close();
        acc2.close();
        return match;
    }


    /**
     * Does the work of the copy method.  Copies all the pixels from a 
     * readable array accessor to a writable one, and closes them.
     * Both accessors must have the same shape (though not necessarily
     * ordering).
     */
    private static void copy( ArrayAccess sAccess, ArrayAccess dAccess )
            throws IOException {

        /* Store some common information about the arrays. */
        NDShape shape = sAccess.getShape();
        long npix = shape.getNumPixels();

        /* Get a type converter object for copying values. */
        Type sType = sAccess.getType();
        Type dType = dAccess.getType();
        Converter conv =  new TypeConverter( sType, sAccess.getBadHandler(),
                                             dType, dAccess.getBadHandler() );

        /* Do the actual copying, ensuring that the accessors are closed
         * at the end. */
        try {

            /* See if they have the same ordering. */
            if ( sAccess.getShape().sameSequence( dAccess.getShape() ) ) {

                /* If both are mappable, do the copy directly. */
                if ( sAccess.isMapped() && dAccess.isMapped() ) {
                    conv.convert12( sAccess.getMapped(), 0, 
                                    dAccess.getMapped(), 0, (int) npix );
                }

                /* If the source object is mappable, and translation is not
                 * required, write directly from the mapped array. */
                else if ( sAccess.isMapped() && conv.isUnit12() ) {
                    dAccess.write( sAccess.getMapped(), 0, (int) npix );
                }

                /* If the destination object is mappable, and translation is not
                 * required, read directly into the mapped array. */
                else if ( dAccess.isMapped() && conv.isUnit12() ) {
                    sAccess.read( dAccess.getMapped(), 0, (int) npix );
                }

                /* If neither is mappable, or translation is required, copy
                 * in chunks. */
                else {
                    ChunkStepper cIt = new ChunkStepper( npix );
                    int size = cIt.getSize();
                    Object sBuffer = sType.newArray( size );
                    Object dBuffer = dType.newArray( size );
                    for ( ; cIt.hasNext(); cIt.next() ) {
                        size = cIt.getSize();
                        sAccess.read( sBuffer, 0, size );
                        conv.convert12( sBuffer, 0, dBuffer, 0, size );
                        dAccess.write( dBuffer, 0, size );
                    }
                }
            }

            /* If they have different ordering, random access will be required
             * for one or other. */
            else {

                /* If the source has random access, copy a pixel at 
                 * a time in destination order. */
                if ( sAccess.isRandom() ) {
                    Object sBuffer1 = sType.newArray( 1 );
                    Object dBuffer1 = dType.newArray( 1 ); 
                    Iterator pIt = dAccess.getShape().pixelIterator();
                    while ( pIt.hasNext() ) {
                        sAccess.setPosition( (long[]) pIt.next() );
                        sAccess.read( sBuffer1, 0, 1 );
                        conv.convert12( sBuffer1, 0, dBuffer1, 0, 1 );
                        dAccess.write( dBuffer1, 0, 1 );
                    }
                }

                /* If the destination has random access, copy a pixel at 
                 * a time in source order. */
                else if ( dAccess.isRandom() ) {
                    Object sBuffer1 = sType.newArray( 1 );
                    Object dBuffer1 = dType.newArray( 1 );
                    Iterator pIt = sAccess.getShape().pixelIterator();
                    while ( pIt.hasNext() ) {
                        dAccess.setPosition( (long[]) pIt.next() );
                        sAccess.read( sBuffer1, 0, 1 );
                        conv.convert12( sBuffer1, 0, dBuffer1, 0, 1 );
                        dAccess.write( dBuffer1, 0, 1 );
                    }
                }

                /* If the ordering is different and neither has random access,
                 * we have to create a random-access copy of one. */
                else {
                    NDArray scratch = new ScratchNDArray( sAccess );
                    try {
                        copy( sAccess, scratch.getAccess() );
                        copy( scratch.getAccess(), dAccess );
                    }
                    finally {
                        scratch.close();
                    }
                }
            }
        }
        finally {
            sAccess.close();
            dAccess.close();
        }
    }


    /**
     * Returns an NDArray whose type, shape, ordering scheme etc are 
     * described by a supplied {@link Requirements} object.
     * The required shape must have the
     * same dimensionality as that of the base array, but there are
     * no other restrictions on shape - it may be wholly, partially,
     * or not at all within the base array.  In the case that the
     * required (and hence returned) shape differs from that of the
     * base array, any given position (coordinate vector) will refer to
     * the same pixel value in both base and returned NDArray.
     * Reading a pixel in the returned array from a position which
     * is outside the bounds of the base array will result in a bad value.
     * Writing such a pixel will have no effect on the base array.
     * <p>
     * The mode field of the Requirements should be set to indicate the
     * use which will be made of the returned NDArray.
     * It cannot be used to turn a non-readable or
     * non-writable NDArray into a readable or writable one,
     * but controls copying of data - for READ or UPDATE access
     * the returned array is guaranteed to contain the
     * same data as the base array, and for WRITE or UPDATE access
     * writing to the returned array is guaranteed to modify
     * the base array.  If null, a mode will be chosen based on the
     * read/writability of the base NDArray, which ought to give correct
     * results, but may cause more work to be done than is necessary.
     *
     * @param  nda  the NDArray on which to base the result
     * @param  req  a Requirements object indicating the characteristics
     *              required.  If <code>null</code>, then <code>nda</code>
     *              is returned with no further action
     * @return   an NDArray with the same data as nda and the characteristics
     *           indicated by req
     * @throws   IOException  if a new scratch array has to be created and
     *                        filled, and an I/O error occurs at this stage
     * @throws   IllegalArgumentException if mode implies an access mode
     *              not provided by the base array
     */
    public static NDArray toRequiredArray( NDArray nda, Requirements req ) 
            throws IOException {

        /* Degenerate case of no requirements - return original. */
        if ( req == null ) {
            return nda;
        }

        /* Get required characteristics. */
        Type type = req.getType();
        NDShape window = req.getWindow();
        Order order = req.getOrder();
        BadHandler badHandler = req.getBadHandler();
        boolean random = req.getRandom();
        AccessMode mode = req.getMode();
        if ( mode == null ) {
            if ( nda.isReadable() && nda.isWritable() ) {
                mode = AccessMode.UPDATE;
            }
            else if ( nda.isWritable() ) {
                mode = AccessMode.WRITE;
            }
            else {
                mode = AccessMode.READ;
            }
        }

        /* Change the type and bad value handler if necessary. */
        if ( ( type != null && type != nda.getType() ) ||
             ( badHandler != null && ( badHandler.getBadValue() != 
                                       nda.getBadHandler().getBadValue() ) ) ) {
            if ( badHandler == null ) {
                badHandler = type.defaultBadHandler();
            }
            Converter conv = 
                new TypeConverter( nda.getType(), nda.getBadHandler(),
                                   type, badHandler );
            nda = new BridgeNDArray( new ConvertArrayImpl( nda, conv ) );
        }

        /* Copy to a random array if necesary. */
        if ( ( random || order != null && order != nda.getShape().getOrder() ) 
             && ! nda.isRandom() ) {
            nda = new CopyNDArray( nda, mode );
        }

        /* Change the shape and ordering if necessary. */
        if ( order != null && order != nda.getShape().getOrder() ) {
            assert nda.isRandom();  // from previous stanza
            if ( window == null ) {
                window = new NDShape( nda.getShape() );
            }
            OrderedNDShape oshape = new OrderedNDShape( window, order );
            OffsetMapper mapper;
            if ( window.sameShape( nda.getShape() ) ) {
                mapper = new ReorderingMapper( window, order, 
                                               nda.getShape().getOrder() );
            }
            else {
                mapper = new OrderedShapeMapper( oshape, nda.getShape() );
            }
            nda = new BridgeNDArray( new PixelMapArrayImpl( nda, oshape, 
                                                            mapper ) );
        }

        /* Change just the shape if necessary (ordering is the same). */
        else if ( window != null && ! window.sameShape( nda.getShape() ) ) {
            nda = new BridgeNDArray( new WindowArrayImpl( nda, window ) );
        }

        /* Return the same or different NDArray. */
        return nda;
    }


    /**
     * Provides an independent copy of a readable <code>NDArray</code> 
     * with data stored in a scratch array.  
     * The data is copied from the base array
     * but the returned copy will have random access, read access and
     * write access, and is likely to have mapped access
     * (though cannot do so in the case in which the requested 
     * dimensions imply more than Integer.MAX_VALUE pixels).
     * Reads and writes from/to the data of the copy have no effect
     * on the original and vice versa.
     * <p>
     * Invoking this method is equivalent to creating an scratch array and
     * copying the data from the base <code>NDArray</code> into it.
     * <p>
     * A sensible decision about the backing store to use (memory or disk)
     * is made by this class on the basis of the size of array requested.
     *
     * @param  nda   the NDArray whose data will be copied
     * @return   a scratch NDArray holding the same data as nda
     * @throws  IOException  if an I/O error occurs during the copying
     * @throws  UnsupportedOperationException if <code>nda</code> is not
     *          readable or it does not support multiple access and its
     *          <code>getAccess</code> method has already been called
     * @throws  IllegalStateException if close has been called
     *          on <code>nda</code>
     */
    public static NDArray scratchCopy( NDArray nda ) throws IOException {
        NDArray copy = new ScratchNDArray( nda );
        copy( nda, copy );
        return copy;
    }


    /**
     * Provides an independent <code>NDArray</code> based on a readable 
     * <code>ArrayImpl</code>, with data stored in a scratch array. 
     * <p>
     * This convenience method does just the same as invoking
     * <pre>
     *    NDArrays.scratchCopy( new BridgeNDArray( impl ) )
     * </pre>
     *
     * @param  impl   the ArrayImpl whose data will be copied
     * @return   a scratch NDArray backed by the same data as nda
     * @throws  IOException  if an I/O error occurs during the copying
     * @see #scratchCopy(NDArray)
     */
    public static NDArray scratchCopy( ArrayImpl impl ) throws IOException {
        return scratchCopy( new BridgeNDArray( impl ) );
    }


    private static void checkReadable( NDArray nda ) {
        if ( ! nda.isReadable() ) {
            throw new UnsupportedOperationException(
                nda + " is not readable" );
        }
    }
    private static void checkWritable( NDArray nda ) {
        if ( ! nda.isWritable() ) {
            throw new UnsupportedOperationException(
                nda + " is not writable" );
        }
    }
    private static void checkSameShape( NDArray nda1, NDArray nda2 ) {
        if ( ! nda1.getShape().sameShape( nda2.getShape() ) ) {
            throw new IllegalArgumentException( 
                nda1 + " and " + nda2 + " have different shapes" );
        }
    }
    private static void checkSameType( NDArray nda1, NDArray nda2 ) {
        if ( nda1.getType() != nda2.getType() ) {
            throw new IllegalArgumentException(
                nda1 + " and " + nda2 + " have different types" );
        }
    }
    private static void checkMode( NDArray nda, AccessMode mode ) {
        if ( mode.isReadable() && ! nda.isReadable() ) {
            throw new IllegalArgumentException(
                "Read mode requested on unreadable base NDArray " + nda );
        }       
        if ( mode.isWritable() && ! nda.isWritable() ) {
            throw new IllegalArgumentException(
                "Write mode requested on unwritable base NDArray " + nda );
        }   
    }


}

