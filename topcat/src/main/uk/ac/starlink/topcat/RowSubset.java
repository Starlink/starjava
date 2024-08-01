package uk.ac.starlink.topcat;

/**
 * Defines a selection of rows in a table model.
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class RowSubset {

    private String name_;
    private Key key_;

    /**
     * A subset containing all rows (<code>isIncluded</code> always true).
     */
    public static RowSubset ALL = new RowSubset( "All" ) {
        public boolean isIncluded( long lrow ) {
            return true;
        }
    };

    /**
     * A subset containing no rows (<code>isIncluded</code> always false).
     */
    public static RowSubset NONE = new RowSubset( "None" ) {
        public boolean isIncluded( long lrow ) {
            return false;
        }
    };

    /**
     * Constructor.
     *
     * @param   name  subset name
     */
    @SuppressWarnings("this-escape")
    public RowSubset( String name ) {
        name_ = name;
        key_ = new Key( TopcatUtils.identityString( this ) );
    }

    /**
     * Returns the name of this subset.
     *
     * @return name
     */
    public String getName() {
        return name_;
    }

    /**
     * Sets the name of this subset.
     *
     * @param  name  new name
     */
    public void setName( String name ) {
        name_ = name;
    }

    /**
     * Returns the key identifying this subset.
     * This value is intended for use by the GUI; only one subset in use
     * may have the same key at any one time, but if a subset goes out of use,
     * its key may be passed on to a different one that is intended as
     * a replacement that should inherit configuration set up for the
     * original owner.
     *
     * @return   identifer
     */
    public Key getKey() {
        return key_;
    }

    /**
     * Sets the key identifying this subset.
     * A key no longer in use may be passed on to a new subset intended
     * as its replacement.
     *
     * @param  key  new key
     */
    public void setKey( Key key ) {
        key_ = key;
    }

    /**
     * Indicates whether a given row is in the subset or not.
     *
     * @param  lrow  the index of the row in question
     * @return  <code>true</code> iff row <code>lrow</code> is to be included
     */
    public abstract boolean isIncluded( long lrow );

    /**
     * Returns the mask identifier by which the content of this subset
     * is recognised.
     *
     * <p>In particular this value is used as a
     * {@link uk.ac.starlink.ttools.plot2.data.DataSpec#getMaskId maskId} by the
     * {@link uk.ac.starlink.topcat.plot2.GuiDataSpec GuiDataSpec} class,
     * which means that changing it will generally signal to the plotting
     * system that the content of this subset has changed, and thus provoke
     * a replot of layers dependent on it.
     *
     * <p>The default implementation returns a value determined
     * by the identity of this RowSubset object
     * ({@link TopcatUtils#identityString}),
     * but this method may be overridden by subclasses that wish to
     * signal their changes, and in particular provoke replots,
     * according to state.
     * Implementations are not however obliged to make this value
     * reflect their internal state, especially if it would be
     * expensive to do so.  Implementations should be fast.
     *
     * @return  mask content identifier
     */
    public String getMaskId() {
        return TopcatUtils.identityString( this );
    }

    /**
     * Returns this subset's name.
     */
    public String toString() {
        return getName();
    }

    /**
     * Class used as subset identifier.
     */
    public static class Key {

        private final String key_;

        /**
         * Constructor.
         *
         * @param  key  key text
         */
        private Key( String key ) {
            key_ = key;
        }

        @Override
        public int hashCode() {
            return key_.hashCode();
        }

        @Override
        public boolean equals( Object o ) {
            return o instanceof Key && ((Key) o).key_.equals( this.key_ );
        }
    }
}
