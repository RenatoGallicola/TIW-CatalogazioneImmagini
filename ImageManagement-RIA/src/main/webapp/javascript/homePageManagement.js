{
	
	let form;
	let tree;
	let pageOrchestrator = new PageOrchestrator(); // main controller
	
	
	//------------------------------------------------------------------------------------
	//SETUP:
	window.addEventListener("load", () => {
	    if (sessionStorage.getItem("username") == null) {
	      window.location.href = "index.html";
	    } 
	    else 
	    {
	      pageOrchestrator.start(); // initialize the components
	      pageOrchestrator.refresh();
	      
	      //Setup username:
	      var node = document.createTextNode(sessionStorage.getItem('username'));
	      document.getElementById("username").appendChild(node);
	      	
	      	
	      //Setup listener logout:
	      document.getElementById("logout").addEventListener("click", (e) =>
		  {
			  makeCall("POST", 'CheckLogout', null,
			  function(x) {
		          if (x.readyState == XMLHttpRequest.DONE) {
		            //var message = x.responseText;
		             window.location.href = "index.html";
		          }
		  	  }
		  	  );
	      });
	      
	    } // display initial content
	  }, false);
	
	//------------------------------------------------------------------------------------
	
	
	//OBJECTS OF THE PAGE:
	function formObj(_createCategoryForm, _errorDiv, _nameField, _categoryIdForm, _submitForm)
	{
		//All the objects owned by the form:
		this.errorDiv = _errorDiv;
		this.nameField = _nameField;
		this.categoryIdForm = _categoryIdForm;
		this.submitForm = _submitForm;
		this.createCategoryForm = _createCategoryForm;
		var self = this;
		
		//When create category send message to server:
		this.createCategoryForm.addEventListener('submit', (e) => {
			 e.preventDefault();
			 var form = e.target.closest("form");
			 if (form.checkValidity()) {
				 
				 makeCall("POST", 'CreateCategory', e.target.closest("form"), function(x) {
					 if (x.readyState == XMLHttpRequest.DONE)
					 {
						 var message = x.responseText;
						 switch (x.status)
						 {
							 case 200: //ok
							 	self.show();
							 	break;
							 	
							 case 400: //bad request
							 
								
								self.updateError(self.errorDiv, message);
							 	break;
						 }
					 }
				 })
				 
			}
			else
			{
				form.reportValidity();
			}
		
		//end addEventListener
		})
		
		
		//When insertion category fails
		this.updateError = function(div, message)
		{
			if(div.className !== "error_div")
          	{
				div.className = "error_div";
				
              	var lab1 = document.createElement("label");
              	lab1.className = "error_symbol";
              	
              	var content1 = document.createTextNode('\u26A0');
              	lab1.appendChild(content1);
              	
              	var span = document.createElement("span");
              	
              	var lab2 = document.createElement("label");
              	lab2.className = "error_label";
              	
              	var content2 = document.createTextNode(message);
              	lab2.appendChild(content2);
              	
              	div.appendChild(lab1);
              	div.appendChild(span);
              	div.appendChild(lab2);
			}
		//end updateError
		}
	
	
		//Get all categories from server
		this.show = function(){
			
			 makeCall("GET", 'GetCategories', null, function(x) {
					 if (x.readyState == XMLHttpRequest.DONE)
					 {
						 switch (x.status)
						 {
							 case 200: //ok
								 var categories = JSON.parse(x.responseText);
								 if(categories.length != 0)
								 {
									 self.update(categories);
								 }
								 else
								 {
									 alert("error download categories");
								 }
							 	break;
							 	
							 case 400: //bad request
							 	alert("bad request during downloading categories");
							 	break;
						 }
					 }
				 })
				 
		//end show function
		}
	
	
		//Update UI with categories got from server
		this.update = function(categories){
			
			var obj;
			var text;
			categories.forEach(function(category)
			{
				obj = document.createElement('option');
				obj.value = category.id;
				//obj.innerHTML = category.id + category.name;
				text = document.createTextNode(category.id +" - "  + category.name);
				obj.appendChild(text);
				self.categoryIdForm.appendChild(obj);
				
			});
			
		//end update function
		}
	
	
	//end form constructor
	}
	
	function Tree(_treeDivObj)
	{
		
		this.treeDivObj = _treeDivObj;
		var self = this;
		
		
		//Get all categories and subtrees from server
		this.show = function(){
			
			 makeCall("GET", 'GetTree', null, function(x) {
					 if (x.readyState == XMLHttpRequest.DONE)
					 {
						 switch (x.status)
						 {
							 case 200: //ok
								 var categories = JSON.parse(x.responseText);
								 if(categories.length != 0)
								 {
									 //self.treeDivObj.className = "treeDiv";
									 self.update(categories, self.treeDivObj);
									 //alert(categories[0].id);
								 }
								 else
								 {
									 alert("error download categories and subtrees");
								 }
							 	break;
							 	
							 case 400: //bad request
							 	alert("bad request during downloading categories");
							 	break;
						 }
					 }
				 })
				 
		//end show function
		}
		
		
		//Update UI with categories got from server
		this.update = function(categories, fatherTag){
			
			var ulTag;
			//var self = this;
			if(fatherTag!== null && categories!== null && categories!== undefined)
			{
				//Create tag <ul>
				ulTag = document.createElement('ul');
				fatherTag.appendChild(ulTag);
				
				//if(fatherTag === this.treeDivObj)
				//{
					//ulTag.className = "categoryTree";
				//}	
				
				//For each top Category create a subTree:
				categories.forEach(function(category)
				{
					//insert now list items:
					var liTag = document.createElement('li');
					ulTag.appendChild(liTag);
					
					//liTag.className = "subCategory";
					liTag.appendChild(document.createTextNode(category.id +" - "  + category.name));
					
					self.update(category.subCategories, liTag);
				});
			}
			
		//end update function
		}
	}
	
	
	
	 function PageOrchestrator()
	 {
		 this.start = function()
		 {
			 form = new formObj(
				 document.getElementById("createCategory"),
				 document.getElementById("error_div"),
				 document.getElementById("nameField"),
				 document.getElementById("categoryIdForm"),
				 document.getElementById("submitForm")
			 );
			 
			 tree = new Tree(
				 document.getElementById("treeDivObj")
				 );
		 }
		 
		 
		 this.refresh = function()
		 {
			 form.show();
			 tree.show();
		 }
	 }
}