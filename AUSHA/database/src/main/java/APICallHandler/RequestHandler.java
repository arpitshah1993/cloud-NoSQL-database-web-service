package APICallHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.JSONObject;

import Authorizor.UserData;
import QueryEngine.Query;

@Path("/db")
public class RequestHandler {
	static Query q=new Query();
	static ExecutorService executor=Executors.newFixedThreadPool(5);
	static UserData user=UserData.getInstance();
	
	@POST
	@Path("/createUser")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.TEXT_PLAIN)
	public String craeteuser(String inputJsonObjString) throws JSONException, IOException{
		System.out.println("creatUser Called");
		System.out.println(inputJsonObjString);
		
		JSONObject inputJsonObj=new JSONObject(inputJsonObjString);
		return user.createUser(inputJsonObj.getString("username"), inputJsonObj.getString("password"), inputJsonObj.getString("permission"));
	}
	
	@GET
	@Path("/{queryString}")
	@Produces(MediaType.APPLICATION_JSON)
	public String executeQuery(@PathParam("queryString") String queryString){
		String s="";
		long startTime=System.currentTimeMillis();
		try{
		try{
			Future<String> res=executor.submit(()->{
				return Query.queryParser(queryString);
			});
			s=res.get();
		}
		catch(Exception e){
			s= e.getMessage();
		}
		}
		catch(Exception e){
			s= e.getMessage();
		}
		return s+System.lineSeparator()+"Execution Time: "+(((long)System.currentTimeMillis())-startTime)+" ms";
	}
}
