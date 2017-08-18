package com.cn.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.cn.common.LogHelper;
import com.cn.common.Utils;

/**
 * 
 * @author songzhili
 * 2017年1月16日下午5:37:31
 */
public class MerchantJdbcClient {
   
    Connection oracle_conn = null;  
    /**数据库配置文件名称**/
    private String dataFile;
    /****/
    private String url = null;
    /****/
    private String dataBaseName;
    /****/
    private String dataBasePassword;
    /****/
    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public MerchantJdbcClient(String dataFile){
	   	this.dataFile = dataFile;
	   	init();
    }
    
    public MerchantJdbcClient(){
   	   init();
    }
    /**
     * 初始化
     */
    private void init(){
   	 
   	 try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			String[] props = readProperties();
			if(props != null){
				this.oracle_conn = DriverManager.getConnection(props[0],props[1], props[2]);
				this.url = props[0];
				this.dataBaseName = props[1];
				this.dataBasePassword = props[2];
			}
		} catch (ClassNotFoundException e) {
			//ignore
		} catch (SQLException e) {
			//ignore
		} 
    }
    /**
     * 读取配置文件
     * @return
     */
	private String[] readProperties() {

		Properties properties = new Properties();
		InputStream is = MerchantJdbcClient.class.getClassLoader()
				.getResourceAsStream(this.dataFile);
		try {
			properties.load(is);
		} catch (IOException e) {
			// ignore
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return new String[] { properties.getProperty("url"),
				properties.getProperty("username"),
				properties.getProperty("password") };
	}
    /**
     * 
     * @param userName
     * @return
     */
    public Map<String, Object> queryAdmin(String userName){
        
    	Map<String, Object> result = new HashMap<String, Object>();
		try {
			if(this.oracle_conn == null){
				this.oracle_conn = DriverManager.getConnection(this.url,this.dataBaseName, this.dataBasePassword);
			}
			Statement oracle_stmt = this.oracle_conn.createStatement();  
		    ResultSet oracle_rs = null; 
			StringBuilder together = new StringBuilder();
			together.append("select sho.SHOPUSER_ID,sho.SHOPUSER_NICKNAME,sho.SHOPUSER_NAME");
			together.append(",sho.SHOPUSER_PHONE,sho.SHOPUSER_PASSWORD,sho.SHOPUSER_ISSTART,sho.SHOPUSER_STOREID");
			together.append(",sho.SHOPUSER_ROLE,mer.MERCHANT_MERCHNAME,mer.MERCHANT_ID,mer.ALLPAY_ISSTART");
			together.append(",mer.MERCHANT_BRANCHCOMPANYID,mer.MERCHANT_BRANCHCOMPANYNAME");
			together.append(" from ALLPAY_SHOPUSER sho inner join ALLPAY_MERCHANT mer ");
			together.append(" on sho.SHOPUSER_SHOPID = mer.MERCHANT_ID");
			together.append(" where 1=1 AND mer.ALLPAY_LOGICDEL = '1' AND mer.ALLPAY_ISSTART = 1");
			together.append(" AND sho.SHOPUSER_ISSTART = 1 AND sho.ALLPAY_LOGICDEL = '1'");
			/****/
			String sql = together.toString();
			together.setLength(0);
			/***SHOPUSER_NAME**/
			together.append(sql).append(" AND sho.SHOPUSER_NAME = '").append(userName).append("'");
			LogHelper.info(together);
			oracle_rs = oracle_stmt.executeQuery(together.toString());
			boolean hasResult = false;
			while (oracle_rs.next()) {
				hasResult = true;
				transferData(result, oracle_rs);
				break;
			}
			if(!hasResult){
				hasResult = true;
				together.setLength(0);
				together.append(sql).append(" AND sho.SHOPUSER_PHONE = '").append(userName).append("'");
				LogHelper.info(together);
				oracle_rs = oracle_stmt.executeQuery(together.toString());
				while (oracle_rs.next()) {
					transferData(result, oracle_rs);
					break;
				}
			}
			if(!hasResult){
				hasResult = true;
				together.setLength(0);
				together.append(sql).append(" AND sho.SHOPUSER_ACCOUNTID = '").append(userName).append("'");
				LogHelper.info(together);
				oracle_rs = oracle_stmt.executeQuery(together.toString());
				while (oracle_rs.next()) {
					transferData(result, oracle_rs);
					break;
				}
			}
			if(hasResult){
				together.setLength(0);
				together.append("update ALLPAY_SHOPUSER sho set sho.LAST_LOGIN_TIME = '");
				together.append(format.format(new Date())).append("' where sho.SHOPUSER_ID = '");
				together.append(result.get("userId")).append("'");
				LogHelper.info(together);
				oracle_stmt.executeUpdate(together.toString());
			}
			/**关闭数据库**/
			oracle_stmt.close();
			oracle_rs.close();
		} catch (SQLException e) {
			// ignore
			try {
				this.oracle_conn = null;
				this.oracle_conn = DriverManager.getConnection(this.url,this.dataBaseName, this.dataBasePassword);
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		return result;
    }
    /**
     *    
     * @param merchantId
     * @return
     */
    public Map<String, Object> queryAdminByMerchantId(String merchantId){
    	
    	Map<String, Object> result = new HashMap<String, Object>();
    	try {
			if(this.oracle_conn == null){
				this.oracle_conn = DriverManager.getConnection(this.url,this.dataBaseName, 
						this.dataBasePassword);
			}
			Statement oracle_stmt = this.oracle_conn.createStatement();  
		    ResultSet oracle_rs = null; 
			StringBuilder together = new StringBuilder();
			together.append("select * from (select sho.SHOPUSER_ID,sho.SHOPUSER_NICKNAME");
			together.append(",sho.SHOPUSER_NAME,sho.SHOPUSER_PHONE,sho.SHOPUSER_PASSWORD");
			together.append(",sho.SHOPUSER_ISSTART,sho.SHOPUSER_ROLE,mer.MERCHANT_MERCHNAME");
			together.append(",mer.MERCHANT_BRANCHCOMPANYID,mer.MERCHANT_BRANCHCOMPANYNAME");
			together.append(",mer.MERCHANT_ID,mer.ALLPAY_ISSTART from ALLPAY_SHOPUSER sho");
			together.append(" inner join ALLPAY_MERCHANT mer on sho.SHOPUSER_SHOPID = mer.MERCHANT_ID");
			together.append(" where 1=1 AND sho.SHOPUSER_ISSTART = 1 AND sho.SHOPUSER_ROLE = '2'");
			together.append(" AND sho.ALLPAY_LOGICDEL = '1' AND mer.ALLPAY_LOGICDEL = '1' AND mer.ALLPAY_ISSTART = 1");
			together.append(" AND mer.MERCHANT_ID = '").append(merchantId).append("'");
			together.append(" order by sho.ALLPAY_CREATETIME ASC) where ROWNUM <=1");
			/****/
			LogHelper.info(together);
			oracle_rs = oracle_stmt.executeQuery(together.toString());
			while (oracle_rs.next()) {
				transferData(result, oracle_rs);
				break;
			}
			/**关闭数据库**/
			oracle_stmt.close();
			oracle_rs.close();
		} catch (SQLException e) {
			// ignore
		}
    	return result;
    }
    /**
     * 
     */
    public void close(){
    	if(this.oracle_conn != null){
    		try {
				this.oracle_conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
    	}
    }
	 /**
	  * 
	  * @param result
	  * @param oracle_rs
	  * @throws SQLException
	  */
    private void transferData(Map<String, Object> result,ResultSet oracle_rs) throws SQLException{
    	result.put("userId", oracle_rs.getString("SHOPUSER_ID"));
    	result.put("mer_userName", oracle_rs.getString("SHOPUSER_NAME"));
    	result.put("nickName", oracle_rs.getString("SHOPUSER_NICKNAME"));
    	result.put("passWord", oracle_rs.getString("SHOPUSER_PASSWORD"));
    	result.put("userRole", oracle_rs.getString("SHOPUSER_ROLE"));
    	result.put("userType", oracle_rs.getString("SHOPUSER_ROLE"));
    	result.put("merchantId", oracle_rs.getString("MERCHANT_ID"));
    	result.put("shopName", oracle_rs.getString("MERCHANT_MERCHNAME"));
    	result.put("merchantState", oracle_rs.getString("ALLPAY_ISSTART"));
    	result.put("branchCompanyId", oracle_rs.getString("MERCHANT_BRANCHCOMPANYID"));
    	result.put("branchCompanyName", oracle_rs.getString("MERCHANT_BRANCHCOMPANYNAME"));
    	if(!Utils.isEmpty(oracle_rs.getString("SHOPUSER_STOREID"))){
    		result.put("storeId", oracle_rs.getString("SHOPUSER_STOREID"));
    	}
    }
    
    
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
    	Class.forName("oracle.jdbc.driver.OracleDriver");
    	 Statement oracle_stmt = null;  
    	 ResultSet oracle_rs = null; 
    	 Connection oracle_conn = DriverManager.getConnection("jdbc:oracle:thin:@192.168.15.122:1521:jingjing",
    			 "miteno", "a123456");
    	 oracle_stmt = oracle_conn.createStatement();
    	 StringBuilder together = new StringBuilder();
			together.append("select sho.SHOPUSER_ID,sho.SHOPUSER_NICKNAME,sho.SHOPUSER_NAME");
			together.append(",sho.SHOPUSER_PHONE,sho.SHOPUSER_PASSWORD,sho.SHOPUSER_ISSTART,sho.SHOPUSER_STOREID");
			together.append(",sho.SHOPUSER_ROLE,mer.MERCHANT_MERCHNAME,mer.MERCHANT_ID,mer.ALLPAY_ISSTART");
			together.append(" from ALLPAY_SHOPUSER sho inner join ALLPAY_MERCHANT mer ");
			together.append(" on sho.SHOPUSER_SHOPID = mer.MERCHANT_ID");
			together.append(" where 1=1 AND mer.ALLPAY_LOGICDEL = '1'");
			together.append(" AND sho.SHOPUSER_ISSTART = 1 AND sho.ALLPAY_LOGICDEL = '1'");
			together.append(" AND sho.SHOPUSER_NAME = '").append("hongw").append("'");
			
		oracle_rs = oracle_stmt.executeQuery(together.toString());
		while(oracle_rs.next()){
			System.out.println(oracle_rs.getString("SHOPUSER_ID"));
		}
		together.setLength(0);
		together.append("update ALLPAY_SHOPUSER sho set sho.SHOPUSER_ACCOUNTID = '");
		together.append("zl_3893012427").append("' where sho.SHOPUSER_ID = '")
		.append("ff8080815a170368015a17b43abd007b'");
		oracle_stmt.executeUpdate(together.toString());
	}
}
