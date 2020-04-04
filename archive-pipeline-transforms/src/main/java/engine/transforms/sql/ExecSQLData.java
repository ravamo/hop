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

package org.apache.hop.pipeline.transforms.sql;

import org.apache.hop.core.Result;
import org.apache.hop.core.database.Database;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

import java.util.List;

/**
 * @author Matt
 * @since 20-jan-2005
 */
public class ExecSQLData extends BaseTransformData implements ITransformData {
  public Database db;
  public Result result;
  public int[] argumentIndexes;
  public List<Integer> markerPositions;
  public IRowMeta outputRowMeta;
  public String sql;
  public boolean isCanceled;
  public IRowMeta paramsMeta;

  public ExecSQLData() {
    super();

    db = null;
    result = null;
    argumentIndexes = null;
    markerPositions = null;
    paramsMeta = null;
  }
}