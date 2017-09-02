package CustomException;

public class InvalidSyntax extends Exception{
	public InvalidSyntax(){
		super("Invalid JSON input provided for current Operation!");
	}
}
