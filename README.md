# metrics-zabbix

Updated zabbixReporter.java file in order to compatible with zabbix key format

now we are able to get the below key format 
timers.count[{#APINAME}] --> timers.stddev[mss.gateway.api.all.requests]

# Functionality update:


{code:title=toDataObject}
private DataObject toDataObject(String type, String suffix, String key, Object value) {
		return DataObject.builder().host(this.hostName).key(type + suffix + "[" + key + "]").value("" + value).build();
	}
{code}

{code:title=addSnapshotDataObject}

private void addSnapshotDataObject(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "histograms";
		dataObjectList.add(toDataObject(type, ".min", key, Long.valueOf(snapshot.getMin())));
		}
{code}
{code:title=addSnapshotDataObjectWithConvertDuration}

private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, List<DataObject> dataObjectList) {
		String type = "timers";
		dataObjectList.add(toDataObject(type, ".min", key, Double.valueOf(convertDuration(snapshot.getMin())))); 
		}
{code}


# Real example:
{"clock":1453998878592,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.stddev[mss.gateway.api.all.requests]","value":"0.0"},
{"clock":1453998878592,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.median[mss.gateway.api.all.requests]","value":"2802.600693"},
{"clock":1453998878592,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p50[mss.gateway.api.all.requests]","value":"2802.600693"},
{"clock":1453998878592,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p95[mss.gateway.api.all.requests]","value":"2802.600693"},
{"clock":1453998878592,"host":"te2.oss-hub.uk3.ribob01.net","key":"timers.p99[mss.gateway.api.all.requests]","value":"2802.600693"}],"request":"sender data"}'