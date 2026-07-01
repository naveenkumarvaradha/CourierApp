package com.courierapp.dto.flexfield;

import java.util.Map;

/** Map of fieldId → raw value (String for text/single, comma-separated for multi). */
public record FlexFieldValueRequest(Map<Long, String> values) {}
