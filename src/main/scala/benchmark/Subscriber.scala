package benchmark

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message.{MessageType, Message, Subscription}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory._
import ca.utoronto.msrg.padres.common.util.Sleep

/**
  * Created by pxsalehi on 07.02.17.
  */
class Subscriber(var id: String, var brokerURI: String) extends Client(id) {
  val sub: Subscription = createSubscriptionFromString ("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
  var received = 0
  var done: Boolean = false

  def subscribe(): Unit = subscribe(sub)

  def receiveAndCheck(round: Int, msgSize: Int, batchSize: Int): Unit = {
    println("Collecting publications...")
    done.wait
  }

  override def processMessage(msg: Message): Unit = {
    super.processMessage(msg)
    if (msg.getType.equals(MessageType.PUBLICATION)) {
      // check correctness
      received += 1
      if (received == Benchmark.noOfPublications) {
        received = 0
        done.notify
      }
    }
  }

  def writeStats(filename: String): Unit = {
    println("Write stats!")
  }
}
