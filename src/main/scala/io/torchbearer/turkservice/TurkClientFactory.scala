package io.torchbearer.turkservice

import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.service.exception.ServiceException
import com.amazonaws.mturk.util.{ClientConfig, PropertiesClientConfig}
import com.amazonaws.mturk.requester.HIT

object TurkClientFactory {
  println(System.getProperty("user.dir"))
  private val config = new PropertiesClientConfig("./src/main/resources/mturk.properties")
  config.setAccessKeyId("AKIAJQ72DSDXNTL746QA")
  config.setSecretAccessKey("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")
  config.setServiceURL("https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester")

  private val client = new RequesterService(config)

  def getClient = client

}
