package de.tum.msrg.benchmark

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message.{Publication, Advertisement}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory._

import scala.collection.mutable
import scala.util.Random

/**
  * Created by pxsalehi on 07.02.17.
  */
class Publisher(var id: String, var brokerURI: String, var msgSize: Int, var batchSize: Int) extends Client(id) {
  val adv: Advertisement = createAdvertisementFromString("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")

  private def createPubs(count: Int, payloadSize: Int): List[Publication] = {
    val pubs = mutable.ListBuffer[Publication]()
    for(i <- 0 until count) {
      val pubStr = s"[class,'temp'],[attr0,${Random.nextInt(1000)}],[attr1,${Random.nextInt(1000)}]"
      val pub = createPublicationFromString(pubStr)
      val payload = new Array[Byte](payloadSize)
      Random.nextBytes(payload)
      pub.setPayload(payload)
      pubs += pub
    }
    return pubs.toList
  }
}
