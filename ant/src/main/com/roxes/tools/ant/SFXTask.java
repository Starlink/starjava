package com.roxes.tools.ant;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.zip.*;

import java.util.*;
import java.io.*;
/**
 * @version 	1.0
 * @since 		11.08.2003
 * @author		alvaro.diaz@roxes.com
 *
 * (c) Copyright 2003 ROXES Technologies (www.roxes.com).
 */
public class SFXTask extends Task
{
	File 	archive=null; 
	File	output=null; 
	String  workingDirectory="", 
			mode = null,
		 	execute=null;
	
	static final int TOKEN_LENGTH = "__EXECUTABLE__NAME__".length();
	
	public void setArchive( File archive)
	{
		this.archive = archive; 
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

	public void setWorkingDirectory(String string)
	{
		workingDirectory = string;
	}
	
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
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

		Properties p = new Properties();
		p.put( "__EXECUTABLE__PATH__", workingDirectory);
		p.put( "__EXECUTABLE__NAME__", execute);
		
		log( "Generating " + mode + " executable " + output);

		if( mode.startsWith( "java"))
		{
			try
			{
				byte[] buffer = new byte[8192];
				
				ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( output));
				ZipEntry ze = new ZipEntry( stub.class.getName().replace( '.', '/')+"16x16.gif");
				zos.putNextEntry( ze);
				int count;
				InputStream in = stub.class.getResourceAsStream( "stub16x16.gif");
				while((count = in.read( buffer)) != -1) {
				   zos.write( buffer, 0, count);
				} 
				ze = new ZipEntry( stub.class.getName().replace( '.', '/')+".class");
				zos.putNextEntry( ze);
				in = stub.class.getResourceAsStream( "stub.class");
				while((count = in.read( buffer)) != -1) {
				   zos.write( buffer, 0, count);
				}
				ze = new ZipEntry( stub.class.getName().replace( '.', '/')+".properties");
				zos.putNextEntry( ze);
				
				Properties stubProperties = new Properties();
				stubProperties.put( "__MODE__", mode.equals( "java-console") ? "console" : "gui");
				stubProperties.put( "__EXEC_NAME__", execute); 
				stubProperties.put( "__EXEC_PATH__", workingDirectory);
				stubProperties.save( zos, "ROXES stub 1.0 properties");
				zos.close();
			}
			catch(Exception e)
			{
				throw new  BuildException( "Unable to copy jar stub", e);
			}
			
			
			
			org.apache.tools.ant.taskdefs.Jar jar = new org.apache.tools.ant.taskdefs.Jar();
			//jar.setBasedir( archive.getParentFile());
			jar.setCompress( true);
			jar.setUpdate( true);
			ZipFileSet zfs = new ZipFileSet();
			zfs.setSrc( archive);
			jar.addZipfileset( zfs);
			
			//jar.setIncludes( archive.getName() + "/**/*");
			jar.setDestFile( output);
			
			try
			{
				Manifest manifest = Manifest.getDefaultManifest();
				Manifest.Section section = manifest.getMainSection();
				section.addConfiguredAttribute( new Manifest.Attribute( "Main-Class", "com.roxes.tools.ant.stub"));
				/*
				section = new Manifest.Section();
				section.setName( "stub");
				
				section.addConfiguredAttribute( new Manifest.Attribute( "__MODE__", mode.equals( "java-console") ? "console" : "gui"));
				section.addConfiguredAttribute( new Manifest.Attribute( "__EXEC_NAME__", execute));
				section.addConfiguredAttribute( new Manifest.Attribute( "__EXEC_PATH__", workingDirectory));
				
				manifest.addConfiguredSection( section);
				*/
				jar.addConfiguredManifest( manifest);
				jar.setOwningTarget( getOwningTarget());
				jar.setProject( getProject());
				jar.setTaskName( getTaskName());
				jar.execute();
			}
			catch (ManifestException ex)
			{
				throw new BuildException( "ManifestException occured.", ex);
			}
		}
		else
		{
			try
			{
				FileOutputStream out = new FileOutputStream( output);
			
				byte scanBuffer[] = new byte[ TOKEN_LENGTH];
			
				if( mode.startsWith( "win32"))
				{
					InputStream in = SFXTask.class.getResourceAsStream( mode.indexOf( "console")!=-1 ? "stub-console.exe" : "stub.exe");
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
					InputStream in = SFXTask.class.getResourceAsStream( "stub.sh");
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
	}

	static byte[] buffer = new byte[8192];

	public static void copy( File file, OutputStream out) throws IOException
	{
		FileInputStream in = new FileInputStream( file);
		int read = 0;
		while( (read=in.read( buffer))!=-1)
		{
			out.write( buffer, 0, read);
		}
		in.close();
	}

	private static void shiftArray(byte[] array)
	{
		for(int i=0; i<(array.length-1); i++)
			array[i]=array[i+1];

		array[array.length-1]=0;
	}
}
