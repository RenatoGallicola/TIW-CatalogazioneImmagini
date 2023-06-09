/**
 * Utility functions
 */

function makeCall(method, url, formElement, cback, reset = true) {
	var req = new XMLHttpRequest(); // visible by closure
    req.onreadystatechange = function() {
      cback(req)
    }; // closure
    req.open(method, url);
    if (formElement == null) {
      req.send();
    } else {
      req.send(new FormData(formElement));
    }
    if (formElement !== null && reset === true) {
      formElement.reset();
    }
}
	  
function createForm(fields){
	var f = document.createElement("form");
	
	fields.forEach(el => {
		var n = el[0];
		var v = el[1];
		
		var i = document.createElement("input");
		i.name = n;
		i.value = v;
		i.id = n;
		
		f.appendChild(i);
		});
		
	return f;
}