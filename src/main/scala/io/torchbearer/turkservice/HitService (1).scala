package io.torchbearer.turkservice

import io.torchbearer.ServiceCore.DataModel.ExecutionPoint
import io.torchbearer.ServiceCore.Utils._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Created by fredricvollmer on 11/11/16.
  */
object HitService {
  private val turkClient = TurkClientFactory.getClient

  private def getHitReward(t: Task): Double = {
    t match {
      case SALIENCY_DETECTION => 0.05
      case OBJECT_DESCRIPTION => 0.05
    }
  }

  private def submitHitForExecutionPoint(pointId: Int, t: Task, n: Int, reward: Double, lifetime: Long) {
    val baseUrl = s"${Constants.EXTERNAL_QUESTION_BASE_URL}/external/hit/${t.name}"
    val url = formatURLWithQueryParams(baseUrl, "epId" -> pointId)

    val questionXML =
      <ExternalQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd">
        <ExternalURL>{url}</ExternalURL>
        <FrameHeight>700</FrameHeight>
      </ExternalQuestion>
    val question = questionXML.toString()
    val hit = turkClient.createHIT(null, t.title, t.description, t.keywords, question, reward, t.assingmentDuration, t.autoApprovalDelay, lifetime, n, null, t.qualificationRequirements, null)

    println(s"Created ${t.name} hit for execution point $pointId. (${hit.getHITId})")
    // For now, don't bother adding this hit to DB
  }

  def processExecutionPoints(epIds: List[Int]): Unit = {
    println(s"Processing ${epIds.length} execution points...")
    epIds.par.foreach(epId => {
      submitHitForExecutionPoint(epId, SALIENCY_DETECTION, Constants.INITIAL_ASSIGNMENT_COUNT, getHitReward(SALIENCY_DETECTION), Constants.INITIAL_HIT_LIFETIME)
    })
    println("Processing complete")
  }
}
