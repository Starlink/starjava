package com.roxes.tools.ant;

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;

import java.io.*;
import java.util.*;
import java.util.prefs.*;
/**
 * @version 	1.0
 * @since 		25.09.2003
 * @author		alvaro.diaz@roxes.com
 *
 * (c) Copyright 2003 ROXES Technologies (www.roxes.com).
 */

public class PreferencesTask extends Task
{
	String type=null, path=null;
	
	Preferences rootPrefs = null;
	
	ArrayList actions = new ArrayList();
	
	public abstract class Action implements Runnable
	{
		String name=null;
		
		String value=null;
		
		String path=null;
		
		public void setName(String string)
		{
			name = string;
		}

		public void setValue(String string)
		{
			value = string;
		}

		public void setPath(String string)
		{
			path = string;
		}
		
		public void addText( String s)
		{
			if( value==null && s.trim().length()>0)
				value = s.trim();
		}
		
		public void run()
		{
			if( name==null)
				throw new BuildException( "Attribute name is required in element " + getSubTaskName());
			
			if( value==null)
				throw new BuildException( "Attribute value is required in element " + getSubTaskName());
				
			Preferences p = rootPrefs; 	
			
			if( path!=null)
				p = p.node( path);
			
			execute( p);
		}
		
		String getSubTaskName()
		{
			String s = getClass().getName().substring( getClass().getName().lastIndexOf( '$')+1).toLowerCase();
			return s.startsWith( "_") ? s.substring( 1) : s;
		}
		
		public abstract void execute( Preferences p);
	}
	
	public class Int extends Action
	{
		public void execute( Preferences p)
		{
			try
			{
				int i = Integer.parseInt( value);
				p.putInt( name, i);
				
				rootPrefs.sync();
			}
			catch (NumberFormatException e)
			{
				throw new BuildException( getSubTaskName() + " : " + value + "Value is not parable as integer");
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}			
		}
	};
	
	public Int createInt()
	{
		Int r = new Int();
		actions.add( r);
		return r;
	}

	public class _String extends Action
	{
		public void execute( Preferences p)
		{
			p.put( name, value);
			
			try
			{
				rootPrefs.sync();
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}		
		}
	};

	public _String createString()
	{
		_String r = new _String();
		actions.add( r);
		return r;
	}

	public Test createTest()
	{
		Test r = new Test();
		actions.add( r);
		return r;
	}
	
	public class Import implements Runnable
	{
		java.io.File file=null;
		
		ArrayList fileSets = new ArrayList();
		
		public void addFileSet( FileSet fileSet)
		{
			fileSets.add( fileSet);
		}
		
