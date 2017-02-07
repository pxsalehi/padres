package de.tum.msrg.benchmark

import ca.utoronto.msrg.padres.common.message.{Subscription, Advertisement, Publication}
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory
import ca.utoronto.msrg.padres.client.Client

import scala.util.Random
import scala.collection._

/**
  * Created by pxsalehi on 07.02.17.
  */
object Benchmark {
  val noOfPublications = 10 * 1024
  val benchmarkRounds = 10

  // all pubs and sub should match!
  def main(args: Array[String]) {
    if (args.length < 5) {
      println("Run with: publisher|subscriber client_id broker_uri msg_size(bytes) batch_size")
      sys.exit
    }
    val client = args(0)
    val clientID = args(1)
    val brokerURI = args(2)
    val msgSize = args(3).toInt
    val batchSize = args(4).toInt

    client.toLowerCase match {
      case "publisher" => {
        val publisher = new Publisher(clientID, brokerURI, msgSize, batchSize)
        // connect to broker and advertise. After some pause run the benchmark N times
      }
      case "subscriber" => {
        val subscriber = new Subscriber(clientID, brokerURI, batchSize)
      }
      case _ => println("ERROR: Client can only be publisher or subscriber!")
    }
  }
}
