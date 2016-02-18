package io.github.hengyunabc.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.SenderResult;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Updated mkumar
 */
public class ZabbixReporter extends ScheduledReporter
{
	private static final Logger logger = LoggerFactory.getLogger(ZabbixReporter.class);
	String replacePercentSign = "";
	private ZabbixSender zabbixSender;
	private String hostName;
	private String prefix;

	public static Builder forRegistry(MetricRegistry registry) {
		return new Builder(registry);
	}

	public static class Builder {
		private String replacePercentSign = "";
		private final MetricRegistry registry;
		private String name = "zabbix-reporter";
		private TimeUnit rateUnit;
		private TimeUnit durationUnit;
		private MetricFilter filter;
		private String hostName;
		private String prefix = "";

		public Builder(MetricRegistry registry) {
			this.registry = registry;

			this.rateUnit = TimeUnit.SECONDS;
			this.durationUnit = TimeUnit.MILLISECONDS;
			this.filter = MetricFilter.ALL;
		}

		public Builder convertRatesTo(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return this;
		}

		public Builder convertDurationsTo(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return this;
		}

		public Builder filter(MetricFilter filter) {
			this.filter = filter;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder hostName(String hostName) {
			this.hostName = hostName;
			return this;
		}

		public Builder prefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public Builder replacePercentSign(String replacePercentSign) {
			this.replacePercentSign = replacePercentSign;
			return this;
		}

		public ZabbixReporter build(ZabbixSender zabbixSender) {
			if (this.hostName == null) {
				this.hostName = HostUtil.getHostName();
				ZabbixReporter.logger.info(this.name + " detect hostName: " + this.hostName);
			}
			return new ZabbixReporter(this.registry, this.replacePercentSign, this.name, this.rateUnit, this.durationUnit, this.filter, zabbixSender, this.hostName, this.prefix);
		}
	}

	private ZabbixReporter(MetricRegistry registry, String replacePercentSign, String name, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter, ZabbixSender zabbixSender, String hostName, String prefix) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.replacePercentSign = replacePercentSign;
		this.zabbixSender = zabbixSender;
		this.hostName = hostName;
		this.prefix = prefix;
	}

	private DataObject toDataObject(String type, String suffix, String key, Object value) {
		return DataObject.builder().host(this.hostName).key(type + suffix + "[" + key + "]").value("" + value).build();
	}

