package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import uk.ac.starlink.util.gui.CustomComboBoxRenderer;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component which allows the user to identify a region by selecting
 * vertices using the mouse.  Various figure modes are available.
 *
 * @author  Mark Taylor
 * @since   14 Sep 2018
 */
public abstract class FigurePanel extends JComponent {

    private final PlotPanel<?,?> plotPanel_;
    private final FigureMode[] figureModes_;
    private final boolean isDisplayExpr_;
    private final FigureListener figureListener_;
    private final Action basicFigureAction_;
    private final ModeFigureAction[] modeFigureActions_;
    private final FigureMode dfltMode_;
    private ModeEnquiryPanel enquiryPanel_;
    private boolean isActive_;
    private FigureMode currentMode_;
    private List<Point> points_;
    private int zoneIndex_;
    private Point activePoint_;
    private static final Color fillColor_ = new Color( 0, 0, 0, 64 );
    private static final Color pathColor_ = Color.BLACK;
    private static final Color vertexOutColor_ = Color.BLACK;
    private static final Color vertexInColor_ = new Color( 255, 255, 255, 128 );

    /* Name of boolean property associated with isActive method. */
    public static final String PROP_ACTIVE = "active";

    /**
     * Constructor.
     *
     * @param  plotPanel   plot panel
     * @param  figureModes   available modes
     * @param  isDisplayExpr  true to display current expression on screen
     */
    @SuppressWarnings("this-escape")
    public FigurePanel( PlotPanel<?,?> plotPanel, FigureMode[] figureModes,
                        boolean isDisplayExpr ) {
        plotPanel_ = plotPanel;
        figureModes_ = figureModes;
        isDisplayExpr_ = isDisplayExpr;
        dfltMode_ = figureModes[ 0 ];
        setOpaque( false );
        figureListener_ = new FigureListener();

        /* Abort current figure in the event of a resize. */
        addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent evt ) {
                setActive( false );
            }
        } );

        /* Prepare an action which allows figure drawing using a mode
         * acquired from user interaction.
         * The same action completes the drawing. */
        basicFigureAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( isActive() ) {
                    if ( ! points_.isEmpty() ) {
                        if ( zoneIndex_ >= 0 ) {
                            Surface surf =
                                plotPanel_.getLatestSurface( zoneIndex_ );
                            Point[] points = points_.toArray( new Point[ 0 ] );
                            Figure fig =
                                currentMode_.createFigure( surf, points );
                            if ( fig != null ) {
                                figureCompleted( fig, zoneIndex_ );
                            }
                        }
                    }
                    else {
                        setActive( false );
                    }
                }
                else {
                    setActive( true );
                    FigureMode fmode = enquireMode();
                    if ( fmode != null ) {
                        currentMode_ = fmode;
                    }
                    else {
                        setActive( false );
                    }
                }
            }
        };

        /* Prepare per-mode actions.  Clicking on the basic action
         * completes the drawing. */
        List<ModeFigureAction> modeActs = new ArrayList<ModeFigureAction>();
        for ( FigureMode fmode : figureModes_ ) {
            modeActs.add( new ModeFigureAction( fmode ) );
        }
        modeFigureActions_ = modeActs.toArray( new ModeFigureAction[ 0 ] );

        /* Initialise the action and this component. */
        clear();
        setActive( false );
    }

    /**
     * Resets the current figure to an empty one.
     */
    public void clear() {
        points_ = new ArrayList<Point>();
        activePoint_ = null;
        zoneIndex_ = -1;
        repaint();
    }

    /**
     * Returns the action for drawing a figure with the default mode.
     * This is also used to signal that drawing a figure is complete.
     *
     * @return  action
     */
    public Action getBasicFigureAction() {
        return basicFigureAction_;
    }

    /**
     * Returns a menu of options with one item for each figure mode.
     *
     * @return  mode-specific figure action menu
     */
    public JMenuItem getModeFigureMenu() {
        JMenu menu = new JMenu( "Draw Algebraic Subset" );
        menu.setIcon( ResourceIcon.POLY_SUBSET );
        menu.setToolTipText( "Options to draw figures on the plot defining "
                           + "row subsets by shape" );
        for ( Action act : modeFigureActions_ ) {
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
        basicFigureAction_.putValue( Action.NAME,
                                     active ? "Finish Drawing Subset"
                                            : "Draw Algebraic Subset" );
        basicFigureAction_.putValue( Action.SMALL_ICON,
                                     active ? ResourceIcon.POLY_SUBSET_END
                                            : ResourceIcon.POLY_SUBSET );
        basicFigureAction_.putValue( Action.SHORT_DESCRIPTION,
                                     active ? "Define susbset from " +
                                              "currently-drawn figure"
                                            : "Draw a shape on the plot " +
                                              "to define a new Row Subset" );
        basicFigureAction_.putValue( PROP_ACTIVE, Boolean.valueOf( active ) );
        for ( ModeFigureAction act : modeFigureActions_ ) {
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
            addMouseListener( figureListener_ );
            addMouseMotionListener( figureListener_ );
        }
        else {
            removeMouseListener( figureListener_ );
            removeMouseMotionListener( figureListener_ );
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
     * figure drawing session.  Implementations of this method are expected
     * to clear up by calling <code>setActive(false)</code> when the
     * figure representation is no longer required.
     *
     * @param  figure   completed figure, not null
     * @param  zoneIndex   index of the plot zone in which the figure
     *                     is considered to exist
     */
    protected abstract void figureCompleted( Figure figure, int zoneIndex );

    @Override
    protected void paintComponent( Graphics g ) {
        Surface surf = zoneIndex_ >= 0
                     ? plotPanel_.getLatestSurface( zoneIndex_ )
                     : null;
        if ( surf != null ) {
            Rectangle bounds = surf.getPlotBounds();
            Point[] points = points_.toArray( new Point[ 0 ] );

            /* Save state. */
            Color color0 = g.getColor();
            Shape clip0 = g.getClip();
            Graphics2D g2 = (Graphics2D) g;

            /* Clip to the current zone. */
            g2.clip( bounds );
            Figure fig0 = currentMode_.createFigure( surf, points );

            /* Fill the defined region. */
            if ( fig0 != null ) {
                g2.setColor( fillColor_ );
                g2.fill( fig0.getArea() );
            }

            /* Mark vertex positions. */
            for ( Point p : ( fig0 == null ? points : fig0.getVertices() ) ) {
                paintVertex( g2, p );
            }

            /* Draw lines joining up the points so far added. */
            List<Point2D> pathPoints = new ArrayList<Point2D>( points_ );
            if ( activePoint_ != null ) {
                pathPoints.add( activePoint_ );
            }
            Figure fig1 =
                currentMode_
               .createFigure( surf, pathPoints.toArray( new Point[ 0 ] ) );
            if ( fig1 != null ) {
                g2.setColor( pathColor_ );
                fig1.paintPath( g2 );
            }

            /* Display the generic expression at the bottom of the screen.
             * Use the figure corresponding to the current mouse position
             * if available, otherwise the most recently completed one. */
            if ( isDisplayExpr_ ) {
                Figure tfig = fig1 != null ? fig1 : fig0;
                if ( tfig != null ) {
                    String expr = tfig.getExpression();
                    if ( expr != null ) {
                        int pad = 4;
                        int tx = bounds.x + 1;
                        int ty = bounds.y + bounds.height - 2 * pad;
                        g2.translate( tx, ty );
                        g2.setColor( new Color( 0xa0ffffff, true ) );
                        FontMetrics fm = g2.getFontMetrics();
                        Rectangle box =
                            fm.getStringBounds( expr, g ).getBounds();
                        box.width += 2 * pad;
                        box.height += 2 * pad;
                        g2.fill( box );
                        g2.setColor( color0 );
                        g2.drawString( expr, pad, pad - fm.getMaxDescent() );
                        g2.translate( -tx, -ty );
                    }
                }
            }

            /* Restore state. */
            g.setColor( color0 );
            g.setClip( clip0 );
        }

        /* Display a marker under the mouse pointer, whether or not
         * the actual drawing has started; this is useful visual feedback
         * that you are in figure mode. */
        if ( activePoint_ != null ) {
            paintVertex( g, activePoint_ );
        }
    }

    /**
     * Marks the position of a user-chosen point that will define the
     * output figure.
     *
     * @param   g  graphics context
     * @param   p  vertex position
     */
    private void paintVertex( Graphics g, Point p ) {
        Color color0 = g.getColor();
        g.setColor( vertexOutColor_ );
        g.drawRect( p.x - 3, p.y - 3, 6, 6 );
        g.setColor( vertexInColor_ );
        g.fillRect( p.x - 2, p.y - 2, 5, 5 );
        g.setColor( color0 );
    }

    /**
     * Indicates whether a given graphics position is a point that's allowed
     * to be added to the figure currently under construction.
     *
     * @param  p  point
     * @return  true iff it's OK to add it
     */
    private boolean isPointLegal( Point p ) {

        /* Throw out unsuitable values. */
        if ( p == null ) {
            return false;
        }
        if ( ! getBounds().contains( p ) ) {
            return false;
        }
        int iz = zoneIndex_ >= 0 ? zoneIndex_ : plotPanel_.getZoneIndex( p );
        if ( iz < 0 ) {
            return false;
        }
        Surface surf = plotPanel_.getLatestSurface( iz );
        if ( ! surf.getPlotBounds().contains( p ) ||
             surf.graphicsToData( p, null ) == null ) {
            return false;
        }

        /* If no shape yet, it's OK to start one. */
        if ( currentMode_
            .createFigure( surf, points_.toArray( new Point[ 0 ] ) ) == null ) {
            return true;
        }

        /* Otherwise, make sure we're not invalidating an existing shape. */
        int np = points_.size();
        Point[] tps = points_.toArray( new Point[ np + 1 ] );
        tps[ np ] = p;
        return currentMode_.createFigure( surf, tps ) != null;
    }

    /**
     * Asks the user for a FigureMode.  This method pops up a dialog
     * that also takes the opportunity to explain how to work the
     * figure drawing.
     *
     * @return   selected mode, or null if operation is cancelled
     */
    private FigureMode enquireMode() {

        /* Lazily create the dialogue contents. */
        if ( enquiryPanel_ == null ) {
            enquiryPanel_ = new ModeEnquiryPanel();
        }

        /* Create the dialogue window itself. */
        final Integer okOption = Integer.valueOf( JOptionPane.OK_OPTION );
        final JOptionPane optionPane =
             new JOptionPane( enquiryPanel_, JOptionPane.QUESTION_MESSAGE,
                              JOptionPane.OK_CANCEL_OPTION );
        final JDialog dialog = optionPane.createDialog( this, "Figure Mode" );

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
     * Component that explains briefly how to use figure drawing,
     * and provides a selector which the user can use to choose
     * which FigureMode will be used.
     */
    private class ModeEnquiryPanel extends JPanel {
        private final JComboBox<FigureMode> modeSelector_;

        /**
         * Constructor.
         */
        ModeEnquiryPanel() {
            setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
            modeSelector_ = new JComboBox<FigureMode>( figureModes_ );
            modeSelector_.setRenderer(
                    new CustomComboBoxRenderer<FigureMode>() {
                @Override
                protected String mapValue( FigureMode figmode ) {
                    return figmode.getName();
                }
            } );
            modeSelector_.setSelectedItem( dfltMode_ );
            addl( new JLabel( "Click on points in plot to define a shape." ) );
            addl( new JLabel( "Right-click/CTRL-click removes last point." ) );
            JLabel completeLabel =
                 new JLabel( "When complete, click action button again: " );
            completeLabel.setIcon( ResourceIcon.POLY_SUBSET_END );
            completeLabel.setHorizontalTextPosition( SwingConstants.LEADING );
            addl( completeLabel );
            JComponent selectLine = Box.createHorizontalBox();
            selectLine.add( new JLabel( "Shape mode: " ) );
            selectLine.add( new ShrinkWrapper( modeSelector_ ) );
            addl( selectLine );
        }

        /**
         * Returns the currently selected figure mode.
         *
         * @return  selected mode (not null)
         */
        public FigureMode getSelectedMode() {
            return modeSelector_.getItemAt( modeSelector_.getSelectedIndex() );
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
    private class FigureListener extends MouseAdapter {

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
     * FigureMode-specific drawing start action.
     */
    private class ModeFigureAction extends AbstractAction {
        private final FigureMode actFmode_;

        /**
         * Constructor.
         *
         * @param  fmode  figure mode
         */
        ModeFigureAction( FigureMode fmode ) {
            super( "Draw Algebraic Subset: " + fmode.getName(),
                   ResourceIcon.POLY_SUBSET );
            actFmode_ = fmode;
            putValue( SHORT_DESCRIPTION,
                      "Draw a figure on the plot to define a row subset "
                    + "algebraically using mode " + fmode.getName() );
            basicFigureAction_.addPropertyChangeListener(
                    new PropertyChangeListener() {
                public void propertyChange( PropertyChangeEvent evt ) {
                    String pname = evt.getPropertyName();
                    if ( "enabled".equals( pname ) ||
                         PROP_ACTIVE.equals( pname ) ) {
                        updateState();
                    }
                }
            } );
            updateState();
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( ! isActive() ) {
                FigurePanel.this.currentMode_ = actFmode_;
                FigurePanel.this.setActive( true );
            }
        }
        void updateState() {
            setEnabled( basicFigureAction_.isEnabled() && ! isActive() );
        }
    }
}
