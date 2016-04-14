package com.dscn.helloworld;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.StringScheme;

import java.util.*;

import com.dscn.helloworld.bolt.PrintBolt;
import com.dscn.helloworld.bolt.SurfBolt;
import com.dscn.helloworld.bolt.WordCountBolt;
import com.dscn.helloworld.bolt.WordNormalizerBolt;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import backtype.storm.spout.SchemeAsMultiScheme;

/**
 *
 * 调优点1：设置worker数量
 * Worker是运行在工作节点上面，被Supervisor守护进程创建的用来干活的进程。
 * 每个Worker对应于一个给定topology的全部执行任务的一个子集。
 * 反过来说，一个Worker里面不会运行属于不同的topology的执行任务。
 * 数目至少应该大于machines的数目
 *
 * 调优点2：给指定component创建的executor数量。通过setSpout/setBolt的参数来设置。
 * Executor可以理解成一个Worker进程中的工作线程。
 * 一个Executor中只能运行隶属于同一个component（spout/bolt）的task。
 * 一个Worker进程中可以有一个或多个Executor线程。在默认情况下，一个Executor运行一个task。
 *
 * 调优点3：给指定 component 创建的task数量。通过调用setNumTasks()方法来设置。
 * Task则是spout和bolt中具体要干的活了。
 * 一个Executor可以负责1个或多个task。
 * 每个component（spout/bolt）的并发度就是这个component对应的task数量。
 * 同时，task也是各个节点之间进行grouping（partition）的单位。
 * 默认和executor1:1
 * 
 */
public class WordCountTopology {
    public static void main(String[] args) throws InterruptedException {
    	System.out.println("WordCountTopology main start!");

		BrokerHosts brokerHosts = new ZkHosts("192.168.93.128:2181,192.168.93.129:2181,192.168.93.130:2181");
		//BrokerHosts brokerHosts = new ZkHosts("192.168.1.36:2181,192.168.1.37:2181,192.168.1.38:2181");
		SpoutConfig spoutConfig = new SpoutConfig(brokerHosts, "qchlocaltest20160414", "", "topo");
		spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
		spoutConfig.forceFromStart = true;
		spoutConfig.zkServers = Arrays.asList(new String[] {"192.168.93.128", "192.168.93.129", "192.168.93.130"});
		//spoutConfig.zkServers = Arrays.asList(new String[] {"192.168.1.36", "192.168.1.37", "192.168.1.38"});
		spoutConfig.zkPort = 2181;

	    TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("RandomSentence", new KafkaSpout(spoutConfig), 1/* 调优点2 */).setNumTasks(1/* 调优点3 */);
        //builder.setBolt("SurfBolt", new SurfBolt(), 1/* 调优点2 */).shuffleGrouping("RandomSentence").setNumTasks(1/* 调优点3 */);

        builder.setBolt("WordNormalizer", new WordNormalizerBolt(), 1/* 调优点2 */).shuffleGrouping("RandomSentence").setNumTasks(1/* 调优点3 */);
        builder.setBolt("WordCount", new WordCountBolt(), 1/* 调优点2 */).fieldsGrouping("WordNormalizer", new Fields("word")).setNumTasks(1/* 调优点3 */);
        builder.setBolt("Print", new PrintBolt(), 1/* 调优点2 */).shuffleGrouping("WordCount").setNumTasks(1/* 调优点3 */);

        Config config = new Config();
        config.setDebug(false);

        if (args != null && args.length > 0) {
        	System.out.println("WordCountTopology not local. ");

        	config.put(Config.NIMBUS_HOST, args[0]);
            config.setNumWorkers(1/* 调优点1 */);

        	try {
	            StormSubmitter.submitTopology("WordCountTopology", config, builder.createTopology());
	        	System.out.println("WordCountTopology submitTopology end.");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
        } else {
        	System.out.println("WordCountTopology local.");
            config.setMaxTaskParallelism(1);
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("WordCountTopology", config, builder.createTopology());
        	System.out.println("WordCountTopology submitTopology end.");
        }

    	System.out.println("WordCountTopology main end!");
    }
}