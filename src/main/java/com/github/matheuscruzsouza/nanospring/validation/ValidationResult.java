package com.github.matheuscruzsouza.nanospring.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValidationResult {
    private final List<ConstraintViolation> violations;

    public ValidationResult() {
        this.violations = new ArrayList<>();
    }

    public ValidationResult(List<ConstraintViolation> violations) {
        this.violations = violations != null ? violations : new ArrayList<ConstraintViolation>();
    }

    public void addViolation(ConstraintViolation violation) {
        this.violations.add(violation);
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public List<ConstraintViolation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public Map<String, Object> toErrorMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", 400);
        map.put("error", "Bad Request");
        map.put("message", "Validation failed. " + violations.size() + " error(s) found.");

        List<Map<String, Object>> errorList = new ArrayList<>();
        for (ConstraintViolation cv : violations) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("field", cv.getField());
            err.put("message", cv.getMessage());
            err.put("rejectedValue", cv.getRejectedValue());
            errorList.add(err);
        }
        map.put("errors", errorList);
        return map;
    }
}
