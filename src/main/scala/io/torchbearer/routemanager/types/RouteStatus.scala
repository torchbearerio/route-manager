package io.torchbearer.routemanager.types

/**
  * Created by fredricvollmer on 2/26/17.
  */
object RouteStatus {
  sealed trait RouteStatus

  case object OK extends RouteStatus
  case object ERROR extends RouteStatus
}
