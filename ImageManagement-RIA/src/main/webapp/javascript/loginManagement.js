/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

  document.getElementById("loginForm").addEventListener('click', (e) => {
	  
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
                  document.getElementById("loginError").textContent = message;
                  document.getElementById("ErrorBox").className = "error_div_show";
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