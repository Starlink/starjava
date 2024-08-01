package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Contains two adjacent vertical JLists in the same component.  
 * This is a presentation component, not concerned with access to
 * the data in the contained lists.
 * Scrolling is taken care of.
 */
public class JList2 extends JPanel implements Scrollable {

    private final JList<?> list1_;
    private final JList<?> list2_;

    /**
     * Constructor.
     *
     * @param  list1  first list
     * @param  list2  second list
     */
    @SuppressWarnings("this-escape")
    public JList2( JList<?> list1, JList<?> list2 ) {
        super( new GridBagLayout() );
        list1_ = list1;
        list2_ = list2;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.weightx = 1;
        gbc.weighty = 0;
        add( list1_, gbc );
        gbc.gridy++;
        gbc.weighty = 1;
        add( list2_, gbc );
        setBackground( list1.getBackground() );
        setOpaque( true );
    }

    public Dimension getPreferredSize() {
        return combineSizes( list1_.getPreferredSize(),
                             list2_.getPreferredSize() );
    }

    public Dimension getMaximumSize() {
        return combineSizes( list1_.getMaximumSize(),
                             list2_.getMaximumSize() );
    }

    public Dimension getMinimumSize() {
        return combineSizes( list1_.getMinimumSize(),
                             list2_.getMinimumSize() );
    }

    public Dimension getPreferredScrollableViewportSize() {
        return combineSizes( list1_.getPreferredScrollableViewportSize(),
                             list2_.getPreferredScrollableViewportSize() );
    }

    public int getScrollableBlockIncrement( Rectangle visibleRect,
                                            int orientation, int direction ) {
        return list1_.getScrollableBlockIncrement( visibleRect, orientation,
                                                   direction );
    }

    public boolean getScrollableTracksViewportHeight() {
        Container parent = getParent();
        return parent instanceof JViewport
            && ((JViewport) parent).getHeight() > getPreferredSize().height;
    }

    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();
        return parent instanceof JViewport
            && ((JViewport) parent).getWidth() > getPreferredSize().width;
    }

    public int getScrollableUnitIncrement( Rectangle visibleRect,
                                           int orientation, int direction ) {
        return list1_.getScrollableUnitIncrement( visibleRect, orientation,
                                                  direction );
    }

    /**
     * Gives a size made by stacking two given sizes on top of each other.
     *
     * @param  d1  first size
     * @param  d2  second size
     */
    private Dimension combineSizes( Dimension d1, Dimension d2 ) {
        return new Dimension( Math.max( d1.width, d2.width ),
                              d1.height + d2.height );
    }
}
