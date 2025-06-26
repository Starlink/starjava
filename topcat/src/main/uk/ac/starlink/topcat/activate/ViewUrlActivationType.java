package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.Outcome;
import uk.ac.starlink.topcat.ResourceInfo;
import uk.ac.starlink.topcat.Safety;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.UrlOptions;
import uk.ac.starlink.topcat.UrlPanel;

/**
 * Activation type for viewing a URL when its MIME type is available.
 *
 * @author   Mark Taylor
 * @since    26 Jun 2025
 */
public class ViewUrlActivationType implements ActivationType {

    private static final String MIMECOL_KEY = "mime";
    private static final String AUTO_KEY = "auto";

    public String getName() {
        return "View URL";
    }

    public String getDescription() {
        return "Displays a type-sensitive view of the resource in a URL column";
    }

    public ActivatorConfigurator createConfigurator( TopcatModelInfo tinfo ) {
        return new ViewUrlConfigurator( tinfo );
    }

    public Suitability getSuitability( TopcatModelInfo tinfo ) {
        return ( tinfo.tableHasFlag( ColFlag.URL ) &&
                 tinfo.tableHasFlag( ColFlag.MIME ) )
             ? Suitability.SUGGESTED
             : Suitability.AVAILABLE;
    }

    /**
     * Configurator implementation for this activation type.
     */
    private static class ViewUrlConfigurator extends UrlColumnConfigurator {

        private final TopcatModel tcModel_;
        private final JComboBox<ColumnData> mimeSelector_;
        private final JCheckBox autoToggle_;
        private final UrlPanel urlPanel_;
        private final JFrame urlWindow_;
        private boolean isUrlWindowPosted_;

        /**
         * Constructor.
         *
         * @param  tinfo  topcat model information
         */
        ViewUrlConfigurator( TopcatModelInfo tinfo ) {
            super( tinfo, "Resource URL", new ColFlag[] { ColFlag.URL } );
            setLocationLabel( "Resource URL" );
            tcModel_ = tinfo.getTopcatModel();
            JComponent queryPanel = getQueryPanel();
            ColumnDataComboBoxModel mimeColModel =
                new ColumnDataComboBoxModel( tcModel_, String.class, true );
            configureDefaultSelection( mimeColModel, tinfo,
                                       new ColFlag[] { ColFlag.MIME } );
            mimeSelector_ = new ColumnDataComboBox();
            mimeSelector_.setModel( mimeColModel );
            mimeSelector_.addActionListener( getActionForwarder() );
            JComponent mimeLine = Box.createHorizontalBox();
            mimeLine.add( new JLabel( "MIME Type: " ) );
            mimeLine.add( mimeSelector_ );
            queryPanel.add( mimeLine );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            autoToggle_ = new JCheckBox();
            autoToggle_.setSelected( true );
            autoToggle_.addActionListener( getActionForwarder() );
            JComponent autoLine = Box.createHorizontalBox();
            autoLine.add( new JLabel( "Auto Invoke: " ) );
            autoLine.add( autoToggle_ );
            autoLine.add( Box.createHorizontalGlue() );
            queryPanel.add( autoLine );
            queryPanel.add( Box.createVerticalStrut( 5 ) );
            JComponent panel = getPanel();
            panel.addPropertyChangeListener( "enabled", evt -> {
                boolean isEnabled = panel.isEnabled();
                mimeSelector_.setEnabled( isEnabled );
                autoToggle_.setEnabled( isEnabled );
            } );
            boolean hasAutoInvoke = false;
            urlPanel_ = new UrlPanel( UrlOptions.createOptions( null, null ),
                                      hasAutoInvoke );
            String title = "TOPCAT(" + tinfo.getTopcatModel().getID() + "): "
                         + "Activation - View URL";
            urlWindow_ = new JFrame( title );
            urlWindow_.getContentPane().add( urlPanel_ );
            urlPanel_.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ));
            urlWindow_.pack();
        }

        protected Activator createActivator( ColumnData cdata ) {
            boolean invokeOnEdt = false;
            ColumnData mimeCdata = getMimeColumnData();
            return new UrlColumnActivator( cdata, invokeOnEdt ) {
                protected Outcome activateUrl( URL url, long lrow ) {
                    String mime;
                    if ( mimeCdata == null ) {
                        mime = null;
                    }
                    else {
                        try {
                            Object mimeObj = mimeCdata.readValue( lrow );
                            if ( mimeObj instanceof String ) {
                                mime = (String) mimeObj;
                            }
                            else {
                                mime = null;
                            }
                        }
                        catch ( IOException e ) {
                            mime = null;
                        }
                    }
                    final String mime0 = mime;
                    urlPanel_.configureResource( new ResourceInfo() {
                        public String getStandardId() {
                            return null;
                        }
                        public String getContentQualifier() {
                            return null;
                        }
                        public String getContentType() {
                            return mime0;
                        }
                        public URL getUrl() {
                            return url;
                        }
                    } );
                    if ( url != null ) {
                        if ( autoToggle_.isSelected() ) {
                            return urlPanel_.invokeUrl();
                        }
                        else {
                            SwingUtilities.invokeLater( () -> {
                                setUrlWindowVisible();
                            } );
                            return Outcome.success();
                        }
                    }
                    else {
                        return Outcome.failure( "No URL" );
                    }
                }
            };
        }

        public String getConfigMessage( ColumnData cdata ) {
            return null;
        }

        public Safety getSafety() {
            return Safety.SAFE;
        }

        public ConfigState getState() {
            ConfigState state = getUrlState();
            state.saveSelection( MIMECOL_KEY, mimeSelector_ );
            state.saveFlag( AUTO_KEY, autoToggle_.getModel() );
            return state;
        }

        public void setState( ConfigState state ) {
            setUrlState( state );
            state.restoreSelection( MIMECOL_KEY, mimeSelector_ );
            state.restoreFlag( AUTO_KEY, autoToggle_.getModel() );
        }

        /**
         * Returns the currently selected ColumnData for reading MIME type.
         *
         * @return  MIME type string-valued ColumnData, or null
         */
        private ColumnData getMimeColumnData() {
            Object mimeObj = mimeSelector_.getSelectedItem();
            return mimeObj instanceof ColumnData ? (ColumnData) mimeObj : null;
        }

        /**
         * Ensures the URL window is visible.
         * The first time it's posted, this also locates it relative to
         * the query panel so it doesn't end up in a forgotten corner
         * of the screen (it's quite small and inconspicuous).
         */
        private void setUrlWindowVisible() {
            if ( ! isUrlWindowPosted_ ) {
                isUrlWindowPosted_ = true;
                urlWindow_.setLocationRelativeTo( getQueryPanel() );
            }
            urlWindow_.setVisible( true );
        }
    }
}
