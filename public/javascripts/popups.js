
/* Dims the background */
function toggle_background(dim){
	if(dim == true)
		document.getElementsByName("darkBackgroundLayer")[0].style.display = "";
	else
		document.getElementsByName("darkBackgroundLayer")[0].style.display = "none";
}

/* What happens when the login button is clicked */
function login(){
	return function(){
		/* Dim the background */
		toggle_background(true);
		
		/* Present popup screen */
		document.getElementById("popup").style.display = "";

		/* Present the login */
		document.getElementById("login").style.display = "";
	}
}

/* Used to exit the login sequence */
function exit_login(){
	return function(){
		/* Dim the background */
		toggle_background(false);
		
		/* Hide popup screen */
		document.getElementById("popup").style.display = "none";

		/* Hide the login */
		document.getElementById("login").style.display = "none";
	}
}

function register(){
	return function(){
		/* Dim the background */
		toggle_background(true);
		
		/* Present register screen */
		document.getElementById("popup").style.display = "";
		
		/* Show the register */
		document.getElementById("register").style.display = "";
	}
}

function exit_register(){
	return function(){
		/* Dim the background */
		toggle_background(false);
		
		/* Hide register screen */
		document.getElementById("popup").style.display = "none";
		
		/* Hide the register */
		document.getElementById("register").style.display = "none";
	}
}

function edit_item(id){
	return function(){
		console.log("Editing an item");
		/* Dim the background */
		toggle_background(true);
		
		/* Present register screen */
		document.getElementById("popup").style.display = "";
		
		/* Show the edit item */
		
		/* Create the necessary inputs */
		var idim = document.createElement("input");
		idim.setAttribute("type", "hidden");
		idim.setAttribute("id", "item_id_m");
		idim.setAttribute("name", "id");
		idim.setAttribute("value", id);
		
		var idir = document.createElement("input");
		idir.setAttribute("type", "hidden");
		idir.setAttribute("id", "item_id_r");
		idir.setAttribute("name", "id");
		idir.setAttribute("value", id);
		
		document.getElementById("edit_item_form").appendChild(idim);
		document.getElementById("remove_item_form").appendChild(idir);
		
		document.getElementById("edit_item").style.display = "";
	}
}

function exit_edit_item(){
	return function(){
		/* Dim the background */
		toggle_background(false);
		
		/* Hide register screen */
		document.getElementById("popup").style.display = "none";
		
		/* Hide the edit item */
		
		/* Remove all the inputs */
		var idim = document.getElementById("item_id_m");
		idim.parentNode.removeChild(idim);
		var idr = document.getElementById("item_id_r");
		idr.parentNode.removeChild(idr);
		
		/* Remove all the previous values written */
		document.getElementById("edit_name").value ="";
		document.getElementById("edit_quantity").value ="";
		
		document.getElementById("edit_item").style.display = "none";
	}
}