# Metrics-zabbix

This project is addon for [ops-asg-metrics-zabbix-servlet](https://github.com/omnifone/ops-asg-metrics-zabbix-servlet) in order to report measures to Zabbix.

## Import with Maven: ##
    ...
    <dependency>
        <groupId>io.github.hengyunabc</groupId>
        <artifactId>metrics-zabbix</artifactId>
        <version>0.0.1-OMNIFONE</version>
    </dependency>
    ...


## Functionality update:
###report()
report() is created to send API list and result to zabbix sender in JASON format  
SenderResult senderAPIsResult = this.zabbixSender.send(toDataObjects(keys));

###JVM : toDataObjectsJvm()
toDataObjectsJvm() created to list out all JVM list.
This function is used for JVM discovery through zabbix frontend
You need to define discovery rule with 'dropwizard.lld.key.jvm' key and Prototypes (Ex: gauge.time[{#JVM_GC}]).

toDataObject
```java
private DataObject toDataObject(String type, String suffix, String key, Object value) {
		return DataObject.builder().host(this.hostName).key(type + suffix + "[" + key + "]").value("" + value).build();
	}
```
######supported metric-keys:
  
    COUNT,
    USAGE,
    TIME

######Example Output:
```JSON
trappergot'{
	"clock": 1460361570763,
	"data": [{
		"clock": 1460361570763,
		"host": "te2.oss-hub.uk3.ribob01.net",
		"key": "dropwizard.lld.key.jvm",
		"value": "{\"data\":[\n {\"{#JVM_GC}\":\"jvm.gc.ConcurrentMarkSweep\"},\n {\"{#JVM_GC}\":\"jvm.gc.ParNew\"},\n {\"{#JVM_MEM_POOL}\":\"jvm.memory.pools.CMS-Old-Gen\"},\n {\"{#JVM_MEM_POOL}\":\"jvm.memory.pools.CMS-Perm-Gen\"},\n {\"{#JVM_MEM_POOL}\":\"jvm.memory.pools.Code-Cache\"},\n {\"{#JVM_MEM_POOL}\":\"jvm.memory.pools.Par-Eden-Space\"},\n {\"{#JVM_MEM_POOL}\":\"jvm.memory.pools.Par-Survivor-Space\"}]}"
	}],
	"request": "sender data"
}'

```

###Timers : timersToDataObjects()
This function is used for Timers discovery through zabbix frontend
You need to define discovery rule with 'dropwizard.lld.key.timers' key and Prototypes (EX: timers.p50[{#TIMERS}]).

addSnapshotDataObjectWithConvertDuration
```java
private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".min", key, Double.valueOf(convertDuration(snapshot.getMin())))); 
		}
```

######supported metric-keys:
  
    COUNT
    MEAN,
    P50TH,
    P95TH,
    P999TH

######Example Output:
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
```

###Meters : metersToDataObjects()
This function is used for Meters discovery through zabbix frontend
You need to define discovery rule with 'dropwizard.lld.key.timers' key and Prototypes (EX: timers.p50[{#TIMERS}]).

addMeterDataObject
```java
private void addMeterDataObject(String key, Metered meter, List<DataObject> dataObjectList) {
		String type = "meters";
		dataObjectList.add(toDataObject(type, ".count", key, Long.valueOf(meter.getCount())));
		}
```
######supported metric-keys:

    COUNT
    	ok,
    	created,
    	noContent,
    	badRequest,
    	notFound,
    	internalServerError,
    	badGateway,
    	serviceUnavailable,
    	gatewayTimeout,
    	other
    	
######Example Output:
```JSON
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

## Example: For metrics values
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
