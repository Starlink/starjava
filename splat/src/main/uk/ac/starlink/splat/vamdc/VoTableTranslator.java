package uk.ac.starlink.splat.vamdc;


import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import uk.ac.starlink.splat.data.ssldm.PhysicalQuantity;
import uk.ac.starlink.splat.data.ssldm.SpectralLine;






public class VoTableTranslator {
     
    /**
     *  VoTable Translator
     *  This class constructs a results VOTable containing data from spectral lines 
     *  (similar to  results from a SLAP query).
     *  It also contains methods to write the VOTable to a stream or to a file.
     */
    
    ArrayList<String> tableLines;
    String[] columns   = { "title", "wavelength", "error", "element", "initial energy", "final energy", 
                           "ion charge", "einsteinA", "initial level", "final level", "air wavelength", "oscillator strength","weighted oscillator strength", "strength","intensity"}; 
    
    String[] ucds      = { "em.line", "em.wl", "", "phys.atmol.element", "phys.energy;phys.atmol.initial;phys.atmol.level", "phys.energy;phys.atmol.final;phys.atmol.level", 
                           "phys.atmol.ionCharge", "phys.atmol.transProb", "phys.atmol.initial;phys.atmol.level", "phys.atmol.final;phys.atmol.level", "em.wl", "phys.atmol.oscStrength","phys.atmol.wOscStrength", "spect.line.strength","spect.line.intensity"}; 

    String[] utypes    = { "ssldm:line.title", "ssldm:line.wavelength.value", "", "ssldm:line.species", "ssldm:line.initialLevel.energy.value", "ssldm:line.finalLevel.energy.value", 
                           "", "ssldm:line.einsteinA.value", "ssldm:line.initialLevel", "ssldm:line.finalalLevel", "ssldm:line.airWavelength.value", "ssldm:line.oscillatorStrength","ssldm:line.weightedOscillatorStrength","ssldm:line.strength", "ssldm:line.intensity"}; 

    String[] datatypes = { "char", "double", "double", "char", "double", "double", 
                           "char", "double", "char", "char", "double", "double", "double"}; 

    ArrayList<SpectralLine> data;
    String query;
    
    
    public VoTableTranslator() {
        tableLines = new ArrayList<String>();
        
    }
    
    public VoTableTranslator(ArrayList<SpectralLine> data, String query) {
        this.data=data;
        this.query=query;
        tableLines = new ArrayList<String>();
        
    }
    
    public ArrayList<String> makeTable( ArrayList<SpectralLine> data, String query) {
        this.data=data;
        this.query=query;
        return makeTable();
       
    }
    
    public ArrayList<String> makeTable() {
        openTable();
        writeInfos(query);
        writeToTable(data);
        closeVoTable();
        return tableLines;
    }
    
    private void openTable() {
        tableLines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        tableLines.add("<VOTABLE xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        tableLines.add("xsi:noNamespaceSchemaLocation=\"xmlns:http://www.ivoa.net/xml/VOTable/VOTable-1.1.xsd\"");
        tableLines.add("xmlns:ssldm =\"http://www.ivoa.net/xml/SimpleSpectrumLineDM/SimpleSpectrumLineDMv1.0.xsd\" version=\"1.0\">");
        tableLines.add("<RESOURCE type=\"results\">");
    }
    public void writeInfos(String vamdcrequest){
        tableLines.add("<DESCRIPTION>GAVO VAMDC to SLAP translator</DESCRIPTION>");
        tableLines.add("<INFO name=\"QUERY_STATUS\" value=\"OK\"/>");
        tableLines.add("<INFO name=\"SERVICE_PROTOCOL\" value=\"1.0\">SLAP</INFO>");
       // tableLines.add("<INFO name=\"VAMDCREQUEST\" value=\""+vamdcrequest+"\"/>");        
    }
    public void writeToTable(ArrayList<SpectralLine> data){
        
     
     
        SpectralLine d=data.get(0);
        tableLines.add("<TABLE>");
       
        String [] units={"", makeUnit(d.getWavelength()),"", makeUnit(d.getInitialLevel().getEnergy()), makeUnit(d.getFinalLevel().getEnergy()),
                      "",  makeUnit(d.getEinsteinA()), "", "", makeUnit(d.getAirWavelength()),makeUnit(d.getOscillatorStrength()), makeUnit(d.getWeightedOscillatorStrength()) };

        for (int i=0;i<columns.length;i++) {
            if (datatypes[i].equals("char"))
                tableLines.add("<FIELD ucd=\""+ucds[i]+"\" name=\""+columns[i]+"\" utype=\""+utypes[i]+"\" datatype=\""+datatypes[i]+"\" arraysize=\"*\"/>");  
            else {
                tableLines.add("<FIELD ucd=\""+ucds[i]+"\" name=\""+columns[i]+"\" utype=\""+utypes[i]+"\" datatype=\""+datatypes[i]+"\""+units[i]+"\"/>");  
            }
        }
 
          writeTableData(data);
          tableLines.add("</TABLE>");
    }
    
  

