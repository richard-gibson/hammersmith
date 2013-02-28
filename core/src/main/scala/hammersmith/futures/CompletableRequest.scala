/**
 * Copyright (c) 2011-2013 Brendan W. McAdams <http://evilmonkeylabs.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package hammersmith
package futures

import hammersmith.wire._
import hammersmith.bson.SerializableBSONObject

trait CompletableRequest {
  val request: MongoClientMessage
  val future: RequestFuture
}

object CompletableRequest {

  def apply[F](m: MongoClientMessage, f: RequestFuture): CompletableRequest = apply((m, f))

  def apply[F]: PartialFunction[(MongoClientMessage, RequestFuture), CompletableRequest] = {
    case (q: QueryMessage, f: SingleDocQueryRequestFuture) ⇒ CompletableSingleDocRequest(q, f)
    case (q: QueryMessage, f: CursorQueryRequestFuture) ⇒ CompletableCursorRequest(q, f)
    case (gm: GetMoreMessage, f: GetMoreRequestFuture) ⇒ CompletableGetMoreRequest(gm, f)
    case (w: MongoClientWriteMessage, f: WriteRequestFuture) ⇒ CompletableWriteRequest(w, f)
    case (k: KillCursorsMessage, f: NoOpRequestFuture.type) ⇒ NonCompletableWriteRequest(k, f)
    case default ⇒ throw new IllegalArgumentException("Cannot handle a CompletableRequest of '%s'".format(default))
  }
}

trait CompletableReadRequest extends CompletableRequest {
  type T
  override val future: QueryRequestFuture
  val decoder = future.decoder
}

case class CompletableSingleDocRequest(override val request: QueryMessage, override val future: SingleDocQueryRequestFuture) extends CompletableReadRequest
case class CompletableCursorRequest(override val request: QueryMessage, override val future: CursorQueryRequestFuture) extends CompletableReadRequest
case class CompletableGetMoreRequest(override val request: GetMoreMessage, override val future: GetMoreRequestFuture) extends CompletableReadRequest
case class CompletableWriteRequest(override val request: MongoClientWriteMessage, override val future: WriteRequestFuture) extends CompletableRequest
case class NonCompletableWriteRequest(override val request: MongoClientMessage, override val future: NoOpRequestFuture.type) extends CompletableRequest

