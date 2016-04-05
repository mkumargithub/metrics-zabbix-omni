package io.github.hengyunabc.metrics;

import com.alibaba.fastjson.JSON;
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
import java.util.*;
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
	 * All JVM APIs List for zabbix lld
	 */

	private DataObject toDataObjectsJvm(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			/*if (key.matches("jvm.thread.*") ) {
				stringBuilder.append("\n {\"{#JVM_THREAD}\":\"").append(key).append("\"},");
			}
			if (key.matches("jvm.memory.heap.*")) {
				stringBuilder.append("\n {\"{#JVM_HEAP}\":\"").append(key).append("\"},");
			}
			if (key.matches("jvm.memory.non-heap.*")) {
				stringBuilder.append("\n {\"{#JVM_NONHEAP}\":\"").append(key).append("\"},");
			}
			if (key.matches("jvm.memory.total.*")) {
				stringBuilder.append("\n {\"{#JVM_MEM_TOTAL}\":\"").append(key).append("\"},");
			}*/
			if (key.matches("jvm.gc.*")) {
				stringBuilder.append("\n {\"{#JVM_GC}\":\"").append(key).append("\"},");
			}
			if (key.matches("jvm.memory.pools.*")) {
				stringBuilder.append("\n {\"{#JVM_MEM_POOL}\":\"").append(key).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	/**
	 * All APIs List for zabbix lld
	 */
	private DataObject countersToDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String countersKey : keys) {
			if (countersKey.contains(".activeRequests")) {
				stringBuilder.append("\n {\"{#COUNTERS}\":\"").append(countersKey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.counters").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject timersToDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String timersKey : keys) {
			if (timersKey.contains(".requests")) {
				stringBuilder.append("\n {\"{#TIMERS}\":\"").append(timersKey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.timers").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject metersToDataObjects(List<String> meterskeys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String mkey : meterskeys) {
			if (mkey.contains(".responseCodes.")) {
				int index = mkey.indexOf(".responseCodes.") + ".responseCodes".length();
				String subKey = mkey.substring(0, index);
				stringBuilder.append("\n {\"{#METERS}\":\"").append(subKey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.meters").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	/**
	 * for histograms.
	 */
	private void addSnapshotDataObject(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "histograms";
		dataObjectList.add(toDataObject(type, ".p50", key, Double.valueOf(convertDuration(snapshot.getMedian()))));
		dataObjectList.add(toDataObject(type, ".p95", key, Double.valueOf(snapshot.get95thPercentile())));
		dataObjectList.add(toDataObject(type, ".p999", key, Double.valueOf(convertDuration(snapshot.get999thPercentile()))));
	}

	/**
	 * for timers.
	 */
	private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".mean", key, Double.valueOf(convertDuration(snapshot.getMean()))/1000));
		dataObjectList.add(toDataObject(type, ".p50", key, Double.valueOf(convertDuration(snapshot.getMedian()))/1000));
		dataObjectList.add(toDataObject(type, ".p95", key, Double.valueOf(convertDuration(snapshot.get95thPercentile()))/1000));
		dataObjectList.add(toDataObject(type, ".p999", key, Double.valueOf(convertDuration(snapshot.get999thPercentile()))/1000));
	}
	private void addTimerDataObject(String key, Timer timer, List<DataObject> dataObjectList) {
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".count", key, Long.valueOf(timer.getCount())));
	}

	/**
	 * for meters.
	 */
	private void addMeterDataObject(String key, Metered meter, List<DataObject> dataObjectList) {
		String type = "meters";
		String responseType = key.substring(key.indexOf(".responseCodes.") + ".responseCodes.".length());
		int index = key.indexOf(".responseCodes.") + ".responseCodes".length();
		String subKey = key.substring(0, index);
		dataObjectList.add(toDataObject(type, ".count." +  responseType, subKey, Long.valueOf(meter.getCount())));
	}

	public static HashSet removeDuplicatesFromList(List list) {
		HashSet<String> set = new LinkedHashSet<String>();
		set.addAll(list);
		return set;
	}

	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		List<DataObject> dataObjectList = new LinkedList();
		List<String> keys = new LinkedList();
		List<String> cKeys = new LinkedList();
		LinkedList<String> mKeys = new LinkedList();
		List<String> tKeys = new LinkedList();
		HashSet<String> meterKey = removeDuplicatesFromList(mKeys);


		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			String type ="gauge";
			String key = entry.getKey();
			String responseType =  key.substring(key.lastIndexOf(".") + "".length());
			int index = key.lastIndexOf(".") + "".length();
			String subKey = key.substring(0, index);
			DataObject dataObject = DataObject.builder().host(this.hostName).key(type + responseType + "[" + (String) subKey + "]").value(((Gauge) entry.getValue()).getValue().toString()).build();
			DataObject apidataObject = DataObject.builder().host(this.hostName).key((String) subKey).value(((Gauge) entry.getValue()).getValue().toString()).build();
			dataObjectList.add(dataObject);
			keys.add(apidataObject.getKey());
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
			DataObject apidataObject = DataObject.builder().host(this.hostName).key((String) entry.getKey() ).value("" + ((Counter) entry.getValue()).getCount()).build();
			dataObjectList.add(dataObject);
			cKeys.add(apidataObject.getKey());
		}
		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = (Histogram) entry.getValue();
			Snapshot snapshot = histogram.getSnapshot();
			addSnapshotDataObject((String) entry.getKey(), snapshot, dataObjectList);
			keys.add(entry.getKey());
		}
		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			Meter meter = (Meter) entry.getValue();
			// for metrics value
			addMeterDataObject((String) entry.getKey(), meter, dataObjectList);
			//for LLD discovery
			mKeys.add(entry.getKey());


		}
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = (Timer) entry.getValue();
			addTimerDataObject((String) entry.getKey(), timer, dataObjectList);
			addSnapshotDataObjectWithConvertDuration((String) entry.getKey(), timer.getSnapshot(), dataObjectList);
			tKeys.add(entry.getKey());
		}


		try {
			SenderResult senderResult = this.zabbixSender.send(dataObjectList);
			//JVM
			SenderResult senderGaugesAPIsList = this.zabbixSender.send(toDataObjectsJvm(keys));
			//counters
			SenderResult senderCountersAPIsList = this.zabbixSender.send(countersToDataObjects(cKeys));
			//meters
			SenderResult senderMetersAPIsList = this.zabbixSender.send(metersToDataObjects((List<String>) meterKey));
			//timers
			SenderResult senderTimersAPIsList = this.zabbixSender.send(timersToDataObjects(tKeys));

			if (!!senderResult.success() && !!senderGaugesAPIsList.success() && !!senderMetersAPIsList.success() && !!senderTimersAPIsList.success() && !!senderCountersAPIsList.success()) {
				logger.warn("report APIs List & metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report metrics to zabbix success. " + senderResult);
			}
		} catch (IOException e) {
			logger.error("report APIs List & metrics to zabbix error!");
		}
	}
}
