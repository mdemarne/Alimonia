/* What happens when the add ingredient button is clicked */
var id = 0;
function createIngredientDiv(nameArg, quantityArg, unitsArg) {

		var name = document.createElement("div");
		name.setAttribute("class", "name");
		name.innerHTML = nameArg;

		var quantity = document.createElement("div");
		quantity.setAttribute("class", "quantity");
		quantity.innerHTML = quantityArg + " " + unitsArg;

		var namef = document.createElement("input");
		namef.setAttribute("type", "hidden");
		namef.setAttribute("name", "ingr_name[" + id + "]");
		namef.setAttribute("value", nameArg);

		var quanf = document.createElement("input");
		quanf.setAttribute("type", "hidden");
		quanf.setAttribute("name", "ingr_quantity[" + id + "]");
		quanf.setAttribute("value", quantityArg);

		var unitsf = document.createElement("input");
		unitsf.setAttribute("type", "hidden");
		unitsf.setAttribute("name", "ingr_units[" + id + "]");
		unitsf.setAttribute("value", unitsArg);

		var close = document.createElement("img");
		close.setAttribute("class", "item_close_button");
		close.setAttribute("name", name);
		close.src = "/assets/images/close_icon_gray_x.gif";

		var item = document.createElement("div");
		item.setAttribute("class", "item");
		item.setAttribute("id", "item_" + nameArg);

		item.appendChild(name);
		item.appendChild(quantity);
		item.appendChild(close);
		item.appendChild(namef);
		item.appendChild(quanf);
		item.appendChild(unitsf);

		document.getElementById("items").appendChild(item);
		close.addEventListener("click", removeingredient(item));
		id++;
};
function addingredient() {
	return function() {
		
		var name = document.getElementById("insert_item_name").value;
		var quantity = document.getElementById("insert_item_quantity").value;
		var units = document.getElementById("insert_item_units").value;
		
		if (document.getElementById("insert_item_quantity").value > 0) {

			createIngredientDiv(name, quantity, units);

			document.getElementById("insert_item_name").value = "";
			document.getElementById("insert_item_quantity").value = "";
		} else {
			alert("quantity should be larger than 0 !");
			document.getElementById("insert_item_quantity").value = "";
		}
	}
}

/* What happens when the close ingredient button is clicked */
function removeingredient(item) {
	return function() {
		console.log(item);
		document.getElementById("items").removeChild(item);
	}
}

window.onload = function() {

};