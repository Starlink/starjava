package uk.ac.starlink.ttools.plot2;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;

/**
 * Aggregates a plot style and a label to be paired together as one entry
 * in a plot legend.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
public class LegendEntry {
    private final Style style_;
    private final String label_;

    /**
     * Constructor.
     *
     * @param   style   dataset style
     * @param   label   dataset label
     */
    public LegendEntry( Style style, String label ) {
        style_ = style;
        label_ = label;
    }

    /**
     * Returns the icon associated with this entry.
     *
     * @return  icon
     */
    public Icon getIcon() {
        return style_.getLegendIcon();
    }

    /**
     * Returns the text label associated with this entry.
     *
     * @return  label
     */
    public String getLabel() {
        return label_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof LegendEntry ) {
            LegendEntry other = (LegendEntry) o;
            return this.style_.equals( other.style_ )
                && this.label_.equals( other.label_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 29119;
        code = 23 * code + style_.hashCode();
        code = 23 * code + label_.hashCode();
        return code;
    }
}
