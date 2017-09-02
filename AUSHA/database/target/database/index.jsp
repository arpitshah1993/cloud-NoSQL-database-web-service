<!-- <html>
<body>
    <h2>Jersey RESTful Web Application!</h2>
    <p><a href="webapi/myresource">Jersey resource</a>
    <p>Visit <a href="http://jersey.java.net">Project Jersey website</a>
    for more information on Jersey!
</body>
</html>
 -->
<html>
<head>
<script>
function clickEvent() {
    var url="http://localhost:8080/database/api/db/"+document.getElementById("query").value;
    document.getElementById("result").value="Working...";
	var request = new XMLHttpRequest();
	request.onreadystatechange = function() {
	    if (request.readyState === 4) {
	        if (request.status === 200) {
	        	var val=document.getElementById("result").value;
	        	document.getElementById("result").value=request.responseText;
	        } else {
	        	document.getElementById("result").value="Error Occurred";
	        }
	    }
	};
	request.open("GET", url , true);
	request.send(null);
}
</script>
</head>
<body>
<u><h3>AUSHA</h3></u>
<h2>NoSQL Pluggable Database</h2>
Enter your query:<br>
<textarea id="query" type="text"  rows="4" cols="90" value="">
</textarea>
<br>
<button id="button0" onclick='clickEvent()'> Execute </button>
<br>
Resukllt<br>
<textarea id="result" type="text"  rows="4" cols="90" value="">
<br>
<br>
<br>
Not have an account? Want to add user?<br>
<br>
</textarea>
<a href="/createUser.jsp">Visit our HTML tutorial</a>
</body>
</html>
 