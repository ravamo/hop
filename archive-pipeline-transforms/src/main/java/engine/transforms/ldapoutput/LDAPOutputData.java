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

package org.apache.hop.pipeline.transforms.ldapoutput;

import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transforms.ldapinput.LDAPConnection;

/**
 * @author Samatar Hassan
 * @since 21-09-2007
 */
public class LDAPOutputData extends BaseTransformData implements ITransformData {
  public LDAPConnection connection;
  public int indexOfDNField;
  public int[] fieldStream;
  public String[] fieldsAttribute;
  public int nrFields;
  public int nrfieldsToUpdate;
  public String separator;
  public String[] attributes;
  public String[] attributesToUpdate;

  public int[] fieldStreamToUpdate;
  public String[] fieldsAttributeToUpdate;

  public int indexOfOldDNField;
  public int indexOfNewDNField;

  public LDAPOutputData() {
    super();
    this.indexOfDNField = -1;
    this.nrFields = 0;
    this.separator = null;
    this.fieldStreamToUpdate = null;
    this.fieldsAttributeToUpdate = null;
    this.attributesToUpdate = null;
    this.nrfieldsToUpdate = 0;
    this.indexOfOldDNField = -1;
    this.indexOfNewDNField = -1;
  }

}
