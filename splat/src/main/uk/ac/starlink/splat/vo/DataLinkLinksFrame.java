package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import jsky.util.Logger;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.BasicStarPopupTable;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.util.JTableUtilities;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vo.ResultsPanel.SpecPopupMenuAction;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarJTable;

public class DataLinkLinksFrame extends JFrame implements ActionListener, MouseListener{

	
	
	private StarTable linksTable;
	private DataLinkResponse dlparams;
	private JButton closeButton;

	private ImageIcon preview;
	private SodaPanel servicePanel;
	private JButton displayButton;
	private VOBrowser browser;
	private JMenuItem accurlMenuItem;
	private JMenuItem curCellMenuItem;
	private  int WIDTH=800;
	private  int HEIGHT=450; 
	private int  PREVIEWMAXWIDTH=800;
	private int PREVIEWMAXHEIGHT=100;
	
//	JPanel tablepanel= null;

	public DataLinkLinksFrame(DataLinkResponse dlp, VOBrowser browser)  {
		 this.setSize(WIDTH,HEIGHT);
		 this.setMinimumSize(new Dimension(800, 350));
		 linksTable = dlp.getLinksTable();
		 dlparams=dlp;		
		 preview = getPreview();
		 servicePanel = null;
		 this.browser = browser;
	     initUI(); 
	}	

	private ImageIcon getPreview() {
		// getPreviewRow
		String previewURL;
		try {
			previewURL = dlparams.getAccessURL("#preview");
		} catch (IOException e) {
			previewURL = null;
		}
		if (previewURL == null)
			return null;
		return addPreview(previewURL);
		
	}
	
	private int getProcRow() {
		
		int s = dlparams.getSemanticsIndex();
		for (int row=0; row< linksTable.getRowCount(); row++) {
			String semantics = dlparams.getSemanticsValue(row);
			if (semantics.equals("#proc")) 
				return row;
		}		
		return -1;
	}

	

	private void initUI() {
		this.setVisible(false);
        this.getContentPane().removeAll();
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weighty=0;
        gbc.weightx=1;
        gbc.fill=GridBagConstraints.NONE;
    
        
		if (preview != null ) {
		    JPanel previewPanel = new JPanel();
	        previewPanel.setBorder(new TitledBorder("Preview"));
	        previewPanel.add(new JLabel(preview));
			panel.add(previewPanel, gbc);
			gbc.gridy++;
		}
		
		
		JPopupMenu popup = new JPopupMenu();
		accurlMenuItem = new JMenuItem("Copy access url  to clipboard");
        accurlMenuItem.addActionListener(this);
        popup.add(accurlMenuItem);
        curCellMenuItem = new JMenuItem("Copy current cell to clipboard");
        curCellMenuItem.addActionListener(this);
        popup.add(curCellMenuItem);
		//popup.add( new SSAQueryResultsTableSelectionMenu(true));
		//popup.add( new SSAQueryResultsTableSelectionMenu(false));
	
     
        BasicStarPopupTable table = new BasicStarPopupTable(linksTable, false);
        table.setComponentPopupMenu(popup);
        table.addMouseListener(this);
        int tableheight= table.getRowCount()*table.getRowHeight();
        table.setPreferredScrollableViewportSize(new Dimension(750, tableheight));
        
		JScrollPane tablepanel = new JScrollPane(table);
		tablepanel.setBorder(new TitledBorder("Links "));
		
		JPanel buttonsPanel = new JPanel();
	    closeButton = new JButton("Close");
	    closeButton.addActionListener(this);
		buttonsPanel.add(closeButton);
		
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weighty=0;		
		panel.add(tablepanel, gbc);
		gbc.gridy++;
		
		int row;
		if ( ( row = getProcRow()) >= 0) {
			JPanel procPanel = getServiceFormPanel( row);
			panel.add( new JScrollPane(procPanel), gbc);
			gbc.gridy++;
		}
		if (servicePanel != null) {
			panel.add( new JScrollPane(servicePanel), gbc);
			gbc.gridy++;
		}
		gbc.weighty=1;
		//panel.add(buttonsPanel, gbc);
		this.add(panel, BorderLayout.PAGE_START);
		this.add(buttonsPanel, BorderLayout.PAGE_END);
		
		//this.add(new JScrollPane(panel));
		//Dimension size = panel.getSize();
		//this.setPreferredSize(size);
		this.repaint();
		this.setVisible(true);
	}

	

