package io.torchbearer.routemanager.types

import com.javadocmd.simplelatlng.util.LengthUnit
import com.mapbox.services.directions.v5.models.DirectionsResponse
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
    val route = response.getRoutes.get(0)
    val steps = route.getLegs.get(0).getSteps

    steps.map(step => {
      val maneuver = step.getManeuver
      val bearing = maneuver.getBearingBefore.toInt

      val executionPointType = if (maneuver.getType == "arrive") {
        maneuver.getModifier match {
          case "left" => Constants.EXECUTION_POINT_TYPE_DESTINATION_LEFT
          case "right" => Constants.EXECUTION_POINT_TYPE_DESTINATION_RIGHT
          case _ => Constants.EXECUTION_POINT_TYPE_MANEUVER
        }
      }
      else {
        Constants.EXECUTION_POINT_TYPE_MANEUVER
      }

      // NOTE: MapBox is really stupid, and reverses {lat, long} to {long, lat}
      val lat = maneuver.getLocation()(1)
      val long = maneuver.getLocation()(0)

      val thisPoint = new LatLng(lat, long)
      val intersectionDistances = step.getIntersections.map(i => {
          val intersectionPoint = new LatLng(i.getLocation()(1), i.getLocation()(0))
          LatLngTool.distance(intersectionPoint, thisPoint, LengthUnit.MILE)
      }).filterNot(d => d == 0)

      val closestIntersectionDistance = if (intersectionDistances.nonEmpty) intersectionDistances.min else 9999.0

      ExecutionPoint(lat, long, bearing, executionPointType, closestIntersectionDistance)
    })
      .toList
  }

  def getMBRoute(executionPointMap: Map[(Double, Double, Int), Int], landmarkMap: Map[Int, Landmark]): Map[String, _] = {
    val route = response.getRoutes.get(0)

    // NOTE: As per usual, we must reverse the location arrays returned by MapBox as they are in {long, lat} form
    Map(
      "legs" -> route.getLegs.map(l => {
        Map(
          "steps" -> l.getSteps.map(s => {
            val epId = executionPointMap((
              s.getManeuver.getLocation()(1),
              s.getManeuver.getLocation()(0),
              s.getManeuver.getBearingBefore.toInt
            ))
            val landmark = landmarkMap.get(epId)

            Map(
              "geometry" -> s.getGeometry,
              "maneuver" -> Map(
                "bearing_after" -> s.getManeuver.getBearingAfter,
                "location" -> s.getManeuver.getLocation.reverse,
                "bearing_before" -> s.getManeuver.getBearingBefore,
                "type" -> s.getManeuver.getType,
                "instruction" -> s.getManeuver.getInstruction,
                "execution_point" -> epId,
                "landmark" -> landmark
              ),
              "distance" -> s.getDistance,
              "duration" -> s.getDuration,
              "name" -> s.getName,
              "mode" -> s.getMode,
              "executionPointId" -> epId
            )
          }),
          "distance" -> l.getDistance,
          "duration" -> l.getDuration,
          "summary" -> l.getSummary
        )
      }
      ),
      "duration" -> route.getDuration,
      "distance" -> route.getDistance,
      "geometry" -> route.getGeometry
    )
  }

  /**
    * Returns a map of (lat,long,bearing) onto Instruction
    *
    * @return
    */
  def getInstructions(): Map[(Double, Double, Int), Instruction] = {
    val route = response.getRoutes.get(0)
    val steps = route.getLegs.get(0).getSteps

    steps.zipWithIndex.map { case (step, index) =>
      val maneuver = step.getManeuver

      val action = maneuver.getInstruction
      val bearing = maneuver.getBearingBefore.toInt
      val lat = maneuver.getLocation()(1)
      val long = maneuver.getLocation()(0)

      val inst = Instruction(action, index, index == steps.length - 1)

      (lat, long, bearing) -> inst
    }
      .toMap
  }

  def getDuration = response.getRoutes.get(0).getDuration

  def getDistance = response.getRoutes.get(0).getDistance

  def getStatus = response.getCode
}
