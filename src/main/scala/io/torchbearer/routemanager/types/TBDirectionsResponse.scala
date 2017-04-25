package io.torchbearer.routemanager.types
import com.mapbox.services.directions.v5.models.DirectionsResponse
import io.torchbearer.ServiceCore.DataModel.ExecutionPoint

import scala.collection.JavaConversions._
import scala.collection.immutable.ListMap

/**
  * Created by fredricvollmer on 1/30/17.
  */
class TBDirectionsResponse(response: DirectionsResponse) {

  /**
    * Returns a list of ExecutionPoints
    * @return
    */
  def getExecutionPoints(): List[ExecutionPoint] = {
    val route = response.getRoutes.get(0)
    val steps = route.getLegs.get(0).getSteps

    steps.map(step => {
      val maneuver = step.getManeuver

      val bearing = maneuver.getBearingBefore.toInt

      // NOTE: MapBox is really stupid, and reverses {lat, long} to {long, lat}
      val lat = maneuver.getLocation()(1)
      val long = maneuver.getLocation()(0)

      ExecutionPoint(lat, long, bearing)
    })
      .toList
  }

  def getMBRoute(executionPointIds: Map[(Double, Double, Int), Int], landmarks: Map[Int, Option[String]]): Map[String , _] = {
    val route = response.getRoutes.get(0)
    Map(
      "legs" -> route.getLegs.map(l => {
        Map(
          "steps" -> l.getSteps.map(s => {
            val epId = executionPointIds((
              s.getManeuver.getLocation()(1),
              s.getManeuver.getLocation()(0),
              s.getManeuver.getBearingBefore.toInt
              ))
            val landmark = landmarks.get(epId).flatten

            Map(
              "geometry" -> s.getGeometry,
              "maneuver" -> Map(
                "bearing_after" -> s.getManeuver.getBearingAfter,
                "location" -> s.getManeuver.getLocation,
                "bearing_before" -> s.getManeuver.getBearingBefore,
                "type" -> s.getManeuver.getType,
                "instruction" -> s.getManeuver.getInstruction,
                "execution_point" -> epId,
                "landmark" -> landmark
              ),
              "distance" -> s.getDistance,
              "duration" -> s.getDuration,
              "name" -> s.getName,
              "mode" -> s.getMode
            )
          }),
          "distance" -> l.getDistance,
          "duration" -> l.getDuration,
          "summary" -> l.getSummary
        )}
      ),
      "duration" -> route.getDuration,
      "distance" -> route.getDistance,
      "geometry" -> route.getGeometry
    )
  }

  /**
    * Returns a map of (lat,long,bearing) onto Instruction
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
