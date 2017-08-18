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
 * 2016年11月1日下午2:02:02
 */
public class AgentsJdbcClient {
  
	 Connection oracle_conn = null;  
     /**数据库配置文件名称**/
     private String dataFile;
     /****/
     private String url;
     /****/
     private String dataBaseName;
     /****/
     private String dataBasePassword;
     
     public AgentsJdbcClient(String dataFile){
    	this.dataFile = dataFile;
    	init();
     }
     public AgentsJdbcClient(){
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
     private String[] readProperties(){
     	
     	Properties properties = new Properties();  
        InputStream is = AgentsJdbcClient.class
         		.getClassLoader().getResourceAsStream(this.dataFile);  
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
     	return new String[]{properties.getProperty("url")
     			,properties.getProperty("username")
     			,properties.getProperty("password")
     			};
     }
     /**
      * 
      * @param userName
      * @return
      */
	public Map<String, Object> queryAdmin(String userName) {

		Map<String, Object> result = new HashMap<String, Object>();
		try {
			if (this.oracle_conn == null) {
				this.oracle_conn = DriverManager.getConnection(this.url,
						this.dataBaseName, this.dataBasePassword);
			}
			Statement oracle_stmt = this.oracle_conn.createStatement();
			ResultSet oracle_rs = null;
			StringBuilder together = new StringBuilder();
			together.append("select age.AGENTUSER_ID,age.AGENTUSER_NAME,age.AGENTUSER_PASSWORD");
			together.append(",age.AGENTUSER_NICKNAME,age.AGENTUSER_ISSTART,age.AGENTUSER_PHONE");
			together.append(",agef.AGENT_BRANCH_ID,agef.AGENT_ID,agef.AGENT_LOCATION,ageg.ORGANIZATION_NAME");
			together.append(",agef.AGENT_LEVEL,agef.AGENT_NAME from ALLPAY_AGENTUSER age");
			together.append(" inner join ALLPAY_AGENT agef on age.AGENTUSER_AGENTID = agef.AGENT_ID");
			together.append(" inner join ALLPAY_ORGANIZATION ageg on agef.AGENT_BRANCH_ID = ageg.ORGANIZATION_ID");
			together.append(" where 1=1 AND age.ALLPAY_LOGICDEL = '1' AND age.AGENTUSER_ISSTART = 1");
			together.append(" AND agef.ALLPAY_LOGICDEL = '1' AND agef.ALLPAY_ISSTART = 1");
			together.append(" AND ageg.ALLPAY_LOGICDEL = '1' AND ageg.ORGANIZATION_STATE = 1");
			String sql = together.toString();
			together.setLength(0);
			together.append(sql);
			together.append(" AND age.AGENTUSER_NAME = '").append(userName).append("'");
			LogHelper.info(together);
			oracle_rs = oracle_stmt.executeQuery(together.toString());
			boolean hasResult = false;
			while (oracle_rs.next()) {
				hasResult = true;
				transferData(result, oracle_rs);
				break;
			}
			/****/
			if (!hasResult) {
				together.setLength(0);
				together.append(sql);
				together.append(" AND age.AGENTUSER_PHONE = '").append(userName).append("'");
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
	
	public Map<String, Object> queryForAgentId(String agentId){
		return null;
	}
    /**
     * 
     * @param result
     * @param oracle_rs
     * @throws SQLException 
     */
    private void transferData(Map<String, Object> result,ResultSet oracle_rs) throws SQLException{
    	result.put("userId", oracle_rs.getString("AGENTUSER_ID"));
    	result.put("age_userName", oracle_rs.getString("AGENTUSER_NAME"));
    	result.put("passWord", oracle_rs.getString("AGENTUSER_PASSWORD"));
    	result.put("nickName", oracle_rs.getString("AGENTUSER_NICKNAME"));
    	result.put("phone", oracle_rs.getString("AGENTUSER_PHONE"));
    	result.put("userState", oracle_rs.getString("AGENTUSER_ISSTART"));
    	result.put("companyId", oracle_rs.getString("AGENT_BRANCH_ID"));
    	result.put("companyName", oracle_rs.getString("ORGANIZATION_NAME"));
    	result.put("agentId", oracle_rs.getString("AGENT_ID"));
    	result.put("agentName", oracle_rs.getString("AGENT_NAME"));
    	result.put("superAgentId", oracle_rs.getString("AGENT_LOCATION"));
    	result.put("level", oracle_rs.getString("AGENT_LEVEL"));
    }
     
     
	public void close() {
		try {
			if (this.oracle_conn != null) {
				this.oracle_conn.close();
			}
		} catch (SQLException se) {
			// ignore
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException,
			SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		Statement oracle_stmt = null;
		ResultSet oracle_rs = null;
		Connection oracle_conn = DriverManager.getConnection(
				"jdbc:oracle:thin:@192.168.15.122:1521:jingjing", "miteno",
				"a123456");
		oracle_stmt = oracle_conn.createStatement();
		StringBuilder together = new StringBuilder();
		together.append("select age.AGENTUSER_ID,age.AGENTUSER_NAME,age.AGENTUSER_PASSWORD");
		together.append(",age.AGENTUSER_NICKNAME,age.AGENTUSER_ISSTART,age.AGENTUSER_PHONE");
		together.append(",agef.AGENT_BRANCH_ID,agef.AGENT_ID,agef.AGENT_LOCATION,ageg.ORGANIZATION_NAME");
		together.append(" from ALLPAY_AGENTUSER age");
		together.append(" inner join ALLPAY_AGENT agef on age.AGENTUSER_AGENTID = agef.AGENT_ID");
		together.append(" inner join ALLPAY_ORGANIZATION ageg on agef.AGENT_BRANCH_ID = ageg.ORGANIZATION_ID");
		together.append(" where 1=1 AND age.ALLPAY_LOGICDEL = '1' AND age.AGENTUSER_ISSTART = 1");
		together.append(" AND agef.ALLPAY_LOGICDEL = '1'");
		together.append(" AND ageg.ALLPAY_LOGICDEL = '1' ");
		together.append(" AND age.AGENTUSER_NAME = '").append("hongw").append("'");
		oracle_rs = oracle_stmt.executeQuery(together.toString());
		Map<String, Object> result = new HashMap<String, Object>();
		while (oracle_rs.next()) {
			//transferData(result, oracle_rs);
		}
		System.out.println(result);
	}
}
