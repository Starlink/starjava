package uk.ac.starlink.topcat;

/**
 * Aggregates an integer with a label.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2025
 */
public class LabelledCount {

    private final String label_;
    private final long count_;

    /**
     * Constructor.
     *
     * @param  label   label
     * @param  count   count
     */
    public LabelledCount( String label, long count ) {
        label_ = label;
        count_ = count;
    }

    /**
     * Returns the label.
     *
     * @return  label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the count.
     *
     * @return  count; negative values indicate a missing value
     */
    public long getCount() {
        return count_;
    }
}
