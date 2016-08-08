package com.footballradar.prometheus

import java.io.StringWriter
import com.twitter.app.App
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.Buf
import com.twitter.server.Admin.Grouping
import com.twitter.server.{Stats, Admin, AdminHttpServer}
import com.twitter.server.AdminHttpServer.Route
import com.twitter.util.Future
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

trait PrometheusMetricsExporter extends Admin { self: App with AdminHttpServer with Stats =>

  FinagleMetricsCollector().register()

  override protected def routes: Seq[Route] = {
    super.routes ++ Seq(Route(path = "/metrics", handler = new PrometheusMetricsHandler, alias = "Prometheus", group = Some(Grouping.Metrics), includeInIndex = true))
  }
}

class PrometheusMetricsHandler(registry: CollectorRegistry = CollectorRegistry.defaultRegistry) extends Service[Request, Response] {
  override def apply(request: Request): Future[Response] = {
    val response = Response(request.version, Status.Ok)
    val w = new StringWriter()
    TextFormat.write004(w, registry.metricFamilySamples())
    val content = Buf.Utf8(w.toString)
    response.content = content
    response.headerMap.add("Content-Language", "en")
    response.headerMap.add("Content-Length", content.length.toString)
    response.headerMap.add("Content-Type", TextFormat.CONTENT_TYPE_004)
    Future.value(response)
  }
}
