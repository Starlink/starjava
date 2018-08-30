package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component which allows the user to identify a polygonal region
 * using the mouse.  Various polygonal modes are available.
 *
 * @author  Mark Taylor
 * @since   14 Sep 2018
 */
public abstract class PolygonPanel extends JComponent {

    private final PlotPanel plotPanel_;
    private final PolygonListener polygonListener_;
    private final Action basicPolygonAction_;
    private final ModePolygonAction[] modePolygonActions_;
    private ModeEnquiryPanel enquiryPanel_;
    private boolean isActive_;
    private PolygonMode pmode_;
    private List<Point> points_;
    private int zoneIndex_;
    private Point activePoint_;
    private static final Color fillColor_ = new Color( 0, 0, 0, 64 );
    private static final Color pathColor_ = new Color( 0, 0, 0, 128 );
    private static final PolygonMode DFLT_POLYGON_MODE = PolygonMode.BELOW;

    /**
     * Constructor.
     *
     * @param  plotPanel   plot panel
     */
    public PolygonPanel( PlotPanel plotPanel ) {
        plotPanel_ = plotPanel;
        setOpaque( false );
        polygonListener_ = new PolygonListener();

        /* Abort current polygon in the event of a resize. */
        addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent evt ) {
                setActive( false );
            }
        } );

        /* Prepare an action which allows polygon drawing using a mode
         * acquired from user interaction.
         * The same action completes the drawing. */
        basicPolygonAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( isActive() ) {
                    if ( ! points_.isEmpty() ) {
                        polygonCompleted( points_.toArray( new Point[ 0 ] ),
                                          pmode_, zoneIndex_ );
                    }
                    else {
                        setActive( false );
                    }
                }
                else {
                    setActive( true );
                    PolygonMode pmode = enquireMode();
                    if ( pmode != null ) {
                        pmode_ = pmode;
                    }
                    else {
                        setActive( false );
                    }
                }
            }
        };

        /* Prepare per-mode actions.  Clicking on the basic action
         * completes the drawing. */
        List<ModePolygonAction> modeActs = new ArrayList<ModePolygonAction>();
        for ( PolygonMode pmode : PolygonMode.values() ) {
            modeActs.add( new ModePolygonAction( pmode ) );
        }
        modePolygonActions_ = modeActs.toArray( new ModePolygonAction[ 0 ] );

        /* Initialise the action and this component. */
        clear();
        setActive( false );
    }

    /**
     * Resets the current polygon to an empty one.
     */
    public void clear() {
        points_ = new ArrayList<Point>();
        activePoint_ = null;
        zoneIndex_ = -1;
        repaint();
    }

    /**
     * Returns the action for drawing a polygon with the default mode.
     * This is also used to complete a polygon.
     *
     * @return  action
     */
    public Action getBasicPolygonAction() {
        return basicPolygonAction_;
    }

    /**
     * Returns a menu of options with one item for each polygon mode.
     *
     * @return  mode-specific polygon action menu
     */
    public JMenuItem getModePolygonMenu() {
        JMenu menu = new JMenu( "Draw Subset Polygons" );
        menu.setIcon( ResourceIcon.POLY_SUBSET );
        menu.setToolTipText( "Options to draw polygons on the plot defining "
                           + "row subsets in various modes" );
        for ( Action act : modePolygonActions_ ) {
            menu.add( act );
        }
        return menu;
    }

    /**
     * Sets whether this panel is active (visible, accepting mouse gestures,
     * drawing shapes) or inactive (invisible).
     *
     * @param   active  true to select activeness
     */
    public void setActive( boolean active ) {
        if ( active != isActive_ ) {
            clear();
        }
        isActive_ = active;
        basicPolygonAction_.putValue( Action.NAME,
                                      active ? "Finish Drawing Polygon"
                                             : "Draw Subset Polygon" );
        basicPolygonAction_.putValue( Action.SMALL_ICON,
                                      active ? ResourceIcon.POLY_SUBSET_END
                                             : ResourceIcon.POLY_SUBSET );
        basicPolygonAction_.putValue( Action.SHORT_DESCRIPTION,
                                      active ? "Define susbset from " +
                                               "currently-drawn polygon"
                                             : "Draw a polygon on the plot " +
                                               "to define a new row subset" );
        for ( ModePolygonAction act : modePolygonActions_ ) {
            act.updateState();
        }
        setListening( active );
        setVisible( active );
    }

    /**
     * Changes whether this component is listening to mouse gestures to
     * modify the shape.  This method is called by <code>setActive</code>,
     * but may be called independently of it as well.
     *
     * @param  isListening   whether mouse gestures can affect current shape
     */
    public void setListening( boolean isListening ) {
        if ( isListening ) {
            addMouseListener( polygonListener_ );
            addMouseMotionListener( polygonListener_ );
        }
        else {
            removeMouseListener( polygonListener_ );
            removeMouseMotionListener( polygonListener_ );
        }
    }

    /**
     * Indicates whether this component is currently active.
     *
     * @return   true iff this component is active (visible and drawing)
     */
    public boolean isActive() {
        return isActive_;
    }

    /**
     * Invoked when this component's action is invoked to terminate a
     * polygon drawing session.  Implementations of this method are expected
     * to clear up by calling <code>setActive(false)</code> when the
     * polygon representation is no longer required.
     *
     * @param  points  completed polygon
     * @param  pmode   polygon mode
     * @param  zoneIndex   index of the plot zone in which the polygon
     *                     is considered to exist
     */
    protected abstract void polygonCompleted( Point[] points, PolygonMode pmode,
                                              int zoneIndex );

    @Override
    protected void paintComponent( Graphics g ) {
        if ( zoneIndex_ < 0 ) {
            return;
        }

        /* Save state. */
        Color color0 = g.getColor();
        Shape clip0 = g.getClip();
        Graphics2D g2 = (Graphics2D) g;

        /* Clip to the current zone. */
        Surface surf = plotPanel_.getLatestSurface( zoneIndex_ );
        if ( surf != null ) {
            g2.clip( surf.getPlotBounds() );
        }

        /* Fill the defined region. */
        Area area = pmode_.createArea( getBounds(),
                                       points_.toArray( new Point[ 0 ] ) );
        if ( area != null ) {
            g2.setColor( fillColor_ );
            g2.fill( area );
        }

        /* Draw lines joining up the points so far added. */
        int np = points_.size();
        int np1 = activePoint_ == null ? np : np + 1;
        int[] xs = new int[ np1 ];
        int[] ys = new int[ np1 ];
        for ( int i = 0; i < np; i++ ) {
            Point p = points_.get( i );
            xs[ i ] = p.x;
            ys[ i ] = p.y;
        }
        if ( activePoint_ != null ) {
            xs[ np ] = activePoint_.x;
            ys[ np ] = activePoint_.y;
        }
        g2.setColor( pathColor_ );
        g2.drawPolyline( xs, ys, np1 );

        /* Restore state. */
        g.setColor( color0 );
        g.setClip( clip0 );
    }

    /**
     * Indicates whether a given graphics position is a point that's allowed
     * to be added to the polygon currently under construction.
     *
     * @param  p  point
     * @return  true iff it's OK to add it
     */
    private boolean isPointLegal( Point p ) {

        /* Throw out unsuitable values. */
        if ( p == null ) {
            return false;
        }
        Rectangle bounds = getBounds();
        if ( ! bounds.contains( p ) ) {
            return false;
        }

        /* If no shape yet, it's OK to start one. */
        if ( pmode_.createArea( bounds,
                                points_.toArray( new Point[ 0 ] ) ) == null ) {
            return true;
        }

        /* Otherwise, make sure we're not invalidating an existing shape. */
        int np = points_.size();
        Point[] tps = points_.toArray( new Point[ np + 1 ] );
        tps[ np ] = p;
        return pmode_.createArea( bounds, tps ) != null;
    }

    /**
     * Asks the user for a PolygonMode.  This method pops up a dialog
     * that also takes the opportunity to explain how to work the
     * polygon drawing.
     *
     * @return   selected mode, or null if operation is cancelled
     */
    private PolygonMode enquireMode() {

        /* Lazily create the dialogue contents. */
        if ( enquiryPanel_ == null ) {
            enquiryPanel_ = new ModeEnquiryPanel();
        }

        /* Create the dialogue window itself. */
        final Integer okOption = new Integer( JOptionPane.OK_OPTION );
        final JOptionPane optionPane =
             new JOptionPane( enquiryPanel_, JOptionPane.QUESTION_MESSAGE,
                              JOptionPane.OK_CANCEL_OPTION );
        final JDialog dialog = optionPane.createDialog( this, "Polygon Mode" );

        /* Position the dialogue in the center of the plotting surface. */
        dialog.pack();
        Point pos = this.getLocation();
        SwingUtilities.convertPointToScreen( pos, this );
        pos.x += ( this.getWidth() - dialog.getWidth() ) / 2;
        pos.y += ( this.getHeight() - dialog.getHeight() ) / 2;
        dialog.setLocation( pos );

        /* Pop up the dialogue and wait for a response.
         * If the user selects a mode, it will have the effect of choosing
         * that mode and closing the dialogue immediately,
         * rather than having to hit the OK button as well. */
        ActionListener listener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                optionPane.setValue( okOption );
                dialog.dispose();
            }
        };
        enquiryPanel_.modeSelector_.addActionListener( listener );
        dialog.setVisible( true );
        enquiryPanel_.modeSelector_.removeActionListener( listener );

        /* Return the selected value or null if the user cancelled. */
        return okOption.equals( optionPane.getValue() )
             ? enquiryPanel_.getSelectedMode()
             : null;
    }

    /**
     * Component that explains briefly how to use polygon drawing,
     * and provides a selector which the user can use to choose
     * which PolygonMode will be used.
     */
    private class ModeEnquiryPanel extends JPanel {
        private final JComboBox modeSelector_;
 
        /**
         * Constructor.
         */
        ModeEnquiryPanel() {
            setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
            modeSelector_ = new JComboBox( PolygonMode.values() );
            modeSelector_.setSelectedItem( DFLT_POLYGON_MODE );
            addl( new JLabel( "Click on points in plot to outline a shape." ) );
            addl( new JLabel( "Right-click/CTRL-click removes last point." ) );
            JLabel completeLabel =
                 new JLabel( "When complete, click action button again: " );
            completeLabel.setIcon( ResourceIcon.POLY_SUBSET_END );
            completeLabel.setHorizontalTextPosition( SwingConstants.LEADING );
            addl( completeLabel );
            JComponent selectLine = Box.createHorizontalBox();
            selectLine.add( new JLabel( "Point inclusion mode: " ) );
            selectLine.add( new ShrinkWrapper( modeSelector_ ) );
            addl( selectLine );
        }

        /**
         * Returns the currently selected polygon mode.
         *
         * @return  selected mode (not null)
         */
        public PolygonMode getSelectedMode() {
            return (PolygonMode) modeSelector_.getSelectedItem();
        }

        /**
         * Adds a component to this box with left alignment.
         *
         * @param  c  component to add
         */
        private void addl( JComponent c ) {
            c.setAlignmentX( 0 );
            add( c );
        }
    }

    /**
     * Mouse listener that reacts to mouse events by adding new points
     * to this panel's list as appropriate.
     */
    private class PolygonListener extends MouseAdapter {

        @Override
        public void mouseMoved( MouseEvent evt ) {
            Point p = evt.getPoint();
            activePoint_ = isPointLegal( p ) ? p : null;
            repaint();
        }

        @Override
        public void mouseExited( MouseEvent evt ) {
            activePoint_ = null;
            repaint();
        }

        @Override
        public void mouseClicked( MouseEvent evt ) {
            int iButt = PlotUtil.getButtonChangedIndex( evt );
            if ( iButt == 1 ) {
                Point p = evt.getPoint();
                if ( isPointLegal( p ) ) {
                    points_.add( p );
                    activePoint_ = null;
                }
            }
            else if ( iButt == 3 && points_.size() > 0 ) {
                points_.remove( points_.size() - 1 );
            }
            if ( points_.size() == 0 ) {
                zoneIndex_ = -1;
            }
            else if ( zoneIndex_ < 0 ) {
                zoneIndex_ = plotPanel_.getZoneIndex( evt.getPoint() );
            }
            repaint();
        }
    }

    /**
     * PolygonMode-specific drawing start action.
     */
    private class ModePolygonAction extends AbstractAction {
        private final PolygonMode actPmode_;

        /**
         * Constructor.
         *
         * @param  pmode  polygon mode
         */
        ModePolygonAction( PolygonMode pmode ) {
            super( "Draw Subset Polygon: " + pmode, ResourceIcon.POLY_SUBSET );
            actPmode_ = pmode;
            putValue( SHORT_DESCRIPTION,
                      "Draw a polygon on the plot to define a row subset "
                    + pmode + " the drawn region" );
            addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    if ( "enabled".equals( evt.getPropertyName() ) ) {
                        updateState();
                    }
                }
            } );
            updateState();
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( ! isActive() ) {
                PolygonPanel.this.pmode_ = actPmode_;
                PolygonPanel.this.setActive( true );
            }
        }
        void updateState() {
            setEnabled( basicPolygonAction_.isEnabled() && ! isActive() );
        }
    }
}
