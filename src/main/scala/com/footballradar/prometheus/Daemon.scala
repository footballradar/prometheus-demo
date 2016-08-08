package com.footballradar.prometheus

import com.google.common.util.concurrent.ServiceManager
import com.twitter.finagle.stats.LoadedStatsReceiver
import com.twitter.finagle.util.LoadService
import com.twitter.finagle.{Service, Http}
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.server.TwitterServer
import com.twitter.util.{Await, Future}

object Daemon extends TwitterServer with PrometheusMetricsExporter {

  val receiver = LoadedStatsReceiver.scope("prometheus_demo")
  val requests = receiver.counter("http_requests")

  val httpService = new Service[Request, Response] {
    def apply(request: Request): Future[Response] = {
      requests.incr()
      val response = Response(request.version, Status.Ok)
      response.contentString = "Football Radar!"
      Future.value(response)
    }
  }

  premain {
    val servicesToStart = LoadService[com.google.common.util.concurrent.Service]()

    import scala.collection.JavaConverters._

    val serviceManager: ServiceManager = new ServiceManager(servicesToStart.asJava)

    serviceManager.startAsync()

    onExit {
      log.info("Shutting down loaded services")
      serviceManager.stopAsync()
    }
  }


    // TODO: check for intransative dependencies

  def main(): Unit = {
    val server = Http.serve(":8888", httpService)
    closeOnExit(server)
    Await.ready(adminHttpServer)
  }
}
