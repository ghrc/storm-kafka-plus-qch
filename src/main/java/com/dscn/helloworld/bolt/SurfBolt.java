package com.dscn.helloworld.bolt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.json.JSONObject;

import com.dscn.helloworld.common.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

@SuppressWarnings({ "deprecation", "serial" })
public class SurfBolt extends BaseRichBolt {
	private static Configuration _conf = HBaseConfiguration.create();
	private static HTable _hTable = null;
	private OutputCollector _collector;
	private HashMap<String, String> map = new HashMap<String, String>();

	long startTime = System.currentTimeMillis();
    long count = 0;

    public SurfBolt() {
		_conf.set("hbase.zookeeper.quorum", Constants.hostList);
		try {
			_hTable = new HTable(_conf, "test");
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @SuppressWarnings("rawtypes")
	public void prepare(Map sconf, TopologyContext context, OutputCollector collector) {
    	_collector = collector;
    }

	@SuppressWarnings("rawtypes")
	public void execute(Tuple tuple) {
    	System.out.println("SurfBolt sentence=" + tuple.getString(0));
		String jsonObject = tuple.getStringByField("JsonMsg");
		JSONObject jsonArray = new JSONObject(jsonObject);

    	count++;
        if (count % 20000 == 0) {
        	long sumTime = System.currentTimeMillis();
        	System.out.println("[RESULT]the time of 20000 is [" + (sumTime - startTime) + "]");
        }

        try {
			String row_key = jsonArray.get("logisticProviderID").toString() + ":" + jsonArray.get("mailNo").toString();
			map.put("mailType", jsonArray.get("mailType").toString());				// �浥����
			map.put("weight", jsonArray.get("weight").toString());					// ����
			map.put("senAreaCode", jsonArray.get("senAreaCode").toString());		// �ļ����Ҵ���
			map.put("recAreaCode", jsonArray.get("recAreaCode").toString());		// �ռ����Ҵ���
			map.put("senCityCode", jsonArray.get("senCityCode").toString());		// �ļ����д���
			map.put("recCityCode", jsonArray.get("recCityCode").toString());		// �ռ����д���
			map.put("senProv", jsonArray.get("senProvCode").toString());			// �ļ����д���
			map.put("senCity", jsonArray.get("senCity").toString());				// �ռ����д���
			map.put("senCountyCode", jsonArray.get("senCountyCode").toString());	// �ļ����Ҵ���
			map.put("senAddress", jsonArray.get("senAddress").toString());			// �ռ����Ҵ���
			map.put("senName", jsonArray.get("senName").toString());				// �ļ�������
			map.put("senMobile", jsonArray.get("senMobile").toString());			// �ļ����ƶ��绰
			map.put("senPhone", jsonArray.get("senPhone").toString());				// �ļ��˹̶��绰
			map.put("recProvCode", jsonArray.get("recProvCode").toString());		// �ռ�������
			map.put("recCity", jsonArray.get("recCity").toString());				// �ռ����ƶ��绰
			map.put("recCountyCode", jsonArray.get("recCountyCode").toString());	// �ռ��˹̶��绰
			map.put("recAddress", jsonArray.get("recAddress").toString());			// �ռ���ַʡ��
			map.put("recName", jsonArray.get("recName").toString());				// �ռ���ַ����
			map.put("recMobile", jsonArray.get("recMobile").toString());			// �ռ���ַ����
			map.put("recPhone", jsonArray.get("recPhone").toString());				// �ռ���ϸ��ַ
			map.put("typeOfContents", jsonArray.get("typeOfContents").toString());	// �ڼ�����
			map.put("nameOfCoutents", jsonArray.get("nameOfCoutents").toString());	// �ڼ�Ʒ��
			map.put("mailCode", jsonArray.get("mailCode").toString());				// ��Ʒ����
			map.put("recDatetime", jsonArray.get("recDatetime").toString());		// �ļ�����
			map.put("insuranceValue", jsonArray.get("insuranceValue").toString());	// ���۽��

			Put put = new Put(row_key.getBytes());
			Set set = map.entrySet();
			Iterator iterator = set.iterator();

			while (iterator.hasNext()) {
				Map.Entry mapentry = (Map.Entry) iterator.next();
				put.add("info".getBytes(), ((String)mapentry.getKey()).getBytes(), ((String)mapentry.getValue()).getBytes());
			}

			List<Row> bath = new ArrayList<Row>();
			bath.add(put);
			if (bath.size() == 1000) {
				Object[] reluts = new Object[bath.size()];
				try {
					_hTable.batch(bath, reluts);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				bath.clear();
			}
			map.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}

		_collector.ack(tuple);
    }

	public void declareOutputFields(OutputFieldsDeclarer arg0) {
	}

	@SuppressWarnings("rawtypes")
	public void prepare(Map arg0, TopologyContext arg1) {
	}
}
