package uk.ac.starlink.topcat.plot;

/**
 * Characterises a plottable set of points selected within a PointSelector.
 * Instances of this class are used as labels to keep track of plotting sets.
 * 
 * @author   Mark Taylor
 * @since    11 Jan 2005
 */
public class SetId {

    private final PointSelector psel_;
    private final int iset_;

    /**
     * Constructor.
     *
     * @param   psel  point selector owning the set
     * @param   iset  index of the set within <code>psel</code>
     */
    public SetId( PointSelector psel, int iset ) {
        psel_ = psel;
        iset_ = iset;
    }

    /**
     * Returns the PointSelector which generated this ID.
     *
     * @return  point selector
     */
    public PointSelector getPointSelector() {
        return psel_;
    }

    /**
     * Returns the index of the set within its PointSelector which identifies
     * this ID.
     *
     * @return   set index
     */
    public int getSetIndex() {
        return iset_;
    }

    public boolean equals( Object o ) {
        if ( o instanceof SetId ) {
            SetId other = (SetId) o;
            return other.psel_ == this.psel_
                && other.iset_ == this.iset_;
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return psel_.hashCode() * 23 + iset_;
    }
}
