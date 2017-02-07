package de.tum.msrg.benchmark

import ca.utoronto.msrg.padres.client.Client
import ca.utoronto.msrg.padres.common.message.Subscription
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory._

/**
  * Created by pxsalehi on 07.02.17.
  */
class Subscriber(var id: String, var brokerURI: String, var batchSize: Int) extends Client(id) {
  val sub: Subscription = createSubscriptionFromString ("[class,eq,'temp'],[attr0,<,1000],[attr1,>,-1]")
}