		public void run()
		{
			if( (file==null && fileSets.size()==0) || (file!=null && fileSets.size()>0))
				throw new BuildException( "Either attribute file or child fileset(s) are required in element import");

			Preferences p = rootPrefs; 	

			try
			{
				if( file!=null)
				{
					log( "import preferences from " + file.getAbsolutePath(), Project.MSG_INFO);
					Preferences.importPreferences( new java.io.FileInputStream( file));
				}
				else
				{
					for( int i=0; i<fileSets.size(); i++)
					{
						FileSet fileSet = (FileSet)fileSets.get( i);
						DirectoryScanner directoryscanner = fileSet.getDirectoryScanner( getProject());
						String files[] = directoryscanner.getIncludedFiles();

						for (int j = 0; j < files.length; j++)
						{
							String string = files[j];
							log( "import preferences from " + string, Project.MSG_INFO);
							Preferences.importPreferences( new java.io.FileInputStream( new File( directoryscanner.getBasedir(), string)));
						}						
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				throw new BuildException( "Cannot access preferences from element remove", ex);
			}						
		}

		public void setFile(java.io.File file)
		{
			this.file = file;
		}
	}
	
	public Import createImport()
	{
		Import r = new Import();
		actions.add( r);
		return r;
	}

	public class Export implements Runnable
	{
		java.io.File file=null;
		boolean noSubElements = false;
		
		String path=null;
		
		public void run()
		{
			if( file==null)
				throw new BuildException( "Attribute file ist required in element export");

			Preferences p = rootPrefs; 	

			try
			{
				if( path!=null)
				{
					if( p.nodeExists( path))
						p = p.node( path);
					else
						throw new BuildException( "Export failed : node " + path + " doesnt exist within " + p);
				}				
				
				log( "export preferences tree " + p + " to " + file.getAbsolutePath(), Project.MSG_INFO);
				if( noSubElements)
					p.exportNode( new java.io.FileOutputStream( file));
				else	
					p.exportSubtree( new java.io.FileOutputStream( file));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				throw new BuildException( "Error executing element export", ex);
			}						
		}

		public void setFile(java.io.File file)
		{
			this.file = file;
		}
		

		
		public void setPath(String string)
		{
			path = string;
		}
		
		public void setNoSubElements(boolean b)
		{
			noSubElements = b;
		}

	}

	public Export createExport()
	{
		Export r = new Export();
		actions.add( r);
		return r;
	}

	
	public Get createGet()
	{
		Get r = new Get();
		actions.add( r);
		return r;
	}

	public Remove createRemove()
	{
		Remove r = new Remove();
		actions.add( r);
		return r;
	}

	public class Long extends Action
	{
		public void execute( Preferences p)
		{
			try
			{
				long i = java.lang.Long.valueOf( value).longValue();
				p.putLong( name, i);
				
				rootPrefs.sync();
			}
			catch (NumberFormatException e)
			{
				throw new BuildException( getSubTaskName() + " : " + value + "Value is not parsable as long");
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}						
		}
	};
	
	public Long createLong()
	{
		Long r = new Long();
		actions.add( r);
		return r;
	}
	
	public class Bool extends Action
	{
		public void execute( Preferences p)
		{
			value = value.toLowerCase().trim();
			if( !(value.equals("true") || value.equals("false")))
				throw new BuildException( "Attribute value must be either true or false for " + getSubTaskName());
		
			try
			{
				p.putBoolean( name, value.equals( "true"));
				
				rootPrefs.sync();
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}
		}
	};

	public Bool createBoolean()
	{
		Bool r = new Bool();
		actions.add( r);
		return r;
	}

	public class Float extends Action
	{
		public void execute( Preferences p)
		{
			try
			{
				float i = java.lang.Float.valueOf( value).floatValue();
				p.putDouble( name, i);
				
				rootPrefs.sync();
			}
			catch (NumberFormatException e)
			{
				throw new BuildException( getSubTaskName() + " : " + value + "Value is not parsable as float");
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}		
		}
	};

	public Float createFloat()
	{
		Float r = new Float();
		actions.add( r);
		return r;
	}
	
	public class Double extends Action
	{
		public void execute( Preferences p)
		{
			try
			{
				double i = java.lang.Double.valueOf( value).doubleValue();
				p.putDouble( name, i);
				
				rootPrefs.sync();
			}
			catch (NumberFormatException e)
			{
				throw new BuildException( getSubTaskName() + " : " + value + "Value is not parsable as double");
			}				
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}	
		}
	};

	public Double createDouble()
	{
		Double r = new Double();
		actions.add( r);
		return r;
	}

	public class ByteArray extends Action
	{
		String separator = ", ";
		int radix = 10; 
		
		public void execute( Preferences p)
		{
			StringTokenizer st = new StringTokenizer( value, separator);
			byte[] array = new byte[ st.countTokens()];
			int i = 0;
			while( st.hasMoreTokens())
			{
				String token = st.nextToken();
				try
				{
					array[i++] = Byte.parseByte( token, radix);
				}
				catch (NumberFormatException e)
				{
					throw new BuildException( getSubTaskName() + " : Cannot parse byte element " + token + " at positition " + i + " as byte.");
				}
			}
			
			try
			{
				p.putByteArray( name, array);
				rootPrefs.sync();
			}	
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element "+ getSubTaskName(), ex);
			}	
		}

		public String getSeparator()
		{
			return separator;
		}

		public void setSeparator(String string)
		{
			separator = string;
		}

		public int getRadix()
		{
			return radix;
		}

		public void setRadix(int i)
		{
			radix = i;
		}
	};

	public ByteArray createByteArray()
	{
		ByteArray r = new ByteArray();
		actions.add( r);
		return r;
	}

	public class Get implements Runnable
	{
		String name=null, path=null;
		String defaultValue=null;
		String property = null;
		
