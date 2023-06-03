/**
 * ERROR PAGE HANDLER
 */

{
	window.addEventListener("load", () => {
		if (sessionStorage.getItem("username") == null) {
			let h = document.getElementsByClassName("home")[0];
			h.classList.remove("home");
			h.classList.add("disabled");
			
			let l = document.getElementsByClassName("logout")[0];
			l.href = "/ImageManagement-RIA/index.html";
			l.innerHTML = "INDEX";
		} else {
			document.getElementsByClassName("home")[0].addEventListener("click", (e) => {
				e.preventDefault();
				window.location.href = "/ImageManagement-RIA/home.html";
			});
			
			let l = document.getElementsByClassName("logout")[0];
			l.href = "/ImageManagement-RIA/CheckLogout";
			l.addEventListener("click", () => {
				window.sessionStorage.removeItem("username");
			});
		}
	}, false);
}