package gaia.cu9.tools.parallax;

import java.io.IOException;

import java.util.HashMap;

import java.util.logging.Logger;

import gaia.cu9.tools.parallax.DistanceEstimator.EstimationType;
import gaia.cu9.tools.parallax.datamodel.DistanceEstimation;
import gaia.cu9.tools.parallax.datamodel.StarVariables;
import gaia.cu9.tools.parallax.readers.StarVariablesCsvReader;
import gaia.cu9.tools.parallax.readers.TestCsvSchema;
import gaia.cu9.tools.parallax.writers.CsvWriter;

/**
 * This class processes all the entries in an input file and 
 * stores the results in an output file.
 * 
 * There should be several input and output formats. The current
 * implementation is a single input format hardcoded for testing.
 * 
 * @author eutrilla
 *
 */
public class ParallaxProcessor {
	
	public void run() throws IOException{
		
		String inputPath = "testdata/sample_thin_disc_7000.csv";
//		String inputPath = "testdata/sim_7000.csv";
		String outputPath = "testdata/sample_thin_disc_7000_hs_expdecr_16380.csv";
		
		HashMap<String, String> parameters = new HashMap<String, String>();
//		parameters.put("logAxis", "false");
		parameters.put("nPoints", "16380");
//		parameters.put("integrateToInfinite", "false");
		
		StarVariablesCsvReader reader = new StarVariablesCsvReader(new TestCsvSchema());
		DistanceEstimator estimator = new DistanceEstimator(EstimationType.HS_EXP_DECR, parameters);
		CsvWriter writer = new CsvWriter();
		
		int nObjects = 0;
		long startTime = System.nanoTime();
		
		reader.load(inputPath);
		writer.open(outputPath);
		
		StarVariables star = reader.next();
		
		while(star!=null){
			
			DistanceEstimation estimation = estimator.estimate(star);
			writer.dump(estimation);
			nObjects++;
			
			star = reader.next();
		}
		
		long endTime = System.nanoTime();
		long ellapsedTime = (long)((endTime-startTime)/1000000);
		
		Logger.getLogger(this.getClass().getName()).info(nObjects + " processed in " + ellapsedTime + " ms.");

	}
	
	
	public static void main(String args[]) throws IOException{
		ParallaxProcessor instance = new ParallaxProcessor();
		instance.run();
		
	}

}
