package io.torchbearer.routemanager.types

import io.torchbearer.ServiceCore.AWSServices.SQS
import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Hit}
import io.torchbearer.ServiceCore.Redis.RedisClient
import io.torchbearer.ServiceCore.tyoes.Instruction
import io.torchbearer.routemanager.{Constants, MapboxService}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization._

/**
  * Created by fredricvollmer on 1/29/17.
  */
class Route(
                  val routeId: String,
                  val originLat: Double,
                  val originLong: Double,
                  val destLat: Double,
                  val destLong: Double,
                  val saliencyReward: Int = Constants.DEFAULT_SALIENCY_REWARD,
                  val descriptionReward: Int = Constants.DEFAULT_DESCRIPTION_REWARD,
                  val saliencyAssignmentCount: Int = Constants.DEFAULT_SALIENCY_ASSIGNMENTS,
                  val descriptionAssignmentCount: Int = Constants.DEFAULT_DESCRIPTION_ASSIGNMENTS,
                  val distance: Int = Constants.DEFAULT_DISTANCE,
                  var duration: Double = 0,
                  val clientUuid: Option[String] = None,
                  var instructionPoints: List[InstructionPoint] = List()) {

  var status: RouteStatus.RouteStatus = RouteStatus.OK

  def ingestRoute(): Unit = {
    // Retrieve directions from Mapbox
    val directions = MapboxService.retrieveRoute(this) getOrElse { return }

    // If Mapbox gave us back an error, set this error and return
    if (directions.getStatus != "Ok") {
      this.status = RouteStatus.ERROR
      return
    }

    val eps = directions.getExecutionPoints()

    // Update route duration
    this.duration = directions.getDuration

    // Retrieve points from db, insert if needed
    val ingestedPoints = ExecutionPoint.ingestExecutionPoints(eps)

    // Initialize instructions map
    val instructions = directions.getInstructions()

    // Merge points and instructions
    val keys = instructions.keySet & ingestedPoints.keySet
    this.instructionPoints = keys.map(k => InstructionPoint(ingestedPoints(k), instructions(k))).toList

    // Order instruction points by arrival
    this.instructionPoints = this.instructionPoints.sortBy(_.instruction.order)

    // Retrieve hit for each execution point
    this.instructionPoints.par.foreach(ip => {
      val hit = Hit.getHitForExecutionPointId(ip.executionPoint.executionPointId, this.saliencyReward,
        this.descriptionReward, this.distance, this.saliencyAssignmentCount, this.descriptionAssignmentCount)

      // If hit exists, great. Update Instruction.
      // Otherwise, submit this execution point to Turk Service.
      hit match {
        case Some(existingHit) => {
          ip.instruction.updateWithHit(existingHit)
        }

        case None => {
          // Submit execution point to Turk Service
          SQS.submitExecutionPointToTurkService(ip.executionPoint.executionPointId, saliencyReward, descriptionReward,
            saliencyAssignmentCount, descriptionAssignmentCount, distance)
        }
      }
    })
  }

}

object Route {
  private val redis = RedisClient.getClient
  implicit val formats = DefaultFormats

  def apply(originLat: Double, originLong: Double, destLat: Double, destLong: Double,
            saliencyReward: Int, descriptionReward: Int, saliencyAssignmentCount: Int,
            descriptionAssignmentCount: Int, distance: Int): Route = {
    val uuid = java.util.UUID.randomUUID.toString
    val route = new Route(uuid, originLat, originLong, destLat, destLong, saliencyReward, descriptionReward,
      saliencyAssignmentCount, descriptionAssignmentCount, distance)

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
