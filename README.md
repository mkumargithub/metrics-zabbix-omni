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

addMeterDataObject
```java
private void addMeterDataObject(String key, Metered meter, List<DataObject> dataObjectList) {
		String type = "meters";
		dataObjectList.add(toDataObject(type, ".count", key, Long.valueOf(meter.getCount())));
		}
```

report: keys added 
SenderResult senderAPIsResult = this.zabbixSender.send(toDataObjects(keys));
```java
public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		List<DataObject> dataObjectList = new LinkedList();
		List<String> keys = new LinkedList();
		List<String> cKeys = new LinkedList();
		List<String> mKeys = new LinkedList();
		List<String> tKeys = new LinkedList();

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
			//timers
			SenderResult senderTimersAPIsList = this.zabbixSender.send(timersToDataObjects(tKeys));
			//meters
			SenderResult senderMetersAPIsList = this.zabbixSender.send(metersToDataObjects(mKeys));
			//counters
			SenderResult senderCountersAPIsList = this.zabbixSender.send(countersToDataObjects(cKeys));

			if (!!senderResult.success() && !!senderGaugesAPIsList.success() && !!senderTimersAPIsList.success() && !!senderMetersAPIsList.success() && !!senderCountersAPIsList.success()) {
				logger.warn("report APIs List & metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report metrics to zabbix success. " + senderResult);
			}
		} catch (IOException e) {
			logger.error("report APIs List & metrics to zabbix error!");
		}
```

methods created to list out all APIs and JVM list
```java
/**
	 * All JVM APIs List for zabbix lld
	 */
private DataObject toDataObjectsJvm(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<String> gcKeys = new LinkedHashSet<>();
		HashSet<String> memKeys = new LinkedHashSet<>();
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
				gcKeys.add(key);
			}
			if (key.matches("jvm.memory.pools.*")) {
				memKeys.add(key);
			}
		}
		for(String gcSubKey : gcKeys) {
			stringBuilder.append("\n {\"{#JVM_GC}\":\"").append(gcSubKey).append("\"},");
		}
		for(String memSubKey : memKeys) {
			stringBuilder.append("\n {\"{#JVM_MEM_POOL}\":\"").append(memSubKey).append("\"},");
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}
```
# Counters
```java
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
```

# Timers
```java
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
```

# Meters
```java
	private DataObject metersToDataObjects(List<String> meterskeys) {
		StringBuilder stringBuilder = new StringBuilder();
		HashSet<String> subKeys = new LinkedHashSet<>();
		for (String mkey : meterskeys) {
			if (mkey.contains(".responseCodes.")) {
				int index = mkey.indexOf(".responseCodes.") + ".responseCodes".length();
				String subKey = mkey.substring(0, index);
				subKeys.add(subKey);
			}
		}
		for(String subKey : subKeys) {
			stringBuilder.append("\n {\"{#METERS}\":\"").append(subKey).append("\"},");
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.meters").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}
```

# Real example: 
For APIs List
```JSON
trappergot'{
	"clock": 1460361570791,
	"data": [{
		"clock": 1460361570791,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "dropwizard.lld.key.timers",
		"value": "{\"data\":[\n {\"{#TIMERS}\":\"mss.gateway.api.all.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetArtists.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetRecordings.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetReleases.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetSets.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetTrackAudioAssets.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.batchGetTracks.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.deleteArtist.requests\"},\n {\"{#TIMERS}\":\"mss.gateway.api.updateUserDevice.requests\"}]}"
	}],
	"request": "sender data"
}'

trappergot'{
	"clock": 1460361511709,
	"data": [{
		"clock": 1460361511709,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "dropwizard.lld.key.meters",
		"value": "{\"data\":[\n {\"{#METERS}\":\"mss.gateway.api.all.responseCodes\"},\n {\"{#METERS}\":\"mss.gateway.api.batchGetArtists.responseCodes\"},\n {\"{#METERS}\":\"mss.gateway.api.updateUser.responseCodes\"},\n {\"{#METERS}\":\"mss.gateway.api.updateUserDevice.responseCodes\"}]}"
	}],
	"request": "sender data"
}'
```

For metrics values
```JSON
 27875: 20160411: 075930.698trappergot'{
	"clock": 1460361570690,
	"data": [{
		"clock": 1460361570685,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "gauge.usage[jvm.fd]",
		"value": "0.06201171875"
	},
	{
		"clock": 1460361570685,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "gauge.count[jvm.gc.ConcurrentMarkSweep]",
		"value": "2"
	},
	{
		"clock": 1460361570685,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "gauge.time[jvm.gc.ConcurrentMarkSweep]",
		"value": "1248"
	},
	{
		"clock": 1460361570689,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "meters.count.other[mss.gateway.api.batchGetTrackAudioAssets.responseCodes]",
		"value": "0"
	},
	{
		"clock": 1460361570689,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "meters.count.serviceUnavailable[mss.gateway.api.batchGetTrackAudioAssets.responseCodes]",
		"value": "0"
	},
	{
		"clock": 1460361570690,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "timers.p95[mss.gateway.api.getRelease.requests]",
		"value": "0.6108730819999999"
	},
	{
		"clock": 1460361570690,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "timers.p999[mss.gateway.api.getRelease.requests]",
		"value": "0.6108730819999999"
	},
	{
		"clock": 1460361570690,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "timers.count[mss.gateway.api.getReleaseIds.requests]",
		"value": "8"
	},
	{
		"clock": 1460361570690,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "timers.p999[mss.gateway.api.updateUserDevice.requests]",
		"value": "0.819407643"
	}],
	"request": "sender data"
}'
```
