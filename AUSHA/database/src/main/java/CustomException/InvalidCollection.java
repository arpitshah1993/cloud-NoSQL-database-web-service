package CustomException;

public class InvalidCollection extends Exception{
	public InvalidCollection(){
		super("Collection doesn't Exits");
	}
}
