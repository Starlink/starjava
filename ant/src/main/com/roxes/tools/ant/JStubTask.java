package com.roxes.tools.ant;

import org.apache.tools.ant.*;
import java.io.*;
import java.util.*;

/**
 * @version 	1.0
 * @since 		11.08.2003
 * @author		alvaro.diaz@roxes.com
 *
 * (c) Copyright 2003 ROXES Technologies (www.roxes.com).
 */
public class JStubTask extends Task
{
	File archive=null, output=null;
	
	String mode=null, execute=null;
	
	static byte[] buffer = new byte[8192];

	static void copy( File file, OutputStream out) throws IOException
	{
		FileInputStream in = new FileInputStream( file);
		int read = 0;
		while( (read=in.read( buffer))!=-1)
		{
			out.write( buffer, 0, read);
		}
		in.close();
	}

	static final int TOKEN_LENGTH = "__EXECUTABLE_STRING__".length();
	
	public void execute() throws BuildException
	{
		if( archive==null)
			throw new BuildException( "archive attribute not set");

		if( execute==null)
			throw new BuildException( "execute attribute not set");
	
		if( mode==null)
			throw new BuildException( "mode attribute not set");			

		if( output==null)
			throw new BuildException( "output attribute not set");	
			
		log( "Generating " + mode + " executable " + output);
		
		Properties p = new Properties();
		p.put( "__EXECUTABLE_STRING__", execute);
		
		try
		{
			FileOutputStream out = new FileOutputStream( output);
			
			byte scanBuffer[] = new byte[ TOKEN_LENGTH];
			
			if( mode.startsWith( "win32"))
			{
				InputStream in = JStubTask.class.getResourceAsStream( mode.indexOf( "console")!=-1 ? "jstub-console.exe" : "jstub.exe");
				in.read( scanBuffer, 0, scanBuffer.length);
				long position=1;
				while( p.size()>0)
				{
					while( !p.containsKey( new String(scanBuffer)))
					{
						out.write( scanBuffer[0]);
							// read a byte
						int nextb=in.read();
						position++;
						shiftArray(scanBuffer);
							// write a byte
						scanBuffer[scanBuffer.length-1]=(byte)nextb;
					}
			
					String value = p.getProperty( new String(scanBuffer));
					p.remove( new String(scanBuffer));
					log( new String(scanBuffer) + " at " + (position-1) + " replaced by " + value);
					out.write( value.getBytes());
					for( int j=value.getBytes().length; j<128; j++)
					{
						out.write( 0);
					}
					in.skip( 128 - TOKEN_LENGTH);
					position+=128 - TOKEN_LENGTH;
					in.read( scanBuffer, 0, scanBuffer.length);
				}
				out.write( scanBuffer);
				int read = 0;
				while( (read=in.read( buffer))!=-1)
					out.write( buffer, 0, read);
				in.close();
			}
			else if( mode.equals( "unix"))
			{
				InputStream in = JStubTask.class.getResourceAsStream( "jstub.sh");
				in.read( scanBuffer, 0, scanBuffer.length);
				long position=1;
				while( p.size()>0)
				{
					while( !p.containsKey( new String(scanBuffer)))
					{
						out.write( scanBuffer[0]);
							// read a byte
						int nextb=in.read();
						position++;
						shiftArray(scanBuffer);
							// write a byte
						scanBuffer[scanBuffer.length-1]=(byte)nextb;
					}
			
					String value = p.getProperty( new String(scanBuffer));
					p.remove( new String(scanBuffer));
					System.out.println( new String(scanBuffer) + " at " + (position-1) + " replaced by " + value);
					out.write( value.getBytes());
					in.read( scanBuffer, 0, scanBuffer.length);
				}
				out.write( scanBuffer);
				int read = 0;
				while( (read=in.read( buffer))!=-1)
					out.write( buffer, 0, read);
				in.close();
			}
			else
				throw new BuildException( "Unknown mode " + mode);
			
			copy( archive, out);
			
			out.close();
		}
		catch (FileNotFoundException e)
		{
			throw new BuildException( "Cannot open archive", e);
		}
		catch (IOException e)
		{
			throw new BuildException( "IO Problem occured", e);
		}
	}

	public void setArchive(File file)
	{
		archive = file;
	}

	public void setExecute(String string)
	{
		execute = string;
	}

	public void setMode(String string)
	{
		mode = string;
	}

	public void setOutput(File file)
	{
		output = file;
	}
	
	private static void shiftArray(byte[] array)
	{
		for(int i=0; i<(array.length-1); i++)
			array[i]=array[i+1];

		array[array.length-1]=0;
	}
}
