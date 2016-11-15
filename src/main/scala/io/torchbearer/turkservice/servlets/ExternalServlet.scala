package io.torchbearer.turkservice

import io.torchbearer.ServiceCore.DataModel._
import org.scalatra._
import io.torchbearer.ServiceCore.TorchbearerDB._

class HitService extends TurkServiceStack {

  get("/external/hit/objectsampling") {
    val assignmentId = params('assignmentId)
    val epId = params('epId)

    getExecutionPoint(epId) map { point =>
      contentType = "text/html"
      mustache("/objectSampling",
        "instruction" -> "first right turn",
        "assignmentId" -> assignmentId,
        "lat" -> point.lat,
        "long" -> point.long,
        "bearing" -> point.bearing)
    } getOrElse halt(409, "Invalid execution point")
  }

  override def error(handler: ErrorHandler): Unit = ???
}
