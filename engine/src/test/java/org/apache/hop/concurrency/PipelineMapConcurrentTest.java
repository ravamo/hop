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
package org.apache.hop.concurrency;

import org.apache.hop.www.HopServerObjectEntry;
import org.apache.hop.www.SocketPortAllocation;
import org.apache.hop.www.PipelineMap;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;


public class PipelineMapConcurrentTest {

  PipelineMap pipelineMap;
  int numberOfSameAllocations = 10;
  int numberOfSameSourceAndTargetSlaveNameAllocations = 40;
  int numberOfDifferentAllocations = 100;
  int numberOfSameHosts = 100;
  int numberOfDeallocateTasks = 100;

  List<ConcurrentAllocate> concurrentAllocateTasks;
  List<ConcurrentDeallocate> concurrentDeallocateTasks;

  @Before
  public void setup() throws Exception {

    pipelineMap = new PipelineMap();
    concurrentAllocateTasks = new ArrayList<>();
    concurrentDeallocateTasks = new ArrayList<>();

    // adding equal port allocations
    for ( int i = 1; i <= numberOfSameAllocations; i++ ) {
      concurrentAllocateTasks.add( new ConcurrentAllocate( 40000, "host0", "id0", "pipeline0", "slave0", "source0", "0", "slave-0", "target0", "0" ) );
    }
    // adding allocations with equal hostname, source and slave name. Should not allocate one port as there is
    // no deallocate tasks and !spa.isAllocated() returns true
    for ( int i = 1; i <= numberOfSameSourceAndTargetSlaveNameAllocations; i++ ) {
      concurrentAllocateTasks.add( new ConcurrentAllocate( 40000, "host1", "id" + i, "pipeline" + i, "slave-1", "source" + i, "" + i, "slave-2", "target" + i, "" + i ) );
    }

    // adding different allocations
    for ( int i = 2; i <= numberOfDifferentAllocations + 1; i++ ) {
      concurrentAllocateTasks.add( new ConcurrentAllocate( 40000, "host" + i, "id" + i, "pipeline" + i, "slave-" + i, "source" + i, "" + i, "slave-" + i, "target" + i, "" + i ) );
    }

    // adding allocations which have the same hostname as different ones but diff properties
    for ( int i = 1; i <= numberOfSameHosts; i++ ) {
      concurrentAllocateTasks.add( new ConcurrentAllocate( 40000, "host" + i, "diff", "diff", "diff", "diff", "diff", "diff", "diff", "diff" ) );
    }

    for ( int i = 0; i < numberOfDeallocateTasks; i++ ) {
      HopServerObjectEntry hopServerObjectEntry = new HopServerObjectEntry( "pipeline" + i, "id" + 1 );
      concurrentDeallocateTasks.add( new ConcurrentDeallocate( i, "host" + i, hopServerObjectEntry ) );
    }

  }

  @Test
  public void testConcurrentAllocateServerSocketPort() throws Exception {
    ConcurrencyTestRunner.runAndCheckNoExceptionRaised( concurrentAllocateTasks, Collections.emptyList(), new AtomicBoolean( true ) );

  }

  @Test
  public void testConcurrentAllocateAndDeallocateServerSocketPort() throws Exception {
    ConcurrencyTestRunner.runAndCheckNoExceptionRaised( concurrentDeallocateTasks, concurrentAllocateTasks, new AtomicBoolean( true ) );
  }


  private class ConcurrentAllocate implements Callable<SocketPortAllocation> {

    int portRangeStart;
    String hostname;
    String clusteredRunId;
    String pipelineName;
    String sourceSlaveName;
    String sourceTransformName;
    String sourceTransformCopy;
    String targetSlaveName;
    String targetTransformName;
    String targetTransformCopy;

    public ConcurrentAllocate( int portRangeStart, String hostname, String clusteredRunId,
                               String pipelineName, String sourceSlaveName, String sourceTransformName,
                               String sourceTransformCopy, String targetSlaveName, String targetTransformName, String targetTransformCopy ) {
      this.portRangeStart = portRangeStart;
      this.hostname = hostname;
      this.clusteredRunId = clusteredRunId;
      this.pipelineName = pipelineName;
      this.sourceSlaveName = sourceSlaveName;
      this.sourceTransformName = sourceTransformName;
      this.sourceTransformCopy = sourceTransformCopy;
      this.targetSlaveName = targetSlaveName;
      this.targetTransformName = targetTransformName;
      this.targetTransformCopy = targetTransformCopy;
    }

    @Override
    public SocketPortAllocation call() throws Exception {
      return pipelineMap.allocateServerSocketPort( portRangeStart, hostname, clusteredRunId, pipelineName,
        sourceSlaveName, sourceTransformName, sourceTransformCopy, targetSlaveName, targetTransformName, targetTransformCopy );
    }
  }


  private class ConcurrentDeallocate implements Callable<Object> {

    int port;
    String hostname;
    HopServerObjectEntry entry;

    ConcurrentDeallocate( int port, String hostname, HopServerObjectEntry entry ) {
      this.port = port;
      this.hostname = hostname;
      this.entry = entry;
    }

    @Override
    public Object call() throws Exception {
      pipelineMap.deallocateServerSocketPorts( entry );
      pipelineMap.deallocateServerSocketPort( port, hostname );
      return null;
    }
  }
}

