# Metrics-zabbix

Updated zabbixReporter.java file in order to compatible with zabbix key format

now we are able to get the below key format 
timers.count[{#APINAME}] --> timers.stddev[mss.gateway.api.all.requests]

# Functionality update:

toDataObject
```java
private DataObject toDataObject(String type, String suffix, String key, Object value) {
		return DataObject.builder().host(this.hostName).key(type + suffix + "[" + key + "]").value("" + value).build();
	}
```

addSnapshotDataObject
```java
private void addSnapshotDataObject(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "histograms";
		dataObjectList.add(toDataObject(type, ".min", key, Long.valueOf(snapshot.getMin())));
		}
```
addSnapshotDataObjectWithConvertDuration
```java
private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".min", key, Double.valueOf(convertDuration(snapshot.getMin())))); 
		}
```

report: keys added 
SenderResult senderAPIsResult = this.zabbixSender.send(toDataObjects(keys));
```java
public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		List<DataObject> dataObjectList = new LinkedList();
		List<String> keys = new LinkedList();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			DataObject dataObject = DataObject.builder().host(this.hostName).key(this.prefix + (String) entry.getKey()).value(((Gauge) entry.getValue()).getValue().toString()).build();
			dataObjectList.add(dataObject);
			keys.add(dataObject.getKey());
		}

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
			keys.add(entry.getKey());
		}
		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = (Timer) entry.getValue();
			addMeterDataObject((String) entry.getKey(), timer, dataObjectList);
			addSnapshotDataObjectWithConvertDuration((String) entry.getKey(), timer.getSnapshot(), dataObjectList);
			keys.add(entry.getKey());
		}

		try {
			SenderResult senderAPIsResult = this.zabbixSender.send(toDataObjects(keys));
			SenderResult senderResult = this.zabbixSender.send(dataObjectList);
			if (!senderAPIsResult.success() && !!senderResult.success()) {
				logger.warn("report APIs List & metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report APIs List & metrics to zabbix success. " + senderResult);
			}
		} catch (IOException e) {
			logger.error("report APIs List & metrics to zabbix error!");
		}
```

toDataObjects created to list out all APIs
```java
private DataObject toDataObjects(List<String> keys) {
		StringBuilder builder = new StringBuilder();
		for (String key : keys) {
			builder.append("\"{#APINAME}\":\"").append(key).append("\",");
			logger.debug("AllAPIsKeys: " +key);
		}
		builder.deleteCharAt(builder.length() - 1);
		return DataObject.builder().key("dropwizard.lld.key").value(builder.toString()).build();
	}
```

# Real example: 
For APIs List
```
3415 : 20160216 : 141919.987 trapper got '{"clock":1455632359983,"data":[{"clock":1455632359983,"key":"dropwizard.lld.key","value":"\"{#APINAME}\":\"jvm.fd.usage\",\"{#APINAME}\":\"jvm.gc.ConcurrentMarkSweep.count\",\"{#APINAME}\":\"jvm.gc.ConcurrentMarkSweep.time\",\"{#APINAME}\":\"jvm.gc.ParNew.count\",\"{#APINAME}\":\"jvm.gc.ParNew.time\",\"{#APINAME}\":\"jvm.memory.heap.committed\",\"{#APINAME}\":\"jvm.memory.heap.init\",\"{#APINAME}\":\"jvm.memory.heap.max\",\"{#APINAME}\":\"jvm.memory.heap.usage\",\"{#APINAME}\":\"jvm.memory.heap.used\",\"{#APINAME}\":\"jvm.memory.non-heap.committed\",\"{#APINAME}\":\"jvm.memory.non-heap.init\",\"{#APINAME}\":\"jvm.memory.non-heap.max\",\"{#APINAME}\":\"jvm.memory.non-heap.usage\",\"{#APINAME}\":\"jvm.memory.non-heap.used\",\"{#APINAME}\":\"jvm.memory.pools.CMS-Old-Gen.usage\",\"{#APINAME}\":\"jvm.memory.pools.CMS-Perm-Gen.usage\",
```

For metrics values
```
3413 : 20160216 : 141920.045 trapper got '{"clock":1455632359987,"data":[{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.fd.usage","value":"0.125732421875"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.gc.ConcurrentMarkSweep.count","value":"1"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.gc.ConcurrentMarkSweep.time","value":"549"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.gc.ParNew.count","value":"2"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.gc.ParNew.time","value":"326"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.memory.heap.committed","value":"1986461696"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.memory.heap.init","value":"2147483648"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.memory.heap.max","value":"1986461696"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.memory.heap.usage","value":"0.5180930949095934"},{"clock":1455632359874,"host":"te2.oss-hub.uk3.ribob01.net","key":"jvm.memory.heap.used","value":"1029172088"},
```