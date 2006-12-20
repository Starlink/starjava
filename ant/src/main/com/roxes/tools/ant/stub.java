/**
 * @version 	1.0
 * @since 		29.08.2003
 * @author		alvaro.diaz@roxes.com
 *
 * (c) Copyright 2003 ROXES Technologies (www.roxes.com).
 */

package com.roxes.tools.ant;
import java.util.jar.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class stub
{
	String __EXECUTABLE_NAME__ = null,
		   __EXECUTABLE_PATH__ = null; 
	
	boolean consoleMode = false;
		
	File tempDir = null;
	
	byte[] buffer = new byte[8192];
	
	JFrame jframe = null;
	JProgressBar progressBar=null;
	
	public static void main(String[] args) throws Exception
	{
		/*
		Enumeration e = System.getProperties().keys();
				while( e.hasMoreElements())
				{
					String key = (String)e.nextElement();
					System.out.println( key + "=" + System.getProperty( key));
				} */
		
		stub s = new stub();
		
		s.unjar( System.getProperty( "java.class.path"));
	}
	
	int bytesToWrite = 0;
	int bytesWritten = 0;
	
	public void unjar( String s) throws Exception
	{
		if( s.indexOf( File.separatorChar)==-1)
			tempDir = new File( new File( System.getProperty( "java.io.tmpdir")), s);
		else
			tempDir = new File( new File( System.getProperty( "java.io.tmpdir")), s.substring( s.lastIndexOf( File.separatorChar)+1));
		
		JarFile jf = new JarFile( new File( s));
	
		Properties properties = new Properties();
		properties.load( stub.class.getResourceAsStream( "stub.properties"));

		__EXECUTABLE_NAME__ = properties.getProperty( "__EXEC_NAME__");		
		__EXECUTABLE_PATH__ = properties.getProperty( "__EXEC_PATH__");
		//System.out.println( __EXECUTABLE_NAME__ + ", " + __EXECUTABLE_PATH__);

		consoleMode = "console".equalsIgnoreCase( properties.getProperty( "__MODE__"));
		
		if( !consoleMode)
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName());
			
			jframe = new JFrame( "Preparing ...");
			jframe.setIconImage( new ImageIcon( stub.class.getResource( "stub16x16.gif")).getImage());
			((JComponent)jframe.getContentPane()).setBorder( BorderFactory.createEmptyBorder( 10, 10 , 10, 10));
			jframe.getContentPane().add( progressBar = new JProgressBar( JProgressBar.HORIZONTAL));

			jframe.setCursor( Cursor.WAIT_CURSOR);
			jframe.pack();
			
			Dimension d = jframe.getSize();
			Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
			Rectangle pp = new Rectangle( 0, 0, ss.width, ss.height);
			if( pp.x + (pp.width-d.width)/2<0 || pp.y + (pp.height-d.height)/2<0)
				pp = new Rectangle( 0, 0, ss.width, ss.height);
			jframe.setBounds( pp.x + (pp.width-d.width)/2, pp.y + (pp.height-d.height)/2, d.width, d.height);
			
			jframe.setVisible( true);
		}
		
		ArrayList al = new ArrayList();
		
		Enumeration e = jf.entries();
		while (e.hasMoreElements())
		{
			JarEntry entry = (JarEntry) e.nextElement();
			if( !(entry.isDirectory() && entry.getName().startsWith( "META-INF") && entry.getName().equals( "com/roxes/tools/ant/stub.class") && entry.getName().equals( "com/roxes/tools/ant/stub16x16.gif")))
			{
				if( entry.isDirectory())
				{
					String name = entry.getName();
					name = name.replace( '/', File.separatorChar); 

					File file = new File( tempDir, name);

					if( !file.exists())
					{	
						boolean success = file.mkdirs();
						if( !success)
							alert( "Error occured while unpack distribution:\nCreating directory\n" + file.getAbsolutePath() + "\nfailed !");  
					} 
				}
				else
				{
					//System.out.println( "adding " + entry.getName());
					al.add( entry);
					bytesToWrite += entry.getSize();
				}
			}
		}

		if( !consoleMode)
		{
			progressBar.setMaximum( bytesToWrite);
			progressBar.setMinimum( 0);
			progressBar.setValue( 0);
		}
		
		for (int i = 0; i < al.size(); i++)
		{
			JarEntry entry = (JarEntry)al.get( i);
			writeEntry( jf, entry);
		}
		System.out.println( "");
		

		File execDir = tempDir;
		if( __EXECUTABLE_PATH__!=null)
			execDir = new File( tempDir, __EXECUTABLE_PATH__.replace( '/', File.separatorChar));
		
		String osName = System.getProperty("os.name" );
		if( !consoleMode && osName.indexOf( "Windows")!=-1)
		{
			__EXECUTABLE_NAME__ = "start /B " + __EXECUTABLE_NAME__;
		}
		
		StringTokenizer st = new StringTokenizer( __EXECUTABLE_NAME__);
		int count = st.countTokens();
		String[] cmdarray = new String[ count];
		count = 0;
		while (st.hasMoreTokens()) 
			cmdarray[count++] = st.nextToken();
		
		for (int i = 0; i < cmdarray.length; i++)
		{
			String string = cmdarray[i];
			System.out.println( "cmd["  + i + "]=" + string);
		}
		
		System.out.println( "executed in directory " + execDir);
		
		Process p=null;
		try
		{
			p = Runtime.getRuntime().exec( cmdarray, null, execDir);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			alert( "Error occured while executing\n" + __EXECUTABLE_NAME__ + "\nin path\n" + execDir);
		}
		/*
		InputStream inputstream = p.getInputStream();
		InputStreamReader inputstreamreader =
			new InputStreamReader(inputstream);
		BufferedReader bufferedreader =
			new BufferedReader(inputstreamreader);

		// read the ls output

		String line;
		while ((line = bufferedreader.readLine()) 
				  != null) {
			System.out.println(line);
		}
		*/
		if( !consoleMode)
			jframe.dispose();
	}
	
	public void writeEntry( JarFile jarFile, JarEntry entry)
	{
		String name = entry.getName();
		name = name.replace( '/', File.separatorChar); 
		
		File file = new File( tempDir, name);
		
		File dir = file.getParentFile();
		if( !dir.exists())
		{	
			boolean success = dir.mkdirs();
			if( !success)
				alert( "Error occured while unpack distribution:\nCreating directory\n" + dir.getAbsolutePath() + "\nfailed !");  
		} 
		
		try
		{
			BufferedInputStream bis = new BufferedInputStream( jarFile.getInputStream( entry));
			BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream( file));
			
			// System.out.println( "Copying " + file.getName() + " to " + dir.getAbsolutePath());
			if( consoleMode)
				System.out.print( ".");
	
			int read;
			while( (read=bis.read( buffer))!=-1)
			{
				bos.write( buffer, 0, read);
				if( progressBar!=null)
					progressBar.setValue( (bytesWritten+=read));
			}
			
			bis.close();
			bos.close();
		}
		catch( Exception e)
		{
			alert( "Error occured while unpack distribution:\nCopying resource\n" + entry.getName() + "\nto directory\n" + dir.getAbsolutePath() + "\nfailed !");
		}		
	}
	
	public void alert( String s)
	{
		if( consoleMode)
			System.err.println( "Error: " + s);
		else
			JOptionPane.showMessageDialog( jframe, s, "Error", JOptionPane.ERROR_MESSAGE);
		
		if( !consoleMode)
			jframe.dispose();
			 	
		System.exit( 0);
	}
}