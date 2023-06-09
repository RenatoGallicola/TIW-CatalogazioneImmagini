/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

	document.getElementById("loginButton").addEventListener('click', (e) => {
		e.preventDefault();
		var form = document.getElementById("loginForm");
		if (form.checkValidity()) {
			makeCall("POST", 'CheckLogin', form,
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

								if (div.className !== "error_div") {
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

									document.getElementById("text_u").className = "text_error";
									document.getElementById("user").className = "user_error";
									document.getElementById("user").getElementsByTagName("img")[0].className = "img_error";
									document.getElementById("user").getElementsByTagName("span")[0].className = "span_error";
									document.getElementById("user").getElementsByTagName("label")[0].className = "label_error";
									document.getElementById("text_p").className = "text_error";
									document.getElementById("pwd").className = "password_error";
									document.getElementById("pwd").getElementsByTagName("img")[0].className = "img_error";
									document.getElementById("pwd").getElementsByTagName("span")[0].className = "span_error";
									document.getElementById("pwd").getElementsByTagName("label")[0].className = "label_error";
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
