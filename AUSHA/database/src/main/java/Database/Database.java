package Database;
import CollectionManager.Collection;
import java.util.concurrent.Future;
import CustomException.CollectionAlreadyPresent;
import CustomException.GException;
import CustomException.InvalidCollection;
import CustomException.InvalidSyntax;
import KeyCreator.iBigInteger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.json.JSONArray;

public class Database {

	//private iBigInteger id;
	private HashMap<String, Collection> dataCollectionMap;
	private String databaseName;

	public Database(String databaseName) throws GException, InvalidSyntax, InterruptedException, ExecutionException{
		this.databaseName=databaseName;
		File dir = new File(databaseName);
		dir.mkdir();
		dataCollectionMap=new HashMap<>();
		initializer(); 
	}

	//delete the current database
	public void deleteDatabase() throws IOException{
		FileUtils.deleteDirectory(new File(databaseName));
	}
	
	//return the status of collection existence
	public boolean isCollectionExist(String collectionName) throws InvalidCollection{
		if(dataCollectionMap.containsKey(collectionName))
			return true;
		else
			throw new InvalidCollection();
	}

	//initialize the database properties
	public void initializer() throws GException, InvalidSyntax, InterruptedException, ExecutionException{
		//try {
		ExecutorService executor=Executors.newFixedThreadPool(8);
		List<Future<Boolean>> lis=new ArrayList<>();
			//read all collection folder from Database folder and set default database
			File[] directories = new File(databaseName).listFiles(File::isDirectory);
			for(File directory: directories){
				lis.add(executor.submit(()->{
					try {
						Collection newCollection = new Collection(directory.getName(),this.databaseName);
						dataCollectionMap.put(directory.getName(), newCollection);	
					   } catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return true;
				}));
				
	//		}
//			//set the last running key
//			String text = new String(Files.readAllBytes(Paths.get("Key.txt")), StandardCharsets.UTF_8);
//			id=new iBigInteger(text.replace("\n", "").replace("\r", ""));
		}
			for(Future<Boolean> f:lis) f.get();
			executor.shutdown();
//		catch (IOException e) {
//			throw new GException();
//		}
	}

	//create the collection and its folder
	public boolean createCollection(String collectionName) throws InvalidSyntax, CollectionAlreadyPresent{
		if(dataCollectionMap.containsKey(collectionName))
			throw new CollectionAlreadyPresent();
		Collection newCollection= new Collection(collectionName,databaseName);
		dataCollectionMap.put(collectionName, newCollection);
		(new File(databaseName+"/"+collectionName)).mkdir();
		return true;
	}

	//insert the data into given collection
	public boolean insert(String collectionName, String dataEntry) throws InvalidSyntax {
		return dataCollectionMap.get(collectionName).insert(dataEntry);
	}

	//delete the data from given collection
	public String delete(String collectionName, String deleteData) throws InvalidSyntax{
		return dataCollectionMap.get(collectionName).delete(deleteData);
	}
	
	//delete the data from given collection
		public String createIndex(String collectionName, String entity) throws InvalidSyntax{
			return dataCollectionMap.get(collectionName).createIndex(entity);
		}

	//update the data by condition on given collection
	public String update(String collectionName, String updateQuery) throws InvalidSyntax{	
		return dataCollectionMap.get(collectionName).update(updateQuery);
	}

	//select the data by condition and return data in JSON format
	public JSONArray select(String collectionName, String dataQuery) throws InvalidSyntax{		
		return dataCollectionMap.get(collectionName).Select(dataQuery.equals("")?"{}":dataQuery);
	}
	
	//delete collection from data collection
	public boolean deleteCollection(String collectionName){
		dataCollectionMap.remove(collectionName);
		(new File(databaseName+"/"+collectionName)).delete();
		return true;
	}

	//print the collection
	public void print(String collectionName){
		if(dataCollectionMap.containsKey(collectionName)){
			dataCollectionMap.get(collectionName).printAllData();;
		}
	}
	
	//print the collection
		public void printIndedx(String collectionName){
			if(dataCollectionMap.containsKey(collectionName)){
				dataCollectionMap.get(collectionName).print();
			}
		}
}


