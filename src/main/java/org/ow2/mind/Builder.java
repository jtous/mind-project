package org.ow2.mind;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 
 * @author Julien TOUS
 * Front end class to the various mind tools (mindc, mindunit, minddoc)
 * FIXME(minddoc) isn't done yet.
 */

public class Builder  {
	
	/**
	 * Display a custom message and the help message and then exit with error
	 * @param errorString A custom message to print out.
	 * Should be representative of the error
	 */
	private static void error(String errorString) {
		final PrintStream ps = System.err;
		String progName="mind-builder";
		ps.println("Error : " + errorString);
		ps.println("Usage: " + progName	+ " ConfigurationFile");
		ps.println("  Where ConfigurationFile contains the necessary build properties");				
	}

	/**
	 * Entry point for the application
	 * @param args 
	 * @param args[0] Should be the command to execute
	 * @param args[1] Should be the file containing the project properties
	 */
	public static void main(final String... args)
	{
		/*Instantiate an object of this class*/
		Builder builder = new Builder();
		/*Initialize environment properties and mindRoot and mindDocRoot*/
		builder.setMindEnv();
		/*Initialize configuration properties and command*/
		builder.treatArgs(args);
		/*Enhance the classpath with required jars*/
		builder.setClassPath();
		
		if (builder.command.equals("doc")) {
			//FIXME handle mindoc stuff here!
		} else if (builder.command.equals("build")){
			builder.build();
		} else if (builder.command.equals("test")){
			builder.test();
		} else if (builder.command.equals("draw")){
			builder.draw();
		} else if (builder.command.equals("clean-build")){
			builder.cleanBuild();
		} else if (builder.command.equals("clean-test")){
			builder.cleanTest();
		} else if (builder.command.equals("clean-draw")){
			builder.cleanDraw();
		} else {
			error("Unhandled command \"" + builder.command + "\".");
		}
	}
	/**
	 * Helper to add an URL to the current classpath
	 * @param u the URL
	 * @throws IOException
	 */
	private static void addURL(URL u) throws IOException
	{	
		final Class[] parameters = new Class[] {URL.class};
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;

		try {
			Method method = sysclass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] {u});
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}
	}

	/**
	 * Helper to delete directory recursively
	 * @param dir the directory to delete
	 * @return the status of the operation
	 */
	private static boolean deleteDir(File dir) 
	{ 
		if (dir.isDirectory()) 
		{ 
			String[] children = dir.list(); 
			for (int i=0; i<children.length; i++)
			{ 
				boolean success = deleteDir(new File(dir, children[i])); 
				if (!success) 
				{  
					return false; 
				} 
			} 
			// The directory is now empty so delete it 
			return dir.delete(); 
		} else {
			return dir.delete();
		}
	}

	/**
	 * mindc instalation directory
	 * Searched in the MIND_ROOT environment variable first
	 * Then inferred from the this class file location if necessary
	 */
	private File mindRoot = null;
	/**
	 * minddoc instalation directory
	 * Searched in the MINDOC_HOME environment variable
	 */
	private File mindDocRoot = null;
	/**
	 * Used to hold the environment variables
	 */
	private Properties env = null;

	/**
	 * All the options that can be used for the different commands
	 */
	private Opt srcPathOpt;
	private Opt testSrcPathOpt;
	private Opt incPathOpt;
	private Opt testIncPathOpt;
	private Opt depPathOpt;
	private Opt testDepPathOpt;
	private Opt testDirOpt;
	private Opt drawDirOpt;
	private Opt execOpt;
	private Opt buildDirOpt;
	private Opt compilerOpt;
	private Opt linkerOpt;
	private Opt assemblerOpt;
	private Opt cFlagsOpt;
	private Opt cppFlagsOpt;
	private Opt ldFlagsOpt;
	private Opt asFlagsOpt;
	/**
	 * All available properties from project configuration file
	 */
	private Prop configuration;
	private Prop executableComponent;
	private Prop binaryName;
	private Prop outputDirectory;
	private Prop sourcePath;
	private Prop testSourcePath= new Prop("testSourcePath","src/test/mind");
	private Prop includePath;
	private Prop testIncludePath= new Prop("testIncludePath");
	private Prop compilerCommand;
	private Prop assemblerCommand;
	private Prop linkerCommand;
	private Prop asFlags;
	private Prop cppFlags;
	private Prop cFlags;
	private Prop ldFlags;
	private Prop extraOptions= new Prop("extraOptions");
	
	/**
	 * Compilation command
	 * Retrieved from the command line
	 */
	String command = null;
	/**
	 * File name of the properties configuration file
	 * Retrieved from the command line
	 */
	private String configurationFileName = null;
	/**
	 * properties configuration file
	 */
	private File configurationFile = null;
	/**
	 * Properties holding the configuration
	 */
	private Properties prop = null;

	/**
	 * Argument list for the command
	 */
	private MindcArgs mindcArgs = new MindcArgs();
	
	

	/**
	 * Get the system properties, mindRoot and mindDocRoot variables
	 */
	private void setMindEnv() {
		/* Initialize the environment properties */
		env = System.getProperties();
		String mindRootName = env.getProperty("MIND_ROOT");
		if ((mindRootName == null) || (mindRootName.length()==0)) {
			/* MIND_ROOT wasn't found in the environment variable let's infer it*/
			URL url = Builder.class.getClassLoader().getResource("org/ow2/mind/Builder.class");      
			String proto = url.getProtocol();
			if(proto.equals("jar"))
			{
				try {
					url = new URL(url.getPath());
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // this nicely strips off the leading jar: 
				proto = url.getProtocol();
				if(proto.equals("file")) {
					mindRootName=url.getPath().substring(0, url.getPath().indexOf('!'));
				} else {
					//mindRoot=new File("/usr/local/mind"); //For debuging purposes
					error("Bad environement !");
				}
				mindRoot=new File(mindRootName).getParentFile().getParentFile();
			} else {
				//mindRoot=new File("/usr/local/mind"); //For debuging purposes
				error("Bad environement !");
			}
		} else {
			/* MIND_ROOT was found in the environment use it*/
			mindRoot=new File(mindRootName);
		}
		String mindDocRootName = env.getProperty("MINDOC_HOME");
		if ((mindDocRootName!=null) && !(mindDocRootName.length()==0))
			mindDocRoot=new File(mindDocRootName);
	}
	
	/**
	 * Extract configuration properties and build command from the command line args
	 * @param args
	 */
	void treatArgs(final String... args) {
		//Checking for argument presence
		if (args.length < 1) {
			error("No command given.");
		}
		command = args[0];
		if (args.length < 2) {
			//If no properties file is passes searching for system properties instead
			//FIXME(is this useful ?)
			prop = env;
		} else if (args.length > 2) {
			//Only two argument is allowed.
			error("To many arguments.");
		}
		else {
			//Exactly two arguments
			configurationFileName = args[1];
			configurationFile = new File(configurationFileName);
			try {
				prop = new Properties();
				prop.load(new FileInputStream(configurationFile));
				prop.setProperty("configuration", configurationFileName.substring(0, configurationFileName.lastIndexOf('.')));
			} catch (FileNotFoundException e) {
				error("Configuration file " + configurationFileName + " do not exist.");
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	

	/**
	 * Build an appropriate classpath  for the build command
	 */
	private void setClassPath() {

		if (command.equals("doc")) {
			//FIXME handle mindoc stuff here!
		} else {
			try {
				//Location for mindc and mindunit classes
				File mindLib=new File(mindRoot.getCanonicalFile() + "/lib");
				File mindExt=new File(mindRoot.getCanonicalFile() + "/ext");
				File mindBin=new File(mindRoot.getCanonicalFile() + "/bin");
				//Add every jar found to the class path
				for(File f : new File[]{mindLib,mindExt,mindBin})
					for(File j: f.listFiles() ){
						String s = j.getAbsolutePath();
						if (s.contains(".jar")){
							URL u;
							try {
								u = j.toURI().toURL();
								addURL(u);
							} catch (MalformedURLException e) {
								e.printStackTrace();
							}
						}
					}

			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}
	
	/**
	 * Steps for building the project
	 */
	private void build() {
		treatConfiguration();
		treatExecutable();
		treatOutputDir();
		treatSourcePath();
		treatIncludePath();
		treatCompiler();
		treatLinker();
		treatAssembler();
		treatCFlags();
		treatLDFlags();
		treatASFlags();
		treatCPPFlags();
		
		mindcArgs.append(execOpt);
		mindcArgs.append(buildDirOpt);
		mindcArgs.append(srcPathOpt);
		mindcArgs.append(depPathOpt);
		mindcArgs.append(incPathOpt);
		mindcArgs.append(compilerOpt);
		mindcArgs.append(linkerOpt);
		mindcArgs.append(assemblerOpt);
		mindcArgs.append(cFlagsOpt);
		mindcArgs.append(cppFlagsOpt);
		mindcArgs.append(asFlagsOpt);
		mindcArgs.append(ldFlagsOpt);
		
		File outputDir = new File(buildDirOpt.value);
		if (!outputDir.exists())
			outputDir.mkdirs();		
		org.ow2.mind.Launcher.main(mindcArgs.toarray());
	}

	/**
	 * Steps to build the tests
	 */
	private void test() {
		treatConfiguration();
		treatOutputDir();
		treatSourcePath();
		treatIncludePath();
		treatTestIncludePath();
		treatCompiler();
		treatLinker();
		treatAssembler();
		treatTestSourcePath();
		treatCFlags();
		treatLDFlags();
		treatASFlags();
		treatCPPFlags();
		
		mindcArgs.append(testDirOpt);
		mindcArgs.append(srcPathOpt);
		mindcArgs.append(depPathOpt);
		mindcArgs.append(testDepPathOpt);
		mindcArgs.append(incPathOpt);
		mindcArgs.append(testIncPathOpt);
		mindcArgs.append(compilerOpt);
		mindcArgs.append(linkerOpt);
		mindcArgs.append(assemblerOpt);
		mindcArgs.append(cFlagsOpt);
		mindcArgs.append(cppFlagsOpt);
		mindcArgs.append(asFlagsOpt);
		mindcArgs.append(ldFlagsOpt);
		mindcArgs.append(testSrcPathOpt);

		File outputDir = new File(testDirOpt.value);
		if (!outputDir.exists())
			outputDir.mkdirs();
		org.ow2.mind.unit.Launcher.main( mindcArgs.toarray() );
	}

	/**
	 * Steps to buld the graphics
	 */
	private void draw() {
		treatConfiguration();
		treatOutputDir();
		treatSourcePath();
		treatDraw();

		mindcArgs.append(drawDirOpt);
		mindcArgs.append(srcPathOpt);
		mindcArgs.append(depPathOpt);
		mindcArgs.append(execOpt);
		mindcArgs.append( new Opt("", "--check-adl"));

		File outputDir = new File(drawDirOpt.value);
		if (!outputDir.exists())
			outputDir.mkdirs();
		org.ow2.mind.Launcher.main( mindcArgs.toarray() );
	}

	/**
	 * Remove build directory
	 */
	private void cleanBuild() {
		treatConfiguration();
		treatOutputDir();
		File outputDir = new File(buildDirOpt.value);
		deleteDir(outputDir);
	}
	
	/**
	 * Remove the test directory
	 */
	private void cleanTest() {
		treatConfiguration();
		treatOutputDir();
		File outputDir = new File(testDirOpt.value);
		deleteDir(outputDir);
	}	
	
	/**
	 * Remove the graphic directory
	 */
	private void cleanDraw() {
		treatConfiguration();
		treatOutputDir();
		File outputDir = new File(drawDirOpt.value);
		deleteDir(outputDir);
	}

	/**
	 * Extract the configuration name
	 */
	private void treatConfiguration() {
		configuration = new Prop("configuration","Default");
		configuration.setValue(prop.getProperty(configuration.key));
	}

	/**
	 * Set the compiler command
	 */
	private void treatCompiler()
	{
		compilerCommand= new Prop("compilerCommand","gcc");
		compilerCommand.setValue(prop.getProperty(compilerCommand.key));
		compilerOpt = new Opt("--compiler-command=", compilerCommand.value);
	}

	/**
	 * Set the C flags
	 */
	private void treatCFlags()
	{	
		cFlags = new Prop("cFlags");
		cFlags.setValue(prop.getProperty(cFlags.key));
		cFlagsOpt = new Opt("--c-flags=", cFlags.value);
	}

	/**
	 * Set the C preprocessor flags
	 */
	private void treatCPPFlags()
	{	
		cppFlags = new Prop("cppFlags");
		cppFlags.setValue(prop.getProperty(cppFlags.key));
		cppFlagsOpt = new Opt("--cpp-flags=", cppFlags.value);
	}
	
	/**
	 * Set the linker command
	 */
	private void treatLinker()
	{
		linkerCommand= new Prop("linkerCommand", compilerCommand.value);
		linkerCommand.setValue(prop.getProperty(linkerCommand.key));
		linkerOpt = new Opt("--linker-command=", linkerCommand.value);
	}

	/**
	 * Set the LD flags
	 */
	private void treatLDFlags()
	{	
		ldFlags = new Prop("ldFlags");
		ldFlags.setValue(prop.getProperty(ldFlags.key));
		ldFlagsOpt = new Opt("--ld-flags=", ldFlags.value);
	}
	
	/**
	 * Set the assembler command
	 */
	private void treatAssembler()
	{
		assemblerCommand= new Prop("assemblerCommand",compilerCommand.value);
		assemblerCommand.setValue(prop.getProperty(assemblerCommand.key));
		assemblerOpt = new Opt("--assembler-command=", assemblerCommand.value);
	}


	/**
	 * Set the assembler flags
	 */
	private void treatASFlags()
	{	
		asFlags = new Prop("asFlags");
		asFlags.setValue(prop.getProperty(asFlags.key));
		asFlagsOpt = new Opt("--as-flags=", asFlags.value);
	}
	
	/**
	 * Set the include paths
	 */
	private void treatIncludePath() {
		includePath = new Prop("includePath");
		includePath.setValue(prop.getProperty(includePath.key));
		incPathOpt= new Opt("--inc-path=", includePath.value);
	}

	/**
	 * Set the source paths
	 */
	private void treatSourcePath() {
		sourcePath = new Prop("sourcePath","src/main/mind");
		sourcePath.setValue(prop.getProperty(sourcePath.key));
		srcPathOpt = new Opt("--src-path=", sourcePath.value);
	}
	/**
	 * Set the include path for tests 
	 */
	private void treatTestIncludePath() {
		testIncludePath = new Prop("testIncludePath");
		testIncludePath.setValue(prop.getProperty(testIncludePath.key));
		testIncPathOpt= new Opt("--inc-path=", testIncludePath.value);
	}

	/**
	 * Set the source paths for tests
	 */
	private void treatTestSourcePath() {
		testSourcePath = new Prop("testSourcePath","src/test/mind");
		testSourcePath.setValue(prop.getProperty(testSourcePath.key));
		testSrcPathOpt = new Opt("", testSourcePath.value);
	}

	/**
	 * Set all the possible output directories
	 */
	private void treatOutputDir() {
		outputDirectory = new Prop("outputDirectory", "target");
		outputDirectory.setValue(prop.getProperty(outputDirectory.key));

		depPathOpt = new Opt("--src-path=", outputDirectory.value + "/" + configuration.value + "/dependencies");
		buildDirOpt = new Opt("--out-path=", outputDirectory.value + "/" + configuration.value + "/binary");
		drawDirOpt = new Opt("--out-path=", outputDirectory.value + "/" + configuration.value + "/graphic");
		testDirOpt = new Opt("--out-path=", outputDirectory.value + "/" + configuration.value + "/test-binary");
		testDepPathOpt = new Opt("--src-path=", outputDirectory.value + "/" + configuration.value + "/test-dependencies");
	}

	/**
	 * Set the executable component and binary name
	 */
	private void treatExecutable(){
		executableComponent = new Prop("executableComponent");
		executableComponent.setValue(prop.getProperty(executableComponent.key));
		binaryName = new Prop("binaryName");
		binaryName.setValue(prop.getProperty(binaryName.key));
		if (binaryName.key.length()!=0)
			execOpt=new Opt(executableComponent.value+":",binaryName.value);
		else
			execOpt=new Opt("",executableComponent.value);

	}
	/**
	 * Set a executable component for graphic generation
	 */
	private void treatDraw() {
		executableComponent = new Prop("executableComponent");
		executableComponent.setValue(prop.getProperty(executableComponent.key));
		if (executableComponent.value.length()!=0)
			executableComponent.setValue("dot.DumpComponentDot<" + executableComponent.value + ">");
		execOpt=new Opt("",executableComponent.value);
	}

}

/**
 * 
 * @author Julien TOUS
 * Representation of a project property with a default value
 */
class Prop {
	public String key;
	public String value;
	/**
	 * Key value constructor
	 * @param key
	 * @param value
	 */
	public Prop(String key,String value){
		this.key=key;
		this.value=value;
	}
	/**
	 * Empty value constructor
	 * @param key
	 */
	public Prop(String key){
		this.key=key;
		this.value="";
	}
	/**
	 * Set a value if it's valid
	 * @param value
	 */
	public void setValue(String value) {
		if (value!=null)
			if (value.length()!=0)
				this.value = value;
	}
}

/**
 * 
 * @author Julien TOUS
 * Prefix of an option together with the option value
 */
class Opt {
	public String prefix;
	public String value;
	Opt(String prefix, String value) {
		this.prefix = prefix;
		this.value = value;
	}
}

/**
 * 
 * @author Julien TOUS
 * List of options for a build 
 */
class MindcArgs {
	private List<String> mindcArgsList = new ArrayList<String>();
	/**
	 * Append an option to the list.
	 * Filter out empty value options
	 * @param opt the option to add
	 */
	public void append(Opt opt){
		if (opt.value.length()!=0)
			mindcArgsList.add(opt.prefix + opt.value);
	}
	/**
	 * Export option list as a string array suitable for compiler call 
	 * @return
	 */
	public String[] toarray(){
		return mindcArgsList.toArray(new String[mindcArgsList.size()]);
	}
}