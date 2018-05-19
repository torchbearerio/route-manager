package io.torchbearer.routemanager

import com.mapbox.api.directions.v5.{DirectionsCriteria, MapboxDirections}
import com.mapbox.geojson.Point
import io.torchbearer.ServiceCore.AWSServices.KeyStore
import io.torchbearer.routemanager.types.{Route, TBDirectionsResponse}


/**
  * Created by fredricvollmer on 1/30/17.
  */
object MapboxService {
  val mapboxKey = KeyStore.getKey("mapbox-key")

  def retrieveRoute(route: Route): Option[TBDirectionsResponse] = {

    // NOTE: MapBox is stupid. It expects coordinates as (long, lat)
    val directionsRequest = MapboxDirections.builder()
      .accessToken(mapboxKey)
      .origin(Point.fromLngLat(route.originLong, route.originLat))
      .destination(Point.fromLngLat(route.destLong, route.destLat))
      .radiuses(100, 100)
      .profile(DirectionsCriteria.PROFILE_DRIVING)
      .steps(true)
      .overview("false")

    if (route.initialBearing.isDefined) {
      // Specify heading restriction on origin waypoint
      directionsRequest.addBearing(route.initialBearing.get, 45.0)
      // We don't care about approach bearing of destination waypoint
      directionsRequest.addBearing(null, null)
    }

    val directionsResponse = directionsRequest
      .build()
      .executeCall()

    if (!directionsResponse.isSuccessful) {
      print(s"Error fetching route form MapBox: ${directionsResponse.message}")
      return None
    }

    Some(new TBDirectionsResponse(directionsResponse.body))
  }
}
