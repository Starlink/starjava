package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import uk.ac.starlink.table.gui.TableLoadWorker;

/**
 * Component which displays a list of LoadWorker components, including
 * their progress bars and cancel buttons.
 * Suitable for scrolling vertically.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2010
 */
public class LoadWorkerStack extends JPanel implements Scrollable {

    private final Map<TableLoadWorker,JComponent> panelMap_;
    private int rowHeight_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public LoadWorkerStack() {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        panelMap_ = new HashMap<TableLoadWorker,JComponent>();
        setBackground( Color.WHITE );
    }

    /**
     * Adds a worker to this stack.
     *
     * @param  worker  worker
     * @param  icon    optional icon indicating table source
     */
    public void addWorker( TableLoadWorker worker, Icon icon ) {
        JComponent labLine = Box.createHorizontalBox();
        labLine.add( new JLabel( worker.getLoader().getLabel() ) );
        labLine.add( Box.createHorizontalGlue() );
        JComponent progLine = Box.createHorizontalBox();
        progLine.add( new JLabel( icon == null ? ResourceIcon.BLANK : icon ) );
        progLine.add( Box.createHorizontalStrut( 10 ) );
        progLine.add( worker.getProgressBar() );
        progLine.add( Box.createHorizontalStrut( 10 ) );
        progLine.add( new JButton( worker.getCancelAction() ) );
        JComponent panel = Box.createVerticalBox();
        panel.add( labLine );
        panel.add( progLine );
        panel.setBorder( BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder( 0, 0, 1, 0, Color.GRAY ),
            BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) ) );
        panelMap_.put( worker, panel );
        add( panel );
        if ( rowHeight_ <= 0 ) {
            rowHeight_ = panel.getPreferredSize().height;
        }
        revalidate();
    }

    /**
     * Removes a worker which was previously added to this stack.
     *
     * @param  worker   worker to remove
     */
    public void removeWorker( TableLoadWorker worker ) {
        JComponent panel = panelMap_.remove( worker );
        if ( panel != null ) {
            remove( panel );
            revalidate();
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension( 400, 120 );
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public int getScrollableUnitIncrement( Rectangle visRect, int orientation,
                                           int direction ) {
        if ( orientation == SwingConstants.HORIZONTAL ) {
            return 10;
        }
        else {
            return getRowHeight();
        }
    }

    public int getScrollableBlockIncrement( Rectangle visRect, int orientation,
                                            int direction ) {
        if ( orientation == SwingConstants.HORIZONTAL ) {
            return visRect.width;
        }
        else {
            return visRect.height;
        }
    }

    /**
     * Returns the current best guess for the height of a single worker 
     * representation in this stack.
     *
     * @return  row height in pixels
     */
    private int getRowHeight() {
        return rowHeight_ > 0 ? rowHeight_ : 32;
    }
}
