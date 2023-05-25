/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

  document.getElementById("loginForm").addEventListener('submit', (e) => {
	  
	e.preventDefault();
    var form = e.target.closest("form");
    if (form.checkValidity()) {
      makeCall("POST", 'CheckLogin', e.target.closest("form"),
        function(x) {
          if (x.readyState == XMLHttpRequest.DONE) {
            var message = x.responseText;
            switch (x.status) {
              case 200:
            	sessionStorage.setItem('username', message);
                window.location.href = "home.html";
                break;
              case 401: // unauthorized
              
              	var div = document.getElementById("errorBox");
              	
              	if(div.className !== "error_div")
              	{
					div.className = "error_div";
					
	              	var lab1 = document.createElement("label");
	              	lab1.className = "error_symbol";
	              	
	              	var content1 = document.createTextNode('\u26A0');
	              	lab1.appendChild(content1);
	              	
	              	var span = document.createElement("span");
	              	
	              	var lab2 = document.createElement("label");
	              	lab2.id = "loginError";
	              	lab2.className = "error_label";
	              	
	              	var content2 = document.createTextNode(message);
	              	lab2.appendChild(content2);
	              	
	              	div.appendChild(lab1);
	              	div.appendChild(span);
	              	div.appendChild(lab2);
				}
				
                break;
            }
          }
        }
      );
    } else {
    	 form.reportValidity();
    }
  });

})();
