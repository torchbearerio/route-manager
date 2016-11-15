package io.torchbearer.turkservice

import com.amazonaws.services.sqs.model.{DeleteMessageRequest, Message, ReceiveMessageRequest}
import scala.collection.JavaConversions._
import org.slf4j.{LoggerFactory, Logger}
import io.torchbearer.ServiceCore.AWSServices.SQS

/**
  * Created by fredricvollmer on 10/30/16.
  */
object PollingTask extends Runnable {
  val logger = LoggerFactory.getLogger(this.getClass)

  // Build SQS client
  val sqs = SQS.createClient
  val request = new ReceiveMessageRequest(Constants.SQS_URL)

  logger.debug("SQS client built.")


  override def run() = {
    log("Polling task running.")
    val messages: Seq[Message] = sqs.receiveMessage(request).getMessages
    log(s"Received ${messages.length} messages")

    messages.foreach((m: Message) => {
      val handle = m.getReceiptHandle
      log(s"Message body: ${m.getMD5OfBody}")

      // Delete message from queue
      val deleteReq = new DeleteMessageRequest(Constants.SQS_URL, handle)
      sqs.deleteMessage(deleteReq)
    })

    log("Polling task complete.")
  }

  def log(message: String) = {
    println("PollingService: " + message)
  }
}
