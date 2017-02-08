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

  def subscribe(): Unit = subscribe(sub)

  def receiveAndCheck(round: Int, msgSize: Int, batchSize: Int): Unit = {
    println("Collecting publications...")
    while(received != Benchmark.noOfPublications)
      Sleep.sleep(1000)
    received = 0
  }

  override def processMessage(msg: Message): Unit = {
    super.processMessage(msg)
    if (msg.getType.equals(MessageType.PUBLICATION))
      received += 1
  }

  def writeStats(filename: String): Unit = {
    println("Write stats!")
  }
}
