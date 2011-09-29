package org.weceem.binding

import org.apache.commons.lang.StringUtils
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.codehaus.groovy.grails.web.binding.StructuredPropertyEditor
import org.springframework.beans.PropertyAccessException

import java.text.DateFormat
import java.util.*

/**
 * Structured editor for editing dates that takes 1 date field and two for hour and minute
 * and constructs a Date instance
 *
 * Adapted from code by Graeme Rocher in Grails coreStructuredDateEditor
 */
public class DateAndTimeDateEditor extends CustomDateEditor implements StructuredPropertyEditor {

    DateAndTimeDateEditor(DateFormat dateFormat, boolean b) {
        super(dateFormat, b);
    }

    DateAndTimeDateEditor(DateFormat dateFormat, boolean b, int i) {
        super(dateFormat, b, i);
    }

    public List getRequiredFields() {
        return Collections.EMPTY_LIST;
    }

    public List getOptionalFields() {
        ['date', 'hour', 'minute']
    }

    public Object assemble(Class type, Map fieldValues) {
        def d = fieldValues.date

        Integer hour = getIntegerValue(fieldValues, "hour", 0);
        Integer minute = getIntegerValue(fieldValues, "minute", 0);

        if (d?.trim() && (minute != null) && (hour != null)) {
            // Get the date part
            def dateDate = Date.parse('yyyy/MM/dd', d)
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
        } else if (d?.trim()) {
            throw new StructuredBindingException("You must provide values for all parts of the date and time or none at all")
        }
        
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

class StructuredBindingException extends RuntimeException {
    StructuredBindingException(str) {
        super(str)
    }
}