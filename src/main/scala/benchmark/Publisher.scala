package benchmark

import java.nio.file.{Paths, Files}

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message.{Publication, Advertisement}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory

import scala.collection._
import scala.util.Random

case class RawResult(msgSize: Int, batchSize: Int, time: Long)
case class AggregatedResult(msgSize: Int, batchSize: Int, times: List[Long])
case class NormalizedResult(msgSize: Int, batchSize: Int, value: Double)

/**
  * Created by pxsalehi on 07.02.17.
  */
class Publisher(var id: String, var brokerURI: String) extends Client(id) {
  val adv: Advertisement = MessageFactory
              .createAdvertisementFromString("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
  // record time it takes to send all pubs for each pair of msg size and batch size in each round
  val rawResults = mutable.ListBuffer[RawResult]()

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
    rawResults += RawResult(msgSize, batchSize, endTime - startTime)
  }

  def writeStats(filename: String): Unit = {
    calcAndWriteThroughputs(rawResults.toList)
    val normalized = calcNormalizedResults(rawResults.toList)
    // interpolate value for other batch sizes
    val interpolatedResults = interpolateResults(normalized)
    val all = (interpolatedResults ++ normalized).sortBy(r => (r.msgSize, r.batchSize))
    writeBatchFactors(all)
  }

  def writeBatchFactors(results: List[NormalizedResult]): Unit = {
    val batchWriter = Files.newBufferedWriter(Paths.get("batch_factors"))
    results.foreach{
      case NormalizedResult(msgSize, batchSize, result) =>
        batchWriter.write(s"($msgSize,$batchSize)\t$result\n")}
    batchWriter.close()
  }

  def calcAndWriteThroughputs(results: List[RawResult]): Unit = {
    val groupedResults = results.groupBy(r => (r.msgSize, r.batchSize))
    val aggResults = (for {
      (msgSize, batchSize) <- groupedResults.keys
      rs = groupedResults((msgSize, batchSize)).map(_.time)
    } yield AggregatedResult(msgSize, batchSize, rs.toList)).toList
    // collect throughput values for batch size 1 and smallest message
    val batchSize1Throughputs = for {
      res <- aggResults
      if (res.msgSize, res.batchSize) == (Benchmark.msgSizes.head, 1)
      t <- res.times
    } yield Benchmark.noOfPublications * 1000 / t
    val tpWriter = Files.newBufferedWriter(Paths.get("throughputs"))
    batchSize1Throughputs.foreach(t => tpWriter.write(s"$t\n"))
    tpWriter.close()
  }

  def calcNormalizedResults(results: List[RawResult]): List[NormalizedResult] = {
    val groupedResults = results.groupBy(r => (r.msgSize, r.batchSize))
    val aggResults = (for {
      (msgSize, batchSize) <- groupedResults.keys
      rs = groupedResults((msgSize, batchSize)).map(_.time)
    } yield AggregatedResult(msgSize, batchSize, rs.toList)).toList
    // calculate throughput and normalize
    val throughputs = for {
      res <- aggResults
      throughputs = res.times.map(Benchmark.noOfPublications * 1000.0 / _)
    } yield NormalizedResult(res.msgSize, res.batchSize, throughputs.sum / throughputs.size)
    val baseThroughput = throughputs
      .filter(r => (r.msgSize, r.batchSize) == (Benchmark.msgSizes.head, 1))
      .head.value
    val normalized = throughputs.map(
      r => NormalizedResult(r.msgSize, r.batchSize, r.value/baseThroughput) )
    return normalized
  }

  def interpolateResults(results: List[NormalizedResult]): List[NormalizedResult] = {
    // interpolate value for other batch sizes
    val interpolatedResults = for {
      msgSize <- Benchmark.msgSizes
      (bs1, bs2) <- Benchmark.batchSizes.sliding(2, 1).collect{case x::y::Nil => (x, y)}
      batchSize <- (bs1 + 1) until bs2
      bs1tp = results.filter(r => (r.msgSize, r.batchSize) == (msgSize, bs1)).head.value
      bs2tp = results.filter(r => (r.msgSize, r.batchSize) == (msgSize, bs2)).head.value
      newtp = interpolate(bs1, bs1tp, bs2, bs2tp, batchSize)
    } yield NormalizedResult(msgSize, batchSize, newtp)
    return interpolatedResults
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

object Test {
  def main(args: Array[String]) {
    val publisher = new Publisher("p1", "testBroker")
    val raw = List((512, 1, 654), (512, 2, 321), (512, 4, 234), (512, 8, 678), (512, 4, 678),
      (1024, 1, 789), (1024, 2, 123), (1024, 4, 234), (1024, 8, 77), (1024, 8, 85), (1024, 2, 234)
    ).map(r => RawResult(r._1, r._2, r._3))
    publisher.calcAndWriteThroughputs(raw)
    val normalized = publisher.calcNormalizedResults(raw)
    val inter = publisher.interpolateResults(normalized)
    publisher.writeBatchFactors(normalized ++ inter)
  }
}
