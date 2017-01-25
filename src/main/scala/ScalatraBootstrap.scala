import io.torchbearer.routemanager._
import org.scalatra._
import javax.servlet.ServletContext

import _root_.akka.actor.{ActorSystem, Props}
import io.torchbearer.ServiceCore.TorchbearerDB
import io.torchbearer.routemanager.resources.ExecutionPointResource

import scala.concurrent.duration._

class ScalatraBootstrap extends LifeCycle {

  val system = ActorSystem()
  implicit val executor = system.dispatcher

  override def init(context: ServletContext) {
    // Start REST servers
    context.mount(new ExecutionPointResource(system), "/executionpoints/*")

    // Initialize core services
    TorchbearerDB.init()
  }

  override def destroy(context:ServletContext) {
    system.shutdown
  }
}
