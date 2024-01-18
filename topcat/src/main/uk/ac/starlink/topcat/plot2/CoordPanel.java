package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;

/**
 * GUI component for entry of Coord values as table column expressions.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public interface CoordPanel {

    /**
     * Returns the coordinates which this panel is getting values for.
     *
     * @return  coords
     */
    Coord[] getCoords();

    /**
     * Returns this panel's config specifier.
     *
     * @return  specifier for config values, if there are any
     */
    ConfigSpecifier getConfigSpecifier();

    /**
     * Returns the graphical component for this object.
     *
     * @return  component
     */
    JComponent getComponent();

    /**
     * Adds a listener which will be notified when the coordinate selection
     * changes.
     *
     * @param  listener  listener
     */
    void addActionListener( ActionListener listener );

    /**
     * Removes a listener which was added previously.
     *
     * @param  listener  listener
     */
    void removeActionListener( ActionListener listener );

    /**
     * Sets the table with reference to which this panel will resolve
     * coordinate descriptions.
     *
     * <p>If the existing selected coordinate values still make sense
     * (if the new table has sufficiently compatible column names),
     * they are retained.  If the columns cannot be retained they are
     * cleared, and in that case if the <code>autopopulate</code> parameter
     * is set, some default columns will be used.
     *
     * @param  tcModel   table from which coordinate values will be drawn
     * @param  autoPopulate   whether to autopopulate columns when old ones
     *                        can't be used or are absent
     */
    void setTable( TopcatModel tcModel, boolean autoPopulate );

    /**
     * Returns the coordinate values currently selected in this panel.
     * If there is insufficient information to contribute to a plot
     * (not all of the
     * {@link uk.ac.starlink.ttools.plot2.data.Coord#isRequired required}
     * coord values are filled in)
     * then null will be returned.
     *
     * @return   nCoord-element array of coord contents, or null
     */
    GuiCoordContent[] getContents();

    /**
     * Returns the selector component model for a given user coordinate.
     * If no columndata-specific model has been set, null may be returned.
     *
     * @param  ic   coord index
     * @param  iu   user info index for the given coord
     * @return   selector model, or null
     */
    ColumnDataComboBoxModel getColumnSelector( int ic, int iu );

    /**
     * Returns a list of coordinates which do not correspond to the
     * selectors displayed here, but which should not be acquired by
     * other means.
     *
     * <p>This is a hack to work round situations when coordinates are
     * added into results by non-obvious means.  In most cases
     * the output result will be an empty array, which is what the
     * implementation in this class does.  But subclasses can override
     * it for special behaviour.
     *
     * @return   list of coords which this panel will arrange to provide
     *           values for in some non-standard way
     */
    Coord[] getAdditionalManagedCoords();

    /**
     * Returns the config map associated with this panel.
     *
     * @return   result of <code>getConfigSpecifier().getSpecifiedValue()</code>
     */
    default ConfigMap getConfig() {
        return getConfigSpecifier().getSpecifiedValue();
    }
}
