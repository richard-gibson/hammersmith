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

import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder
import com.mongodb.casbah.commons.Logging
import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer

/**
 * Decoder capable of safely decoding fragmented frames from BSON
 *
 * @TODO - Toggleable setting of maxFrameLength based on server BSON Size
 * (Currently defaults to a max of 4MB)
 */
protected[mongodb] class BSONFrameDecoder extends LengthFieldBasedFrameDecoder(1024 * 1024 * 4, 0, 4, -4, 0) with Logging