package Database;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import CustomException.GException;
import CustomException.InvalidCollection;
import CustomException.InvalidDatabse;
import CustomException.InvalidSyntax;

public class DatabaseFactory {
	private static HashMap<String,Database> databaseInstances=new HashMap<String,Database>();
	private DatabaseFactory(){
		
	}
	
	public static boolean isDatabaseExist(String databaseName) throws InvalidDatabse{
		if(new File(databaseName).exists())
			return true;
		else
			throw new InvalidDatabse();
	}

	public static Database getDatabase(String databaseName,boolean isCreateDatabase) throws GException, InvalidSyntax, InvalidDatabse, InterruptedException, ExecutionException{
		if(!isCreateDatabase){
			DatabaseFactory.isDatabaseExist(databaseName);
			if(!databaseInstances.containsKey(databaseName))	
				databaseInstances.put(databaseName, new Database(databaseName));
			return databaseInstances.get(databaseName);
		}
			Database db=new Database(databaseName);
			databaseInstances.put(databaseName, db);
			return db;
	}
	
	public static Database deleteDatabase(String databaseName) throws InvalidDatabse, GException, InvalidSyntax, InterruptedException, ExecutionException, IOException{

			DatabaseFactory.isDatabaseExist(databaseName);
			if(databaseInstances.containsKey(databaseName))	
				databaseInstances.remove(databaseName);

			Database db=new Database(databaseName);
			db.deleteDatabase();
			return db;
	}
}
