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

package org.apache.hop.ui.hopgui.file.workflow.context;

import org.apache.hop.core.gui.Point;
import org.apache.hop.core.gui.plugin.GuiAction;
import org.apache.hop.core.gui.plugin.GuiActionLambdaBuilder;
import org.apache.hop.ui.hopgui.file.workflow.HopGuiWorkflowGraph;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.action.ActionCopy;
import org.apache.hop.ui.hopgui.context.BaseGuiContextHandler;
import org.apache.hop.ui.hopgui.context.IGuiContextHandler;

import java.util.ArrayList;
import java.util.List;

public class HopGuiWorkflowActionContext extends BaseGuiContextHandler implements IGuiContextHandler {

  public static final String CONTEXT_ID = "HopGuiWorkflowActionContext";

  private WorkflowMeta workflowMeta;
  private ActionCopy actionCopy;
  private HopGuiWorkflowGraph jobGraph;
  private Point click;
  private GuiActionLambdaBuilder<HopGuiWorkflowActionContext> lambdaBuilder;

  public HopGuiWorkflowActionContext( WorkflowMeta workflowMeta, ActionCopy actionCopy, HopGuiWorkflowGraph jobGraph, Point click ) {
    super();
    this.workflowMeta = workflowMeta;
    this.actionCopy = actionCopy;
    this.jobGraph = jobGraph;
    this.click = click;
    this.lambdaBuilder = new GuiActionLambdaBuilder<>();
  }

  public String getContextId() {
    return CONTEXT_ID;
  }

  /**
   * Create a list of supported actions on a action.
   *
   * @return The list of supported actions
   */
  @Override public List<GuiAction> getSupportedActions() {
    List<GuiAction> actions = new ArrayList<>();

    // Get the actions from the plugins, sorted by ID...
    //
    List<GuiAction> pluginActions = getPluginActions( true );
    if ( pluginActions != null ) {
      for ( GuiAction pluginAction : pluginActions ) {
        actions.add( lambdaBuilder.createLambda( pluginAction, jobGraph, this, jobGraph ) );
      }
    }

    return actions;
  }

  /**
   * Gets workflowMeta
   *
   * @return value of workflowMeta
   */
  public WorkflowMeta getWorkflowMeta() {
    return workflowMeta;
  }

  /**
   * @param workflowMeta The workflowMeta to set
   */
  public void setWorkflowMeta( WorkflowMeta workflowMeta ) {
    this.workflowMeta = workflowMeta;
  }

  /**
   * Gets actionCopy
   *
   * @return value of actionCopy
   */
  public ActionCopy getActionCopy() {
    return actionCopy;
  }

  /**
   * @param actionCopy The actionCopy to set
   */
  public void setActionCopy( ActionCopy actionCopy ) {
    this.actionCopy = actionCopy;
  }

  /**
   * Gets jobGraph
   *
   * @return value of jobGraph
   */
  public HopGuiWorkflowGraph getJobGraph() {
    return jobGraph;
  }

  /**
   * @param jobGraph The jobGraph to set
   */
  public void setJobGraph( HopGuiWorkflowGraph jobGraph ) {
    this.jobGraph = jobGraph;
  }

  /**
   * Gets click
   *
   * @return value of click
   */
  public Point getClick() {
    return click;
  }

  /**
   * @param click The click to set
   */
  public void setClick( Point click ) {
    this.click = click;
  }

  /**
   * Gets lambdaBuilder
   *
   * @return value of lambdaBuilder
   */
  public GuiActionLambdaBuilder<HopGuiWorkflowActionContext> getLambdaBuilder() {
    return lambdaBuilder;
  }

  /**
   * @param lambdaBuilder The lambdaBuilder to set
   */
  public void setLambdaBuilder( GuiActionLambdaBuilder<HopGuiWorkflowActionContext> lambdaBuilder ) {
    this.lambdaBuilder = lambdaBuilder;
  }
}