    private String makeUnit(PhysicalQuantity pq) {
        try {
            String u= pq.getUnit().getExpression();
            if (u!=null)
                return "unit=\""+u+"\"";
        }catch(Exception e) {
        }
        return "";
        
    }
    public void writeTableData(ArrayList<SpectralLine> data){
        
        tableLines.add("<DATA><TABLEDATA>");
      
        for (int i=0;i<data.size();i++ ) {
            SpectralLine line = data.get(i);
            tableLines.add("<TR>");
            tableLines.add("<TD>"+ line.getTitle()+"</TD>"); 
            tableLines.add("<TD>"+ line.getWavelength().getValue() +"</TD>"); 
            tableLines.add("<TD>"+ line.getWavelength().getError() +"</TD>"); 
            tableLines.add("<TD>"+ line.getInitialElement().getElementName()+"</TD>"); 
            tableLines.add("<TD>"+ line.getInitialLevel().getEnergy().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getFinalLevel().getEnergy().getValue() +"</TD>");
            //if (line.getInitialElement().getIonizationStage() >= 0)
            //	tableLines.add("<TD>"+ line.getInitialElement().getIonizationStageRoman() +"</TD>");   
            tableLines.add("<TD>"+ line.getInitialElement().getIonizationStage() +"</TD>"); 
           // else tableLines.add("<TD></TD>");
            tableLines.add("<TD>"+ line.getEinsteinA().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getInitialLevel().getConfiguration()+"</TD>"); 
            tableLines.add("<TD>"+ line.getFinalLevel().getConfiguration()+"</TD>"); 
            tableLines.add("<TD>"+ line.getAirWavelength().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getOscillatorStrength().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getWeightedOscillatorStrength().getValue()+"</TD>"); 
            tableLines.add("<TD>"+ line.getStrength().getValue()+"</TD>"); 
      //      tableLines.add("<TD>"+ line.getIntensity().getValue()+"</TD>"); 
            tableLines.add("</TR>");
        }
        tableLines.add("</TABLEDATA></DATA>");
    }
    
    public void closeVoTable() {
        tableLines.add("</RESOURCE>");
        tableLines.add("</VOTABLE>");
        
    }
    public void writeToFile(String file) throws IOException {
        BufferedWriter bw = null;
        bw = new BufferedWriter(new FileWriter(file));
        for (String l :tableLines) {
          bw.write(l);
          bw.newLine();
        }
        bw.flush();  
        bw.close();  
    }
    
    
    public void writeToStream(OutputStream out) throws IOException {
        BufferedOutputStream bw = null;
        bw = new BufferedOutputStream(out);
        for (String l :tableLines) {
          bw.write(l.getBytes());
         // bw.newLine();
        }
        bw.flush();  
        bw.close();  
    }
    
    public void writeToFile() throws IOException {
        
        JFileChooser fc = new JFileChooser();
        int returnval = fc.showOpenDialog(null);
        if (returnval == JFileChooser.APPROVE_OPTION) {
            String filename=fc.getSelectedFile().getName();
            writeToFile( filename );
        }
    }
    
   
    public String[] getColumns() {
     
        return columns;
    }
    
   public String getUtype(int index) {
           return utypes[index];
       
   }
    
 
        
   
}
