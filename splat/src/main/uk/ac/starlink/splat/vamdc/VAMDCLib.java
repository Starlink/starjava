package uk.ac.starlink.splat.vamdc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.vamdc.registry.client.Registry;
import org.vamdc.registry.client.RegistryFactory;


import gavo.spectral.lines.VoTableTranslator;
import gavo.spectral.lines.XSAMSParser;

import gavo.spectral.ssldm.PhysicalQuantity;
import gavo.spectral.ssldm.SpectralLine;
import net.ivoa.xml.voresource.v1.Contact;
import net.ivoa.xml.voresource.v1.Resource;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowListStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;


public class VAMDCLib {
    
    /* 
     * This class includes several functions to communicate with VAMDC services and
     * interpret the returned data.
     * @author Margarida Castro Neves
     * 09.2016
     */
    
    public VAMDCLib() {
        // Constructor
    }
    
    
    /* 
     * Query the VAMDC Registry and get a table of databases 
     */
    
    public static JTable queryRegistry(){
        
        JTable table = new JTable(new DefaultTableModel(new Object[]{"short name", "title", "description", "identifier",
                "publisher", "contact", "access URL", "reference URL"}, 0));

        // from SSAP server table, how to map? "waveband", "content type", "data source", "creation type", "stantardid", "version", "subjects", "tags"
        DefaultTableModel model = (DefaultTableModel) table.getModel();
   
        try{
             
            Registry reg = RegistryFactory.getClient(RegistryFactory.REGISTRY_12_07);
            System.out.println("Queried Registry");
           
            for (String ivoid : reg.getIVOAIDs(Registry.Service.VAMDC_TAP)){
                try {
                    Resource r = reg.getResourceMetadata(ivoid);
                    String title = r.getTitle();
                    String description = r.getContent().getDescription();
                    String shortname = r.getShortName();
                    if (shortname == null || shortname.isEmpty() ) {
                        shortname = title;
                    }

                    String publisher = r.getCuration().getPublisher().getValue();
                    
                    List<Contact> contacts = r.getCuration().getContact();
                    String contact="";
                    if (contacts!= null)
                        contact = contacts.get(0).getName().getValue()+" "+contacts.get(0).getEmail();
                    String refURL= r.getContent().getReferenceURL();
                    System.out.println(title);
                    System.out.println(ivoid);

                    System.out.println("URL : "+reg.getVamdcTapURL(ivoid));
                    model.addRow(new Object[]{ shortname, title, description, r.getIdentifier(), publisher, contact, reg.getVamdcTapURL(ivoid), refURL });
                } catch (Exception e ) { 
                    System.out.println(e.getMessage());
                }

            }
            table.setModel(model);

        } catch (Exception e ) { //catch (RegistryCommuicationException e) {
            System.out.println(e.getMessage());
            
        }
        return table;
    }
    
    /** 
     * Query a VAMDC database and return a table of results
     *  
     * @param query a string containing a VAMDC-TAP Query
     * @param inps the input string of the connected database where to send the query
     * @return a StarTable containing the parsed response
     * @throws Exception
     */
    public StarTable getResultStarTable(String query, InputStream inps ) throws Exception { 
        
         
        ArrayList<SpectralLine> lines = new ArrayList<SpectralLine>();
        XSAMSParser  xsams;
        try {
            xsams = new XSAMSParser(inps); 
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw e;
            //return null;
        }
        lines = xsams.getSpectralLines();
        VoTableTranslator votable = new VoTableTranslator(lines, query);
        
        return makeStarTable(votable, lines);
        
    }

    /* 
     *  Transforms the SpectralLine objects into a StarTable
     */
    private StarTable makeStarTable(VoTableTranslator votable , ArrayList<SpectralLine> lines) {
        

      //  ArrayList<String> tableLines = votable.makeTable();
        // do not create votable; translate direct from the line array - easier to parse right now
        String [] cols = votable.getColumns();
        ColumnInfo[] columns = new ColumnInfo[cols.length];
        for (int i=0;i<cols.length;i++) {
            columns[i]=new ColumnInfo(getValueInfo(lines.get(0), cols[i]));
        }
       
        RowListStarTable rlst = new RowListStarTable(columns);
        for (SpectralLine l :lines) {
    //        Object[] line = new Object[cols.length];
           // Row
           rlst.addRow(getRow(l, columns));
          }
        
        return rlst;
    }

    private ValueInfo getValueInfo(SpectralLine line, String colname) {
 
        if (colname.equals("title")) 
            return  makeValueInfo(colname, "ssldm:Line.title");
        if (colname.equals("wavelength")) 
            return  makeValueInfo(colname, line.getWavelength());

        if (colname.equals("initial energy")) 
            return  makeValueInfo(colname, line.getInitialLevel().getEnergy());
           
        if (colname.equals("final energy"))
            return makeValueInfo(colname, line.getFinalLevel().getEnergy());
        if (colname.equals("einsteinA"))
            return makeValueInfo(colname, line.getEinsteinA());
        if (colname.equals("initial level"))
            return makeValueInfo(colname,  "ssldm:line.initialLevel.name");
        if (colname.equals("final level"))
            return makeValueInfo(colname,  "ssldm:line.finalLevel.name");
        if (colname.equals("air wavelength"))
            return makeValueInfo(colname, line.getAirWavelength());
        if (colname.equals("oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength());
        if (colname.equals("weighted oscillator strength"))
            return makeValueInfo(colname, line.getOscillatorStrength());
        return null;
       
}
    private ValueInfo makeValueInfo(String colname,  String utype) {
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUtype(utype);       
        return vinfo;
        
    }

    private DefaultValueInfo makeValueInfo(String colname, PhysicalQuantity pq) {
        //System.out.println("colname "+ colname);
        DefaultValueInfo vinfo = new DefaultValueInfo(colname);
        vinfo.setUnitString(pq.getUnitExpression());
        vinfo.setUCD(pq.getUcd());
        vinfo.setUtype(pq.getUtype());
    
        return vinfo;
    }

 
    private Object[] getRow(SpectralLine line, ColumnInfo[] cols) {
        Object[] row = new Object[cols.length];
        for (int i=0;i<cols.length;i++) {
            if (cols[i].getName().equals("title"))
                row[i]=line.getTitle();            
            if (cols[i].getName().equals("wavelength"))
                row[i]=line.getWavelength().getValue(); 
            if (cols[i].getName().equals("initial energy"))
                row[i]=line.getInitialLevel().getEnergy().getValue();
            if (cols[i].getName().equals("final energy"))
                row[i]=line.getFinalLevel().getEnergy().getValue();
            if (cols[i].getName().equals("Einstein A"))
                row[i]=line.getEinsteinA().getValue();
            if (cols[i].getName().equals("initial level"))
                row[i]=line.getInitialElement();
            if (cols[i].getName().equals("initial level"))
                row[i]=line.getFinalElement();
            if (cols[i].getName().equals("air wavelength"))
                row[i]=line.getAirWavelength().getValue();
            if (cols[i].getName().equals("oscillator strength"))
                row[i]=line.getOscillatorStrength().getValue();
            if (cols[i].getName().equals("weighted oscillator strength"))
                row[i]=line.getOscillatorStrength().getValue();                
        }
        
        return row;
    }

}
