<html>
<head>
<script>
function clickEvent() {
    var url="http://localhost:8080/database/api/db/"+document.getElementById("query").value;
    var authName=document.getElementById("AuthName").value;
	var authPass=document.getElementById("AuthPass").value;
	
	var authorizationBasic = btoa( authName+ ":" +authPass );
	document.getElementById("result").value="Working...";
	var request = new XMLHttpRequest();
	request.onreadystatechange = function() {
	    if (request.readyState === 4) {
	        if (request.status === 200) {
	        	var val=document.getElementById("result").value;
	        	document.getElementById("result").value=request.responseText;
	        } else {
	        	document.getElementById("result").value="Error Occurred. \n"+request.responseText;
	        }
	    }
	};
	request.open("GET", url , true);
	request.setRequestHeader('Authorization', 'Basic ' + authorizationBasic);
	request.setRequestHeader('Accept', 'application/json');
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
 Privileged User:<br>
<input type="text" id="AuthName" name="AuthName" placeholder="Username"><input type="password" id="AuthPass" name="AuthPass" placeholder="Password"><button id="button" onclick='clickEvent()'> Execute </button>
<!-- <br>
<button id="button0" onclick='clickEvent()'> Execute </button> -->
<br>
ResultNN<br>
<textarea id="result" type="text"  rows="4" cols="90" value="">
</textarea>
<br>
<br>
<br>
Not have an account? Want to add user?<br>
<a href="/database/createUser.jsp">Add User</a>
</body>
</html>
 
 
 
 