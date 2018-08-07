/*
 * Copyright 2017 Barclays Africa Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.harvester.listener

import java.util.UUID.randomUUID

import org.apache.spark.sql.FileSinkObj
import org.apache.spark.sql.execution.streaming._
import org.apache.spark.sql.kafka010.KafkaSinkObj
import org.apache.spark.sql.streaming.{StreamingQuery, StreamingQueryListener, StreamingQueryManager}
import org.slf4s.Logging
import za.co.absa.spline.sparkadapterapi.StructuredStreamingListenerAdapter.instance._
import za.co.absa.spline.harvester.LogicalPlanLineageHarvester
import za.co.absa.spline.harvester.conf.LineageDispatcher
import za.co.absa.spline.model.DataLineage
import za.co.absa.spline.model.endpoint.{ConsoleEndpoint, FileEndpoint, KafkaEndpoint, StreamEndpoint}
import za.co.absa.spline.model.op.{OperationProps, StreamWrite}

import scala.language.postfixOps

/**
  * Not finished. Please ignore.
  */
class StructuredStreamingListener(
  queryManager: StreamingQueryManager,
  lineageHarvester: LogicalPlanLineageHarvester,
  lineageDispatcher: LineageDispatcher)
  extends StreamingQueryListener with Logging {

  override def onQueryStarted(event: StreamingQueryListener.QueryStartedEvent): Unit = {
    log debug s"Structured streaming query(id: ${event.id}, runId: ${event.runId}) has started."
    processQuery(queryManager.get(event.id))
  }

  override def onQueryProgress(event: StreamingQueryListener.QueryProgressEvent): Unit = ()

  override def onQueryTerminated(event: StreamingQueryListener.QueryTerminatedEvent): Unit = {
    val queryStringRep = s"Structured streaming query(id: ${event.id}, runId: ${event.runId})"

    event.exception match {
      case None => log debug s"'$queryStringRep' successfully terminated."
      case Some(err) => log debug s"'$queryStringRep' terminated with the exception '$err'"
    }
  }

  private def processQuery(query: StreamingQuery): Unit = query match {
    case se: StreamExecution => processExecution(se)
    case sw: StreamingQueryWrapper => processQuery(sw.streamingQuery)
    case x => log error s"Trying to process unknown query '$x'."
  }

  private def processExecution(se: StreamExecution): Unit = {
    assume(se.logicalPlan.resolved, "we harvest lineage from analyzed logic plans")

    val logicalPlanLineage = lineageHarvester.harvestLineage(se.sparkSession.sparkContext, se.logicalPlan)

    val endpoint = se.sink match {
      case FileSinkObj(path, fileFormat) => FileEndpoint(path, fileFormat.toString)
      case KafkaSinkObj(cluster, topic) => KafkaEndpoint(cluster, topic.getOrElse(""))
      case x if Set(consoleSinkClass(), classOf[ForeachSink[_]], classOf[MemorySink]).exists(assignableFrom(_, x)) => ConsoleEndpoint()
      case sink => throw new IllegalArgumentException(s"Unsupported sink type: ${sink.getClass}")
    }

    val metaDataset = logicalPlanLineage.rootDataset.copy(randomUUID)
    val mainProps = OperationProps(randomUUID, endpoint.getClass.getSimpleName, Seq(logicalPlanLineage.rootDataset.id), metaDataset.id)
    val writeOperation = StreamWrite(mainProps, endpoint, endpoint.getClass.getSimpleName, endpoint.path.toString)

    val streamingLineage = logicalPlanLineage.copy(
        operations = writeOperation +: logicalPlanLineage.operations,
        datasets = metaDataset +: logicalPlanLineage.datasets)
    lineageDispatcher.send(streamingLineage)
  }

  private def assignableFrom(runtimeClass: Class[_], anyRef: AnyRef) = {
    runtimeClass.isAssignableFrom(anyRef.getClass)
  }

}
