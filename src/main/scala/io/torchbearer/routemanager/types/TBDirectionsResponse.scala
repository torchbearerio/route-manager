package io.torchbearer.routemanager.types
import com.mapbox.services.directions.v5.models.DirectionsResponse
import io.torchbearer.ServiceCore.DataModel.ExecutionPoint
import io.torchbearer.ServiceCore.tyoes.Instruction

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
      val lat = maneuver.getLocation()(0)
      val long = maneuver.getLocation()(1)

      ExecutionPoint(lat, long, bearing)
    })
      .toList
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

      val instruction = maneuver.getInstruction
      val bearing = maneuver.getBearingBefore.toInt
      val lat = maneuver.getLocation()(0)
      val long = maneuver.getLocation()(1)

      val inst = Instruction(instruction)

      inst.order = index

      (lat, long, bearing) -> inst
    }
      .toMap
  }

  def getDuration = response.getRoutes.get(0).getDuration

  def getDistance = response.getRoutes.get(0).getDistance

  def getStatus = response.getCode
}
