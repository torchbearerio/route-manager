import io.torchbearer.turkservice._
import org.scalatra._
import javax.servlet.ServletContext

import _root_.akka.actor.{ActorSystem, Props}
import io.torchbearer.ServiceCore.TorchbearerDB
import io.torchbearer.turkservice.servlets.{ExternalServlet, InternalServlet}

import scala.concurrent.duration._

class ScalatraBootstrap extends LifeCycle {

  val system = ActorSystem()
  implicit val executor = system.dispatcher

  override def init(context: ServletContext) {
    // Start REST servers
    context.mount(new ExternalServlet, "/external/*")
    context.mount(new InternalServlet(system), "/internal/*")

    // Start continuous tasks
    system.scheduler.schedule(30.seconds, 30.seconds, PollingTask)

    // Initialize core services
    TorchbearerDB.init()
  }

  override def destroy(context:ServletContext) {
    system.shutdown
  }
}
