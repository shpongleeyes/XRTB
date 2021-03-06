package com.xrtb.bidder;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.xrtb.commands.StopBidder;
import com.xrtb.common.Configuration;

/**
 * A class used to stop runaway bidders. If deadmanswitch is set in the startup json, it must be present in the
 * REDIs store before the bidder will bid. The switch is set by the accounting program.
 * @author Ben M. Faul
 *
 */
public class DeadmanSwitch implements Runnable {

	JedisPool pool;
	Thread me;
	String key;
	boolean sentStop = false;
	public static boolean testmode = false;
	
	
	
	public DeadmanSwitch(String host, int port, String password,  String key) {
		JedisPoolConfig cfg = new JedisPoolConfig();
		cfg.setMaxTotal(1000);
		pool = new JedisPool(cfg,host, port, 10000, password);
		this.key = key;
		me = new Thread(this);
		me.start();
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				if (canRun() == false) {
					if (sentStop == false) {
						try {
							if (!testmode) {
								Controller.getInstance().sendLog(1, "DeadmanSwitch",
										("Switch error: " + key + ", does not exist, no bidding allowed!"));
								StopBidder cmd = new StopBidder();
								cmd.from = Configuration.getInstance().instanceName;
								Controller.getInstance().stopBidder(cmd);
							} else {
								System.out.println("Deadman Switch is thrown");
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					sentStop = true;
				} else {
					sentStop = false;
				}
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public boolean canRun() {
		Jedis redis = pool.getResource();
		String value = redis.get(key);
		pool.returnResource(redis);
		redis = null;
		if (value == null) {
			return false;
		}
		sentStop = false;
		return true;
	}
}
