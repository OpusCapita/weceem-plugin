package org.weceem.binding

import org.apache.commons.lang.StringUtils
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.codehaus.groovy.grails.web.binding.StructuredPropertyEditor

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

    public Object assemble(Class type, Map fieldValues) throws IllegalArgumentException {
        if (!fieldValues.containsKey("date") && (fieldValues.containsKey("hour") || fieldValues.containsKey("minute")) ) {
            throw new IllegalArgumentException("Can't populate a date and time without a date string");
        }

        def d = fieldValues.date

        int hour = getIntegerValue(fieldValues, "hour", 0);
        int minute = getIntegerValue(fieldValues, "minute", 0);

        // Get the date part
        def dateDate = Date.parse(d, 'yyyy/MM/dd')
        Calendar dateCal = new GregorianCalendar()
        dateCal.time = dateDate
        Calendar c = new GregorianCalendar(dateCal[Calendar.YEAR],dateCal[Calendar.MONTH],dateCal[Calendar.DAY_OF_MONTH],hour,minute);
        if(type == Date.class) {
            return c.getTime();
        } else if(type == java.sql.Date.class) {
            return new java.sql.Date(c.getTime().getTime());
        }
        return c;
    }

    private int getIntegerValue(Map values, String name, int defaultValue) throws NumberFormatException {
        if (values.get(name) != null) {
            return Integer.parseInt((String) values.get(name));
        }
        return defaultValue;
    }
}
