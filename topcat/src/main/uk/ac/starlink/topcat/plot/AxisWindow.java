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
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.BorderFactory;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.HelpAction;

/**
 * Dialogue window containing one or more {@link AxisEditor} components.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2006
 */
public class AxisWindow extends JDialog {

    private final Action applyAction_;
    private final Action cancelAction_;
    private final Action okAction_;
    private final ActionForwarder forwarder_;
    private final Frame parent_;
    private final JComponent edBox_;
    private final JTextField titleField_;
    private AxisEditor[] editors_;
    private boolean seen_;

    /**
     * Constructor.
     *
     * @param  parent  owner frame
     */
    @SuppressWarnings("this-escape")
    public AxisWindow( Frame parent ) {
        super( parent );
        parent_ = parent;
        setTitle( "Axis Configuration" );
        forwarder_ = new ActionForwarder();

        /* Configure and place main container. */
        JComponent entryBox = Box.createVerticalBox();
        entryBox.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( entryBox, BorderLayout.CENTER );

        /* Configure and place component for title setting. */
        JComponent titleBox = Box.createHorizontalBox();
        titleField_ = new JTextField();
        titleField_.addActionListener( forwarder_ );
        titleBox.add( new JLabel( "Title: " ) );
        titleBox.add( titleField_ );
        titleBox.setBorder( AuxWindow.makeTitledBorder( "Plot Title" ) );
        entryBox.add( titleBox );

        /* Configure and place container for axis configuration. */
        edBox_ = Box.createVerticalBox();
        entryBox.add( edBox_ );

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
     * Resets the list of axis editor components contained by this window.
     *
     * @param   editors   new editor list
     */
    public void setEditors( AxisEditor[] editors ) {
        edBox_.removeAll();
        editors_ = editors;
        for ( int i = 0; i < editors.length; i++ ) {
            edBox_.add( editors[ i ] );
        }
        pack();
    }

    /**
     * Clears the upper and lower data limits for all the constituent
     * axis editor components.
     */
    public void clearRanges() {
        for ( int i = 0; i < editors_.length; i++ ) {
            editors_[ i ].clearBounds();
        }
    }

    /**
     * Returns the title currently entered for the plot.
     *
     * @return   plot title
     */
    public String getPlotTitle() {
        return titleField_.getText();
    }

    /**
     * Registers a listener to be notified when the OK or Apply action
     * is invoked on this window.  Note this will not receive notifications
     * from the constituent AxisEditor components themselves.
     *
     * @param  listener listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Unregisters a listener added by {@link #addActionListener}.
     *
     * @param  listener   listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    public void setVisible( boolean vis ) {
        if ( vis && ! seen_ ) {
            positionNear( parent_ );
            seen_ = true;
        }
        super.setVisible( vis );
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
                for ( int i = 0; i < editors_.length; i++ ) {
                    editors_[ i ].updateRanges();
                }
                forwarder_.actionPerformed( evt );
            }
            if ( this == okAction_ || this == cancelAction_ ) {
                dispose();
            }
        }
    }
}
