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

package org.apache.hop.pipeline.transforms.csvinput;

import org.apache.hop.core.QueueRowSet;
import org.apache.hop.core.RowSet;
import org.apache.hop.junit.rules.RestoreHopEngineEnvironment;
import org.apache.hop.pipeline.transform.ITransformData;
import org.apache.hop.pipeline.transforms.TransformMockUtil;
import org.apache.hop.pipeline.transforms.mock.TransformMockHelper;
import org.apache.hop.pipeline.transforms.fileinput.TextFileInputField;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CsvInputMultiCharDelimiterTest extends CsvInputUnitTestBase {
  @ClassRule public static RestoreHopEngineEnvironment env = new RestoreHopEngineEnvironment();

  private CsvInput csvInput;
  private TransformMockHelper<CsvInputMeta, ITransformData> transformMockHelper;

  @Before
  public void setUp() throws Exception {
    transformMockHelper =
      TransformMockUtil.getTransformMockHelper( CsvInputMeta.class, "CsvInputMultiCharDelimiterTest" );
    csvInput = new CsvInput(
      transformMockHelper.transformMeta, transformMockHelper.iTransformMeta, transformMockHelper.iTransformData, 0, transformMockHelper.pipelineMeta,
      transformMockHelper.pipeline );
  }

  @After
  public void cleanUp() {
    transformMockHelper.cleanUp();
  }

  @Test
  public void multiChar_hasEnclosures_HasNewLine() throws Exception {
    doTest( "\"value1\"delimiter\"value2\"delimiter\"value3\"\n" );
  }

  @Test
  public void multiChar_hasEnclosures_HasNewLineDoubleEnd() throws Exception {
    doTest( "\"value1\"delimiter\"value2\"delimiter\"value3\"\r\n" );
  }

  @Test
  public void multiChar_hasEnclosures_HasNotNewLine() throws Exception {
    doTest( "\"value1\"delimiter\"value2\"delimiter\"value3\"" );
  }

  @Test
  public void multiChar_hasNotEnclosures_HasNewLine() throws Exception {
    doTest( "value1delimitervalue2delimitervalue3\n" );
  }

  @Test
  public void multiChar_hasNotEnclosures_HasNewLineDoubleEnd() throws Exception {
    doTest( "value1delimitervalue2delimitervalue3\r\n" );
  }

  @Test
  public void multiChar_hasNotEnclosures_HasNotNewLine() throws Exception {
    doTest( "value1delimitervalue2delimitervalue3" );
  }

  private void doTest( String content ) throws Exception {
    RowSet output = new QueueRowSet();

    File tmp = createTestFile( ENCODING, content );
    try {
      CsvInputMeta meta = createMeta( tmp, createInputFileFields( "f1", "f2", "f3" ) );
      CsvInputData data = new CsvInputData();
      csvInput.init();

      csvInput.addRowSetToOutputRowSets( output );

      try {
        csvInput.init();
      } finally {
        csvInput.dispose();
      }

    } finally {
      tmp.delete();
    }

    Object[] row = output.getRowImmediate();
    assertNotNull( row );
    assertEquals( "value1", row[ 0 ] );
    assertEquals( "value2", row[ 1 ] );
    assertEquals( "value3", row[ 2 ] );

    assertNull( output.getRowImmediate() );
  }

  @Override
  CsvInputMeta createMeta( File file, TextFileInputField[] fields ) {
    CsvInputMeta meta = super.createMeta( file, fields );
    meta.setDelimiter( "delimiter" );
    return meta;
  }
}