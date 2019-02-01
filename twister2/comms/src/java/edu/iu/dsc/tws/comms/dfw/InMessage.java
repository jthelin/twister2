//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.comms.dfw;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.iu.dsc.tws.comms.api.MessageHeader;
import edu.iu.dsc.tws.comms.api.MessageType;

public class InMessage {
  private Queue<ChannelMessage> channelMessages = new LinkedBlockingQueue<>();

  /**
   * The buffers added to this message
   */
  private Queue<DataBuffer> buffers = new LinkedBlockingQueue<>();

  /**
   * The overflow buffers created
   */
  private Queue<DataBuffer> overFlowBuffers = new LinkedBlockingQueue<>();

  /**
   * We call this to release the buffers
   */
  private ChannelMessageReleaseCallback releaseListener;

  /**
   * Keep track of the originating id, this is required to release the buffers allocated.
   */
  private int originatingId;

  /**
   * The message header
   */
  protected MessageHeader header;

  /**
   * Keep whether we have all the buffers added
   */
  protected boolean complete = false;

  /**
   * Message type
   */
  private MessageType dataType;

  /**
   * If a keyed message, the key being used
   */
  private MessageType keyType = MessageType.INTEGER;

  /**
   * Type of the message, weather request or send
   */
  private MessageDirection messageDirection;

  /**
   * The deserialized data
   */
  private Object deserializedData;

  /**
   * The object that is been built
   */
  private Object deserializingObject;

  /**
   * Number of buffers added
   */
  private int addedBuffers = 0;

  // the amount of data we have seen for current object
  private int bufferPreviousReadForObject = 0;

  // keep track of the current object length
  private int bufferCurrentObjectLength = 0;

  // the objects we have in buffers so far, this doesn't mean we have un-packed them
  private int bufferSeenObjects = 0;

  /**
   * The messages already consumed
   */
  private Queue<ChannelMessage> builtMessages = new LinkedBlockingQueue<>();

  /**
   * Following variables keep track of the reading object
   */
  private int unPkCurrentObjectLength = 0;

  /**
   * The number of objects unpacked
   */
  private int unPkNumberObjects = 0;

  /**
   * Number of buffers we have unpacked
   */
  private int unPkBuffers = 0;

  /**
   * The current index of unpack
   */
  private int unPkCurrentIndex = 0;


  public enum ReceivedState {
    INIT,
    BUILDING,
    BUILT,
    RECEIVE,
    DONE,
  }

  /**
   * Received state
   */
  private ReceivedState receivedState;

  public InMessage(int originatingId, MessageType messageType,
                        MessageDirection messageDirection,
                        ChannelMessageReleaseCallback releaseListener) {
    this.releaseListener = releaseListener;
    this.originatingId = originatingId;
    this.complete = false;
    this.dataType = messageType;
    this.receivedState = ReceivedState.INIT;
  }

  public void setDataType(MessageType dataType) {
    this.dataType = dataType;
  }

  public MessageType getDataType() {
    return dataType;
  }

  public void setKeyType(MessageType keyType) {
    this.keyType = keyType;
  }

  public MessageType getKeyType() {
    return keyType;
  }

  public void setHeader(MessageHeader header) {
    this.header = header;
  }

  public MessageHeader getHeader() {
    return header;
  }

  public boolean addBufferAndCalculate(DataBuffer buffer) {
    buffers.add(buffer);
    addedBuffers++;

    int expectedObjects = header.getNumberTuples();
    int remaining = 0;
    int currentLocation = 0;
    // if this is the first buffer or, we haven't read the current object length
    if (addedBuffers == 1) {
      currentLocation = 16;
      bufferCurrentObjectLength = buffer.getByteBuffer().getInt(currentLocation);
      remaining = buffer.getSize() - Integer.BYTES - 16;
      currentLocation += Integer.BYTES;
    } else if (bufferCurrentObjectLength == -1) {
      bufferCurrentObjectLength = buffer.getByteBuffer().getInt(0);
      remaining = buffer.getSize() - Integer.BYTES - 16;
      currentLocation += Integer.BYTES;
    }

    while (remaining > 0) {
      // need to read this much
      int moreToReadForCurrentObject = bufferCurrentObjectLength - bufferPreviousReadForObject;
      // amount of data in the buffer
      if (moreToReadForCurrentObject < remaining) {
        bufferSeenObjects++;
        remaining = remaining - moreToReadForCurrentObject;
        currentLocation += moreToReadForCurrentObject;
      } else {
        bufferPreviousReadForObject += remaining;
        break;
      }

      // if we have seen all, lets break
      if (expectedObjects == bufferSeenObjects) {
        complete = true;
        break;
      }

      // we can read another object
      if (remaining > Integer.BYTES) {
        bufferCurrentObjectLength = buffer.getByteBuffer().getInt(currentLocation);
        bufferPreviousReadForObject = 0;
      } else {
        // we need to break, we set the length to -1 because we need to read the length
        // in next buffer
        bufferCurrentObjectLength = -1;
        break;
      }
    }

    return complete;
  }

  public Queue<ChannelMessage> getChannelMessages() {
    return channelMessages;
  }

  public ChannelMessage getFirstChannelMessage() {
    return channelMessages.peek();
  }

  public void addChannelMessage(ChannelMessage channelMessage) {
    channelMessages.add(channelMessage);
  }

  public int getOriginatingId() {
    return originatingId;
  }

  public Queue<DataBuffer> getBuffers() {
    return buffers;
  }

  public ReceivedState getReceivedState() {
    return receivedState;
  }

  public void setReceivedState(ReceivedState receivedState) {
    this.receivedState = receivedState;
  }

  public Queue<ChannelMessage> getBuiltMessages() {
    return builtMessages;
  }

  public Object getDeserializedData() {
    return deserializedData;
  }

  public Object getDeserializingObject() {
    return deserializingObject;
  }

  public void setDeserializingObject(Object deserializingObject) {
    this.deserializingObject = deserializingObject;
  }

  public void addOverFlowBuffer(DataBuffer buffer) {
    overFlowBuffers.offer(buffer);
  }

  public int getBufferSeenObjects() {
    return bufferSeenObjects;
  }

  public int getUnPkCurrentObjectLength() {
    return unPkCurrentObjectLength;
  }

  public int getUnPkNumberObjects() {
    return unPkNumberObjects;
  }

  public int getUnPkBuffers() {
    return unPkBuffers;
  }

  public int getUnPkCurrentIndex() {
    return unPkCurrentIndex;
  }

  public void setUnPkCurrentIndex(int unPkCurrentIndex) {
    this.unPkCurrentIndex = unPkCurrentIndex;
  }
}
