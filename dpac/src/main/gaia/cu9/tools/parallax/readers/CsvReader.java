package gaia.cu9.tools.parallax.readers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gaia.cu9.tools.parallax.datamodel.DistanceEstimation;

public abstract class CsvReader<T> {
	

	
	protected CsvSchema<T> schema = null;
	protected long currentEntryNumber = 0;
	protected Logger logger;
	protected String path = null;
	
	protected BufferedReader reader;
	
	public CsvReader(CsvSchema<T> schema){
		this.schema = schema;
		currentEntryNumber = 0;
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	public void load(String path) throws IOException{
		close();
		
		reader = new BufferedReader(new FileReader(path));
		this.path = path;
		
		for (int i=0; i<schema.getNLinesToDiscard(); i++){
			reader.readLine();
		}
	}
	
	public T next() throws IOException{
		String line = reader.readLine();
	
		while(line!=null && line.trim().startsWith(schema.getSkipLineMarker())){
			line = reader.readLine();
		}
		
		if (line != null){
			return processLine(line);
		} else {
			return null;
		}
	}
	
	public void close(){
		if (reader!=null){
			try {
				reader.close();
			} catch (IOException e) {
				LoggerFactory.getLogger(this.getClass()).warn("Exception closing file " + path, e);
			}
		}
		path = null;
		reader = null;
	}
	
	public List<T> readAll(String inputPath){

		List<T> entries = new ArrayList<T>();
		try {
			this.load(inputPath);
			
			T entry = this.next();
			
			while(entry!=null){
				
				if (entries.size()%1000==0){
					logger.info(entries.size() + " entries processed...");
				}
				entries.add(entry);
				
				entry = this.next();
			}
	
		} catch (Exception e) {
			LoggerFactory.getLogger(this.getClass()).error("Exception", e);
		} finally {
			if (this!=null){
				this.close();
			}
		}
		
		return entries;
	}
	
	
	
	protected T processLine(String line){
		
		StringTokenizer tokenizer= new StringTokenizer(line, schema.getSeparators());
		
		List<String> tokens = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()){
			String token = tokenizer.nextToken();
			if (!token.trim().equals("")){
				tokens.add(token);
			}
		}
		
		currentEntryNumber++;

		return buildObject(currentEntryNumber, tokens);
		
	}
	
	protected abstract T buildObject(long entryNumber, List<String> tokens);
	
	protected double getDouble(String key, List<String> values, double defaultValue){
		double val = defaultValue;
		Integer index = schema.getColumnIndex(key);
		if (index!=null && index>=0){
			try{
				val = Double.valueOf(values.get(schema.getColumnIndex(key)));
			} catch (Exception e){
				logger.error("Error reading value for " + key + " in entry " + currentEntryNumber, e);
			}
		}
		return val;
	}
	
	protected long getLong(String key, List<String> values, long defaultValue){
		long val = defaultValue;
		Integer index = schema.getColumnIndex(key);
		if (index!=null && index>=0){
			try{
				val = Long.valueOf(values.get(schema.getColumnIndex(key)));
			} catch (Exception e){
				logger.error("Error reading value for " + key + " in entry " + currentEntryNumber, e);
			}
		}
		return val;
	}
}
