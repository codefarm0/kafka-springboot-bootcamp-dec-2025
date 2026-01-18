// Scripted Transform to parse JSON string value into JSON object
// After EventRouter, the value is already the JSON string payload
// This transform parses the string value and replaces it with the parsed JSON object

var ObjectMapper = Java.type("com.fasterxml.jackson.databind.ObjectMapper");
var objectMapper = new ObjectMapper();

function transform(record) {
    try {
        var value = record.value();
        if (value == null) {
            return record;
        }
        
        var valueStr = null;
        var className = value.getClass().getName();
        
        // After EventRouter, the value should be a JSON string
        if (className === "java.lang.String") {
            valueStr = String(value);
        } else if (className.contains("Struct")) {
            // If it's a Struct, try to get it as a string
            // This shouldn't happen after EventRouter, but handle it
            try {
                valueStr = String(value);
            } catch (e) {
                // If we can't convert, return original record
                return record;
            }
        } else {
            // Already an object (shouldn't happen, but return as-is)
            return record;
        }
        
        // Remove outer quotes if the string is double-quoted (escaped JSON string)
        if (valueStr != null && valueStr.length() >= 2 && 
            valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
            // Unescape the string - remove outer quotes and unescape internal quotes
            valueStr = valueStr.substring(1, valueStr.length() - 1);
            // Replace escaped quotes
            valueStr = valueStr.replace(/\\"/g, '"');
            // Replace escaped backslashes
            valueStr = valueStr.replace(/\\\\/g, '\\');
        }
        
        // Parse the JSON string into a JSON object
        var parsedValue = objectMapper.readTree(valueStr);
        
        // Replace the value with the parsed JSON object
        return record.newRecord(
            record.topic(),
            record.kafkaPartition(),
            record.keySchema(),
            record.key(),
            null, // schema will be inferred
            parsedValue,
            record.timestamp(),
            record.headers()
        );
    } catch (e) {
        // Log error and return original record
        print("Error parsing JSON value: " + e + ", value type: " + (value != null ? value.getClass().getName() : "null"));
        return record;
    }
}

