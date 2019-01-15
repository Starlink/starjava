package uk.ac.starlink.ttools.plot2.layer;

/**
 * BinList implementation that starts off using a hash-based implementation,
 * but switches to an array-based implementation if the hash gets full
 * enough to make it worth while.
 *
 * @author   Mark Taylor
 * @since    15 Jan 2019
 */
public class AdaptiveBinList implements BinList {

    private final int size_;
    private final Combiner combiner_;
    private final int binThresh_;
    private int isub_;
    private HashBinList hlist_;
    private BinList base_;

    /**
     * Constructor.
     * The <code>factThresh</code> tuning parameter should be set to a
     * value close to the ratio of HashBinList bin size to ArrayBinList
     * bin size (in terms of storage).  Object overhead is typically
     * 2 words (16 bytes), so if the array has one double per bin and
     * the hash has one (Long,Combiner.Container) pair per bin,
     * the ratio will be at least 6.
     *
     * @param  size       maximum number of bins
     * @param  combiner   combiner
     * @param  factThresh  thershold factor - once size/factThresh bins
     *                     are occupied, an array will be used instead
     */
    public AdaptiveBinList( int size, Combiner combiner, int factThresh ) {
        size_ = size;
        combiner_ = combiner;
        hlist_ = new HashBinList( size, combiner );
        base_ = hlist_;
        binThresh_ = size / factThresh;
    }                                        

    public Combiner getCombiner() {
        return combiner_;
    }       

    public long getSize() {
        return size_;
    }   

    public void submitToBin( long index, double datum ) {

        /* One time only, when the hash size threshold is exceeded,
         * try to replace the hash implementation with an array
         * implementation. */
        if ( hlist_ != null && ++isub_ % 1024 == 0 &&
             hlist_.getMap().size() > binThresh_ ) {
            BinList alist = ArrayBinList.fromHashBinList( hlist_ );
            hlist_ = null;
            if ( alist != null ) {
                base_ = alist;
            }
        }
        base_.submitToBin( index, datum );
    }

    public BinList.Result getResult() {
        return base_.getResult();
    }

    /**
     * Indicates which underlying BinList implementation is currently in use.
     * It starts off true at object construction time, but may turn false
     * if enough data are submitted.  It will never change back again.
     *
     * @return   true if underlying bin list is hash-based,
     *           false if it's array-based
     */
    public boolean isHash() {
        return hlist_ != null;
    }
}
