/*
 *  Copyright 2014 Damien Claveau
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package com.criteo.kafka;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import kafka.metrics.KafkaMetricsReporterMBean;
import org.apache.log4j.Logger;
import kafka.metrics.KafkaMetricsConfig;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;

public class KafkaGraphiteMetricsReporter implements KafkaMetricsReporter,
	KafkaGraphiteMetricsReporterMBean {

	static Logger LOG = Logger.getLogger(KafkaGraphiteMetricsReporter.class);
	static String GRAPHITE_DEFAULT_HOST = "localhost";
	static int GRAPHITE_DEFAULT_PORT = 2003;
	static String GRAPHITE_DEFAULT_PREFIX = "kafka";
	
	boolean initialized = false;
	boolean running = false;
	GraphiteReporter reporter = null;
    String graphiteHost = GRAPHITE_DEFAULT_HOST;
    int graphitePort = GRAPHITE_DEFAULT_PORT;
    String graphiteGroupPrefix = GRAPHITE_DEFAULT_PREFIX;

	@Override
	public String getMBeanName() {
		return "kafka:type=com.criteo.kafka.KafkaGraphiteMetricsReporter";
	}

	@Override
	public synchronized void startReporter(long pollingPeriodSecs) {
		if (initialized && !running) {
			reporter.start(pollingPeriodSecs, TimeUnit.SECONDS);
			running = true;
			LOG.info(String.format("Started Kafka Graphite metrics reporter with polling period %d seconds", pollingPeriodSecs));
		}
	}

	@Override
	public synchronized void stopReporter() {
		if (initialized && running) {
			reporter.stop();
			running = false;
			LOG.info("Stopped Kafka Graphite metrics reporter");
            try {
            	reporter = buildGraphiteReporter();
            } catch (Exception e) {
            	LOG.error("Unable to initialize GraphiteReporter", e);
            }
		}
	}

	@Override
	public synchronized void init(VerifiableProperties props) {
		if (!initialized) {
			KafkaMetricsConfig metricsConfig = new KafkaMetricsConfig(props);
            graphiteHost = props.getString("kafka.graphite.metrics.host", GRAPHITE_DEFAULT_HOST);
            graphitePort = props.getInt("kafka.graphite.metrics.port", GRAPHITE_DEFAULT_PORT);
            graphiteGroupPrefix = props.getString("kafka.graphite.metrics.group", GRAPHITE_DEFAULT_PREFIX);
            String regex = props.getString("kafka.graphite.metrics.exclude.regex", null);

            LOG.info("Initialize GraphiteReporter [" + graphiteHost + "," + graphitePort + "," + graphiteGroupPrefix + "]");

            try {

            	/*reporter = new GraphiteReporter(
            			Metrics.defaultRegistry(),
            			graphiteHost,
            			graphitePort,
            			graphiteGroupPrefix*//*,
            			predicate*//*
            			);*/
                reporter=buildGraphiteReporter();
            } catch (Exception e) {
            	LOG.error("Unable to initialize GraphiteReporter", e);
            }
            if (props.getBoolean("kafka.graphite.metrics.reporter.enabled", false)) {
            	initialized = true;
            	startReporter(metricsConfig.pollingIntervalSecs());
                LOG.debug("GraphiteReporter started.");
            }
        }
	}

    public GraphiteReporter buildGraphiteReporter(){
        final Graphite graphite = new Graphite(graphiteHost,graphitePort);
        final MetricRegistry registry = new MetricRegistry();
        return GraphiteReporter.forRegistry(registry)
                               .prefixedWith(graphiteGroupPrefix)
                               .convertRatesTo(TimeUnit.SECONDS)
                               .convertDurationsTo(TimeUnit.MILLISECONDS)
                               .build(graphite);
    }

}
