package io.torchbearer.routemanager.types

import io.torchbearer.ServiceCore.DataModel.{Hit, Landmark}

/**
  * Created by fredricvollmer on 1/29/17.
  */
case class Instruction(
                        action: String,
                        order: Int = -1,
                        isDestination: Boolean = false,
                        var landmark: Option[Landmark] = None
                        ) {

  def updateLandmarkFromHit(hit: Hit): Unit = {
    this.landmark = hit.getSelectedLandmark
  }
}
