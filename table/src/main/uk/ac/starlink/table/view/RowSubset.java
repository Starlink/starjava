package uk.ac.starlink.table.view;

/**
 * Defines a selection of rows in a table model.
 * 
 * @author   Mark Taylor (Starlink)
 */
public interface RowSubset {

    /**
     * A subset containing all rows (<tt>isIncluded</tt> always true).
     */
    RowSubset ALL = new RowSubset() {
        public String getName() {
            return "All";
        }
        public String getExpression() {
            return "true";
        }
        public boolean isIncluded( long lrow ) {
            return true;
        }
    };

    /**
     * A subset containing no rows (<tt>isIncluded</tt> always false).
     */
    RowSubset NONE = new RowSubset() {
        public String getName() {
            return "None";
        }
        public String getExpression() {
            return "false";
        }
        public boolean isIncluded( long lrow ) {
            return false;
        }
    };

    /**
     * The name of this subset.
     *
     * @return name
     */
    String getName();

    /**
     * A string representation of the expression represented by this subset.
     *
     * @return  expression
     */
    String getExpression();

    /**
     * Indicates whether a given row is in the subset or not.
     *
     * @param  lrow  the index of the row in question
     * @return  <tt>true</tt> iff row <tt>lrow</tt> is to be included
     */
    boolean isIncluded( long lrow );
}
