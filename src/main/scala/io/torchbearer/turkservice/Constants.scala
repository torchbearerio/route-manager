package io.torchbearer.turkservice

/**
  * Created by fredricvollmer on 10/30/16.
  */
object Constants {
  // SQS
  final val SQS_URL = "https://sqs.us-west-2.amazonaws.com/814009652816/completed-hits"

  // Turk Questions
  final val EXTERNAL_QUESTION_BASE_URL = "https://turkservice.torchbearer.io"
  final val INITIAL_ASSIGNMENT_COUNT = 5
  final val INITIAL_HIT_LIFETIME = 10000

  final val SALIENCY_QUESTION_VERSION = 1
  final val DESCRIPTION_QUESTION_VERSION = 1

}
