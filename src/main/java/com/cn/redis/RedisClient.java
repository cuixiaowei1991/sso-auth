package com.cn.redis;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.cn.common.Utils;

/**
 * 
 * @author songzhili
 * 2016年11月1日上午10:24:23
 */
@SuppressWarnings("deprecation")
public class RedisClient {
   
	//private Jedis jredis;//非切片客户端连接
    private JedisPool jedisPool;//非切片连接池
    /****/
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    /****/
    private final Lock readLock = readWriteLock.readLock();
    /****/
    private final Lock writeLock = readWriteLock.writeLock();
    /****/
    private String redisFile;
    /****/
    private volatile static RedisClient client = null;
    
    public static RedisClient getRedisClient(String redisFile){
    	
    	synchronized (RedisClient.class) {
    		if(client == null){
        		client = new RedisClient(redisFile);
        	}
		}
    	return client;
    }
    
    private RedisClient(String redisFile){
    	this.redisFile = redisFile;
    	String[] props = readProperties();
        initialPool(props[0],Integer.parseInt(props[1])); 
//        initialShardedPool(props[0],Integer.parseInt(props[1])); 
//        shardedJedis = shardedJedisPool.getResource(); 
//        jredis = jedisPool.getResource();
    }
    
    /**
     * 读取配置文件
     * @return
     */
    private String[] readProperties(){
    	
    	Properties properties = new Properties();  
        InputStream is = RedisClient.class
        		.getClassLoader().getResourceAsStream(this.redisFile);
        try {
			properties.load(is);
		} catch (IOException e) {
			//ignore
		}finally{
			if(is != null){
				try {
					is.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
    	return new String[]{properties.getProperty("address")
    			,properties.getProperty("port")};
    }
    
    /**
     * 初始化非切片池
     */
    private void initialPool(String address,int port){
    	// 池基本配置 
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(150);
        config.setMaxIdle(5); 
        config.setMaxWaitMillis(1000l);
        config.setTestOnBorrow(false); 
        jedisPool = new JedisPool(config,address,port);
    }
    /**
     * 初始化切片池 
     */
//    private void initialShardedPool(String address,int port){
//    	// 池基本配置 
//        JedisPoolConfig config = new JedisPoolConfig(); 
//        config.setMaxTotal(100);
//        config.setMaxIdle(5); 
//        config.setMaxWaitMillis(1000l);
//        config.setTestOnBorrow(true); 
//        // slave链接 
//        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(); 
//        shards.add(new JedisShardInfo(address, port,"master")); 
//        // 构造池 
//        shardedJedisPool = new ShardedJedisPool(config, shards); 
//    }
    
     void keyOperate(){
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		if(jredis != null){
    			Set<String> keys = jredis.keys("*"); 
    			Iterator<String> it = keys.iterator() ;   
    			while(it.hasNext()){   
    				String key = it.next();
    				System.out.println(key);
    			}
    		}
		} catch (Exception ex) {
			throwen = true;
		}finally{
			returnJredisToPool(jredis, throwen);
		}
    }
     
     
	public Iterator<String> keyIterator(){
    	
    	Set<String> keys = null;
    	readLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		keys = jredis.keys("*"); 
		}catch (Exception ex) {
			throwen = true;
		}finally{
			returnJredisToPool(jredis, throwen);
			readLock.unlock();
		}
    	if(keys != null){
    		return keys.iterator();
    	}
    	return new HashSet<String>().iterator();
    }
    
    /**
     * 
     * @param hashName
     * @param userName
     * @return
     */
    public String getUser(String hashName,String userName){
    	
    	if(Utils.isEmpty(userName)){
    		return null;
    	}
    	readLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
		try {
			if (jredis.hexists(hashName, userName)) {
				return jredis.hget(hashName, userName);
			} else {
				return null;
			}
		}catch (Exception ex) {
			throwen = true;
		} finally {
			returnJredisToPool(jredis, throwen);
			readLock.unlock();
		}
		return null;
    }
    /**
     * 
     * @param hashName
     * @param userName
     * @param password
     */
    public void insertUser(String hashName,String userName,String password){
    	
    	writeLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		if(!Utils.areEmpty(userName, password)){
    			jredis.hset(hashName, userName, password);
        	}
		}catch (Exception ex) {
			throwen = true;
		}finally{
			returnJredisToPool(jredis, throwen);
			writeLock.unlock();
		}
    }
    /**
     * 
     * @param ticket
     * @param expireTime
     */
    public void insertTicket(String ticket,String expireTime){
    	
    	if(Utils.areEmpty(ticket, expireTime)){
    		return;
    	}
    	writeLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		jredis.set(ticket, expireTime);
		}catch (Exception ex) {
			throwen = true;
		}finally{
			returnJredisToPool(jredis, throwen);
			writeLock.unlock();
		}
    }
    /**
     * 
     * @param ticket
     * @return
     */
    public String getTicket(String ticket){
    	
    	if(Utils.isEmpty(ticket)){
    		return null;
    	}
    	readLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		if(jredis.exists(ticket)){
    			return jredis.get(ticket);
    		}else{
    			return null;
    		}
		} catch (Exception ex) {
			throwen = true;
		} finally {
			returnJredisToPool(jredis, throwen);
			readLock.unlock();
		}
    	return null;
    }
    
    public void updateTicket(String ticket,String value){
    	if(Utils.isEmpty(ticket)){
    		return;
    	}
    	writeLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		jredis.set(ticket, value);
		}catch (Exception ex) {
			throwen = true;
		} finally {
			returnJredisToPool(jredis, throwen);
			writeLock.unlock();
		}
    }
    
    
    public void deleteTicket(String ticket){
    	
    	if(Utils.isEmpty(ticket)){
    		return;
    	}
    	writeLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		jredis.del(ticket);
		}catch (Exception ex) {
			throwen = true;
		} finally {
			returnJredisToPool(jredis, throwen);
			writeLock.unlock();
		}
    }
    /**
     * 设置ticket的超时时间,超时自动失效
     * @param ticket
     * @param time 单位 秒
     */
    public void invalidTicket(String ticket,int time){
    	
    	if(Utils.isEmpty(ticket)){
    		return;
    	}
    	writeLock.lock();
    	Jedis jredis = this.jedisPool.getResource();
    	boolean throwen = false;
    	try {
    		jredis.expire(ticket, time);
		}catch (Exception ex) {
			throwen = true; 
		} finally {
			returnJredisToPool(jredis, throwen);
			writeLock.unlock();
		}
    }
    /**
     *　归还Jredis实例
     * @param jredis
     * @param isExceptionThrow
     */
    private void returnJredisToPool(Jedis jredis,boolean isExceptionThrow){
        
    	try {
    		if(isExceptionThrow){
        		this.jedisPool.returnBrokenResource(jredis);
        	}else{
        		this.jedisPool.returnResource(jredis);
        	}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
    /**
     * 销毁连接池
     */
    public void destory(){
    	
    	if(this.jedisPool != null){
    		jedisPool.destroy();
    	}
    }
    
    public static void main(String[] args) {
    	RedisClient client = new RedisClient("redis.properties");
//    	ScheduledExecutorService schedulePool = Executors.newScheduledThreadPool(1);
//		schedulePool.scheduleAtFixedRate(new RecoverTicket(client), 
//				1, 1, TimeUnit.SECONDS);
//    	client.keyOperate();
    	
    	client.insertTicket("song", "songzhili");
	}
}




