package org.weceem.export

import org.weceem.content.WcmStatus

import java.text.SimpleDateFormat

class SimpleImportExportConverters {
	private static DATE_FORMAT = "yyyy-mm-dd'T'HH:mm:ssZZ"

	protected static importConverters = [
			(java.util.Date): {value->
			    def dateConv = new SimpleDateFormat(DATE_FORMAT);
			    try {
			    	return dateConv.parse(value)
			    } catch (java.text.ParseException pe) {
			    	// Convert using legacy format
					dateConv = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy", Locale.UK)			    	
				    try {
				    	return dateConv.parse(value)
				    } catch (java.text.ParseException pe2) {
				    	log.warn "Could not parse date value [${value}], default to current date/time"
				    	return new Date()
				    }
			    }
			},
			(java.lang.Float): {value->
			   value.toFloat()},
			(java.lang.Double): {value->
			   value.toDouble()},
			(java.lang.Number): {value->
			   value.toInteger()},
			(java.lang.String): {value -> value},
			(java.lang.Boolean): {value -> value.toBoolean()},
			(org.weceem.content.WcmStatus): {value-> WcmStatus.findByCode(value)}
	]
	protected static def exportConverters = [
		(java.util.Date): {value ->
			 def dateConv = new SimpleDateFormat(DATE_FORMAT);
			 dateConv.format(value)
		},
		(java.lang.Long): {value -> value},
		(java.lang.Integer): {value-> value},
		(java.lang.Short): {value-> value},
		(java.lang.String): {value -> value},
		(org.weceem.content.WcmStatus): {value -> value.code}
	]
}
