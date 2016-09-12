package com.footballradar.prometheus

import java.util

import com.twitter.common.metrics.{Metrics, Snapshot}
import io.prometheus.client.Collector
import io.prometheus.client.Collector.MetricFamilySamples.Sample
import io.prometheus.client.Collector.{MetricFamilySamples, Type}

import scala.collection.convert.WrapAsJava.seqAsJavaList
import scala.collection.convert.WrapAsScala.mapAsScalaMap

/**
  * Based on the DropwizardsExport from the prometheus github.
  *
  * @param registry The Metrics registry
  */
private[prometheus] class FinagleMetricsCollector(registry: Metrics) extends Collector {

  override def collect(): util.List[MetricFamilySamples] = {
    val metrics = new util.LinkedList[MetricFamilySamples]()
    registry.sampleGauges().foreach { case (name: String, value: Number) => metrics.addAll(fromGauge(sanitizeMetricName(name), value)) }
    registry.sampleCounters().foreach { case (name: String, value: Number) => metrics.addAll(fromCounter(sanitizeMetricName(name), value)) }
    registry.sampleHistograms().foreach { case (name: String, value: Snapshot) => metrics.addAll(fromHistogram(sanitizeMetricName(name), value)) }
    metrics
  }

  def fromGauge(n: String, v: Number): util.List[MetricFamilySamples] = {
    util.Arrays.asList(
      new MetricFamilySamples(
        n, Type.GAUGE, getHelpMessage(n, "gauge"), util.Arrays.asList(new Sample(n, new util.ArrayList[String](), new util.ArrayList[String](), v.doubleValue())))
    )
  }

  def fromCounter(n: String, v: Number): util.List[MetricFamilySamples] = {
    util.Arrays.asList(
      new MetricFamilySamples(
        n, Type.COUNTER, getHelpMessage(n, "counter"), util.Arrays.asList(new Sample(n, new util.ArrayList[String](), new util.ArrayList[String](), v.doubleValue())))
    )
  }

  def fromHistogram(n: String, s: Snapshot): util.List[MetricFamilySamples] = {
    fromSnapshotAndCount(n, s, s.count(), 1.0, getHelpMessage(n, "histogram"))
  }

  def fromSnapshotAndCount(n: String, s: Snapshot, count: Long, factor: Double, helpMessage: String): util.List[MetricFamilySamples] = {
    val samples: Seq[Sample] =
      s.percentiles().map(f => new Sample(n, util.Arrays.asList("quantile"), util.Arrays.asList(f.getQuantile.toString), f.getValue * factor)) :+
        new Sample(s"${n}_count", util.Arrays.asList(), util.Arrays.asList(), s.count().toDouble) :+
        new Sample(s"${n}_sum", util.Arrays.asList(), util.Arrays.asList(), s.sum().toDouble)

    util.Arrays.asList(
      new MetricFamilySamples(n, Type.SUMMARY, helpMessage, samples)
    )
  }

  def getHelpMessage(name: String, metricType: String): String = {
    s"Generated from finagle metric import (metric=$name, type=$metricType)"
  }

  def sanitizeMetricName(n: String) = n.replaceAll("[^a-zA-Z0-9:_]", "_")
}

object FinagleMetricsCollector {
  def apply() = new FinagleMetricsCollector(Metrics.root)
  def apply(registry: Metrics) = new FinagleMetricsCollector(registry)
}