	// add datalink service form to the frame
/*	private JPanel getServicesPanel() {
		JPanel srvPanel = new JPanel();
		 srvPanel.setLayout(new BoxLayout(srvPanel, BoxLayout.Y_AXIS));
         Border empty = BorderFactory.createEmptyBorder(); 
         srvPanel.setBorder(empty);
         srvPanel.setAlignmentY(CENTER_ALIGNMENT);
		for (int row=0;row<linksTable.getRowCount();row++) {
			String sdef = dlparams.getServiceDefValue(row);
			if (sdef !=null) {
				DataLinkServiceResource service = dlparams.getDataLinkService(sdef);
				srvPanel.add(getServiceFormPanel(service, sdef));
			}
		}
		return null;
	}
*/
	public void changeContent(DataLinkResponse dlp) {
		this.setVisible(false);
		this.getContentPane().removeAll();
		linksTable = dlp.getLinksTable();
		dlparams=dlp;
		servicePanel=null;
		preview = getPreview();
		initUI();
		this.repaint();
		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		   Object source = e.getSource();
	        
	        if ( source.equals( closeButton ) ) {
	            this.setVisible(false);
	            return;
	        }
	        if ( source.equals( displayButton ) ) {
	        	
	            Logger.info(this,  "display Button clicked");
	            String serviceDef = ((JButton) source).getName();
	            DataLinkServiceResource service = dlparams.getDataLinkService(serviceDef);
	            displaySpectrum( service.getAccessURL(), servicePanel.getContentType(), servicePanel.getSemantics(), 
	            		         service.getDataLinkRequest(), service.getFieldRefID()) ;
	            
	            return;
	        }
	        if (source.getClass().equals(JMenuItem.class)) {
	        	  JMenuItem jmi  = (JMenuItem) e.getSource();
	              JPopupMenu jpm = (JPopupMenu) jmi.getParent();
	              BasicStarPopupTable table = (BasicStarPopupTable) jpm.getInvoker();
	              int row = table.getPopupRow();
	              table.setRowSelectionInterval(row, row);
	              String content="";
	              if ( source.equals(accurlMenuItem)) {
	            	  Logger.info(this,  "copy access url");
	            	  content = (String) table.getValueAt(row, dlparams.getAccessUrlIndex() );	            	 
				  }
	              else if ( source.equals(curCellMenuItem)) {
		            	  Logger.info(this,  "copy current cell");
		            	  content = JTableUtilities.getCurrentCellContent(table);  
				  }
		          if (content != null) 
		          	  Utilities.addStringToClipboard(content);
	            return;
	        }	      
		
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
    	if ( e.getClickCount() == 2 ) {
    		StarJTable table = (StarJTable) e.getSource();
    		Point p = e.getPoint();
    		int row = table.rowAtPoint( p );
    		if (row >= 0 && row < table.getRowCount()) {
                table.setRowSelectionInterval(row, row);
            } else {
                table.clearSelection();
            }
    		processDatalinkService(row);
    		Logger.info(this, "clicked in datalink row"+row);

    	}
    }



	private void processDatalinkService(int row) {
		
		DataLinkResponse dlr = dlparams;
		String id = dlr.getIDValue(row);
		String semantics = dlr.getSemanticsValue(row);
		String accessUrl = dlr.getAccessUrlValue(row);
		String serviceDef = dlr.getServiceDefValue(row);
		String contentType = dlr.getContentTypeValue(row);
		String errorMessage = dlr.getErrorMessageValue(row);
		DataLinkServiceResource service=null;
		
		if (errorMessage != null) {
			Logger.info(this, "error: "+errorMessage);
			return; 
			// TODO : blink line? make its background red? display message?
		}
		
		removeServicePanel();
		//
		// service_def:
		// 
		if (serviceDef != null) {
			// get service with this ID
			service = dlparams.getDataLinkService(serviceDef);
		}
		// 
		// semantics:  http://www.ivoa.net/rdf/datalink/core
		//
		switch (semantics) {
			case "#preview":    // open window with preview
				preview = addPreview(accessUrl);
				initUI();
				break;
			case "#this":	    // open spectrum
				displaySpectrum(accessUrl, contentType, semantics);
				break;
			case "#auxiliary":	// ??? try to open spectrum
				displaySpectrum(accessUrl, contentType, semantics);
				break;
			case "#proc":     // server side data processing
				// should already be added to the panel addServicePanel(service, serviceDef, contentType, semantics, id);
				break;
			case "#cutout":     // if service def is soda - open soda parameters panel
				addServicePanel(service, serviceDef, contentType, semantics, id);
				break;
			default:
				if (serviceDef != null)	{
					// try to add service panel
					addServicePanel(service, serviceDef, contentType, semantics, id);					
				}
		}

	}
	
