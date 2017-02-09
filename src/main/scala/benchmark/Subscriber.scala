package benchmark

import java.util.concurrent.Semaphore

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message._
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory

/**
  * Created by pxsalehi on 07.02.17.
  */
class Subscriber(var id: String, var brokerURI: String) extends Client(id) {
  val sub: Subscription =
    MessageFactory.createSubscriptionFromString ("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
  var received = 0
  var totalReceived = 0
  var semaphore = new Semaphore(0, true)

  def subscribe(): Unit = subscribe(sub)

  def receiveAndCheck(round: Int, msgSize: Int, batchSize: Int): Unit = {
    println("Collecting publications...")
    semaphore.acquire()
  }

  override def processMessage(msg: Message): Unit = {
    super.processMessage(msg)
    if (msg.getType.equals(MessageType.PUBLICATION)) {
      // check correctness
      synchronized {
        val pub = msg.asInstanceOf[PublicationMessage].getPublication
        val batchSize = pub.getPayload.asInstanceOf[List[Publication]].size
        totalReceived += batchSize
        received += batchSize
        if (received >= Benchmark.noOfPublications) {
          received -= Benchmark.noOfPublications
          semaphore.release()
        }
      }
    }
  }

  def writeStats(filename: String): Unit = {
//    println("Write stats!")
    println(s"Total received = $totalReceived")
  }
}
