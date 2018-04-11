package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;

/**
 * Utility class that extends Box so that added components are always
 * considered to have a fixed alignment.  This is useful if you want,
 * for instance, all the components added to a vertical box to appear
 * aligned along the left hand side.
 *
 * @author   Mark Taylor
 * @since    11 Apr 2018
 * @see
 *   <a href="https://docs.oracle.com/javase/tutorial/uiswing/layout/box.html"
 *       >How to use BoxLayout</a>
 */
public class AlignedBox extends Box {

    private final float alignment_;

    /**
     * Constructor.
     *
     * @param   axis  one of the BoxLayout axis values
     * @param   alignment  alignment value applied to the perpendicular axis
     *                     of components added to this container
     */
    public AlignedBox( int axis, float alignment ) {
        super( axis );
        alignment_ = alignment;
    }

    @Override
    public void addImpl( Component c, Object constraints, int index ) {
        int axis = ((BoxLayout) getLayout()).getAxis();
        if ( c instanceof JComponent ) {
            JComponent jc = (JComponent) c;
            if ( axis == BoxLayout.X_AXIS || axis == BoxLayout.LINE_AXIS ) {
                jc.setAlignmentY( alignment_ );
            }
            else {
                jc.setAlignmentX( alignment_ );
            }
        }
        super.addImpl( c, constraints, index );
    }

    /**
     * Creates a vertical box in which all components added will have
     * alignmentX of zero.
     *
     * @return  vertical box with alignment along the left edge
     */
    public static AlignedBox createVerticalBox() {
        return new AlignedBox( BoxLayout.Y_AXIS, 0f );
    }

    /**
     * Creates a horizontal box in which all components added will have
     * alignmentY of zero.
     *
     * @return  horizontal box with alignment along the top edge
     */
    public static AlignedBox createHorizontalBox() {
        return new AlignedBox( BoxLayout.X_AXIS, 0f );
    }
}
