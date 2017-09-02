package KeyCreator;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

public class iBigInteger {
	static BigInteger data;
	
	public iBigInteger(String sdata){
		data=new BigInteger(sdata);
	}
	public synchronized static BigInteger getId(){
		data=data.add(BigInteger.ONE);
		return data;
	}
	
	public static void terminate() {
		
		try {
			FileWriter out = new FileWriter("Key.txt");
			out.write(data.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