	/**
	 *
	 * APIs List for zabbix lld
	 *
	 */
	private DataObject toDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			stringBuilder.append("\n {\"{#APINAME}\":\"").append(key).append("\"},");
			//logger.debug("AllAPIsKeys: " + key);
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key").value(stringBuilder.toString()).build();
	}

	/**
	 *
	 * APIs List for zabbix lld
	 *
	 */
	private DataObject metersDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			stringBuilder.append("\n {\"{#APINAME}\":\"").append(key).append("\"},");
			//logger.debug("AllAPIsKeys: " + key);
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key[meters]").value("{\n\"data\":[" + stringBuilder.toString()).build();
	}

	/**
	 * for histograms.
	 */

	private void addSnapshotDataObject(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "histograms";
		dataObjectList.add(toDataObject(type, ".min", key, Long.valueOf(snapshot.getMin())));
		dataObjectList.add(toDataObject(type, ".max", key, Long.valueOf(snapshot.getMax())));
		dataObjectList.add(toDataObject(type, ".mean", key, Double.valueOf(convertDuration(snapshot.getMean()))));
		dataObjectList.add(toDataObject(type, ".stddev", key, Double.valueOf(convertDuration(snapshot.getStdDev()))));
		//median is a p50
		dataObjectList.add(toDataObject(type, ".median", key, Double.valueOf(convertDuration(snapshot.getMedian()))));
		dataObjectList.add(toDataObject(type, ".p75", key, Double.valueOf(convertDuration(snapshot.get75thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p95", key, Double.valueOf(snapshot.get95thPercentile())));
		dataObjectList.add(toDataObject(type, ".p98", key, Double.valueOf(convertDuration(snapshot.get98thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p99", key, Double.valueOf(snapshot.get99thPercentile())));
		dataObjectList.add(toDataObject(type, ".p999", key, Double.valueOf(convertDuration(snapshot.get999thPercentile()))));
	}


	/**
	 * for timers.
	 */

	private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		// output: timers.min[mss.gateway.api.all.requests]
		// timers.p75[mss.gateway.api.updateUserDevice.requests]
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".min", key, Double.valueOf(convertDuration(snapshot.getMin()))));
		dataObjectList.add(toDataObject(type, ".max", key, Double.valueOf(convertDuration(snapshot.getMax()))));
		dataObjectList.add(toDataObject(type, ".mean", key, Double.valueOf(convertDuration(snapshot.getMean()))));
		dataObjectList.add(toDataObject(type, ".stddev", key, Double.valueOf(convertDuration(snapshot.getStdDev()))));
		dataObjectList.add(toDataObject(type, ".median", key, Double.valueOf(convertDuration(snapshot.getMedian()))));
		dataObjectList.add(toDataObject(type, ".p75", key, Double.valueOf(convertDuration(snapshot.get75thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p95", key, Double.valueOf(convertDuration(snapshot.get95thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p98", key, Double.valueOf(convertDuration(snapshot.get98thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p99", key, Double.valueOf(convertDuration(snapshot.get99thPercentile()))));
		dataObjectList.add(toDataObject(type, ".p999", key, Double.valueOf(convertDuration(snapshot.get999thPercentile()))));
	}

	/**
	 * for meters.
	 */

	private void addMeterDataObject(String key, Metered meter, List<DataObject> dataObjectList) {
		String type = "meters";
		dataObjectList.add(toDataObject(type, ".count", key, Long.valueOf(meter.getCount())));
		dataObjectList.add(toDataObject(type, ".meanRate", key, Double.valueOf(convertRate(meter.getMeanRate()))));
		dataObjectList.add(toDataObject(type, ".1-minuteRate", key, Double.valueOf(convertRate(meter.getOneMinuteRate()))));
		dataObjectList.add(toDataObject(type, ".5-minuteRate", key, Double.valueOf(convertRate(meter.getFiveMinuteRate()))));
		dataObjectList.add(toDataObject(type, ".15-minuteRate", key, Double.valueOf(convertRate(meter.getFifteenMinuteRate()))));
	}

	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		List<DataObject> dataObjectList = new LinkedList();
		List<String> keys = new LinkedList();
		List<String> metersKeys = new LinkedList();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			DataObject dataObject = DataObject.builder().host(this.hostName).key(this.prefix + (String) entry.getKey()).value(((Gauge) entry.getValue()).getValue().toString()).build();
			dataObjectList.add(dataObject);
			keys.add(dataObject.getKey());
		}

		/*for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			DataObject dataObject = DataObject.builder().host(this.hostName).key(this.prefix + (String) entry.getKey()).value("" + ((Counter) entry.getValue()).getCount()).build();
			dataObjectList.add(dataObject);
		}*/

		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			String type ="counters";
			String suffix = ".count";
			DataObject dataObject = DataObject.builder().host(this.hostName).key(type + suffix + "[" + (String) entry.getKey() + "]").value("" + ((Counter) entry.getValue()).getCount()).build();
			// apidataObject for APIs list without type and suffix
			DataObject apidataObject = DataObject.builder().host(this.hostName).key((String) entry.getKey()).value("" + ((Counter) entry.getValue()).getCount()).build();
			dataObjectList.add(dataObject);
			keys.add(apidataObject.getKey());

		}

		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = (Histogram) entry.getValue();
			Snapshot snapshot = histogram.getSnapshot();
			addSnapshotDataObject((String) entry.getKey(), snapshot, dataObjectList);
			keys.add(entry.getKey());
		}
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			Meter meter = (Meter) entry.getValue();
			addMeterDataObject((String) entry.getKey(), meter, dataObjectList);
			//keys.add(entry.getKey());
			metersKeys.add(entry.getKey());
		}
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = (Timer) entry.getValue();
			addMeterDataObject((String) entry.getKey(), timer, dataObjectList);
			addSnapshotDataObjectWithConvertDuration((String) entry.getKey(), timer.getSnapshot(), dataObjectList);
			keys.add(entry.getKey());
		}

		try {
			SenderResult senderAPIsResult = this.zabbixSender.send(toDataObjects(keys));
			SenderResult metersAPIsResult = this.zabbixSender.send(metersDataObjects(metersKeys));
			SenderResult senderResult = this.zabbixSender.send(dataObjectList);
			if (!senderAPIsResult.success() && !!senderResult.success() && !metersAPIsResult.success()) {
				logger.warn("report APIs List & metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report metrics to zabbix success. " + senderResult);
				logger.info("report APIs List to zabbix success. " + senderAPIsResult);
				logger.info("report meters APIs List to zabbix success. " + metersAPIsResult);
			}
		} catch (IOException e) {
			logger.error("report APIs List & metrics to zabbix error!");
		}
	}
}