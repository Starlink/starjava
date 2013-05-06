package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicToolBarUI;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * Manages components which may either be contained in the same window,
 * or one of which may be floated out into a separate dialogue.
 *
 * @author   Mark Taylor
 * @since    25 Mar 2013
 */
public abstract class FloatManager {

    private final JComponent container_;
    private final JComponent fixedPanel_;
    private final JComponent floatablePanel_;

    /**
     * Constructor.
     *
     * @param  container   containing panel which contains one or both of
     *                     the others
     * @param  fixedPanel  component which is always inside container
     * @param  floatablePanel  component which may be inside container
     *                         or may be floated out of it
     */
    protected FloatManager( JComponent container, JComponent fixedPanel,
                            JComponent floatablePanel ) {
        container_ = container;
        fixedPanel_ = fixedPanel;
        floatablePanel_ = floatablePanel;
    }

    /**
     * Returns a toggle button model which can be used to control float status.
     * If this manager implementation is not controlled by a toggler,
     * the return value may be null.
     *
     * @return   float toggler, or null
     */
    public abstract ToggleButtonModel getFloatToggle();

    /**
     * Called to initialise this manager when the components are populated.
     */
    public abstract void init();

    /**
     * Returns the floatable panel.
     *
     * @return  floatable panel
     */
    protected JComponent getFloatablePanel() {
        return floatablePanel_;
    }

    /**
     * Configures the container panel for floating or non-floating status.
     * The floating panel itself is not affected.
     *
     * @param    floating  true for floating, false for not
     */
    protected void configureContainer( boolean floating ) {
        container_.removeAll();
        if ( floating ) {
            container_.add( fixedPanel_ );
        }
        else {
            JSplitPane splitter =
                    new JSplitPane( JSplitPane.VERTICAL_SPLIT,
                                    fixedPanel_, floatablePanel_ );
            splitter.setResizeWeight( 0.75 );
            splitter.setOneTouchExpandable( true );
            container_.add( splitter, BorderLayout.CENTER );
        }
        container_.validate();
    }

    /**
     * Returns an instance of this class.
     *
     * @param  container   containing panel which contains one or both of
     *                     the others
     * @param  fixedPanel  component which is always inside container
     * @param  floatablePanel  component which may be inside container
     *                         or may be floated out of it
     * @return   new float manager
     */
    public static FloatManager createFloatManager( JComponent container,
                                                   JComponent fixedPanel,
                                                   JComponent floatablePanel ) {
        return new ActionFloatManager( container, fixedPanel, floatablePanel );
    }

    /**
     * FloatManager implemenation which is controlled by explicit
     * toggle actions of the user.
     */
    private static class ActionFloatManager extends FloatManager {
        private final JComponent container_;
        private final JComponent floatablePanel_;
        private final ToggleButtonModel floatModel_;
        private JDialog floater_;

        /**
         * Constructor.
         *
         * @param  container   containing panel which contains one or both of
         *                     the others
         * @param  fixedPanel  component which is always inside container
         * @param  floatablePanel  component which may be inside container
         */
        ActionFloatManager( JComponent container, JComponent fixedPanel,
                            JComponent floatablePanel ) {
            super( container, fixedPanel, floatablePanel );
            container_ = container;
            floatablePanel_ = floatablePanel;
            floatModel_ =
                new ToggleButtonModel( "Float Controls", ResourceIcon.FLOAT,
                                       "Present plot controls in a floating "
                                     + "window rather than below the plot" );
            floatModel_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    placeControls();
                }
            } );
        }

        @Override
        public ToggleButtonModel getFloatToggle() {
            return floatModel_;
        }

        @Override
        public void init() {
            placeControls();
        }

        /**
         * Places the controls in appropriate windows.
         * They may go in the same window or in two separate windows according
         * to whether the control panel is currently requested to be floating.
         */       
        private void placeControls() {
            boolean external = floatModel_.isSelected();
            configureContainer( external );
            if ( floater_ != null ) {
                 floater_.getContentPane().removeAll();
                 floater_.dispose();
                floater_ = null;
            }
            if ( external ) {

                /* This should possibly be a JFrame rather than a JDialog.
                 * If it was a JFrame it could go under its controlling window,
                 * which might be useful for screen management.
                 * If so, I'd need to add a WindowListener to make sure that
                 * the floater closes and iconifies when the parent does.
                 * Any other Dialog behaviour I'd need to add by hand? */
                floater_ = createDialog( container_ );
                floater_.getContentPane().setLayout( new BorderLayout() );
                floater_.getContentPane().add( floatablePanel_ );
                floater_.pack();
                floater_.setVisible( true ); 
                floater_.addWindowListener( new WindowAdapter() {
                    public void windowClosing( WindowEvent evt ) {
                        floatModel_.setSelected( false ); 
                        placeControls();
                    }
                } );
            }
        }
    }

    /**
     * FloatManager which places the floatable component in a JToolBar.
     * The point of this is that JToolBars have floatable grab-handles
     * built into the GUI.
     * This could count as abuse of JToolBar, since nothing else about
     * this usage is toolbarish.
     * 
     * <p>This code is not quite working.  Maybe more effort could get it
     * going.
     *
     * @deprecated  Not working
     */
    private static class ToolbarFloatManager extends FloatManager {

        /**
         * Constructor.
         *
         * @param  container   containing panel which contains one or both of
         *                     the others
         * @param  fixedPanel  component which is always inside container
         * @param  floatablePanel  component which may be inside container
         */
        ToolbarFloatManager( JComponent container, JComponent fixedPanel,
                             JComponent floatablePanel ) {
            super( container, fixedPanel, new JToolBar() );
            final JToolBar bar = (JToolBar) getFloatablePanel();
            bar.setLayout( new BorderLayout() );
            bar.setFloatable( true );
            bar.removeAll();
            bar.add( floatablePanel );
            final BasicToolBarUI ui = (BasicToolBarUI) bar.getUI();
            bar.addHierarchyListener( new HierarchyListener() {
                boolean wasFloating;
                public void hierarchyChanged( HierarchyEvent evt ) {
                    if ( ( evt.getChangeFlags()
                           & HierarchyEvent.PARENT_CHANGED ) != 0 ) {
                        final boolean isFloating = ui.isFloating();
                        if ( isFloating ) {
                            setResizable( bar );
                        }
                        if ( isFloating ^ wasFloating ) {
                            wasFloating = isFloating;
                                    configureContainer( isFloating ); 
                        }
                    }
                }
            } );
        }

        @Override
        public void init() {
            configureContainer( false );
        }

        @Override
        public ToggleButtonModel getFloatToggle() {
            return null;
        }
    }

    /**
     * Returns a new dialogue parented from the containing window of a given
     * component.
     *
     * @param  comp   parent component
     * @return   new dialogue
     */
    private static JDialog createDialog( JComponent comp ) {
        Window win = SwingUtilities.windowForComponent( comp );
        if ( win instanceof Frame ) {
            return new JDialog( (Frame) win );
        }
        else if ( win instanceof Dialog ) {
            return new JDialog( (Dialog) win );
        }
        else {
            return new JDialog();
        }
    }

    /**
     * Sets the containing window of a given component.
     *
     * @param  comp   component
     */
    private static void setResizable( JComponent comp ) {
        Window win = SwingUtilities.windowForComponent( comp );
        if ( win instanceof Frame ) {
            ((Frame) win).setResizable( true );
        }
        else if ( win instanceof Dialog ) {
            ((Dialog) win).setResizable( true );
        }
    }
}
