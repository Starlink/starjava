package uk.ac.starlink.splat.vo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.table.gui.StarJTable;


/**
 * Panel to handle the query results (from SSAP or ObsCore queries)
 * 
 *
 * @author Margarida Castro Neves 
 * 
 */
public class ResultsPanel extends JPanel implements ActionListener, MouseListener  {
 
   
    private JTabbedPane resultsPane;
    private boolean dataLinkEnabled = false;
    private JButton displaySelectedButton;
    private JButton displayAllButton;
    private JButton downloadSelectedButton;
    private JButton downloadAllButton;
    private JButton deselectVisibleButton;
    private JButton deselectAllButton;
    private JToggleButton dataLinkButton;
    private SSAQueryBrowser ssaQueryBrowser;
    private ObsCorePanel obsQueryBrowser;
   
    /**
     * @uml.property  name="dataLinkFrame"
     * @uml.associationEnd  
     */
    private DataLinkQueryFrame dataLinkFrame = null;

 

    
    public ResultsPanel( JTabbedPane resultsPane, SSAQueryBrowser browser ) {
        this.resultsPane=resultsPane;
        this.ssaQueryBrowser=browser;
        this.obsQueryBrowser=null;
        initComponents();
    }
    
    public ResultsPanel( /*JTabbedPane resultsPane*/ ObsCorePanel browser ) {
        //this.resultsPane=resultsPane;
        this.obsQueryBrowser=browser;
        this.ssaQueryBrowser=null;
        initComponents();
    }
    
