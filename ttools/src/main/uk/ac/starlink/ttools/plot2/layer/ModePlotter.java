package uk.ac.starlink.ttools.plot2.layer;

import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.Plotter;

/**
 * Plotter sub-interface that marks a family of plotters as having
 * similar characteristics.  These are used by the TOPCAT GUI to
 * group plotters together for display and user interaction.
 * The <code>Mode</code> and <code>Form</code> can in principle
 * be varied separately to form a family of plotters that can share
 * a similar UI if the mode and form implement known subinterfaces
 * of those interfaces.
 *
 * @author   Mark Taylor
 * @since    18 Feb 2013
 */
public interface ModePlotter<S extends Style> extends Plotter<S> {

    /**
     * Returns the mode of this plotter.
     *
     * @return  plotter mode
     */
    Mode getMode();

    /**
     * Returns the form of this plotter.
     *
     * @return  plotter form
     */
    Form getForm();

    /**
     * Marker interface for an object that is shared between plotters
     * sharing characteristics that make it useful to group them.
     * These characteristics may be encoded by the interface of the
     * Mode subclass.
     */
    public interface Mode {

        /**
         * Returns the user-directed name for this mode.
         *
         * @return  mode name
         */
        String getModeName();

        /**
         * Returns an icon to identify this mode in the GUI.
         *
         * @return   mode icon
         */
        Icon getModeIcon();
    }

    /**
     * Marker interface for an object that is shared between plotters
     * sharing characteristics that make it useful to group them.
     * These characteristics may be encoded by the interface of the
     * Form subclass.
     */
    public interface Form {

        /**
         * Returns the user-directed name for this form.
         *
         * @return  form name
         */
        String getFormName();

        /**
         * Returns an icon to identify this form in the GUI.
         *
         * @return   form icon
         */
        Icon getFormIcon();
    }
}
