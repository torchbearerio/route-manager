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
  get("/:epId/landmark") {
    val epId = params("epId").toInt
    val pipeline = params.getOrElse("pipeline", Constants.DEFAULT_PIPELINE)

    new AsyncResult {
      val is = Future {
        val hit = Hit.getHitForExecutionPointId(epId, pipeline) getOrElse halt(200)
        hit.getSelectedLandmark
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
