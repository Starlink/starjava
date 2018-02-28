package gaia.cu9.tools.parallax.writers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.LoggerFactory;

import gaia.cu9.tools.parallax.datamodel.DistanceEstimation;

public class CsvWriter {

	protected BufferedWriter writer = null;
	protected String path = null;
	
	public void open(String path) throws IOException{
		
		close();
		
		writer = new BufferedWriter(new FileWriter(path));
		this.path = path;
		
	}
	
	public void dump(DistanceEstimation estimation) throws IOException{
		String line = estimation.getSourceId() + ", " + 
	                  estimation.getBestDistance() + ", " + 
	                  estimation.getDistanceInterval()[0] + ", " +
	                  estimation.getDistanceInterval()[1] + ", " +
	                  estimation.getBestModulus() + ", " + 
	                  estimation.getModulusInterval()[0] + ", " +
	                  estimation.getModulusInterval()[1] + "\n";
		writer.write(line);
	}
	
	public void close(){
		if (writer!=null){
			try {
				writer.close();
			} catch (IOException e) {
				LoggerFactory.getLogger(this.getClass()).warn("Exception closing file " + path, e);
			}
		}
		path = null;
		writer = null;
	}
}
