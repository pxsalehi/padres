package de.tum.msrg.benchmark

import ca.utoronto.msrg.padres.common.message.{Subscription, Advertisement, Publication}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory
import ca.utoronto.msrg.padres.client.Client

import scala.util.Random
import scala.collection._

/**
  * Created by pxsalehi on 07.02.17.
  */
object Benchmark{
  val adv: Advertisement =
    MessageFactory.createAdvertisementFromString("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
  val sub: Subscription =
    MessageFactory.createSubscriptionFromString ("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
  // all pubs and sub should match!
  def main(args: Array[String]) {
    if (args.length < 4) {
      sys.error("Run with: client_id broker_uri msg_size(bytes) batch_size")
    }
    val pubs = createPubs(10240, 10*1024)

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
