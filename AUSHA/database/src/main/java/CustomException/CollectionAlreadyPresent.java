package CustomException;

public class CollectionAlreadyPresent extends Exception{
	public CollectionAlreadyPresent(){
		super("Collection already exist.");
	}
}