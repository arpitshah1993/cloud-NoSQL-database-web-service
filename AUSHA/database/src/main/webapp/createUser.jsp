<html>
<head>
<script>
function clickEvent() {
	 var pass1 = document.getElementById("pass1").value;
	    var pass2 = document.getElementById("pass2").value;
	    var ok = true;
	    if (pass1 != pass2) {
	        alert("Passwords Do not match");
	        document.getElementById("pass1").style.borderColor = "#E34234";
	        document.getElementById("pass2").style.borderColor = "#E34234";
	        ok = false;
	        return;
	    }
	    
	    
	/*     var data = {};
	     data["Username"]=document.getElementById("Username").value;
	    data["Password"]=document.getElementById("pass1").value;
	    data["Permission"]=document.getElementById("perm").value;  */
	    
	///var data = "{ username :" + document.getElementById("Username").value+", password:"+document.getElementById("pass1").value+", permission :"+ document.getElementById("perm").value +"}";
	
	var authName=document.getElementById("AuthName").value;
	var authPass=document.getElementById("AuthPass").value;
	
	var authorizationBasic = btoa( authName+ ":" +authPass );
	document.getElementById("result").innerHTML="Working...";
    var url="http://localhost:8080/database/api/db/createUser";//+document.getElementById("Username").value+":"+document.getElementById("pass1").value;
     var postData = {
    		 username: document.getElementById("Username").value,
    		 password: document.getElementById("pass1").value,
    		 permission: document.getElementById("perm").value
    		} ;
    var request = new XMLHttpRequest();
	request.onreadystatechange = function() {
	    if (request.readyState === 4) {
	        if (request.status === 200) {
	        	var val=document.getElementById("result").value;
	        	document.getElementById("result").innerHTML=request.responseText;
	        } else {
	        	document.getElementById("result").innerHTML="Error Occurred.\n"+request.responseText;
	        }
	    }
	};
	request.open("POST", url , true);
	request.setRequestHeader("Content-type", "text/plain");
	//request.setRequestHeader("body", "{}");
	//http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	request.setRequestHeader('Authorization', 'Basic ' + authorizationBasic);
	request.setRequestHeader('Accept', 'text/plain');
	//request.send(JSON.stringify('{ "username" :"' + document.getElementById("Username").value+'", "password":"'+document.getElementById("pass1").value+'", "permission" :"'+ document.getElementById("perm").value +'"}'));
	request.send(JSON.stringify(postData));
}
</script>
</head>
<body>

<h3>AUSHA</h3>
<h2>NoSQL Pluggable Database</h2>
Username:<br>
  <input type="text" id="Username" name="Username" value="">
  <br>
 Password:<br>
  <input type="password" name="Password" id="pass1" value="">
  <br>
 Confirm Password:<br>
  <input type="password" name="ConFirmPassword" id="pass2" value="">
  <br>
  Permission-Level:<br>
  <input type="text" id="perm" name="Permission" value="">
  <br>
  <br>
 Privileged User:<br>
<input type="text" id="AuthName" name="AuthName" placeholder="Username"><input type="password" id="AuthPass" name="AuthPass" placeholder="Password"><button id="button" onclick='clickEvent()'> Create </button>
<p id="result">
</p> 
</body>
</html>
 