/*! ******************************************************************************
 *
 * Hop : The Hop Orchestration Platform
 *
 * http://www.project-hop.org
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.apache.hop.cli;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.hop.IExecutionConfiguration;
import org.apache.hop.cluster.SlaveServer;
import org.apache.hop.core.Const;
import org.apache.hop.core.HopEnvironment;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.extension.ExtensionPointHandler;
import org.apache.hop.core.extension.HopExtensionPoint;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.core.parameters.INamedParams;
import org.apache.hop.core.parameters.UnknownParamException;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.core.vfs.HopVfs;
import org.apache.hop.pipeline.config.PipelineRunConfiguration;
import org.apache.hop.pipeline.engine.IPipelineEngine;
import org.apache.hop.pipeline.engine.PipelineEngineFactory;
import org.apache.hop.workflow.WorkflowExecutionConfiguration;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.metastore.MetaStoreConst;
import org.apache.hop.metastore.api.IMetaStore;
import org.apache.hop.metastore.api.exceptions.MetaStoreException;
import org.apache.hop.metastore.persist.MetaStoreFactory;
import org.apache.hop.metastore.stores.delegate.DelegatingMetaStore;
import org.apache.hop.metastore.util.HopDefaults;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.PipelineExecutionConfiguration;
import org.apache.hop.workflow.engine.IWorkflowEngine;
import org.apache.hop.workflow.engine.WorkflowEngineFactory;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class HopRun implements Runnable {
  public static final String XP_HOP_RUN_START = "HopRunStart";
  public static final String XP_CREATE_ENVIRONMENT = "CreateEnvironment";
  public static final String XP_IMPORT_ENVIRONMENT = "ImportEnvironment";

  @Option( names = { "-f", "-z", "--file" }, description = "The filename of the workflow or pipeline to run" )
  private String filename;

  @Option( names = { "-l", "--level" }, description = "The debug level, one of NONE, MINIMAL, BASIC, DETAILED, DEBUG, ROWLEVEL" )
  private String level;

  @Option( names = { "-h", "--help" }, usageHelp = true, description = "Displays this help message and quits." )
  private boolean helpRequested;

  @Option( names = { "-p", "--parameters" }, description = "A comma separated list of PARAMETER=VALUE pairs", split = "," )
  private String[] parameters = null;

  @Option( names = { "-s", "--system-properties" }, description = "A comma separated list of KEY=VALUE pairs", split = "," )
  private String[] systemProperties = null;

  @Option( names = { "-r", "--runconfig" }, description = "The name of the Run Configuration to use" )
  private String runConfigurationName = null;

  @Option( names = { "-t", "--pipeline" }, description = "Force execution of a pipeline" )
  private boolean runPipeline = false;

  @Option( names = { "-w", "--workflow" }, description = "Force execution of a workflow" )
  private boolean runWorkflow = false;

  @Option( names = { "-o", "--printoptions" }, description = "Print the used options" )
  private boolean printingOptions = false;

  private IVariables variables;
  private String realRunConfigurationName;
  private String realFilename;
  private CommandLine cmd;
  private ILogChannel log;
  private DelegatingMetaStore metaStore;

  public void run() {
    validateOptions();

    try {
      initialize( cmd );

      log = new LogChannel( "HopRun" );
      log.logDetailed( "Start of Hop Run" );

      if ( isPipeline() ) {
        runPipeline( cmd, log );
      }
      if ( isJob() ) {
        runWorkflow( cmd, log );
      }
    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "There was an error during execution of file '" + filename + "'", e );
    }
  }

  private void initialize( CommandLine cmd ) {
    try {
      // Set some System properties if there were any
      //
      if (systemProperties!=null) {
        for ( String parameter : systemProperties ) {
          String[] split = parameter.split( "=" );
          String key = split.length > 0 ? split[ 0 ] : null;
          String value = split.length > 1 ? split[ 1 ] : null;
          if ( StringUtils.isNotEmpty( key ) && StringUtils.isNotEmpty( value ) ) {
            System.setProperty( key, value );
          }
        }
      }

      // Picks up these system settings in the variables
      //
      buildVariableSpace();

      // Set up the metastore(s) to use
      //
      buildMetaStore();

      HopEnvironment.init();
    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "There was a problem during the initialization of the Hop environment", e );
    }
  }

  private void buildVariableSpace() throws IOException {
    // Load hop.properties before running for convenience...
    //
    variables = Variables.getADefaultVariableSpace();
    Properties kettleProperties = new Properties();
    kettleProperties.load( new FileInputStream( Const.getHopDirectory() + "/hop.properties" ) );
    for ( final String key : kettleProperties.stringPropertyNames() ) {
      variables.setVariable( key, kettleProperties.getProperty( key ) );
    }
  }

  private void runPipeline( CommandLine cmd, ILogChannel log ) {

    try {
      calculateRealFilename();

      // Run the pipeline with the given filename
      //
      PipelineMeta pipelineMeta = new PipelineMeta( realFilename, metaStore, true, variables );

      // Configure the basic execution settings
      //
      PipelineExecutionConfiguration configuration = new PipelineExecutionConfiguration();

      // Overwrite if the user decided this
      //
      parseOptions( cmd, configuration, pipelineMeta );

      // configure the variables and parameters
      //
      configureParametersAndVariables( cmd, configuration, pipelineMeta, pipelineMeta );

      // Certain Pentaho plugins rely on this.  Meh.
      //
      ExtensionPointHandler.callExtensionPoint( log, HopExtensionPoint.HopUiPipelineBeforeStart.id, new Object[] {
        configuration, null, pipelineMeta, null } );

      // Before running, do we print the options?
      //
      if ( printingOptions ) {
        printOptions( configuration );
      }

      // Now run the pipeline using the run configuration
      //
      runPipeline( cmd, log, configuration, pipelineMeta );

    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "There was an error during execution of pipeline '" + filename + "'", e );
    }
  }

  /**
   * This way we can actually use environment variables to parse the real filename
   */
  private void calculateRealFilename() throws HopException {
    realFilename = variables.environmentSubstitute( filename );

    try {
      FileObject fileObject = HopVfs.getFileObject( realFilename );
      if ( !fileObject.exists() ) {
        // Try to prepend with ${ENVIRONMENT_HOME}
        //
        String alternativeFilename = variables.environmentSubstitute( "${ENVIRONMENT_HOME}/" + filename );
        fileObject = HopVfs.getFileObject( alternativeFilename );
        if ( fileObject.exists() ) {
          realFilename = alternativeFilename;
          log.logMinimal( "Relative path filename specified: " + realFilename );
        }
      }
    } catch ( Exception e ) {
      throw new HopException( "Error calculating filename", e );
    }
  }

  private void runPipeline( CommandLine cmd, ILogChannel log, PipelineExecutionConfiguration configuration, PipelineMeta pipelineMeta ) {
    try {
      String pipelineRunConfigurationName = pipelineMeta.environmentSubstitute( configuration.getRunConfiguration() );
      IPipelineEngine<PipelineMeta> pipeline = PipelineEngineFactory.createPipelineEngine( pipelineRunConfigurationName, metaStore, pipelineMeta );
      pipeline.initializeVariablesFrom( null );
      pipeline.getSubject().setInternalHopVariables( pipeline );
      pipeline.injectVariables( configuration.getVariablesMap() );

      pipeline.setLogLevel( configuration.getLogLevel() );
      pipeline.setMetaStore( metaStore );

      // Also copy the parameters over...
      //
      pipeline.copyParametersFrom( pipelineMeta );
      pipelineMeta.activateParameters();
      pipeline.activateParameters();

      // Run it!
      //
      pipeline.prepareExecution();
      pipeline.startThreads();
      pipeline.waitUntilFinished();
    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "Error running pipeline locally", e );
    }
  }

  private void runWorkflow( CommandLine cmd, ILogChannel log ) {
    try {
      calculateRealFilename();

      // Run the workflow with the given filename
      //
      WorkflowMeta workflowMeta = new WorkflowMeta( variables, realFilename, metaStore );

      // Configure the basic execution settings
      //
      WorkflowExecutionConfiguration configuration = new WorkflowExecutionConfiguration();

      // Overwrite the run configuration with optional command line options
      //
      parseOptions( cmd, configuration, workflowMeta );

      // Certain Pentaho plugins rely on this.  Meh.
      //
      ExtensionPointHandler.callExtensionPoint( log, HopExtensionPoint.HopUiJobBeforeStart.id, new Object[] { configuration, null, workflowMeta, null } );

      // Before running, do we print the options?
      //
      if ( printingOptions ) {
        printOptions( configuration );
      }

      runWorkflow( cmd, log, configuration, workflowMeta );

    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "There was an error during execution of workflow '" + filename + "'", e );
    }
  }

  private void runWorkflow( CommandLine cmd, ILogChannel log, WorkflowExecutionConfiguration configuration, WorkflowMeta workflowMeta ) {
    try {
      String runConfigurationName = workflowMeta.environmentSubstitute(configuration.getRunConfiguration());
      IWorkflowEngine<WorkflowMeta> workflow = WorkflowEngineFactory.createWorkflowEngine( runConfigurationName, metaStore, workflowMeta );
      workflow.initializeVariablesFrom( null );
      workflow.getWorkflowMeta().setInternalHopVariables( workflow );
      workflow.injectVariables( configuration.getVariablesMap() );

      workflow.setLogLevel( configuration.getLogLevel() );

      // Explicitly set parameters
      for ( String parameterName : configuration.getParametersMap().keySet() ) {
        workflowMeta.setParameterValue( parameterName, configuration.getParametersMap().get( parameterName ) );
      }

      // Also copy the parameters over...
      //
      workflow.copyParametersFrom( workflowMeta );
      workflowMeta.activateParameters();
      workflow.activateParameters();

      workflow.startExecution();
    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "Error running workflow locally", e );
    }
  }

  private void parseOptions( CommandLine cmd, IExecutionConfiguration configuration, INamedParams namedParams ) throws MetaStoreException {

    realRunConfigurationName = variables.environmentSubstitute( runConfigurationName );
    configuration.setRunConfiguration( realRunConfigurationName );
    configuration.setLogLevel( LogLevel.getLogLevelForCode( variables.environmentSubstitute( level ) ) );

    // Set variables and parameters...
    //
    parseParametersAndVariables( cmd, configuration, namedParams );
  }

  private void configureSlaveServer( IExecutionConfiguration configuration, String name ) throws MetaStoreException {
    MetaStoreFactory<SlaveServer> slaveFactory = new MetaStoreFactory<>( SlaveServer.class, metaStore, HopDefaults.NAMESPACE );
    SlaveServer slaveServer = slaveFactory.loadElement( name );
    if ( slaveServer == null ) {
      throw new ParameterException( cmd, "Unable to find slave server '" + name + "' in the metastore" );
    }
  }

  private boolean isPipeline() {
    if ( runPipeline ) {
      return true;
    }
    if ( StringUtils.isEmpty( filename ) ) {
      return false;
    }
    return filename.toLowerCase().endsWith( ".hpl" );
  }

  private boolean isJob() {
    if ( runWorkflow ) {
      return true;
    }
    if ( StringUtils.isEmpty( filename ) ) {
      return false;
    }
    return filename.toLowerCase().endsWith( ".hwf" );
  }


  /**
   * Set the variables and parameters
   *
   * @param cmd
   * @param configuration
   * @param namedParams
   */
  private void parseParametersAndVariables( CommandLine cmd, IExecutionConfiguration configuration, INamedParams namedParams ) {
    try {
      String[] availableParameters = namedParams.listParameters();
      if ( parameters != null ) {
        for ( String parameter : parameters ) {
          String[] split = parameter.split( "=" );
          String key = split.length > 0 ? split[ 0 ] : null;
          String value = split.length > 1 ? split[ 1 ] : null;

          if ( key != null ) {
            // We can work with this.
            //
            if ( Const.indexOfString( key, availableParameters ) < 0 ) {
              // A variable
              //
              configuration.getVariablesMap().put( key, value );
            } else {
              // A parameter
              //
              configuration.getParametersMap().put( key, value );
            }
          }
        }
      }
    } catch ( Exception e ) {
      throw new ExecutionException( cmd, "There was an error during execution of pipeline '" + filename + "'", e );
    }
  }

  private void buildMetaStore() throws MetaStoreException {
    metaStore = new DelegatingMetaStore();
    IMetaStore localMetaStore = MetaStoreConst.openLocalHopMetaStore();
    metaStore.addMetaStore( localMetaStore );
    metaStore.setActiveMetaStoreName( localMetaStore.getName() );
  }


  /**
   * Configure the variables and parameters in the given configuration on the given variable space and named parameters
   *
   * @param cmd
   * @param configuration
   * @param namedParams
   */
  private void configureParametersAndVariables( CommandLine cmd, IExecutionConfiguration configuration, IVariables variables, INamedParams namedParams ) {

    // Copy variables over to the pipeline or workflow metadata
    //
    variables.injectVariables( configuration.getVariablesMap() );

    // Set the parameter values
    //
    for ( String key : configuration.getParametersMap().keySet() ) {
      String value = configuration.getParametersMap().get( key );
      try {
        namedParams.setParameterValue( key, value );
      } catch ( UnknownParamException e ) {
        throw new ParameterException( cmd, "Unable to set parameter '" + key + "'", e );
      }
    }
  }

  private void validateOptions() {
    if ( StringUtils.isEmpty( filename ) ) {
      throw new ParameterException( new CommandLine( this ), "A filename is needed to run a workflow or pipeline" );
    }
  }

  private void printOptions( IExecutionConfiguration configuration ) {
    if ( StringUtils.isNotEmpty( realFilename ) ) {
      log.logMinimal( "OPTION: filename : '" + realFilename + "'" );
    }
    if ( StringUtils.isNotEmpty( realRunConfigurationName ) ) {
      log.logMinimal( "OPTION: run configuration : '" + realRunConfigurationName + "'" );
    }
    log.logMinimal( "OPTION: Logging level : " + configuration.getLogLevel().getDescription() );

    if ( !configuration.getVariablesMap().isEmpty() ) {
      log.logMinimal( "OPTION: Variables: " );
      for ( String variable : configuration.getVariablesMap().keySet() ) {
        log.logMinimal( "  " + variable + " : '" + configuration.getVariablesMap().get( variable ) );
      }
    }
    if ( !configuration.getParametersMap().isEmpty() ) {
      log.logMinimal( "OPTION: Parameters: " );
      for ( String parameter : configuration.getParametersMap().keySet() ) {
        log.logMinimal( "OPTION:   " + parameter + " : '" + configuration.getParametersMap().get( parameter ) );
      }
    }
  }

  /**
   * Gets log
   *
   * @return value of log
   */
  public ILogChannel getLog() {
    return log;
  }

  /**
   * Gets metaStore
   *
   * @return value of metaStore
   */
  public IMetaStore getMetaStore() {
    return metaStore;
  }

  /**
   * Gets cmd
   *
   * @return value of cmd
   */
  public CommandLine getCmd() {
    return cmd;
  }

  /**
   * @param cmd The cmd to set
   */
  public void setCmd( CommandLine cmd ) {
    this.cmd = cmd;
  }

  /**
   * Gets filename
   *
   * @return value of filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @param filename The filename to set
   */
  public void setFilename( String filename ) {
    this.filename = filename;
  }

  /**
   * Gets level
   *
   * @return value of level
   */
  public String getLevel() {
    return level;
  }

  /**
   * @param level The level to set
   */
  public void setLevel( String level ) {
    this.level = level;
  }

  /**
   * Gets helpRequested
   *
   * @return value of helpRequested
   */
  public boolean isHelpRequested() {
    return helpRequested;
  }

  /**
   * @param helpRequested The helpRequested to set
   */
  public void setHelpRequested( boolean helpRequested ) {
    this.helpRequested = helpRequested;
  }

  /**
   * Gets parameters
   *
   * @return value of parameters
   */
  public String[] getParameters() {
    return parameters;
  }

  /**
   * @param parameters The parameters to set
   */
  public void setParameters( String[] parameters ) {
    this.parameters = parameters;
  }

  /**
   * Gets runConfigurationName
   *
   * @return value of runConfigurationName
   */
  public String getRunConfigurationName() {
    return runConfigurationName;
  }

  /**
   * @param runConfigurationName The runConfigurationName to set
   */
  public void setRunConfigurationName( String runConfigurationName ) {
    this.runConfigurationName = runConfigurationName;
  }

  /**
   * Gets runPipeline
   *
   * @return value of runPipeline
   */
  public boolean isRunPipeline() {
    return runPipeline;
  }

  /**
   * @param runPipeline The runPipeline to set
   */
  public void setRunPipeline( boolean runPipeline ) {
    this.runPipeline = runPipeline;
  }

  /**
   * Are we running a workflow
   *
   * @return true if a workflow is run
   */
  public boolean isRunWorkflow() {
    return runWorkflow;
  }

  /**
   * @param runWorkflow true if you want to force execution of a workflow
   */
  public void setRunWorkflow( boolean runWorkflow ) {
    this.runWorkflow = runWorkflow;
  }

  /**
   * Gets printingOptions
   *
   * @return value of printingOptions
   */
  public boolean isPrintingOptions() {
    return printingOptions;
  }

  /**
   * @param printingOptions The printingOptions to set
   */
  public void setPrintingOptions( boolean printingOptions ) {
    this.printingOptions = printingOptions;
  }

  /**
   * Gets systemProperties
   *
   * @return value of systemProperties
   */
  public String[] getSystemProperties() {
    return systemProperties;
  }

  /**
   * @param systemProperties The systemProperties to set
   */
  public void setSystemProperties( String[] systemProperties ) {
    this.systemProperties = systemProperties;
  }

  public static void main( String[] args ) {

    HopRun hopRun = new HopRun();
    try {
      CommandLine cmd = new CommandLine( hopRun );
      hopRun.setCmd( cmd );
      CommandLine.ParseResult parseResult = cmd.parseArgs( args );
      if ( CommandLine.printHelpIfRequested( parseResult ) ) {
        System.exit( 1 );
      } else {
        hopRun.run();
        System.exit( 0 );
      }
    } catch ( ParameterException e ) {
      System.err.println( e.getMessage() );
      e.getCommandLine().usage( System.err );
      System.exit( 9 );
    } catch ( ExecutionException e ) {
      System.err.println( "Error found during execution!" );
      System.err.println( Const.getStackTracker( e ) );

      System.exit( 1 );
    } catch ( Exception e ) {
      System.err.println( "General error found, something went horribly wrong!" );
      System.err.println( Const.getStackTracker( e ) );

      System.exit( 2 );
    }

  }
}
