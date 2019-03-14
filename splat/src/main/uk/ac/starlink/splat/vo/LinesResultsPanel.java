package uk.ac.starlink.splat.vo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicButtonUI;

import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.table.gui.StarJTable;



public class LinesResultsPanel extends ResultsPanel {

	protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();
    private JButton changeColourButton=null;
    LineBrowser slQueryBrowser=null;
    private JButton removeLinesButton;
	private JComboBox activePlotBox;
    
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
        
/*       JPanel plotChoicePanel = new JPanel();
        plotChoicePanel.add (new  JLabel("Select PLOT: "));
        activePlotBox=new JComboBox();
        for (int i=0;i<globalList.plotCount();i++)  {
       	 	activePlotBox.addItem(globalList.getPlotName(i));
        }
        plotChoicePanel.add(activePlotBox);
        */
        GridBagConstraints gbcontrol = new GridBagConstraints();
        gbcontrol.gridx=0;
        gbcontrol.gridy=0;
        gbcontrol.weightx=1;
        gbcontrol.weighty=0;
        gbcontrol.fill = GridBagConstraints.HORIZONTAL;

     //   controlPanel.add( plotChoicePanel ,gbcontrol );
        
     //   gbcontrol.gridy=1;
        
        //  Download and display.
        displaySelectedButton = new JButton( "<html>Display<BR> selected Lines</html>" );
        displaySelectedButton.addActionListener( this );
        displaySelectedButton.setMargin(new Insets(1, 10, 1, 10));  
        displaySelectedButton.setToolTipText
        ( "display all selected lines" );
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
        
        removeLinesButton = new JButton( "<html>Remove<BR>lines</html>" );
        removeLinesButton.addActionListener(new LineAction());
        removeLinesButton.setMargin(new Insets(1,10,1,10));  
        removeLinesButton.setToolTipText
        ( "remove lines from plot" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=6;
        controlPanel.add( removeLinesButton , gbcontrol);
        
        changeColourButton = new JButton( "<html>Change<BR> colour </html>" );
        changeColourButton.addActionListener(new LineAction());
        changeColourButton.setMargin(new Insets(1,10,1,10));  
        changeColourButton.setToolTipText
        ( "Change lines colours (random)" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
      //  controlPanel.add( deselectAllButton , gbcontrol);
        
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
          
      JScrollPane pane = (JScrollPane) resultsPane.getSelectedComponent();
      StarPopupTable currentTable = (StarPopupTable) pane.getViewport().getView();
      
      if (selected)
        slQueryBrowser.displayLineSelection(currentTable);
      else
        slQueryBrowser.displayLines( currentTable.getStarTable() );          
    }
    
    public int getPlotIndex() {
    	return activePlotBox.getSelectedIndex();
    }
  
    public void addTab(String name, StarPopupTable ptable) {
    	
    	TabButton removeButton = new TabButton();
    	JPanel tabPanel = new JPanel();
    	tabPanel.add(new JLabel(name));
    	tabPanel.add(removeButton);
    	tabPanel.setOpaque(false);
    	removeButton.addActionListener(new ActionListener() { 
    	    public void actionPerformed(ActionEvent e) { 
    	    	  TabButton tb = (TabButton) e.getSource();    	    	  
    			  removeTab((Component) tb.getParent());    			  
    			  } 
  			} );
    	super.addTab(name, (StarPopupTable)  ptable);
    	int tabindex = resultsPane.indexOfTab(name);
    	resultsPane.setTabComponentAt(tabindex, tabPanel);

    }
    
    private void removeTab(Component  tb) {
    	int i = resultsPane.indexOfTabComponent(tb);
        if (i != -1) {
        	resultsPane.remove(i);
        };
    }
    
    public class LineAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
           
            // button actions
            if (e.getSource() == removeLinesButton) {
                    slQueryBrowser.removeLinesFromPlot( );
                    return;
            }
            
            // popup menu actions
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
            else if (e.getActionCommand().startsWith("Display all lines")) {
               slQueryBrowser.displayLines( table.getStarTable() );
            }  
            else if (e.getActionCommand().contains("colour")) {
               // slQueryBrowser.changecolour( table.getStarTable() );//!!! implement colour change
             }   
        }
    }
    
    private class TabButton extends JButton {

		public TabButton() {
        	
            int size = 15;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
           // setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            
        }
       
    private final MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }
 
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
	}

}