	private void displaySpectrum(String accessUrl, String contentType, String semantics, String dataLinkRequest, String idsrc) {
		
		SpectrumIO.Props [] propList = new SpectrumIO.Props[1];	
		
		int type = SpecDataFactory.mimeToSPLATType(contentType);
		
		propList[0]= new SpectrumIO.Props(accessUrl, type, semantics);
		if (idsrc != null && dataLinkRequest != null) {
			propList[0].setDataLinkRequest(dataLinkRequest);
			propList[0].setIdValue(idsrc);
			propList[0].setServerURL(accessUrl);
			//accessUrl=null;
		}
		
		
		
		browser.displaySpectra(propList, true);
	}
	
	
	private void displaySpectrum(String accessUrl, String contentType, String semantics) {
		
		displaySpectrum( accessUrl, contentType, semantics, null, null );
		
	}

	private void addServicePanel(DataLinkServiceResource service, String serviceDef, String contentType, String semantics, String id) {
		servicePanel = getServiceFormPanel( service, serviceDef, contentType, semantics, id);
		if ( servicePanel != null )
			initUI();		
	}
	private void removeServicePanel() {
		servicePanel = null;
		initUI();		
	}

	private JPanel getServiceFormPanel(int row) {
		// return the server side form for a certain row (mainly semantics=#proc)
		String serviceDef = dlparams.getServiceDefValue(row);		
		return getServiceFormPanel(dlparams.getDataLinkService(serviceDef), serviceDef, dlparams.getContentTypeValue(row), dlparams.getSemanticsValue(row), dlparams.getIDValue(row));
		
	}
	private SodaPanel getServiceFormPanel( DataLinkServiceResource service, String serviceDef, String contentType, String semantics, String id) {
		if (service == null || !service.isSodaService()) {
			Logger.info(this, "No soda service "+serviceDef+"- handling not implemented yet\n");
			return null;
		}
	
		displayButton = new JButton("display");
		displayButton.addActionListener(this);
		displayButton.setName(serviceDef); // pass the service def as name
		
		DataLinkQueryFrame dlf = new DataLinkQueryFrame(displayButton);
		
		dlf.addServer(serviceDef, service);
		dlf.setServer(serviceDef);
		
	//	dlf.addToUI(serviceDef, service, displayButton);
		servicePanel = new SodaPanel();	
		servicePanel.addPanel(dlf.getContentPane());
		servicePanel.setService(service);
		servicePanel.setServiceDef(serviceDef);
		servicePanel.setContentType(contentType);
		servicePanel.setSemantics(semantics);
		servicePanel.setId(id);
		return  servicePanel;
		//JPanel sodaPanel = dlf.initServicePanel(service);// use a DataLinkQueryFrame (embed???)!!!!!!!!!!!!!!!!!!!!!!!
		/*
		JPanel panel = (JPanel) this.getContentPane();
		panel.add(dlf.getContentPane());
		Dimension size = panel.getSize();
		this.setPreferredSize(size);
		this.repaint();
		*/
	}

	private ImageIcon addPreview(String accessUrl) {

		try {

			URL url =new URL(accessUrl);
			BufferedImage preview = ImageIO.read(url);
		
			int h=preview.getHeight();
			int w=preview.getWidth();
			if ( h >  PREVIEWMAXHEIGHT || w > PREVIEWMAXWIDTH) {
				if (h > PREVIEWMAXHEIGHT)
					h = PREVIEWMAXHEIGHT;
				
				if (w > PREVIEWMAXWIDTH)
					w = PREVIEWMAXWIDTH;
				Image scaledpreview =  preview.getScaledInstance(w, h, Image.SCALE_DEFAULT);
				return new ImageIcon(scaledpreview);
				       
			}
			
			return new ImageIcon(preview);

		} catch (MalformedURLException e) {
			//do nothing - no preview
		} catch (IOException e) {
			//do nothing - no preview
		}
		return null;


	}
	
	protected class SodaPanel extends JPanel {
		DataLinkServiceResource service;
		String serviceDef="";
		String contentType="";
		String semantics="";
		String id = "";
		GridBagConstraints gbc;
		
		SodaPanel ( ) {
			gbc = new GridBagConstraints();			
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1;
			gbc.gridx = 0;
			this.setLayout(new GridBagLayout());

		}
		
		public void addPanel( Component panel ) {
			this.add(panel, gbc);
			gbc.gridx++;
		}
		
		public DataLinkServiceResource getService() {
			return service;
		}

		public void setService(DataLinkServiceResource service) {
			this.service = service;
		}

		public String getServiceDef() {
			return serviceDef;
		}

		public void setServiceDef(String serviceDef) {
			this.serviceDef = serviceDef;
		}

		public String getContentType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		public String getSemantics() {
			return semantics;
		}

		public void setSemantics(String semantics) {
			this.semantics = semantics;
		}
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}


		
	}

    
	
}
