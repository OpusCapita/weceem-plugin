package org.weceem.binding

import org.apache.commons.lang.StringUtils
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.codehaus.groovy.grails.web.binding.StructuredPropertyEditor
import org.springframework.beans.PropertyAccessException

import java.text.DateFormat
import java.text.SimpleDateFormat;

/**
 * Variant of DateAndTimeDateEditor supporting a variety of date formats.
 * 
 * Note: I don't consider this implementation good practice, but the html5 implementations
 * of how clients send date formats are really weird.
 */
public class ExtendedDateAndTimeDateEditor extends CustomDateEditor implements StructuredPropertyEditor {

	ArrayList<DateFormat> formats;

	static def dateFormats = [
			"yyyy-MM-dd",
			"yy-MM-dd",
			"yy/MM/dd",
			"yyyy/MM/dd",
			"dd.MM.yy",
			"dd.MM.yyyy",
			"dd/MM/yyyy"
		];
	
	static def timeFormats = [
			"HH:mm:ss.S",
			"HH:mm:ss",
			"HH:mm",
			""
		];
	
	static def dividers = [
		" ",
		"'T'"
	];

	static def tails = [
		"",
		"Z"
	];

	ExtendedDateAndTimeDateEditor(boolean b) {
		super(createDefaultFormats(true)[0], b);
		formats = createDefaultFormats();
	}

	ExtendedDateAndTimeDateEditor(DateFormat dateFormat, boolean b) {
		super(dateFormat, b);
		formats = createDefaultFormats();
	}

	ExtendedDateAndTimeDateEditor(DateFormat dateFormat, boolean b, int i) {
		super(dateFormat, b, i);
		formats = createDefaultFormats();
	}

	/**
	 * Creates the default formats and an returns the first on serving as default. 
	 * @return
	 */
	private static List<DateFormat> createDefaultFormats(boolean onlyFirst = false) {

		ArrayList<DateFormat> result = new ArrayList<DateFormat>(dateFormats.size()* timeFormats.size() * dividers.size() * tails.size());

		OUTER: for (def d : dateFormats ) {
			for (def t : timeFormats ) {
				if (t) {
					for (def di : dividers ) {
						for (def ta : tails ) {
							result.add(new SimpleDateFormat(d+di+t+ta));
							if (onlyFirst) {
								break OUTER
							}
						}
					}
				}
				else {
					result.add(new SimpleDateFormat(d));
					if (onlyFirst) {
						break OUTER
					}
				}
			}
		}

		return result;
	}


	public List getRequiredFields() {
		return Collections.EMPTY_LIST;
	}

	public List getOptionalFields() {
		['date', 'time', 'hour', 'minute']
	}

	public Object assemble(Class type, Map fieldValues) {
		
		def d = fieldValues.date
		    d = d?.trim()
		def t = fieldValues.time
		    t = t?.trim()
			
		Date dateDate = null;
		if (d) {
			// Get the date part
			dateDate = parseDate(t ? (d + ' ' + t) : d)
		}
		
		// time has been provided
		if (t) {
			return dateDate;
		}
		
		// no time provided, so there are hours and minutes separately
		Integer hour = getIntegerValue(fieldValues, "hour", 0);
		Integer minute = getIntegerValue(fieldValues, "minute", 0);
       
		if (dateDate && (minute != null) && (hour != null)) {
			Calendar dateCal = new GregorianCalendar()
			dateCal.time = dateDate
			Calendar c = new GregorianCalendar(
					dateCal.get(Calendar.YEAR),
					dateCal.get(Calendar.MONTH),
					dateCal.get(Calendar.DAY_OF_MONTH),
					hour,minute);
			if(type == Date.class) {
				return c.getTime();
			} else if(type == java.sql.Date.class) {
				return new java.sql.Date(c.getTime().getTime());
			}
			return c;
		}
	}

	private Date parseDate(String d) {
		for(DateFormat df : formats) {
			try {
		            //System.out.println("Try parsing with " + ((SimpleDateFormat) df).toPattern() + " ...");
			    return df.parse(d)
			} 
			catch (Exception e) {
				//System.out.println("... failed.");
				continue
			}
		}
		throw new StructuredBindingException("Date not parsable: " + d)
	}
	
	private getIntegerValue(Map values, String name, int defaultValue) {
		def v = values.get(name)
		if (v != null) {
			v = v.toString().trim()
			if (v.isInteger()) {
				return Integer.parseInt(v);
			} else {
				return null
			}
		}
		return defaultValue;
	}
}