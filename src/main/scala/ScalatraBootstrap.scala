import io.torchbearer.routemanager._
import org.scalatra._
import javax.servlet.ServletContext

import _root_.akka.actor.{ActorSystem, Props}
import io.torchbearer.ServiceCore.TorchbearerDB
import io.torchbearer.routemanager.resources.{ExecutionPointResource, RouteResource}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

class ScalatraBootstrap extends LifeCycle {

  val system = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  override def init(context: ServletContext) {
    // Start REST servers
    context.mount(new ExecutionPointResource(system), "/executionpoint/*")
    context.mount(new RouteResource(system), "/route/*")

    // Initialize core services
    TorchbearerDB.init()
  }

  override def destroy(context:ServletContext) {
    system.shutdown
  }
}
