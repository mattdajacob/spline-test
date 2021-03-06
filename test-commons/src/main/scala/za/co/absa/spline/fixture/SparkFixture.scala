/*
 * Copyright 2017 ABSA Group Limited
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

package za.co.absa.spline.fixture

import java.nio.file.Path

import org.apache.spark.sql.SparkSession
import za.co.absa.spline.common.TempDirectory


trait SparkFixture {

  private val tempWarehouseDirPath: Path =
    TempDirectory("SparkFixture", "UnitTest", pathOnly = true).path

  private val sessionBuilder: SparkSession.Builder =
    customizeBuilder(
      SparkSession.builder.
        master("local[4]").
        config("spark.ui.enabled", "false").
        config("spark.sql.warehouse.dir", tempWarehouseDirPath.toString)
    )

  def withNewSparkSession[T](testBody: SparkSession => T): T = {
    testBody(sessionBuilder.getOrCreate.newSession)
  }

  protected def customizeBuilder(builder: SparkSession.Builder): SparkSession.Builder = builder
}
