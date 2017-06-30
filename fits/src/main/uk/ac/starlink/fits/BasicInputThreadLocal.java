package uk.ac.starlink.fits;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ThreadLocal based on an InputFactory.
 * This can dispense a BasicInput object private to the current thread.
 * The close method will close all the BasicInput objects that this
 * has created so far.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2017
 */
public class BasicInputThreadLocal extends ThreadLocal<BasicInput>
                                   implements Closeable {

    private final InputFactory inputFact_;
    private final boolean isSeq_;
    private final List<BasicInput> inputs_;

    /**
     * Constructor.
     *
     * @param  inputFact   factory for BasicInput objects
     * @param  isSeq    true if created inputs are sequential, false for random
     */
    public BasicInputThreadLocal( InputFactory inputFact, boolean isSeq ) {
        inputFact_ = inputFact;
        isSeq_ = isSeq;
        inputs_ = new ArrayList<BasicInput>();
    }

    @Override
    protected BasicInput initialValue() {
        BasicInput bi = createBasicInput();
        inputs_.add( bi );
        return bi;
    }

    /**
     * Creates a BasicInput object without throwing an exception.
     * If it fails, a dummy instance that will throw a suitable exception
     * when in use is returned.
     *
     * @return   new basic input
     */
    private BasicInput createBasicInput() {
        try {
            return inputFact_.createInput( isSeq_ );
        }
        catch ( final IOException e ) {
            return new FailureBasicInput( e, isSeq_ );
        }
    }

    public synchronized void close() { 
        for ( Iterator<BasicInput> it = inputs_.iterator(); it.hasNext(); ) {
            try {
                it.next().close();
            }
            catch ( IOException e ) {
                // never mind
            }
            it.remove();
        }
    }

    /**
     * BasicInput instance that responds to most method invocations
     * by throwing a previously supplied exception.
     */
    private static class FailureBasicInput implements BasicInput {
        private final IOException err_;
        private final boolean isSeq_;

        /**
         * Constructor.
         *
         * @param  err  exception on which to base thrown ones
         * @param  isSeq    true if created inputs are sequential,
         *                  false for random
         */
        FailureBasicInput( IOException err, boolean isSeq ) {
            isSeq_ = isSeq;
            err_ = err;
        }
        public byte readByte() throws IOException {
            throw failure();
        }
        public short readShort() throws IOException {
            throw failure();
        }
        public int readInt() throws IOException {
            throw failure();
        }
        public long readLong() throws IOException {
            throw failure();
        }
        public float readFloat() throws IOException {
            throw failure();
        }
        public double readDouble() throws IOException {
            throw failure();
        }
        public void skip( long nbyte ) throws IOException {
            throw failure();
        }
        public boolean isRandom() {
            return isSeq_;
        }
        public void seek( long offset ) throws IOException {
            throw failure();
        }
        public long getOffset() {
            return 0;
        }
        public void close() {
        }
        private IOException failure() {
            return (IOException)
                   new IOException( "Input creation failed" ).initCause( err_ );
        }
    }
}
