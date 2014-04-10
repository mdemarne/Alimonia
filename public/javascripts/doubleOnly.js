/* Just to ensure a quantity which could be an integer or a double */
function doubleOnly(myfield, e) {
	var lst = /[0-9\.]/g;
	if (!e) var e = window.event
	if (e.keyCode) code = e.keyCode;
	else if (e.which) code = e.which;
	var character = String.fromCharCode(code);
	if (!e.ctrlKey && code!=9 && code!=8 && code!=36 && code!=37 && code!=38 && (code!=39 || (code==39 && character=="'")) && code!=40) {
		if (character.match(lst)) {
			return true;
		} else {
			return false;
		}
		
	}
}