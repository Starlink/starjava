package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.topcat.Downloader;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;

/**
 * Component that allows the user to select table names for use with
 * the CDS X-Match service.
 *
 * <p>Currently rather basic.
 *
 * @author   Mark Taylor
 * @since    15 May 2014
 */
public class CdsTableSelector extends JPanel {

    private final JLabel label_;
    private final JComboBox nameSelector_;

    private static final Downloader<String[]> aliasDownloader_ =
        createAliasDownloader();

    /**
     * Constructor.
     */
    public CdsTableSelector() {
        setLayout( new BorderLayout() );

        /* Add selector component. */
        nameSelector_ = new JComboBox();
        nameSelector_.setEditable( true );
        nameSelector_.setSelectedItem( null );

        /* Populate with vizier aliases. */
        nameSelector_.addItem( "SIMBAD" );
        if ( aliasDownloader_.isComplete() ) {
            addAliases( aliasDownloader_.getData() );
        }
        else {
            Thread aliasLoader = new Thread( "Vizier Aliases" ) {
                public void run() {
                    final String[] aliases = aliasDownloader_.waitForData();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            addAliases( aliases );
                        }
                    } );
                }
            };
            aliasLoader.setDaemon( true );
            aliasLoader.start();
        }
        label_ = new JLabel( "VizieR Table Name: " );

        /* Place selector and annotaion. */
        JComponent selectorLine = Box.createHorizontalBox();
        selectorLine.add( label_ );
        selectorLine.add( nameSelector_ );
        selectorLine.add( Box.createHorizontalStrut( 5 ) );
        selectorLine.add( aliasDownloader_.createMonitorComponent() );
        JComponent infoLine = Box.createHorizontalBox();
        JLabel infoLabel =
                new JLabel( "Select alias or use VizieR ID like "
                          + "\"II/246/out\"" ) {
            protected void paintComponent( Graphics g ) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
                super.paintComponent( g2 );
            }
        };
        Font font = new Font( "DialogInput", Font.ITALIC, 10 );
        infoLabel.setFont( font );
        infoLine.add( Box.createHorizontalGlue() );
        infoLine.add( infoLabel );
        JComponent main = Box.createVerticalBox();
        add( main, BorderLayout.CENTER );
        main.add( selectorLine );
        main.add( infoLine );
        main.add( Box.createVerticalStrut( 5 ) );
    }

    /**
     * Returns the human-readable name of a selected table.
     *
     * @return  currently selected table name
     */
    public String getTableName() {
        return (String) nameSelector_.getSelectedItem();
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        nameSelector_.setEnabled( isEnabled );
        label_.setEnabled( isEnabled );
    }

    /**
     * Adds a list of aliases to the existing list.
     *
     * @param  aliases  list of vizier table aliases
     */
    private void addAliases( String[] aliases ) {
        if ( aliases != null ) {
            for ( int i = 0; i < aliases.length; i++ ) {
                nameSelector_.addItem( aliases[ i ] );
            }
        }
    }

    /**
     * Returns a downloader for Xmatch service alias names.
     *
     * @return   alias downloader
     */
    private static Downloader<String[]> createAliasDownloader() {
        return new Downloader<String[]>( String[].class, "VizieR aliases" ) {
            public String[] attemptReadData() throws IOException {
                return CdsUploadMatcher.readAliases();
            }
        };
    }
}
