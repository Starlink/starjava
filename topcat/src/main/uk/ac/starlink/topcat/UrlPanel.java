package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Displays a URL and options for invoking it.
 *
 * @author   Mark Taylor
 * @since    6 Feb 2018
 */
public class UrlPanel extends JPanel {

    private final UrlOptions urlopts_;
    private final JTextField urlField_;
    private final JComboBox<ResourceType> typeSelector_;
    private final JComboBox<UrlInvoker> invokeSelector_;
    private final ToggleButtonModel guessTypeModel_;
    private final ToggleButtonModel autoInvokeModel_;
    private final Action invokeAct_;
    private final JLabel statusLabel_;
    private final JTextField msgField_;
    private final boolean isSingleInvocationThread_;
    private ExecutorService queue_;
    private Future<?> lastJob_;
    private URL url_;
    private ResourceType guessType_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     * An auto-invoke button may optionally be added; note however that
     * this component will not auto-invoke the actions, that is the
     * responsibility of calling code based on the result of calling
     * the instance's {@link isAutoInvoke} method.
     *
     * @param  urlopts  options and defaults for URL invocation
     * @param  hasAutoInvoke      true iff an auto-invoke toggle button
     *                            should be displayed
     */
    public UrlPanel( UrlOptions urlopts, boolean hasAutoInvoke ) {
        super( new BorderLayout() );
        urlopts_ = urlopts;
        isSingleInvocationThread_ = true;
        guessTypeModel_ =
            new ToggleButtonModel( "Guess", null,
                                   "Guess resource type"
                                 + " from available information" );
        guessTypeModel_.setSelected( true );
        guessTypeModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( guessTypeModel_.isSelected() && guessType_ != null ) {
                    typeSelector_.setSelectedItem( guessType_ );
                }
            }
        } );
        autoInvokeModel_ =
            new ToggleButtonModel( "Auto-Invoke", null,
                                   "Actions will be invoked without "
                                 + "manual intervention" );
        autoInvokeModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateState();
            }
        } );
        autoInvokeModel_.setSelected( false );
        JComponent vbox = Box.createVerticalBox();
        add( vbox, BorderLayout.NORTH );
        urlField_ = new JTextField();
        urlField_.setEditable( false );
        typeSelector_ = new NonShrinkingComboBox<>( ResourceType.values() );
        ItemListener typeItemListener = new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                ResourceType rtype = getResourceType();
                if ( rtype != null ) {
                    UrlInvoker invoker = urlopts_.getDefaultsMap().get( rtype );
                    invokeSelector_.setSelectedItem( invoker );
                }
            }
        };
        typeSelector_.addItemListener( typeItemListener );
        invokeSelector_ = new NonShrinkingComboBox<>( urlopts_.getInvokers() );
        invokeSelector_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                ResourceType rtype = getResourceType();
                UrlInvoker invoker = getUrlInvoker();
                if ( rtype != null && invoker != null ) {
                    urlopts_.getDefaultsMap().put( rtype, invoker );
                }
                updateState();
            }
        } );
        invokeAct_ = new AbstractAction( "Invoke" ) {
            public void actionPerformed( ActionEvent evt ) {
                final URL url = getUrl();
                if ( url != null ) {
                    final UrlInvoker invoker = getUrlInvoker();
                    if ( invoker != null ) {
                        if ( isSingleInvocationThread_ && lastJob_ != null ) {
                            lastJob_.cancel( true );
                        }
                        lastJob_ = getQueue()
                                  .submit( () -> invokeUrl( invoker, url ) );
                    }
                }
            }
        };
        statusLabel_ = new JLabel();
        msgField_ = new JTextField();
        msgField_.setEditable( false );
        JComponent uline = Box.createHorizontalBox();
        uline.add( urlField_ );
        vbox.add( uline );
        JComponent cline = Box.createHorizontalBox();
        JCheckBox guessButton = guessTypeModel_.createCheckBox();
        guessButton.setHorizontalTextPosition( SwingConstants.LEFT );
        cline.add( new JLabel( "Type: " ) );
        cline.add( typeSelector_ );
        cline.add( guessButton );
        cline.add( Box.createHorizontalStrut( 5 ) );
        cline.add( Box.createHorizontalGlue() );
        cline.add( new LineBox( "Action", invokeSelector_ ) );
        cline.add( Box.createHorizontalGlue() );
        if ( hasAutoInvoke ) {
            cline.add( autoInvokeModel_.createCheckBox() );
            cline.add( Box.createHorizontalStrut( 5 ) );
        }
        cline.add( new JButton( invokeAct_ ) );
        vbox.add( Box.createVerticalStrut( 2 ) );
        vbox.add( cline );
        JComponent rline = Box.createHorizontalBox();
        rline.add( new JLabel( "Result: " ) );
        rline.add( statusLabel_ );
        rline.add( Box.createHorizontalStrut( 5 ) );
        rline.add( msgField_ );
        vbox.add( Box.createVerticalStrut( 2 ) );
        vbox.add( rline );
        typeItemListener.itemStateChanged( null );
        updateState();
    }

    /**
     * Configures this panel for use with a given resource.
     *
     * @param  resourceInfo  resource info, may be null
     */
    public void configureResource( ResourceInfo resourceInfo ) {
        URL url = resourceInfo == null ? null : resourceInfo.getUrl();
        urlField_.setText( url == null ? null : url.toString() );
        urlField_.setCaretPosition( 0 );
        setStatus( null );
        url_ = url;
        guessType_ = ResourceType.guessResourceType( resourceInfo );
        if ( guessTypeModel_.isSelected() ) {
            typeSelector_.setSelectedItem( guessType_ );
        }
        updateState();
    }

    /**
     * Returns the currently displayed URL.
     *
     * @return  URL
     */
    public URL getUrl() {
        return url_;
    }

    /**
     * Returns the currently selected resource type.
     *
     * @return  resource type
     */
    public ResourceType getResourceType() {
        return typeSelector_.getItemAt( typeSelector_.getSelectedIndex() );
    }

    /**
     * Returns the currently selected URL invocation mode.
     *
     * @return   invoker
     */
    public UrlInvoker getUrlInvoker() {
        return invokeSelector_.getItemAt( invokeSelector_.getSelectedIndex() );
    }

    /**
     * Performs a URL invocation based on the current state of this panel.
     * The currently-selected URL is invoked using the currently-selected
     * invoker, assuming both are non-null.
     *
     * @return outcome
     */
    public Outcome invokeUrl() {
        return invokeUrl( getUrlInvoker(), getUrl() );
    }

    /**
     * Indicates whether this panel is currently set up for auto-invoke.
     *
     * @return   whether the auto-invoke toggle button is checked
     */
    public boolean isAutoInvoke() {
        return autoInvokeModel_.isSelected();
    }

    /**
     * Sets the Outcome to be displayed, reporting the result of the
     * most recently invoked URL.
     *
     * @param   outcome  new outcome, or null if nothing complete
     */
    private void setStatus( Outcome outcome ) {
        final String status;
        final String msg;
        if ( outcome == null ) {
            status = null;
            msg = null;
        }
        else {
            status = outcome.isSuccess() ? "OK" : "FAIL";
            msg = outcome.getMessage();
        }
        statusLabel_.setText( status );
        msgField_.setText( msg );
        msgField_.setCaretPosition( 0 );
        statusLabel_.revalidate();
        msgField_.revalidate();
    }

    /**
     * Called when the invocation options may have changed to ensure that
     * the GUI correctly reflects them.
     */
    private void updateState() {
        invokeAct_.setEnabled( ! autoInvokeModel_.isSelected() && canInvoke() );
    }

    /**
     * Indicates whether this panel currently has enough state to be able
     * to invoke its displayed link.
     *
     * @return  true iff invocation may perform some action
     */
    private boolean canInvoke() {
        return getUrlInvoker() != null && getUrl() != null;
    }

    /**
     * Attempts to invoke a given URL with a given invoker.
     * The UI is updated accordingly.
     *
     * @param  invoker  invoker, may be null
     * @param  url   invocation target, may be null
     * @return  outcome of invocation attempt
     */
    private Outcome invokeUrl( UrlInvoker invoker, URL url ) {
        SwingUtilities.invokeLater( () -> setStatus( null ) );
        if ( invoker == null ) {
            return Outcome.failure( "No invocation method selected" );
        }
        else if ( url == null ) {
            return Outcome.failure( "No URL" );
        }
        logger_.info( "Invoke: " + url );
        Outcome outcome = invoker.invokeUrl( url );
        SwingUtilities.invokeLater( () -> setStatus( outcome ) );
        return outcome;
    }

    /**
     * Returns an executor service on which URL invocations can be
     * asynchronously executed.
     *
     * @return  executor service
     */
    private ExecutorService getQueue() {
        if ( queue_ == null ) {
            ThreadFactory tfact = new ThreadFactory() {
                public Thread newThread( Runnable r ) {
                    Thread thread = new Thread( r, "Invoke URL" );
                    thread.setDaemon( true );
                    return thread;
                }
            };
            queue_ = isSingleInvocationThread_
                   ? Executors.newSingleThreadExecutor( tfact )
                   : Executors.newCachedThreadPool( tfact );
        }
        return queue_;
    }

    /**
     * ComboBox that may resize itself larger if its contents change,
     * but will not resize itself smaller.
     * This reduces the amount of jumping around in the GUI.
     */
    private static class NonShrinkingComboBox<T> extends JComboBox<T> {
        private int width_;

        /**
         * Constructor.
         *
         * @param  options  initial combobox contents
         */
        public NonShrinkingComboBox( T[] options ) {
            super( options );
        }
        @Override
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }
        @Override
        public Dimension getMinimumSize() {
            Dimension size = super.getMinimumSize();
            width_ = Math.max( size.width, width_ );
            return new Dimension( width_, size.height );
        }
        @Override
        public Dimension getMaximumSize() {
            return getMinimumSize();
        }
    }
}
