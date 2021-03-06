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

import java.net.URI

import com.arangodb.ArangoDatabase
import org.apache.commons.configuration.SystemConfiguration
import org.scalatest.{BeforeAndAfterEach, Suite}
import za.co.absa.spline.persistence.{ArangoFactory, ArangoInit}
import za.co.absa.spline.common.ConfigurationImplicits._

trait ArangoFixture extends BeforeAndAfterEach {

  this: Suite =>

  val arangoUri: String = new SystemConfiguration().getRequiredString("test.spline.arangodb.url")
  val arangodb: ArangoDatabase = ArangoFactory.create(new URI(arangoUri))

  def dropAndInit(arangodb: ArangoDatabase): Unit = {
    val db = arangodb
    if (db.exists()) {
      db.drop()
    }
    ArangoInit.initialize(db, dropIfExists = true)
  }

  override protected def beforeEach(): Unit = {
    dropAndInit(arangodb)
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    try super.afterEach()
    finally dropAndInit(arangodb)
  }

}
