// DateTextBox specific function, which formats date before it will be sent to the server
function serializeDate(d, options) {
    return dojo.date.locale.format(d, {selector:'date', datePattern:'yyyy-MM-dd'});
}
