/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.openDFDL

import org.apache.daffodil.tdml.Runner
import org.junit.Test

object TestCrossTestRig {
  lazy val runner = Runner("", "crossTestRigTests.tdml")
}

class TestCrossTestRig {
  import TestCrossTestRig._

  @Test def testIBMTDML1() { runner.runOneTest("test1") }

  @Test def testIBMTDML2() { runner.runOneTest("test2") }

  @Test def testIBMTDMLNeg1() { runner.runOneTest("test1Neg") }

  @Test def testIBMTDML_utest1() { runner.runOneTest("utest1") }

  @Test def testIBMTDML_twoPass() { runner.runOneTest("testTwoPass") }

  @Test def testIBMTDML_ptest1_neg() { runner.runOneTest("ptest1-neg") }

}
