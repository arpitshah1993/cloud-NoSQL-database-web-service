package APICallHandler;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
	
	class ErrorMessage{
		public ErrorMessage(String errorMessage, String uRI, int errorcode) {
			this.errorMessage = errorMessage;
			URI = uRI;
			this.errorcode = errorcode;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		public String getURI() {
			return URI;
		}
		public void setURI(String uRI) {
			URI = uRI;
		}
		public int getErrorcode() {
			return errorcode;
		}
		public void setErrorcode(int errorcode) {
			this.errorcode = errorcode;
		}
		String errorMessage;
		String URI;
		int errorcode;
	}

	@Override
	public Response toResponse(Throwable ex) {
		ErrorMessage errorMessage = new ErrorMessage(ex.getMessage(),"db/api",500);
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(errorMessage)
				.build();
	}

}
