package io.torchbearer.routemanager.resources

import io.torchbearer.routemanager.{Constants, RouteManagerStack}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import _root_.akka.actor.ActorSystem
import io.torchbearer.routemanager.types.{Route, RouteStatus}

import scala.concurrent.{ExecutionContext, Future}

class RouteResource(system: ActorSystem) extends RouteManagerStack with JacksonJsonSupport
  with FutureSupport with CorsSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  override protected implicit def executor: ExecutionContext = system.dispatcher

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  /**
    * Respond to preflight requests
    */
  options("/*") {
    response.setHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"))
  }

  /**
    * Create a new route
    */
  post("/") {
    val originLat = (parsedBody \ "origin_lat").extract[Double]
    val originLong = (parsedBody \ "origin_long").extract[Double]
    val destLat = (parsedBody \ "destination_lat").extract[Double]
    val destLong = (parsedBody \ "destination_long").extract[Double]
    val pipeline = (parsedBody \ "pipeline").extractOpt[String]
      .getOrElse(Constants.DEFAULT_PIPELINE)

    println(s"Route request for ($originLat, $originLong) received.")

    new AsyncResult() {
      override val is = Future {
        val route = Route(originLat, originLong, destLat, destLong, pipeline)

        if (route.status != RouteStatus.OK) {
          halt(500)
        }

        println(s"Route request for ($originLat, $originLong) processed.")

        route.navigation

      }
    }
  }

  /*
  -----------------------------------------------------
  This websocket endpoint is not currently used, as no route state is being
  saved at this point. Although it will need refactoring, it would serve
  as a good starting point if route state and duplex communication is ever needed.
  -----------------------------------------------------

  atmosphere("/:routeId") {
    val routeId = params("routeId")

    new AtmosphereClient {
      def receive = {
        case Connected => {
          Future {
            Route.updateClientUuidForRoute(routeId, uuid)

            val instructions = Route.getInstructionsForRoute(routeId)

            // We MUST wait for connection to complete before sending initial state
            Thread.sleep(5000)
            send(instructions)
          }
        }

        case Disconnected(disconnector, Some(error)) =>
        case Error(Some(error)) =>
        case TextMessage(text) => send("ECHO: " + text)
        case JsonMessage(json) => broadcast(json)
      }
    }
  }
  */

  override def error(handler: ErrorHandler): Unit = ???
}
