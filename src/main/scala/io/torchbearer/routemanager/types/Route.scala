package io.torchbearer.routemanager.types

import io.torchbearer.ServiceCore.AWSServices.SQS
import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Hit, Landmark}
import io.torchbearer.ServiceCore.Redis.RedisClient
import io.torchbearer.routemanager.{Constants, MapboxService}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._
import com.mapbox.services.directions.v5.models.DirectionsRoute

/**
  * Created by fredricvollmer on 1/29/17.
  */
class Route(
                  val routeId: String,
                  val originLat: Double,
                  val originLong: Double,
                  val destLat: Double,
                  val destLong: Double,
                  val pipeline: String
                  ) {

  var status: RouteStatus.RouteStatus = RouteStatus.OK
  var landmarks: Map[Int, Landmark] = Map()
  var mapboxRoute: Option[TBDirectionsResponse] = None
  var navigation: Option[Map[String, _]] = None

  def ingestRoute(): Unit = {
    // Retrieve directions from Mapbox
    val directions = MapboxService.retrieveRoute(this) getOrElse { return }

    // If Mapbox gave us back an error, set this error and return
    if (directions.getStatus != "Ok") {
      this.status = RouteStatus.ERROR
      return
    }

    val eps = directions.getExecutionPoints()

    // Retrieve points from db, insert if needed
    val ingestedPoints = ExecutionPoint.ingestExecutionPoints(eps)

    // Retrieve hit for each execution point
    ingestedPoints.values.par.foreach(p => {
      val landmark = Hit.getHitForExecutionPointId(p, this.pipeline).flatMap(h => h.getSelectedLandmark)

      // If hit exists, great. Update Instruction.
      // Otherwise, submit this execution point to Turk Service.
      landmark match {
        case Some(lm) => {
          this.landmarks += (p -> lm)
        }

        case None => {
          // Submit execution point to Turk Service
          /*
          SQS.submitExecutionPointToTurkService(ip.executionPoint.executionPointId, saliencyReward, descriptionReward,
            saliencyAssignmentCount, descriptionAssignmentCount, distance)
          */
        }
      }
    })

    // Initialize instructions map
    /*val instructions = directions.getInstructions()

    // Merge points and instructions
    val keys = instructions.keySet & ingestedPoints.keySet
    this.instructionPoints = keys.map(k => InstructionPoint(ingestedPoints(k), instructions(k))).toList

    // Order instruction points by arrival
    this.instructionPoints = this.instructionPoints.sortBy(_.instruction.order)
    */

    // Attach raw mapbox route object
    this.mapboxRoute = Some(directions)

    // Build navigation object
    val landmarkDescriptionMap: Map[Int, Option[String]] = this.landmarks.map(lm => lm._1 -> lm._2.description)
    this.navigation = Some(directions.getMBRoute(ingestedPoints, landmarkDescriptionMap))
  }
}

object Route {
  private val redis = RedisClient.getClient
  implicit val formats = DefaultFormats

  def apply(originLat: Double, originLong: Double, destLat: Double, destLong: Double,
            pipeline: String): Route = {
    val uuid = java.util.UUID.randomUUID.toString
    val route = new Route(uuid, originLat, originLong, destLat, destLong, pipeline)

    // Process route execution points, retrieve instructions
    route.ingestRoute()

    route
  }

  /*

  -----------------------------------------------------
  These methods are for keeping route state in Redis.
  They are not currently used, and will need refactoring.
  However, they could be a good starting point if we ever decide to save route state.
  -----------------------------------------------------

  def getRoute(id: String): Option[Route] = {
    redis.hgetall1(s"routes:$id").map(m => {
      val instructionPoints = m.get("instructions").map(i =>
        parse(i).extract[Vector[(ExecutionPoint, String, Vector[Instruction])]])
      new Route(id,
        m("origin_lat").toDouble,
        m("origin_long").toDouble,
        m("dest_lat").toDouble,
        m("dest_long").toDouble,
        m.get("distance").map(_.toDouble),
        m.get("duration").map(_.toDouble),
        m.get("client_uuid"),
        instructionPoints)
    })
  }

  def getInstructionsForRoute(routeId: String): String = {
    implicit val formats = Serialization.formats(NoTypeHints)

    redis.get(s"spoken_instructions:$routeId") getOrElse write(Array())
  }

  def saveRoute(route: Route): Unit = {
    implicit val formats = Serialization.formats(NoTypeHints)

    val key = s"routes:${route.routeId}"

    redis.hmset(key, Map(
      "origin_lat" -> route.originLat,
      "origin_long" -> route.originLong,
      "destination_lat" -> route.destLat,
      "destination_long" -> route.destLong))

    route.clientUuid.map(rid => redis.hset(key, "client_uuid", rid))
    route.instructionPoints.map(ips => redis.hset(key, "instructions", write(ips)))
  }

  def updateClientUuidForRoute(routeId: String, clientUuid: String): Unit = {
    val key = s"routes:$routeId"
    redis.hset(key, "clientUuid", clientUuid)
  }

*/
}
