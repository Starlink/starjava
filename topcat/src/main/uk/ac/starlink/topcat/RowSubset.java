package uk.ac.starlink.topcat;

/**
 * Defines a selection of rows in a table model.
 * 
 * @author   Mark Taylor (Starlink)
 */
public abstract class RowSubset {

    private String name_;

    /**
     * A subset containing all rows (<tt>isIncluded</tt> always true).
     */
    public static RowSubset ALL = new RowSubset( "All" ) {
        public boolean isIncluded( long lrow ) {
            return true;
        }
    };

    /**
     * A subset containing no rows (<tt>isIncluded</tt> always false).
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
    public RowSubset( String name ) {
        name_ = name;
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
     * Indicates whether a given row is in the subset or not.
     *
     * @param  lrow  the index of the row in question
     * @return  <tt>true</tt> iff row <tt>lrow</tt> is to be included
     */
    public abstract boolean isIncluded( long lrow );

    /**
     * Returns this subset's name.
     */
    public String toString() {
        return getName();
    }
}
