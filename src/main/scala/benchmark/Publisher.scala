package benchmark

import java.nio.file.{Paths, Files}

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
    println("  Sending publications...")
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
    val aggResults = (for { k <- groupedResults.keys
                           rs = groupedResults(k).map(_._3) } yield (k, rs.toList) ).toList
    // collect throughput values for batch size 1 and smallest message
    val batchSize1Throughputs = for { r <- aggResults
                                      if r._1 == (Benchmark.msgSizes.head, 1)
                                      t <- r._2 } yield Benchmark.noOfPublications * 1000 / t
    val tpWriter = Files.newBufferedWriter(Paths.get("throughputs"))
    batchSize1Throughputs.foreach(t => tpWriter.write(s"$t\n"))
    tpWriter.close()
    // calculate throughput and normalize
    val throughputs = for {
      (params, results) <- aggResults
      tps = results.map(Benchmark.noOfPublications * 1000.0 / _)
    } yield (params, tps.sum / tps.size)
    val baseThroughput = throughputs.filter(_._1 == (Benchmark.msgSizes.head, 1)).head._2
    val normalized = throughputs.map{case (params, tp) => (params, tp/baseThroughput)}
    // interpolate value for other batch sizes
    val interpolatedResults = for {
      msgSize <- Benchmark.msgSizes
      (bs1, bs2) <- Benchmark.batchSizes.sliding(2, 1).collect{case x::y::nill => (x, y)}
      batchSize <- (bs1 + 1) until bs2
      bs1tp = normalized.filter(_._1 == (msgSize, bs1)).head._2
      bs2tp = normalized.filter(_._1 == (msgSize, bs2)).head._2
      newtp = interpolate(bs1, bs1tp, bs2, bs2tp, batchSize)
    } yield ((msgSize, batchSize), newtp)
    val all = (interpolatedResults ++ normalized).sortBy(_._1)
    val batchWriter = Files.newBufferedWriter(Paths.get("batch_factors"))
    all.foreach{case (params, result) => batchWriter.write(s"$params\t$result\n")}
    batchWriter.close()
  }

  private def interpolate(x0: Double, y0: Double, x1: Double, y1: Double, x: Double): Double = {
    y0 + (x - x0) * (y1 - y0) / (x1 - x0)
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
