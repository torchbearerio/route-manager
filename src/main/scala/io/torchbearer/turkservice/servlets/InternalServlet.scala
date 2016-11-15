package io.torchbearer.turkservice.servlets

import io.torchbearer.ServiceCore.DataModel.ExecutionPoint
import io.torchbearer.ServiceCore.TorchbearerDB._
import io.torchbearer.turkservice.{HitService, TurkServiceStack}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Accepted, AsyncResult, ErrorHandler, FutureSupport}
import org.scalatra.json.JacksonJsonSupport
import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, Future}

class InternalServlet(system: ActorSystem) extends TurkServiceStack with JacksonJsonSupport with FutureSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  override protected implicit def executor: ExecutionContext = system.dispatcher

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  post("/process/executionpoint") {
    val pointIds = (parsedBody \ "executionPoints").extract[List[Int]]

    // If the list is empty, return an error
    if (pointIds.isEmpty) {
      halt(409, "No execution point Ids provided")
    }

    Future {
        HitService.processExecutionPoints(pointIds)
    }

    Accepted()
  }

  override def error(handler: ErrorHandler): Unit = ???
}
