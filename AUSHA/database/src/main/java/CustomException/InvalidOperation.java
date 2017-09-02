package CustomException;

public class InvalidOperation  extends Exception{
	public InvalidOperation(){
		super("Operation doesn't exist");
	}
}
