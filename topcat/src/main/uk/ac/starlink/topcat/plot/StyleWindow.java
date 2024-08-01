package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JToolBar;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.HelpAction;

/**
 * Dialogue which houses a StyleEditor.
 *
 * @author   Mark Taylor
 * @since    10 Jan 2006
 */
public class StyleWindow extends JDialog {

    private final Frame parent_;
    private final StyleEditor editor_;
    private final Action cancelAction_;
    private final Action applyAction_;
    private final Action okAction_;
    private ActionListener target_;
    private boolean seen_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );

    /**
     * Constructor.
     *
     * @param   parent  owner frame
     * @param   editor  style editor component
     */
    @SuppressWarnings("this-escape")
    public StyleWindow( Frame parent, StyleEditor editor ) {
        super( parent );
        parent_ = parent;
        editor_ = editor;

        /* Place editor. */
        editor_.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( editor, BorderLayout.CENTER );

        /* Add help button. */
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable( false );
        toolBar.add( Box.createHorizontalGlue() );
        toolBar.add( new HelpAction( editor_.getHelpID(), this ) );
        toolBar.addSeparator();
        getContentPane().add( toolBar, BorderLayout.NORTH );

        /* Construct and place action buttons. */
        Box buttonBox = Box.createHorizontalBox();
        cancelAction_ = new StyleAction( "Cancel" );
        applyAction_ = new StyleAction( "Apply" );
        okAction_ = new StyleAction( "OK" );
        editor_.addActionListener( applyAction_ );
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.add( new JButton( cancelAction_ ) );
        buttonBox.add( Box.createHorizontalStrut( 10 ) );
        buttonBox.add( new JButton( okAction_ ) );
        buttonBox.add( Box.createHorizontalGlue() );
        buttonBox.setBorder( BorderFactory
                            .createEmptyBorder( 10, 10, 10, 10 ) );
        getContentPane().add( buttonBox, BorderLayout.SOUTH );

        pack();
    }

    public void setVisible( boolean vis ) {    
        if ( vis && ! seen_ ) {
            positionNear( parent_ );
            seen_ = true;
        }
        super.setVisible( vis );
    }

    /**
     * Positions this dialogue in a sensible position relative to the parent.
     *
     * @param   parent  owner frame
     */
    private void positionNear( Frame parent ) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if ( gc.equals( parent.getGraphicsConfiguration() ) ) {
            Rectangle ppos = parent.getBounds();
            int bot = ppos.y + ppos.height;
            int side = ppos.x + ppos.width;
            Rectangle dpos = getBounds();
            Rectangle screen = gc.getBounds();
            int y = bot - dpos.height;
            int x = Math.min( side - 10, screen.width - dpos.width );
            setLocation( x, y );
        }
    }

    /**
     * Sets the target listener.  This listener will be notified whenever
     * a new style has been selected.  There is only one target for this
     * StyleWindow, so if this method is called with a different target
     * at a later date, the earlier target will no longer be informed.
     *
     * @param    target   style change listener
     */
    public void setTarget( ActionListener target ) {
        target_ = target;
    }

    /**
     * Returns the StyleEditor component which is doing the hard work for
     * this dialogue.
     *
     * @return  editor
     */
    public StyleEditor getEditor() {
        return editor_;
    }

    /**
     * Implements actions associated with this dialogue.
     */
    private class StyleAction extends AbstractAction {

        StyleAction( String name ) {
            super( name );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == cancelAction_ ) {
                editor_.cancelChanges();
            }
            if ( this == applyAction_ || this == okAction_ || 
                 this == cancelAction_ ) {
                if ( target_ != null ) {
                    target_.actionPerformed( evt );
                }
            }
            if ( this == cancelAction_ || this == okAction_ ) {
                dispose();
            }
        }
    }

}
