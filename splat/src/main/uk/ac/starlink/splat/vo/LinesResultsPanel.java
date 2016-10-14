package uk.ac.starlink.splat.vo;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import uk.ac.starlink.table.gui.StarJTable;


public class LinesResultsPanel extends ResultsPanel {

    private JButton changeColourButton=null;
    LineBrowser slQueryBrowser=null;
    
    public LinesResultsPanel(LineBrowser browser) {
            super();
            slQueryBrowser=browser;
            controlPanel = initControlPanel();
            initComponents();      
            popupMenu = makeLinePopup();
    }
    
    private JPanel initControlPanel() 
    {
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcontrol = new GridBagConstraints();
        gbcontrol.gridx=0;
        gbcontrol.gridy=0;
        gbcontrol.weightx=1;
        gbcontrol.weighty=0;
        gbcontrol.fill = GridBagConstraints.HORIZONTAL;

        //  Download and display.
        displaySelectedButton = new JButton( "<html>Display<BR> selected Lines</html>" );
        displaySelectedButton.addActionListener( this );
        displaySelectedButton.setMargin(new Insets(1, 10, 1, 10));  
        displaySelectedButton.setToolTipText
        ( "display all lines selected" );
        controlPanel.add( displaySelectedButton,gbcontrol );


        displayAllButton = new JButton( "<html>Display<BR>all</html>" );
        displayAllButton.addActionListener( this );
        displayAllButton.setMargin(new Insets(1,10,1,10));  
        displayAllButton.setToolTipText
        ( "display all lines " );
        gbcontrol.gridx=1;
        controlPanel.add( displayAllButton, gbcontrol );

        
        //  Deselect
        deselectVisibleButton = new JButton( "<html>Deselect<br>table</html>" );
        deselectVisibleButton.addActionListener( this );
        deselectVisibleButton.setMargin(new Insets(1,10,1,10));  
        deselectVisibleButton.setToolTipText
        ( "Deselect all lines in displayed table" );
      //  controlPanel2.add( deselectVisibleButton );
        gbcontrol.gridx=4;
        controlPanel.add( deselectVisibleButton, gbcontrol );


        deselectAllButton = new JButton( "<html>Deselect <BR>all</html>" );
        deselectAllButton.addActionListener( this );
        deselectAllButton.setMargin(new Insets(1,10,1,10));  
        deselectAllButton.setToolTipText
        ( "Deselect all lines in all tables" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
        controlPanel.add( deselectAllButton , gbcontrol);
        
        changeColourButton = new JButton( "<html>Change<BR> colour </html>" );
        changeColourButton.addActionListener(new LineAction());
        changeColourButton.setMargin(new Insets(1,10,1,10));  
        changeColourButton.setToolTipText
        ( "Change lines colours (random)" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
        controlPanel.add( deselectAllButton , gbcontrol);
        
        return controlPanel;
    }
    
    
    
    private JPopupMenu makeLinePopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new LineAction());
        popup.add(infoMenuItem);
        JMenuItem dispSelMenuItem = new JMenuItem("Display selected lines in current spectrum");
        dispSelMenuItem.addActionListener(new LineAction());
        popup.add(dispSelMenuItem);
        JMenuItem dispAllMenuItem = new JMenuItem("Display all lines in current spectrum");
        dispAllMenuItem.addActionListener(new LineAction());
        popup.add(dispAllMenuItem);
        
        
        // selection menu
        JMenu selectionMenu = new SSAQueryResultsTableSelectionMenu();
        popup.add(selectionMenu);
        
        return popup;
    }
    
    protected void displaySpectra( boolean selected, boolean display,
            StarJTable table, int row )
    {
      StarJTable currentTable = getCurrentTable(table);      
      if (selected)
        slQueryBrowser.displayLineSelection(currentTable);
      else
        slQueryBrowser.displayLines( currentTable.getStarTable() );          
    }
    
    public class LineAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
            JMenuItem jmi  = (JMenuItem) e.getSource();
            JPopupMenu jpm = (JPopupMenu) jmi.getParent();
            StarPopupTable table = (StarPopupTable) jpm.getInvoker();
           
           int row = table.getPopupRow();
          
            if (e.getActionCommand().equals("Info")) {
                table.showInfo(row);
            }
            else if (e.getActionCommand().startsWith("Display selected")) {
               slQueryBrowser.displayLineSelection(table);          
            }   
            else if (e.getActionCommand().equals("Display all lines")) {
               slQueryBrowser.displayLines( table.getStarTable() );
            }   
            else if (e.getActionCommand().contains("colour")) {
               // slQueryBrowser.changecolour( table.getStarTable() );//!!! implement colour change
             }   
        }
    }


}
