	//javascript console printer;
	/**
	Usage:
	------
	Suppose we want to collect All the names from: https://www.sec.gov/investor/oiepauselist-archive.htm
	Then we need to follow these steps:
	1. Copy following code into the console and execute it (on each page reload we have to do this step again)
	2. Then execute -

  var parentJquerySelector = jQuery("[id*='invAlert']");
  var regOnSelectedElementsWithMinOneGroup = /^(?:[\s\n]*<[^>]+>[\s\n]*)+(\w[^<>]+)[\S\s\n]*/g;
	toTextArea(parentJquerySelector, regOnSelectedElementsWithMinOneGroup);

*/


	(function loadJQ() {
		var e = document.createElement('script');
		e.src = "//code.jquery.com/jquery-latest.min.js";
		e.onload = function() {
			jQuery.noConflict();
			console.log('jQuery loaded');
		};
		document.head.appendChild(e);
	})();

	function toTextArea(parentJquerySelector, regex){
		var text = "";
		var count = 0;
		var $ = jQuery;

		var tArea = $("<textarea id=\"myta_\" rows=\"8\" style=\"margin-left:1%;width:97%\">text here</textarea>")
		if($("#myta_").length<1){
			$("body").prepend(tArea);
		}

		parentJquerySelector.each(function(i){
			var found = false;
			var mVal = "\n";

			$(this).html().replace(regex, function(m, group) {
				mVal += group + " - ";
				found = true;
			});

			if(found){
				count++;
				text += mVal.replace(/[\s\n-]*$/,"");
			}
		});
		text = "Total count: " + count+"\n"+text;
		$("#myta_").text(text);
	}