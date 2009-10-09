	/*EditArea.prototype.comment_or_quotes= function(v0, v1, v2, v3, v4,v5,v6,v7,v8,v9, v10){
		new_class="quotes";
		if(v6 && v6 != undefined && v6!="")
			new_class="comments";
		return "µ__"+ new_class +"__µ"+v0+"µ_END_µ";

	};*/
	
/*	EditArea.prototype.htmlTag= function(v0, v1, v2, v3, v4,v5,v6,v7,v8,v9, v10){
		res="<span class=htmlTag>"+v2;
		alert("v2: "+v2+" v3: "+v3);
		tab=v3.split("=");
		attributes="";
		if(tab.length>1){
			attributes="<span class=attribute>"+tab[0]+"</span>=";
			for(i=1; i<tab.length-1; i++){
				cut=tab[i].lastIndexOf("&nbsp;");				
				attributes+="<span class=attributeVal>"+tab[i].substr(0,cut)+"</span>";
				attributes+="<span class=attribute>"+tab[i].substr(cut)+"</span>=";
			}
			attributes+="<span class=attributeVal>"+tab[tab.length-1]+"</span>";
		}		
		res+=attributes+v5+"</span>";
		return res;		
	};*/
	
	// determine if the selected text if a comment or a quoted text
	EditArea.prototype.comment_or_quote= function(){
		var new_class="";
		var close_tag="";
		for(var i in parent.editAreaLoader.syntax[editArea.current_code_lang]["quotes"]){
			if(EditArea.prototype.comment_or_quote.arguments[0].indexOf(i)==0){
				new_class="quotesmarks";
				close_tag=parent.editAreaLoader.syntax[editArea.current_code_lang]["quotes"][i];
			}
		}
		if(new_class.length==0)
		{
			for(var i in parent.editAreaLoader.syntax[editArea.current_code_lang]["comments"]){
				if(EditArea.prototype.comment_or_quote.arguments[0].indexOf(i)==0){
					new_class="comments";
					close_tag=parent.editAreaLoader.syntax[editArea.current_code_lang]["comments"][i];
				}
			}
		}
		// for single line comment the \n must not be included in the span tags
		if(close_tag=="\n"){
			return "µ__"+ new_class +"__µ"+EditArea.prototype.comment_or_quote.arguments[0].replace(/(\r?\n)?$/m, "µ_END_µ$1");
		}else{
			// the closing tag must be set only if the comment or quotes is closed 
			reg= new RegExp(parent.editAreaLoader.get_escaped_regexp(close_tag)+"$", "m");
			if(EditArea.prototype.comment_or_quote.arguments[0].search(reg)!=-1)
				return "µ__"+ new_class +"__µ"+EditArea.prototype.comment_or_quote.arguments[0]+"µ_END_µ";
			else
				return "µ__"+ new_class +"__µ"+EditArea.prototype.comment_or_quote.arguments[0];
		}
	};
	
