package APICallHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.util.Base64;

import Authorizor.UserData;


@Provider
public class AuthorizationFilter implements ContainerRequestFilter{
	
	private final String AUTHERIZATION_HEADER_KEY="Authorization";
	private final String AUTHERIZATION_HEADER_PREFIX="Basic ";

	private  UserData user=UserData.getInstance();
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		System.out.println("Filter called");
		try{
			List<String> authHeader=requestContext.getHeaders().get(AUTHERIZATION_HEADER_KEY);
			if(authHeader!=null && authHeader.size()>0){
				String ttp=authHeader.get(0).replace(AUTHERIZATION_HEADER_PREFIX, "");
				String temp=Base64.decodeAsString(ttp);
				StringTokenizer tokenizer=new StringTokenizer(temp,":");
				String username=tokenizer.nextToken();
				String password=tokenizer.nextToken();
				System.out.println(username+" "+password);
				System.out.println(requestContext.getUriInfo().getPath());
					if(requestContext.getUriInfo().getPath().contains("createUser")){ 
						if(user.isAdminUserAuthorized(username, password))
							return;
					}
					else if(user.isUserAuthorized(username, password) || user.isAdminUserAuthorized(username, password))
	 					return;
			throw new Exception("");
			}
		}
		catch(Exception e){
			Response unAuthorizedStatus=Response.status(Response.Status.UNAUTHORIZED)
					.entity("Username or Password is wrong OR not authorized for this service!")
					.build();
			requestContext.abortWith(unAuthorizedStatus);
		}
	}
}
