package benchmark

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message.{Publication, Advertisement}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory

import scala.collection._
import scala.util.Random

/**
  * Created by pxsalehi on 07.02.17.
  */
class Publisher(var id: String, var brokerURI: String) extends Client(id) {
  val adv: Advertisement = MessageFactory.createAdvertisementFromString(
                            "[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")

  // record time it takes to send all pubs for each pair of msg size and batch size in each round
  val results = mutable.ListBuffer[(Int, Int, Long)]()

  def advertise(): Unit = advertise(adv)

  def batchPublish(msgSize: Int, batchSize: Int): Unit = {
    val pubs = createPubs(Benchmark.noOfPublications, msgSize)
    val batch = mutable.ListBuffer[Publication]()
    val startTime = System.currentTimeMillis()
    for (pub <- pubs) {
      batch += pub
      if (batch.size == batchSize) {
        val batchedPub = batch.head
        batchedPub.setPayload(batch.toList) // set whole batch as payload
        publish(batchedPub)
        batch.clear()
      }
    }
    val endTime = System.currentTimeMillis()
    results += ((msgSize, batchSize, endTime - startTime))
  }

  def writeStats(filename: String): Unit = {
    val groupedResults = results.groupBy(r => (r._1, r._2))
    val aggResults = for { k <- groupedResults.keys
                           rs = groupedResults(k).map(_._3) } yield (k, rs)
    aggResults.foreach{
      case (params, results) => println(params + ": " + results.mkString(" "))
    }
  }

  private def createPubs(count: Int, payloadSize: Int): List[Publication] = {
    val pubs = mutable.ListBuffer[Publication]()
    for(i <- 0 until count) {
      val pubStr = s"[class,'temp'],[attr0,${Random.nextInt(1000)}],[attr1,${Random.nextInt(1000)}]"
      val pub = MessageFactory.createPublicationFromString(pubStr)
      val payload = new Array[Byte](payloadSize)
      Random.nextBytes(payload)
      pub.setPayload(payload)
      pubs += pub
    }
    return pubs.toList
  }
}
