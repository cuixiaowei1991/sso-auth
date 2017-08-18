package com.cn.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.cn.common.LogHelper;

/**
 * 
 * @author songzhili
 * 2017年1月16日下午5:39:01
 */
public class BusinessJdbcClient {
   
    Connection oracle_conn = null;  
    /**数据库配置文件名称**/
    private String dataFile;
    /****/
    private String url = null;
    /****/
    private String dataBaseName;
    /****/
    private String dataBasePassword;
    
    public BusinessJdbcClient(String dataFile){
	   	this.dataFile = dataFile;
	   	init();
    }
    
    public BusinessJdbcClient(){
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
		InputStream is = BusinessJdbcClient.class.getClassLoader()
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
				this.oracle_conn = DriverManager.getConnection(this.url,
						this.dataBaseName, this.dataBasePassword);
			}
			Statement oracle_stmt = this.oracle_conn.createStatement();  
		    ResultSet oracle_rs = null; 
			StringBuilder together = new StringBuilder();
			together.append("select usr.USER_ID,usr.USER_NAME,usr.USER_PHONE,usr.USER_NICKNAME");
			together.append(",usr.USER_PASSWORD,usr.USER_ISSTART,usr.USER_ORGANIZATIONID,rol.ROLE_ISSTART");
			together.append(",rol.ROLE_NAME,rol.ROLE_ID,org.ORGANIZATION_NAME,org.ORGANIZATION_TYPE");
			together.append(" from ALLPAY_USER usr inner join ALLPAY_ROLE rol on usr.USER_ROLEID = rol.ROLE_ID");
			together.append(" inner join ALLPAY_ORGANIZATION org on usr.USER_ORGANIZATIONID = org.ORGANIZATION_ID");
			together.append(" where 1=1 AND rol.ROLE_ISSTART = 1 AND rol.ALLPAY_LOGICDEL = '1'");
			together.append(" AND org.ORGANIZATION_STATE = 1 AND org.ALLPAY_LOGICDEL = '1'");
			together.append(" AND usr.USER_ISSTART = 1 AND usr.ALLPAY_LOGICDEL = '1'");
			/****/
			String sql = together.toString();
			together.setLength(0);
			together.append(sql).append(" AND usr.USER_NAME = '").append(userName).append("'");
			LogHelper.info(together);
			oracle_rs = oracle_stmt.executeQuery(together.toString());
			boolean hasResult = false;
			while (oracle_rs.next()) {
				hasResult = true;
				transferData(result, oracle_rs);
				break;
			}
			if(!hasResult){
				together.setLength(0);
				together.append(sql).append(" AND usr.USER_PHONE = '").append(userName).append("'");
				LogHelper.info(together);
				oracle_rs = oracle_stmt.executeQuery(together.toString());
				while (oracle_rs.next()) {
					transferData(result, oracle_rs);
					break;
				}
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
    	result.put("userId", oracle_rs.getString("USER_ID"));
    	result.put("bus_userName", oracle_rs.getString("USER_NAME"));
    	result.put("phone", oracle_rs.getString("USER_PHONE"));
    	result.put("nickName", oracle_rs.getString("USER_NICKNAME"));
    	result.put("passWord", oracle_rs.getString("USER_PASSWORD"));
    	result.put("userState", oracle_rs.getString("USER_ISSTART"));
    	result.put("branchId", oracle_rs.getString("USER_ORGANIZATIONID"));
    	result.put("branchName", oracle_rs.getString("ORGANIZATION_NAME"));
    	result.put("branchType", oracle_rs.getString("ORGANIZATION_TYPE"));
    	result.put("roleId", oracle_rs.getString("ROLE_ID"));
    	result.put("roleName", oracle_rs.getString("ROLE_NAME"));
    }
    
    
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
    	Class.forName("oracle.jdbc.driver.OracleDriver");
    	 Statement oracle_stmt = null;  
    	 ResultSet oracle_rs = null; 
    	 Connection oracle_conn = DriverManager.getConnection("jdbc:oracle:thin:@192.168.15.159:1521:bi",
    			 "coupons001", "coupons001");
    	 oracle_stmt = oracle_conn.createStatement();
    	 StringBuilder together = new StringBuilder();
	   		together.append("select usr.USER_ID,usr.USER_NAME,usr.USER_PHONE,usr.USER_NICKNAME");
	   		together.append(",usr.USER_PASSWORD,usr.USER_ISSTART,usr.USER_ORGANIZATIONID,rol.ROLE_ISSTART");
	   		together.append(",rol.ROLE_NAME,rol.ROLE_ID,org.ORGANIZATION_NAME,org.ORGANIZATION_TYPE");
	   		together.append(" from ALLPAY_USER usr inner join ALLPAY_ROLE rol on usr.USER_ROLEID = rol.ROLE_ID");
	   		together.append(" inner join ALLPAY_ORGANIZATION org on usr.USER_ORGANIZATIONID = org.ORGANIZATION_ID");
	   		together.append(" where 1=1 AND rol.ROLE_ISSTART = 1 AND rol.ALLPAY_LOGICDEL = '1'");
	   		together.append(" AND org.ORGANIZATION_STATE = 1 AND org.ALLPAY_LOGICDEL = '1'");
	   		together.append(" AND usr.USER_ISSTART = 1 AND usr.ALLPAY_LOGICDEL = '1'");
	   		together.append(" AND usr.USER_NAME = 'ceshi'");
	   		System.out.println(together);
		oracle_rs = oracle_stmt.executeQuery(together.toString());
		while(oracle_rs.next()){
			System.out.println(oracle_rs.getString("USER_ID"));
			System.out.println(oracle_rs.getString("USER_NAME"));
			System.out.println(oracle_rs.getString("USER_PHONE"));
			System.out.println(oracle_rs.getString("USER_NICKNAME"));
			System.out.println(oracle_rs.getString("USER_PASSWORD"));
			System.out.println(oracle_rs.getString("USER_ISSTART"));
			System.out.println(oracle_rs.getString("USER_ORGANIZATIONID"));
			System.out.println(oracle_rs.getString("ROLE_ISSTART"));
			System.out.println(oracle_rs.getString("ORGANIZATION_NAME"));
			System.out.println(oracle_rs.getString("ORGANIZATION_TYPE"));
			System.out.println(oracle_rs.getString("ROLE_NAME"));
			System.out.println(oracle_rs.getString("ROLE_ID"));
		}
	}
}
