package uk.ac.starlink.util.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.ViewportLayout;

/**
 * This is a tweaked ViewportLayout implementation to work round an issue
 * with scrollbar positioning.
 *
 * <p>The problem arises when you have scrollbar policies
 * VERTICAL_SCROLLBAR_AS_NEEDED and HORIZONTAL_SCROLLBAR_NEVER.
 * When the vertical scrollbar appears, it is not accounted for
 * in the preferred size of the scrollpane component.
 * This can, depending on the parent layout, lead to the right
 * hand side of the view component being obscured by the scrollbar.
 *
 * <p>You can apparently work round the problem with this class, by doing
 * <pre>
 *    scrollPane.getViewport().setLayout(new ConstraintedViewportLayout());
 * </pre>
 * I found this solution at
 * <a href="https://stackoverflow.com/questions/11587292/jscrollpane-not-wide-enough-when-vertical-scrollbar-appears">stack overflow</a>.
 * I don't really understand why it works.
 *
 * @author   Mark Taylor
 * @author   https://stackoverflow.com/users/463018/meyertee
 * @since    27 Nov 2017
 */
public class ConstrainedViewportLayout extends ViewportLayout {
    @Override
    public Dimension preferredLayoutSize( Container parent ) {
        Dimension preferredViewSize = super.preferredLayoutSize( parent );
        Container viewportContainer = parent.getParent();
        if ( viewportContainer != null ) {
            Dimension parentSize = viewportContainer.getSize();
            Insets parentInsets = viewportContainer.getInsets();
            preferredViewSize.height =
                parentSize.height - parentInsets.top - parentInsets.bottom;
        }
        return preferredViewSize;
    }
}