/*
	// apply special tags arround text to highlight
	EditArea.prototype.custom_highlight= function(){
		res= EditArea.prototype.custom_highlight.arguments[1]+"µ__"+ editArea.reg_exp_span_tag +"__µ" + EditArea.prototype.custom_highlight.arguments[2]+"µ_END_µ";
		if(EditArea.prototype.custom_highlight.arguments.length>5)
			res+= EditArea.prototype.custom_highlight.arguments[ EditArea.prototype.custom_highlight.arguments.length-3 ];
		return res;
	};
	*/
	
	// return identication that allow to know if revalidating only the text line won't make the syntax go mad
	EditArea.prototype.get_syntax_trace= function(text){
		if(this.settings["syntax"].length>0 && parent.editAreaLoader.syntax[this.settings["syntax"]]["syntax_trace_regexp"])
			return text.replace(parent.editAreaLoader.syntax[this.settings["syntax"]]["syntax_trace_regexp"], "$3");
	};
	
		
	EditArea.prototype.colorize_text= function(text){
		//text="<div id='result' class='area' style='position: relative; z-index: 4; height: 500px; overflow: scroll;border: solid black 1px;'> ";
	  /*		
		if(this.nav['isOpera']){	
			// opera can't use pre element tabulation cause a tab=6 chars in the textarea and 8 chars in the pre 
			text= this.replace_tab(text);
		}*/
		
		text= " "+text; // for easier regExp
		
		/*if(this.do_html_tags)
			text= text.replace(/(<[a-z]+ [^>]*>)/gi, '[__htmlTag__]$1[_END_]');*/
		if(this.settings["syntax"].length>0)
			text= this.apply_syntax(text, this.settings["syntax"]);
		/*for(var lang in this.settin){
			text=this.apply_syntax(text, lang);
		}*/
		
		text= text.substr(1);	// remove the first space added		
		text= text.replace(/&/g,"&amp;");
		text= text.replace(/</g,"&lt;");
		text= text.replace(/>/g,"&gt;");	// no need if there is no <		
		//text= text.replace(/ /g,"&nbsp;");
		text= text.replace(/µ_END_µ/g,"</span>");
		text= text.replace(/µ__([a-zA-Z0-9]+)__µ/g,"<span class='$1'>");
		
		
		//text= text.replace(//gi, "<span class='quote'>$1</span>");
		//alert("text: \n"+text);
		
		return text;
	};
	
	EditArea.prototype.apply_syntax= function(text, lang){
		this.current_code_lang=lang;
	
		if(!parent.editAreaLoader.syntax[lang])
			return text;
	
		/*alert(typeof(text)+"\n"+text.length);
		
		var parse_index=0;
		for(var script_start in this.code[lang]["script_delimiters"]){
			var pos_start= text.indexOf(script_start);
			var pos_end= text.length;	// MUST BE SET TO CORRECT VAL!!!
			if(pos_start!=-1){
				var start_text=text.substr(0, pos_start);
				var middle_text= text.substring(pos_start, pos_end);
				var end_text= text.substring(pos_end);
				if(this.code[lang]["comment_or_quote_reg_exp"]){
					//setTimeout("document.getElementById('debug_area').value=editArea.comment_or_quote_reg_exp;", 500);
					middle_text= middle_text.replace(this.code[lang]["comment_or_quote_reg_exp"], this.comment_or_quote);
				}
				
				if(this.code[lang]["keywords_reg_exp"]){
					for(var i in this.code[lang]["keywords_reg_exp"]){	
						this.reg_exp_span_tag=i;
						middle_text= middle_text.replace(this.code[lang]["keywords_reg_exp"][i], this.custom_highlight);			
					}			
				}
				
				if(this.code[lang]["delimiters_reg_exp"]){
					middle_text= middle_text.replace(this.code[lang]["delimiters_reg_exp"], 'µ__delimiters__µ$1µ_END_µ');
				}		
				
				if(this.code[lang]["operators_reg_exp"]){
					middle_text= middle_text.replace(this.code[lang]["operators_reg_exp"], 'µ__operators__µ$1µ_END_µ');
				}
			}
			text= start_text+ middle_text + end_text;
		}*/
		if(parent.editAreaLoader.syntax[lang]["custom_regexp"]['before']){
			for( var i in parent.editAreaLoader.syntax[lang]["custom_regexp"]['before']){
				var convert="$1µ__"+ parent.editAreaLoader.syntax[lang]["custom_regexp"]['before'][i]['class'] +"__µ$2µ_END_µ$3";
				text= text.replace(parent.editAreaLoader.syntax[lang]["custom_regexp"]['before'][i]['regexp'], convert);
			}
		}
		
		if(parent.editAreaLoader.syntax[lang]["comment_or_quote_reg_exp"]){
			//setTimeout("document.getElementById('debug_area').value=editArea.comment_or_quote_reg_exp;", 500);
			text= text.replace(parent.editAreaLoader.syntax[lang]["comment_or_quote_reg_exp"], this.comment_or_quote);
		}
		
		if(parent.editAreaLoader.syntax[lang]["keywords_reg_exp"]){
			for(var i in parent.editAreaLoader.syntax[lang]["keywords_reg_exp"]){	
				/*this.reg_exp_span_tag=i;
				text= text.replace(parent.editAreaLoader.syntax[lang]["keywords_reg_exp"][i], this.custom_highlight);			
				*/
				text= text.replace(parent.editAreaLoader.syntax[lang]["keywords_reg_exp"][i], 'µ__'+i+'__µ$2µ_END_µ');
			}			
		}
		
		if(parent.editAreaLoader.syntax[lang]["delimiters_reg_exp"]){
			text= text.replace(parent.editAreaLoader.syntax[lang]["delimiters_reg_exp"], 'µ__delimiters__µ$1µ_END_µ');
		}		
		
		if(parent.editAreaLoader.syntax[lang]["operators_reg_exp"]){
			text= text.replace(parent.editAreaLoader.syntax[lang]["operators_reg_exp"], 'µ__operators__µ$1µ_END_µ');
		}
		
		if(parent.editAreaLoader.syntax[lang]["custom_regexp"]['after']){
			for( var i in parent.editAreaLoader.syntax[lang]["custom_regexp"]['after']){
				var convert="$1µ__"+ parent.editAreaLoader.syntax[lang]["custom_regexp"]['after'][i]['class'] +"__µ$2µ_END_µ$3";
				text= text.replace(parent.editAreaLoader.syntax[lang]["custom_regexp"]['after'][i]['regexp'], convert);			
			}
		}
			
		return text;
	};