    private void initComponents() {
        this.setLayout(new GridBagLayout());
        this.setBorder ( BorderFactory.createTitledBorder( "Query results:" ) );
        this.setToolTipText( "Results of query to the current list of SSAP servers. One table per server" );
     
        GridBagConstraints gbc=new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weighty=1;
        gbc.weightx=1;
        gbc.anchor=GridBagConstraints.NORTHWEST;
        gbc.fill=GridBagConstraints.BOTH;
        resultsPane = new JTabbedPane();
        resultsPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if ( dataLinkEnabled ) {
                   if (resultsPane.getIconAt(resultsPane.getSelectedIndex())!=null) { // it's a datalink service

                        if (dataLinkFrame != null && dataLinkEnabled) {
                            dataLinkFrame.setServer(resultsPane.getTitleAt(resultsPane.getSelectedIndex()));
                            dataLinkFrame.setVisible(true);
                        } 
                    } else {
                        dataLinkFrame=null;
                    }
                }
            }
        });
        this.add( resultsPane , gbc);
    
     
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcontrol = new GridBagConstraints();
        gbcontrol.gridx=0;
        gbcontrol.gridy=0;
        gbcontrol.weightx=1;
        gbcontrol.weighty=0;
        gbcontrol.fill = GridBagConstraints.HORIZONTAL;

        //  Download and display.
        displaySelectedButton = new JButton( "<html>Display<BR> selected</html>" );
        displaySelectedButton.addActionListener( this );
        displaySelectedButton.setMargin(new Insets(1, 10, 1, 10));  
        displaySelectedButton.setToolTipText
        ( "Download and display all spectra selected in all tables" );
        controlPanel.add( displaySelectedButton,gbcontrol );


        displayAllButton = new JButton( "<html>Display<BR>all</html>" );
        displayAllButton.addActionListener( this );
        displayAllButton.setMargin(new Insets(1,10,1,10));  
        displayAllButton.setToolTipText
        ( "Download and display all spectra in all tables" );
        gbcontrol.gridx=1;
        controlPanel.add( displayAllButton, gbcontrol );

        //  Just download.
        downloadSelectedButton = new JButton( "<html>Download<BR>selected</html>" );
        downloadSelectedButton.addActionListener( this );
        downloadSelectedButton.setMargin(new Insets(1,10,1,10));  
        downloadSelectedButton.setToolTipText
        ( "Download all spectra selected in all tables");
        gbcontrol.gridx=2;
        controlPanel.add( downloadSelectedButton, gbcontrol );
      

        downloadAllButton = new JButton( "<html>Download<BR> all</html>" );
        downloadAllButton.addActionListener( this );
        downloadAllButton.setMargin(new Insets(1,10,1,10));  
        downloadAllButton.setToolTipText
        ( "Download all spectra in all tables");
        gbcontrol.gridx=3;
        controlPanel.add( downloadAllButton , gbcontrol);


        //  Deselect
        deselectVisibleButton = new JButton( "<html>Deselect<br>table</html>" );
        deselectVisibleButton.addActionListener( this );
        deselectVisibleButton.setMargin(new Insets(1,10,1,10));  
        deselectVisibleButton.setToolTipText
        ( "Deselect all spectra in displayed table" );
      //  controlPanel2.add( deselectVisibleButton );
        gbcontrol.gridx=4;
        controlPanel.add( deselectVisibleButton, gbcontrol );


        deselectAllButton = new JButton( "<html>Deselect <BR>all</html>" );
        deselectAllButton.addActionListener( this );
        deselectAllButton.setMargin(new Insets(1,10,1,10));  
        deselectAllButton.setToolTipText
        ( "Deselect all spectra in all tables" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
        controlPanel.add( deselectAllButton , gbcontrol);

        dataLinkButton = new JToggleButton( "<html>DataLink<BR>Services</html>" );
        dataLinkButton.addActionListener( this );
        dataLinkButton.setMargin(new Insets(1,10,1,10));  
        dataLinkButton.setToolTipText ( "DataLink parameters" );
        dataLinkButton.setEnabled(false);
        dataLinkButton.setVisible(false);
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=6;
        controlPanel.add( dataLinkButton, gbcontrol );
        gbc.gridx=0;
        gbc.gridy=1;
        gbc.weighty=0;
        gbc.anchor = GridBagConstraints.PAGE_END;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        this.add( controlPanel, gbc );
      makeSpecPopup();
     
    }

    
    private JPopupMenu makeSpecPopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem dlMenuItem = new JMenuItem("Download");
        
        dlMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dlMenuItem);
        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(infoMenuItem);
        JMenuItem dispMenuItem = new JMenuItem("Display");
        dispMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dispMenuItem);
        
        // selection menu
        JMenu selectionMenu = new SSAQueryResultsTableSelectionMenu();
        popup.add(selectionMenu);
        
        return popup;
    }

    //
    // ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        
        if ( source.equals( displaySelectedButton ) ) {
            displaySpectra( true, true, null, -1 );
            return;
        }
        if ( source.equals( displayAllButton ) ) {
            displaySpectra( false, true, null, -1 );
            return;
        }

        if ( source.equals( downloadSelectedButton ) ) {
            displaySpectra( true, false, null, -1 );
            return;
        }
        if ( source.equals( downloadAllButton ) ) {
            displaySpectra( false, false, null, -1 );
            return;
        }

        if ( source.equals( deselectVisibleButton ) ) {
            deselectSpectra( false );
            return;
        }
        if ( source.equals( deselectAllButton ) ) {
            deselectSpectra( true );
            return;
        }
        if ( source.equals( dataLinkButton ) ) { 
            if (dataLinkFrame != null && dataLinkFrame.getParams() != null) {
                if ( dataLinkButton.getModel().isSelected() ) {
                    activateDataLinkSupport();
                    if (resultsPane.isEnabledAt(resultsPane.getSelectedIndex()))
                        dataLinkFrame.setVisible(true);  
                }
                else {
                    if (dataLinkFrame.isVisible()) { // deactivate
                        dataLinkFrame.setVisible(false);
                    }
                    deactivateDataLinkSupport();
                }
            }

            return;
        }

    }
    
    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void displaySpectra( boolean selected, boolean display,
            StarJTable table, int row )
    {
      if (obsQueryBrowser!=null)
          obsQueryBrowser.displaySpectra(selected, display, table, row);
      if (ssaQueryBrowser!=null)
          ssaQueryBrowser.displaySpectra(selected, display, table, row);
    }

    
    /**
     * Deselect all spectra
     * <p>
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void deselectSpectra( boolean all )
    {
      //if (obsQueryBrowser!=null)
      //    obsQueryBrowser.deselectSpectra(all);
     
      if (ssaQueryBrowser!=null)
          ssaQueryBrowser.deselectSpectra(all, resultsPane.getSelectedComponent());
    }
    
    /**
     * ActivateDataLinkSupport
     * deactivate all sites that do not support DataLink
     * activate DataLink queries on supported sites
     */
    protected void activateDataLinkSupport() {
       
        dataLinkEnabled=true;
        int selected=-1;
        //int anyIndex = -1;
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
           if (resultsPane.getIconAt(i) == null) { // no datalink service
               resultsPane.setEnabledAt(i, false);
           }
           else {
               if (resultsPane.getSelectedIndex() == i)
                   selected = i;
           }
        }
        
        // if current selection is not a DataLink service do nothing
        if (selected < 0)
            return;      
         dataLinkFrame.setServer(resultsPane.getTitleAt(selected)); 
    }
    
    /**
     * DeactivateDataLinkSupport
     * activate all sites, without Datalink support
     */
    protected void deactivateDataLinkSupport() {
        
        dataLinkEnabled=false;
        int nrTabs = resultsPane.getTabCount();
        
        for(int i = 0; i < nrTabs; i++)
        {
            resultsPane.setEnabledAt(i, true);      
        }
        dataLinkButton.setSelected(false);
        
    }


    /**
     * Remove all results
     * <p>
     *
     */
    protected void removeAllResults()
    {
      resultsPane.removeAll();
    }
    
    /**
     * Add a results Tab
     * <p>
     *
     */
    protected void addTab(String shortName, JScrollPane scrollpane)
    {
      resultsPane.addTab(shortName, scrollpane);
    }
    
    protected void addTab(String shortName, ImageIcon icon, JScrollPane scrollpane)
    {
      resultsPane.addTab(shortName, icon, scrollpane);
    }

    public void removeDataLinkButton() {
        dataLinkButton.setVisible(false);        
    }
    
    public void enableDataLink(DataLinkQueryFrame datalinkframe) {
        dataLinkButton.setEnabled(true);
        dataLinkButton.setVisible(true);
        dataLinkFrame=datalinkframe;
        
    }

    //
    // MouseListener interface. Double clicks display the clicked spectrum.
    //
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e )
    {
       
        //requestFocusInWindow();
     //   if (e.getSource().getClass() == StarTable.class ) {

            if ( e.getClickCount() == 2 ) {
                StarJTable table = (StarJTable) e.getSource();
                Point p = e.getPoint();
                int row = table.rowAtPoint( p );
                displaySpectra( false, true, table, row );
            }
      //  }
    }
    

    
    public class SpecPopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
            JMenuItem jmi  = (JMenuItem) e.getSource();
            JPopupMenu jpm = (JPopupMenu) jmi.getParent();
            StarPopupTable table = (StarPopupTable) jpm.getInvoker();
           
           int row = table.getPopupRow();
          
            if (e.getActionCommand().equals("Info")) {
                table.showInfo(row);
            }
            else if (e.getActionCommand().equals("Display")) {
               displaySpectra( false, true, (StarJTable) table, row );
            }   
            else if (e.getActionCommand().equals("Download")) {
                displaySpectra( false, false, (StarJTable) table, row );
            }             
        }
    }    
    
}
