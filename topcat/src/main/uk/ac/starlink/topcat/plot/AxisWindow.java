package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JToolBar;
import javax.swing.BorderFactory;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.HelpAction;

/**
 * Dialogue window containing one or more {@link AxisEditor} components.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2006
 */
public class AxisWindow extends JDialog {

    private final AxisEditor[] editors_;
    private final Action applyAction_;
    private final Action cancelAction_;
    private final Action okAction_;
    private final ActionForwarder forwarder_;
    private final Frame parent_;
    private boolean seen_;

    /**
     * Constructor.
     *
     * @param  parent   owner frame
     * @param  editors   AxisEditor components to place
     */
    public AxisWindow( Frame parent, AxisEditor[] editors ) {
        super( parent );
        parent_ = parent;
        setTitle( "Axis Configuration" );
        editors_ = editors;
        int naxis = editors.length;
        forwarder_ = new ActionForwarder();

        /* Configure and place main container. */
        JComponent main = Box.createVerticalBox();
        main.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( main, BorderLayout.CENTER );

        /* Configure and place action buttons. */
        applyAction_ = new AxisAction( "Apply" );
        cancelAction_ = new AxisAction( "Cancel" );
        okAction_ = new AxisAction( "OK" );
        JComponent controlBox = Box.createHorizontalBox();
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( cancelAction_ ) );
        controlBox.add( Box.createHorizontalStrut( 10 ) );
        controlBox.add( new JButton( applyAction_ ) );
        controlBox.add( Box.createHorizontalStrut( 10 ) );
        controlBox.add( new JButton( okAction_ ) );
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.setBorder( BorderFactory
                             .createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().add( controlBox, BorderLayout.SOUTH );
        
        /* Add help button. */
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable( false );
        toolBar.add( Box.createHorizontalGlue() );
        toolBar.add( new HelpAction( "axisConfig", this ) );
        toolBar.addSeparator();
        getContentPane().add( toolBar, BorderLayout.NORTH );

        /* Place editors. */
        for ( int i = 0; i < naxis; i++ ) {
            main.add( editors[ i ] );
        }
        pack();
    }

    /** 
     * Returns the constituent AxisEditor components of this window.
     *
     * @return  axis editors
     */
    public AxisEditor[] getEditors() {
        return editors_;
    }

    /**
     * Clears the upper and lower data limits for all the constituent
     * axis editor components.
     */
    public void clearRanges() {
        for ( int i = 0; i < editors_.length; i++ ) {
            editors_[ i ].setRange( Double.NaN, Double.NaN );
        }
    }

    /**
     * Registers a listener to be notified when the OK or Apply action
     * is invoked on this window.  Note this will not receive notifications
     * from the constituent AxisEditor components themselves.
     *
     * @param  listener listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addListener( listener );
    }

    /**
     * Unregisters a listener added by {@link #addActionListener}.
     *
     * @param  listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeListener( listener );
    }

    public void show() {
        if ( ! seen_ ) {
            positionNear( parent_ );
            seen_ = true;
        }
        super.show();
    }

    /**
     * Positions this dialogue somewhere suitable near its parent.
     *
     * @param  parent  window to sit next to
     */
    private void positionNear( Frame parent ) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if ( gc.equals( parent.getGraphicsConfiguration() ) ) {
            Rectangle ppos = parent.getBounds();
            int bot = ppos.y + ppos.height;
            int side = ppos.x;
            Rectangle dpos = getBounds();
            int y = bot - dpos.height;
            int x = Math.max( side - dpos.width + 10, 0 );
            setLocation( x, y );
        }
    }

    /**
     * Implements button actions for this window.
     */
    private class AxisAction extends AbstractAction {
        AxisAction( String name ) {
            super( name );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == okAction_ || this == applyAction_ ) {
                forwarder_.actionPerformed( evt );
            }
            if ( this == okAction_ || this == cancelAction_ ) {
                dispose();
            }
        }
    }
}
