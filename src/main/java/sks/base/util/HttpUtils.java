package sks.base.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sks.base.Starter;

public class HttpUtils {
	
	private static Logger logger = LoggerFactory.getLogger(Starter.class); 
	private static int sockectTimeout = 5000;
	private static int connTimeout= 3000;
	public static final String CONTENT_TYPE = "application/json";
	public static void initConfig(HttpRequestBase method){
		
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(sockectTimeout).setConnectTimeout(connTimeout).build();
		
		method.setConfig(requestConfig);
		
	}
	
	public static String requestPost(String wechatUrl ,String param) throws ClientProtocolException,ParseException,IOException,Exception{
		
		CloseableHttpClient httpclient = HttpClients.createDefault(); 
		CloseableHttpResponse response = null; 
		HttpPost httpPost = null;
        
		try {  
            // 创建httpPost
			httpPost = new HttpPost(wechatUrl);
             
            initConfig(httpPost);
            
            logger.info("executing request " + httpPost.getURI()); 
            
            StringEntity se = new StringEntity(param);
            se.setContentType(CONTENT_TYPE);
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, CONTENT_TYPE));
            httpPost.setEntity(se);
            
            // 执行post请求.    
            response = httpclient.execute(httpPost);  
         
            // 获取响应实体    
            HttpEntity entity = response.getEntity(); 
            if (entity != null) {  
                // 打印响应内容长度    
            	logger.info("Response content length: " + entity.getContentLength());  
                 
            	String result =  EntityUtils.toString(entity);
            	// 打印响应内容   
            	logger.info("Response content: " + result);
            	
            	return result;
            	
            }else{
            	throw  new  Exception(" response string error ");
            }  
        
        } catch (ClientProtocolException e) {  
            
        	if(httpPost!=null)
        		httpPost.abort();
        	throw  new  ClientProtocolException("httpclient Protocol error : ",e);
        
        } catch (ParseException e) {  
        	
        	if(httpPost!=null)
        		httpPost.abort();
        	throw new ParseException(" httpclient ParseException error ");
        
        } catch (IOException e) {  
        	
        	if(httpPost!=null)
        		httpPost.abort();
        	throw  new  IOException("httpclient IOException error : ",e);
        
        } finally {
        	
        	if(response != null){  		
        		try {
					response.close();
				} catch (IOException e) {

					 logger.error("response close error : ",e);
				}	
        	}
        	
            // 关闭连接,释放资源    
            try {  
                httpclient.close();  
            } catch (IOException e) {  
               logger.error("httpclient close error : ",e);
            }  
        }  
       
	}
	
	
	
	/**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, Map<String, Object> param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
//            conn.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式  
//            conn.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式  
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<String, Object> entry : param.entrySet()) {
            	sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            
            // 发送请求参数
            out.print(sb);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
        	out.close();
        	try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return result;
    } 

}
