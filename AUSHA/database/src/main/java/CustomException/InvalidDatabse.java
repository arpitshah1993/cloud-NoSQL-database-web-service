package CustomException;

public class InvalidDatabse extends Exception{
	public InvalidDatabse(){
		super("Database doesn't Exist");
	}
}

