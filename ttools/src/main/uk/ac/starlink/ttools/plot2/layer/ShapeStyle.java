package uk.ac.starlink.ttools.plot2.layer;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Style that combines information from a ShapeForm and a ShapeMode.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
@Equality
public class ShapeStyle implements Style {
    private final Outliner outliner_;
    private final Stamper stamper_;
    private final Icon legendIcon_;

    /**
     * Constructor.
     *
     * @param   outliner  outline shape of markers
     * @param   stamper   colouring for markers
     */
    public ShapeStyle( Outliner outliner, Stamper stamper ) {
        outliner_ = outliner;
        stamper_ = stamper;
        legendIcon_ = stamper.createLegendIcon( outliner );
    }

    /**
     * Returns the outline shape of this style.
     *
     * @return  outline shape
     */
    public Outliner getOutliner() {
        return outliner_;
    }

    /**
     * Returns the colouring for this style.
     *
     * @return  colour stamper
     */
    public Stamper getStamper() {
        return stamper_;
    }

    public Icon getLegendIcon() {
        return legendIcon_;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ShapeStyle ) {
            ShapeStyle other = (ShapeStyle) o;
            return this.outliner_.equals( other.outliner_ )
                && this.stamper_.equals( other.stamper_ );
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 99701;
        code = 23 * code + outliner_.hashCode();
        code = 23 * code + stamper_.hashCode();
        return code;
    }
}
