/*
 * Copyright 2018 ksilin
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

import org.specs2._
import cats.effect._
import com.example.fpio.SharedApp

class SharedStateSpec extends mutable.Specification {

  // https://typelevel.org/blog/2018/06/07/shared-state-in-fp.html

  "using mutable state 1" >> {
    val res: IO[ExitCode] = SharedApp.run(Nil)
    println(res)
//    res must_== (ExitCode.Success)
//    false must beEqualTo(true)
    false.must_==(true)
  }

}
