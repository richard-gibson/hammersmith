/**
 * Copyright (c) 2010, 2011 10gen, Inc. <http://10gen.com>
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

package com.mongodb
package wire

import org.bson.util.Logging
import java.io.{ByteArrayInputStream , InputStream}
import org.bson._
import scala.util.control.Exception._

/**
* OP_REPLY
*
* OP_REPLY is sent by the database in response to an OP_QUERY or
* OP_GET_MORE message.
*
*/
trait ReplyMessage extends MongoMessage {
  //val header: MessageHeader // Standard message header
  val opCode = OpCode.OpReply
  val header: MessageHeader

  val flags: Int // bit vector of reply flags, available in ReplyFlag
  def cursorNotFound = (flags & ReplyFlag.CursorNotFound.id) > 0
  def queryFailure = (flags & ReplyFlag.QueryFailure.id) > 0
  def awaitCapable = (flags & ReplyFlag.AwaitCapable.id) > 0
  val cursorID: Long // cursorID, for client to do getMores
  val startingFrom: Int // Where in the cursor this reply starts at
  val numReturned: Int // Number of documents in the reply.
  val documents: Seq[BSONDocument] // Sequence of documents

  protected def writeMessage(enc: BSONSerializer) =
    throw new UnsupportedOperationException("This message is not capable of being written. "
      + "Replies come only from the server.")



}

object ReplyMessage extends Logging {

  // TODO - Make it possible to dyamically set a decoder.
  val decoder = new DefaultBSONDeserializer
  def apply(_hdr: MessageHeader, in: InputStream) = {
    import org.bson.io.Bits._
    import MongoMessage.readFromOffset


    log.debug("Finishing decoding Reply Message with Header of '%s'", _hdr)
    val b = new Array[Byte](20) // relevant non-document stream bytes from the reply content.
    readFully(in, b)
    val bin = new ByteArrayInputStream(b)
    log.trace("Offset data for rest of reply read: %s", bin)
    new ReplyMessage {
      val header = _hdr
      val flags = readInt(bin)
      log.trace("[Reply] Flags: %d", flags)
      val cursorID = readLong(bin)
      log.trace("[Reply] Cursor ID: %d", cursorID)
      val startingFrom = readInt(bin)
      log.trace("[Reply] Starting From: %d", startingFrom)
      val numReturned = readInt(bin)
      log.trace("[Reply] Number of Documents Returned: %d", numReturned)
      /*
       * And here comes the hairy part.  Ideally, we want to completely amortize the
       * decoding of these docs.  It makes *zero* sense to me to wait for a whole
       * block of documents to decode from BSON before I can begin iteration.
       */
      val documents = for (i <- 0 until numReturned) yield decoder.decodeAndFetch(in).asInstanceOf[BSONDocument]

      assert(documents.length == numReturned, "Number of parsed documents doesn't match expected number returned." +
             "Wanted: %d Got: %d".format(numReturned, documents.length))
      log.debug("Parsed Out Documents: %s", documents)

      override def toString = "ReplyMessage { " +
        "responseTo: %d, cursorID: %d, startingFrom: %d, numReturned: %d, cursorNotFound? %s, queryFailure? %s, awaitCapable? %s, docs: %s } ".
        format(header.responseTo, cursorID, startingFrom, numReturned, cursorNotFound, queryFailure, awaitCapable, documents)
    }
  }

}

object ReplyFlag extends Enumeration {
  /**
   * Set when getMore is called but the cursorID is not valid
   * on the server.  Returns with 0 results.
   */
  val CursorNotFound = Value(1 << 0)

  /**
   * Set when a query fails.  Results consist of one document
   * containing an "$err" field describing the failure.
   */
  val QueryFailure = Value(1 << 1)

  /**
   * Drivers should ignore this; only mongos ever sees it set,
   * in which case it needs to update config from the server
   */
  val ShardConfigState = Value(1 << 2)

  /**
   * Set when the server supports the AwaitData Query Option.  If
   * it doesn't, a client should sleep a little between getMore's on
   * a Tailable cursor.  MongoDB 1.6+ support AwaitData and thus always
   * sets AwaitCapable
   */
  val AwaitCapable = Value(1 << 3)

  // Bits 4-31 are reserved for future usage
}
