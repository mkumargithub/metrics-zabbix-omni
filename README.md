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
	List<String> gKeys = new LinkedList();
	List<String> cKeys = new LinkedList();
	List<String> mKeys = new LinkedList();
	List<String> tKeys = new LinkedList();

	for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
		String type ="gauge";
		DataObject dataObject = DataObject.builder().host(this.hostName).key(type + "[" + (String) entry.getKey() + "]").value(((Gauge) entry.getValue()).getValue().toString()).build();
		DataObject apidataObject = DataObject.builder().host(this.hostName).key((String) entry.getKey()).value(((Gauge) entry.getValue()).getValue().toString()).build();
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
		addMeterDataObject((String) entry.getKey(), meter, dataObjectList);
		mKeys.add(entry.getKey());
	}
	for (Map.Entry<String, Timer> entry : timers.entrySet()) {
		Timer timer = (Timer) entry.getValue();
		addMeterDataObject((String) entry.getKey(), timer, dataObjectList);
		addSnapshotDataObjectWithConvertDuration((String) entry.getKey(), timer.getSnapshot(), dataObjectList);
		tKeys.add(entry.getKey());
	}

	try {
		SenderResult senderResult = this.zabbixSender.send(dataObjectList);
		SenderResult senderGaugesAPIsList = this.zabbixSender.send(toDataObjectsJvm(keys));
		SenderResult senderJvmGcTimeList = this.zabbixSender.send(toDataObjectsJvmGcTime(keys));
		SenderResult senderJvmMemoryPoolstList = this.zabbixSender.send(toDataObjectsJvmMemoryPools(keys));
		SenderResult senderJvmThreadtList = this.zabbixSender.send(toDataObjectsJvmThread(keys));
		SenderResult senderCountersAPIsList = this.zabbixSender.send(countersToDataObjects(cKeys));
		SenderResult senderMetersAPIsList = this.zabbixSender.send(metersToDataObjects(mKeys));
		SenderResult senderTimersAPIsList = this.zabbixSender.send(timersToDataObjects(tKeys));

		if ( !!senderResult.success() && !!senderGaugesAPIsList.success() && !!senderMetersAPIsList.success() && !!senderTimersAPIsList.success() && !!senderCountersAPIsList.success()) {
			logger.warn("report APIs List & metrics to zabbix not success!" + senderResult);
		} else if (logger.isDebugEnabled()) {
			logger.info("report metrics to zabbix success. " + senderResult);
		}
	} catch (IOException e) {
		logger.error("report APIs List & metrics to zabbix error!");
	}
}
```

methods created to list out all APIs and JVM list
```java

	private DataObject toDataObjectsJvm(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			if (key.matches("jvm.*init") || key.matches("jvm.*committed") || key.matches("jvm.*max") || key.matches("jvm.*used") ) {
				stringBuilder.append("\n {\"{#JVMAPINAME}\":\"").append(key).append("\"},");
				//logger.debug("AllAPIsKeys: " + key);
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject toDataObjectsJvmThread(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			if (key.matches("jvm.*count") ) {
				stringBuilder.append("\n {\"{#JCAPINAME}\":\"").append(key).append("\"},");
				//logger.debug("AllAPIsKeys: " + key);
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm.count").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject toDataObjectsJvmMemoryPools(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			if (key.matches("jvm.*usage")) {
				stringBuilder.append("\n {\"{#JUAPINAME}\":\"").append(key).append("\"},");
				//logger.debug("AllAPIsKeys: " + key);
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm.usage").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject toDataObjectsJvmGcTime(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : keys) {
			if (key.matches("jvm.*time")) {
				stringBuilder.append("\n {\"{#JTAPINAME}\":\"").append(key).append("\"},");
				//logger.debug("AllAPIsKeys: " + key);
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.jvm.time").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}


	private DataObject countersToDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String countersKey : keys) {
			if (countersKey.contains(".activeRequests")) {
				stringBuilder.append("\n {\"{#CAPINAME}\":\"").append(countersKey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.counters").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}


	private DataObject timersToDataObjects(List<String> keys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String timersKey : keys) {
			if (timersKey.contains(".requests")) {
				stringBuilder.append("\n {\"{#TAPINAME}\":\"").append(timersKey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.timers").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}

	private DataObject metersToDataObjects(List<String> meterskeys) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String mkey : meterskeys) {
			if (mkey.contains(".responseCodes.")) {
				stringBuilder.append("\n {\"{#MAPINAME}\":\"").append(mkey).append("\"},");
			}
		}
		stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		return DataObject.builder().host(this.hostName).key("dropwizard.lld.key.meters").value("{\"data\":[" + stringBuilder.toString() + "]}").build();
	}
```

# Real example: 
For APIs List
```
3415 : 20160216 : 141919.987 trapper got '{"clock":1456403903190,"data":[{"clock":1456403903190,"host":"te2.oss-hub.uk3.ribob01.net","key":"dropwizard.lld.key","value":"{\"data\":[\n {\"{#APINAME}\":\"jvm.fd.usage\"},\n {\"{#APINAME}\":\"mss.gateway.api.getTransactionDownloadHistory.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUser.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserContextIdTrackPlayIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserContextTypeTrackPlayIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserDevice.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserDeviceIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserDeviceTrackPlayIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserPurchasedItem.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserPurchasedItemBatch.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.getUserTrackPlayIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.purchaseDownloads.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.refundPurchase.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.reportPurchase.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.reportTrackPlayEvent.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.reportTrackPlays.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.sortArtistIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.sortReleaseIds.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateArtist.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateRelease.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateReleaseLicense.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateSubscription.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateTrack.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateTrackLicense.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateTrackPlayContexts.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateUser.requests\"},\n {\"{#APINAME}\":\"mss.gateway.api.updateUserDevice.requests\"}]}"}],"request":"sender data"}'
```

For metrics values
```
3413 : 20160216 : 141920.045 trapper got '{"clock":1456403902846,"data":[{"clock":1456403902836,"host":"te2.oss-hub.uk3.ribob01.net","key":"gauge[jvm.fd.usage]","value":"0.061767578125"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"meters.1-minuteRate[mss.gateway.api.updateUserDevice.requests]","value":"2.964393875E-314"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"meters.5-minuteRate[mss.gateway.api.updateUserDevice.requests]","value":"7.362691676869196E-129"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"meters.15-minuteRate[mss.gateway.api.updateUserDevice.requests]","value":"3.3071448206190163E-44"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.min[mss.gateway.api.updateUserDevice.requests]","value":"44.605582"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.max[mss.gateway.api.updateUserDevice.requests]","value":"721.268999"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.mean[mss.gateway.api.updateUserDevice.requests]","value":"136.063127375"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.stddev[mss.gateway.api.updateUserDevice.requests]","value":"236.53661471257732"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.median[mss.gateway.api.updateUserDevice.requests]","value":"52.844451"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p75[mss.gateway.api.updateUserDevice.requests]","value":"62.343593999999996"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p95[mss.gateway.api.updateUserDevice.requests]","value":"721.268999"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p98[mss.gateway.api.updateUserDevice.requests]","value":"721.268999"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p99[mss.gateway.api.updateUserDevice.requests]","value":"721.268999"},{"clock":1456403902846,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p999[mss.gateway.api.updateUserDevice.requests]","value":"721.268999"}],"request":"sender data"}'
```