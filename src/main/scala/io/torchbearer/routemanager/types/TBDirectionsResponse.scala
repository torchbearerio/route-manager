package io.torchbearer.routemanager.types

import com.javadocmd.simplelatlng.util.LengthUnit
import com.mapbox.api.directions.v5.models.DirectionsResponse
import io.torchbearer.ServiceCore.DataModel.{ExecutionPoint, Landmark}
import io.torchbearer.ServiceCore.Constants
import com.javadocmd.simplelatlng.{LatLng, LatLngTool}

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap

/**
  * Created by fredricvollmer on 1/30/17.
  */
class TBDirectionsResponse(response: DirectionsResponse) {

  /**
    * Returns a list of ExecutionPoints
    *
    * @return
    */
  def getExecutionPoints(): List[ExecutionPoint] = {
    val route = response.routes.get(0)
    val steps = route.legs.get(0).steps

    steps.map(step => {
      val maneuver = step.maneuver
      val bearing = maneuver.bearingBefore.toInt

      val executionPointType = if (maneuver.`type` == "arrive") {
        maneuver.modifier match {
          case "left" => Constants.EXECUTION_POINT_TYPE_DESTINATION_LEFT
          case "right" => Constants.EXECUTION_POINT_TYPE_DESTINATION_RIGHT
          case _ => Constants.EXECUTION_POINT_TYPE_MANEUVER
        }
      }
      else {
        Constants.EXECUTION_POINT_TYPE_MANEUVER
      }

      val lat = maneuver.location.latitude
      val long = maneuver.location.longitude

      val thisPoint = new LatLng(lat, long)
      val intersectionDistances = step.intersections.map(i => {
          val intersectionPoint = new LatLng(i.location.latitude, i.location.longitude)
          LatLngTool.distance(intersectionPoint, thisPoint, LengthUnit.MILE)
      }).filterNot(d => d == 0)

      val closestIntersectionDistance = if (intersectionDistances.nonEmpty) intersectionDistances.min else 9999.0

      ExecutionPoint(lat, long, bearing, executionPointType, closestIntersectionDistance)
    })
      .toList
  }

  def getMBRoute(executionPointMap: Map[(Double, Double, Int), Int], landmarkMap: Map[Int, Landmark]): Map[String, _] = {
    val route = response.routes.get(0)

    Map(
      "legs" -> route.legs.map(l => {
        Map(
          "steps" -> l.steps.map(s => {
            val epId = executionPointMap((
              s.maneuver.location.latitude,
              s.maneuver.location.longitude,
              s.maneuver.bearingBefore.toInt
            ))
            val landmark = landmarkMap.get(epId)

            Map(
              "geometry" -> s.geometry,
              "maneuver" -> Map(
                "bearing_after" -> s.maneuver.bearingAfter,
                "location" -> List(s.maneuver.location.latitude, s.maneuver.location.longitude),
                "bearing_before" -> s.maneuver.bearingBefore,
                "type" -> s.maneuver.`type`,
                "instruction" -> s.maneuver.instruction,
                "execution_point" -> epId,
                "landmark" -> landmark
              ),
              "distance" -> s.distance,
              "duration" -> s.duration,
              "name" -> s.name,
              "mode" -> s.mode,
              "executionPointId" -> epId
            )
          }),
          "distance" -> l.distance,
          "duration" -> l.duration,
          "summary" -> l.summary
        )
      }
      ),
      "duration" -> route.duration,
      "distance" -> route.distance,
      "geometry" -> route.geometry
    )
  }

  /**
    * Returns a map of (lat,long,bearing) onto Instruction
    *
    * @return
    */
  def getInstructions(): Map[(Double, Double, Int), Instruction] = {
    val route = response.routes.get(0)
    val steps = route.legs.get(0).steps

    steps.zipWithIndex.map { case (step, index) =>
      val maneuver = step.maneuver

      val action = maneuver.instruction
      val bearing = maneuver.bearingBefore.toInt
      val lat = maneuver.location.latitude
      val long = maneuver.location.longitude

      val inst = Instruction(action, index, index == steps.length - 1)

      (lat, long, bearing) -> inst
    }
      .toMap
  }

  def getDuration = response.routes.get(0).duration

  def getDistance = response.routes.get(0).distance

  def getStatus = response.code
}
