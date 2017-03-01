package io.torchbearer.routemanager.resources

import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Hit}
import io.torchbearer.routemanager.{Constants, RouteManagerStack}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import _root_.akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}

class ExecutionPointResource(system: ActorSystem) extends RouteManagerStack with JacksonJsonSupport
  with FutureSupport with CorsSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  override protected implicit def executor: ExecutionContext = system.dispatcher

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  /**
    * Respond to preflight requests
    */
  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  /**
    * Get description of ExecutionPoint executionPoint by ID
    */
  get("/:epId/description") {
    val id = params("epId").toInt

    val saliencyReward = params.get("saliencyReward").map(_.toInt)
      .getOrElse(Constants.DEFAULT_SALIENCY_REWARD)
    val descriptionRewrad = params.get("descriptionReward").map(_.toInt)
      .getOrElse(Constants.DEFAULT_DESCRIPTION_REWARD)
    val saliencyAssignmentCount = params.get("saliencyAssignmentCount").map(_.toInt)
      .getOrElse(Constants.DEFAULT_SALIENCY_ASSIGNMENTS)
    val descriptionAssignmentCount = params.get("descriptionAssignmentCount").map(_.toInt)
      .getOrElse(Constants.DEFAULT_DESCRIPTION_ASSIGNMENTS)
    val distance = params.get("distance").map(_.toInt)
      .getOrElse(Constants.DEFAULT_DISTANCE)

    new AsyncResult {
      val is = Future {
        val hit = Hit.getHitForExecutionPointId(id, saliencyReward, descriptionRewrad, distance, saliencyAssignmentCount,
          descriptionAssignmentCount)
          .getOrElse {
            halt(404, "Hit not found for this execution point.")
          }

        val description = hit.computedDescription
          .getOrElse {
            halt(404, "No description is available yet")
          }

        "description" -> description.getRealization
      }
    }
  }

  /**
    * Get executionPoint by [lat_long_bearing] hash
    * Hash should be base64 encoded
    */
  get("/hash//:hash") {
    val hash = params("hash")

    new AsyncResult {
      val is = Future {
        ExecutionPoint.getExecutionPoint(hash) getOrElse halt(404, "Execution point not found")
      }
    }
  }

  /**
    * Get all execution points
    * Allows offset and limit query params
    */
  get("/") {
    val offset = params.getOrElse("offset", "0").toInt
    val limit = Math.min(params.getOrElse("limit", "100").toInt, 100)

    new AsyncResult {
      val is = Future {
        val points = ExecutionPoint.getPagedExecutionPoints(offset, limit)
        val count = ExecutionPoint.getCount
        Map(
          "count" -> count,
          "points" -> points
        )
      }
    }
  }

  override def error(handler: ErrorHandler): Unit = ???
}
