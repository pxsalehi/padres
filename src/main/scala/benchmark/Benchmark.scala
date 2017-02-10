package benchmark

import ca.utoronto.msrg.padres.common.util.Sleep

/**
  * Created by pxsalehi on 07.02.17.
  */
object Benchmark {
  val noOfPublications = 5 * 1024
  val benchmarkRounds = 7
  val msgSizes = List(512, 1024, 2*1024, 3*1024, 4*1024, 5*1024)
  val batchSizes = List(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)

  // all pubs and sub should match!
  def main(args: Array[String]) {
    if (args.length < 3) {
      println("Run with: publisher|subscriber client_id broker_uri")
      sys.exit
    }
    val client = args(0)
    val clientID = args(1)
    val brokerURI = args(2)

    client.toLowerCase match {
      case "publisher" => {
        val publisher = new Publisher(clientID, brokerURI)
        // connect to broker and advertise. After some pause run the benchmark N times
        publisher connect brokerURI
        publisher.advertise
        Sleep.sleep(6000)  // wait 5 seconds for sub
        for (round <- 1 to benchmarkRounds; msgSize <- msgSizes; batchSize <- batchSizes) {
          println(s"Running benchmark $round: message size = $msgSize, batch size = $batchSize")
          publisher.batchPublish(msgSize, batchSize)
          Sleep.sleep(5.0)
        }
        publisher.writeStats("publisher_benchmark")
        println("Publisher finished running benchmark!")
        publisher shutdown
      }
      case "subscriber" => {
        val subscriber = new Subscriber(clientID, brokerURI)
        subscriber.connect(brokerURI)
        // wait for advertisement then subscribe
        Sleep.sleep(3000)
        subscriber.subscribe
        for (round <- 1 to benchmarkRounds; msgSize <- msgSizes; batchSize <- batchSizes) {
          subscriber.receiveAndCheck(round, msgSize, batchSize)
          println(s"Received all messages for benchmark $round: " +
                  s"message size = $msgSize, batch size = $batchSize")
        }
        subscriber.writeStats("subscriber_benchmark")
        println("Subscriber finished running benchmark!")
        subscriber.shutdown
      }
      case _ => println("ERROR: Client can only be publisher or subscriber!")
    }
  }
}
