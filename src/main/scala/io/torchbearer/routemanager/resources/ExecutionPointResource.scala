package io.torchbearer.routemanager.resources

import java.sql.Timestamp

import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Hit}
import io.torchbearer.routemanager.{Constants, RouteManagerStack}
import org.json4s.{DefaultFormats, Formats}
import io.torchbearer.ServiceCore.{Constants => CoreConstants}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import _root_.akka.actor.ActorSystem
import io.torchbearer.ServiceCore.AWSServices.SFN
import org.scalatra.ActionResult._

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
        val hit = Hit.getHitForExecutionPointId(epId, pipeline) getOrElse halt(404)
        hit.getSelectedLandmark getOrElse halt(404)
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
  /**
    * Manually submit an execution point at given location and bearing for given pipeline.
    */
  post("/") {
    val lat = (parsedBody \ "lat").extract[Double]
    val long = (parsedBody \ "long").extract[Double]
    val bearing = (parsedBody \ "bearing").extract[Int]
    val pipeline = (parsedBody \ "pipeline").extract[String]
    val shouldStartExecution = (parsedBody \ "startPipeline").extractOrElse(true)
    val sampleSet = (parsedBody \ "sampleSet").extractOpt[String]

    new AsyncResult {
      val is = Future {
        val newEp = ExecutionPoint(lat, long, bearing)
        newEp.sampleSet = sampleSet

        // Insert ExecutionPoint if needed
        ExecutionPoint.insertExecutionPointIfNotExists(newEp)

        // Get id for this execution point now that we know it's in DB
        val epId = ExecutionPoint.getExecutionPoint(lat, long, bearing)
          .map(ep => ep.executionPointId)
          .getOrElse({halt(500, "Error creating ExecutionPoint")})

        // Get Hit for this ExecutionPoint and given pipeline
        val hit = Hit.getHitForExecutionPointId(epId, pipeline)

        // If Hit exists, great, no need to do anything!
        // If not, create Hit and send it down the pipeline
        if (hit.isEmpty) {
          var newHit = Hit(epId, pipeline)
          Hit.insertHit(newHit)
          newHit = Hit.getHitForExecutionPointId(epId, pipeline)
            .getOrElse({halt(500, "Couldn't find newly created hit")})

          if (shouldStartExecution) {
            val pipelineARN = SFN.getStateMachineArnForPipeline(pipeline)
            SFN.startExecution(pipelineARN, "epId" -> epId, "hitId" -> newHit.hitId)
            newHit.updateStatus(CoreConstants.HIT_STATUS_PROCESSING)
            newHit.updateProcessingStartTime(new Timestamp(System.currentTimeMillis()))
          }
        }

        Created()
      }
    }
  }

  override def error(handler: ErrorHandler): Unit = ???
}
