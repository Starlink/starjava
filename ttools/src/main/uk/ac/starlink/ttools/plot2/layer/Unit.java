package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines a numeric factor corresponding to an extent on an axis.
 * Functionally, this is just a labelled double precision value,
 * but it has the semantics of a scaling factor along a plot axis.
 *
 * @author   Mark Taylor
 * @since    11 Jan 2018
 */
@Equality
public class Unit {

    private final String label_;
    private final String textName_;
    private final String symbol_;
    private final double extent_;
    private final String description_;

    /** Unit instance with value of unity. */
    public static final Unit UNIT =
        new Unit( "UNIT", "unit", "unit", 1.0, "dimensionless unit" );

    /**
     * Constructor.
     *
     * @param  label     text to appear in a selection interface
     * @param  textName  text to appear in user-directed descriptive text
     * @param  symbol    text to appear as unit metadata,
     *                   preferably compatible with the VOUnit standard
     * @param  extent   distance along an axis in some externally-defined units
     * @param  description  textual description to be included in XML
     */
    public Unit( String label, String textName, String symbol, double extent,
                 String description ) {
        label_ = label;
        textName_ = textName;
        symbol_ = symbol;
        extent_ = extent;
        description_ = description;
    }

    /**
     * Returns the label, suitable for user interface selection.
     *
     * @return  label
     */
    public String getLabel() {
        return label_;
    }

    /**
     * Returns the name suitable for use in descriptive text.
     *
     * @return  text name
     */
    public String getTextName() {
        return textName_;
    }

    /**
     * Returns the unit symbol, suitable for use in unit metadata.
     *
     * @return  symbol, preferably VOUnit-compatible
     */
    public String getSymbol() {
        return symbol_;
    }

    /**
     * Description text to be included in XML documentation.
     *
     * @return  XML-friendly descriptive text
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns the extent along an axis in some externally-defined units
     * represented by this object.
     *
     * @return  extent
     */
    public double getExtent() {
        return extent_;
    }

    @Override
    public int hashCode() {
        return Float.floatToIntBits( (float) extent_ );
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Unit ) {
            Unit other = (Unit) o;
            return this.extent_ == other.extent_;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the label.
     */
    @Override
    public String toString() {
        return label_;
    }
}
