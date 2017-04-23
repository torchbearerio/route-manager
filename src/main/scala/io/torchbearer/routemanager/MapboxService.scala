package io.torchbearer.routemanager

import com.mapbox.services.commons.models.Position
import com.mapbox.services.directions.v5.models.DirectionsResponse
import com.mapbox.services.directions.v5.{DirectionsCriteria, MapboxDirections}
import io.torchbearer.ServiceCore.tyoes.Instruction
import io.torchbearer.routemanager.types.{Route, TBDirectionsResponse}


/**
  * Created by fredricvollmer on 1/30/17.
  */
object MapboxService {
  def retrieveRoute(route: Route): Option[TBDirectionsResponse] = {

    // NOTE: MapBox is stupid. It expects coordinates as (long, lat)
    val directionResults = new MapboxDirections.Builder()
      .setAccessToken(Constants.MAPBOX_KEY)
      .setOrigin(Position.fromCoordinates(route.originLong, route.originLat))
      .setDestination(Position.fromCoordinates(route.destLong, route.destLat))
      .setProfile(DirectionsCriteria.PROFILE_DRIVING)
      //.setRadiuses(Array(50, 50))
      .setSteps(true)
      .setOverview("false")
      .build()
      .executeCall()

    if (!directionResults.isSuccessful) {
      return None
    }

    Some(new TBDirectionsResponse(directionResults.body))
  }
}
