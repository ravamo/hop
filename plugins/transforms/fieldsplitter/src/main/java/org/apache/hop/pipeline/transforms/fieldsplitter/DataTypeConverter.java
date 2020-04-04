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

package org.apache.hop.pipeline.transforms.fieldsplitter;

import org.apache.hop.core.exception.HopValueException;
import org.apache.hop.core.injection.InjectionTypeConverter;
import org.apache.hop.core.row.value.ValueMetaFactory;

/**
 * Converter for field types.
 */
public class DataTypeConverter extends InjectionTypeConverter {

  @Override
  public int string2intPrimitive( String v ) throws HopValueException {
    return ValueMetaFactory.getIdForValueMeta( v );
  }
}