		public void run()
		{
			if( name==null)
				throw new BuildException( "Attribute name is required in element get");
	
			if( property==null)
				throw new BuildException( "Attribute property is required in element get");
				
			Preferences p = rootPrefs; 	

			try
			{
				if( path!=null)
				{
					if( p.nodeExists( path))
					{	
						p = p.node( path);
					}
					else
						throw new BuildException( "subtree " + path + " does not exist within preferences " + p);
				}
				
				List list = Arrays.asList( p.keys());
				if( list.contains( name))
				{	
					getProject().setProperty( property, p.get( name, defaultValue));
				}
				else if( defaultValue!=null)
					getProject().setProperty( property, defaultValue);
				
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element get", ex);
			}
		}

		public String getDefaultValue()
		{
			return defaultValue;
		}

		public String getName()
		{
			return name;
		}

		public String getPath()
		{
			return path;
		}

		public String getProperty()
		{
			return property;
		}

		public void setDefaultValue(String string)
		{
			defaultValue = string;
		}

		public void setName(String string)
		{
			name = string;
		}

		public void setPath(String string)
		{
			path = string;
		}

		public void setProperty(String string)
		{
			property = string;
		}
	}

	public class Remove implements Runnable
	{
		String node=null, name=null;
		String path=null;
		
		public void setName(String string)
		{
			name = string;
		}

		public void setNode(String string)
		{
			node = string;
		}

		public void setPath(String string)
		{
			path = string;
		}

		public void run()
		{
			if(  (name!=null && node!=null) || (name==null && node==null))
				throw new BuildException( "Attribute name OR node is required in element test");

			Preferences p = rootPrefs; 	

			try
			{
				if( path!=null)
				{
					if( p.nodeExists( path))
					{	
						p = p.node( path);
					}
				}

				if( name!=null)
				{
					List list = Arrays.asList( p.keys());
					if( list.contains( name))
					{
						p.remove( name);	
					}
					else
						log( "remove : a key " + name + " doesnt exist within preference " + p, Project.MSG_INFO);
				}
				else
				{
					if( p.nodeExists( node))
						p.node( node).removeNode();
					else
						log( "remove : a node " + node + " doesnt exist within preference " + p, Project.MSG_INFO);		
				}
				rootPrefs.sync();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				throw new BuildException( "Cannot access preferences from element remove", ex);
			}			
		}
	}

	public class Test implements Runnable
	{
		String node=null, name=null;
		String path=null;
		String value=null;
		String property = null;
		
		public void setName(String string)
		{
			name = string;
		}

		public void setNode(String string)
		{
			node = string;
		}

		public void setPath(String string)
		{
			path = string;
		}

		public void setProperty(String string)
		{
			property = string;
		}

		public void setValue(String string)
		{
			value = string;
		}
		
		public void run()
		{
			if( (name!=null && node!=null) || (name==null && node==null))
				throw new BuildException( "Attribute name OR node is required in element test");

			if( property==null)
				throw new BuildException( "Attribute property is required in element test");
		
			if( node!=null && value!=null)
				throw new BuildException( "Using attribute value in conjunction  with attribute node doesnt make sense.");
		
			Preferences p = rootPrefs; 	

			try
			{
				if( path!=null)
				{
					if( p.nodeExists( path))
					{	
						p = p.node( path);
					}
				}
		
				if( name!=null)
				{
					List list = Arrays.asList( p.keys());
					if( list.contains( name))
					{	
						if( value==null || p.get( name, "").equals( value))
							getProject().setProperty( property, "true");  
					}
				}
				else
				{
					if( p.nodeExists( node))
						getProject().setProperty( property, "true");
				}
				
				rootPrefs.sync();
			}
			catch (BackingStoreException ex)
			{
				throw new BuildException( "Cannot access preferences from element get", ex);
			}
		}		
	}
	
	public void execute() throws BuildException
	{			
		if( "system".equals( type))
			rootPrefs = Preferences.systemRoot();
		else if( "user".equals( type))
			rootPrefs = Preferences.userRoot();
		else 
			throw new BuildException( "Attribute type is required (must be \"user\" oder \"system\")");
				
		if( path!=null)
			rootPrefs = rootPrefs.node( path);
			
		if( actions.size()==0)
			log( "No action(s) applied to task preferences.",  Project.MSG_WARN);
			
		Iterator iter = actions.iterator();
		while (iter.hasNext())
		{
			Runnable r = (Runnable)iter.next();
			r.run();
		}
	}	

	public void setPath(String string)
	{
		path = string;
	}

	public void setType(String string)
	{
		type = string;
	}

}
