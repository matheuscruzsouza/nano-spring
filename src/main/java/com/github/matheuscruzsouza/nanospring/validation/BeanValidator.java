package com.github.matheuscruzsouza.nanospring.validation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class BeanValidator {

    private static final java.util.regex.Pattern DEFAULT_EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    public static ValidationResult validate(Object object) {
        ValidationResult result = new ValidationResult();
        if (object == null) {
            return result;
        }
        validateInternal(object, "", result);
        return result;
    }

    private static void validateInternal(Object target, String fieldPrefix, ValidationResult result) {
        if (target == null) return;

        Class<?> currentClass = target.getClass();
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = fieldPrefix.isEmpty() ? field.getName() : fieldPrefix + "." + field.getName();

                Object value = null;
                try {
                    value = field.get(target);
                } catch (Exception ignored) {
                }

                // @NotNull
                if (field.isAnnotationPresent(NotNull.class) && value == null) {
                    NotNull ann = field.getAnnotation(NotNull.class);
                    result.addViolation(new ConstraintViolation(fieldName, ann.message(), null));
                }

                // @NotBlank
                if (field.isAnnotationPresent(NotBlank.class)) {
                    NotBlank ann = field.getAnnotation(NotBlank.class);
                    if (value == null || value.toString().trim().isEmpty()) {
                        result.addViolation(new ConstraintViolation(fieldName, ann.message(), value));
                    }
                }

                // @NotEmpty
                if (field.isAnnotationPresent(NotEmpty.class)) {
                    NotEmpty ann = field.getAnnotation(NotEmpty.class);
                    if (isEmpty(value)) {
                        result.addViolation(new ConstraintViolation(fieldName, ann.message(), value));
                    }
                }

                // @Size
                if (field.isAnnotationPresent(Size.class) && value != null) {
                    Size ann = field.getAnnotation(Size.class);
                    int size = computeSize(value);
                    if (size < ann.min() || size > ann.max()) {
                        String msg = ann.message()
                                .replace("{min}", String.valueOf(ann.min()))
                                .replace("{max}", String.valueOf(ann.max()));
                        result.addViolation(new ConstraintViolation(fieldName, msg, value));
                    }
                }

                // @Min
                if (field.isAnnotationPresent(Min.class) && value != null) {
                    Min ann = field.getAnnotation(Min.class);
                    if (value instanceof Number && ((Number) value).longValue() < ann.value()) {
                        String msg = ann.message().replace("{value}", String.valueOf(ann.value()));
                        result.addViolation(new ConstraintViolation(fieldName, msg, value));
                    }
                }

                // @Max
                if (field.isAnnotationPresent(Max.class) && value != null) {
                    Max ann = field.getAnnotation(Max.class);
                    if (value instanceof Number && ((Number) value).longValue() > ann.value()) {
                        String msg = ann.message().replace("{value}", String.valueOf(ann.value()));
                        result.addViolation(new ConstraintViolation(fieldName, msg, value));
                    }
                }

                // @Email
                if (field.isAnnotationPresent(Email.class) && value != null) {
                    Email ann = field.getAnnotation(Email.class);
                    String strVal = value.toString();
                    if (!strVal.isEmpty()) {
                        java.util.regex.Pattern p = DEFAULT_EMAIL_PATTERN;
                        if (!DEFAULT_EMAIL_PATTERN.pattern().equals(ann.regexp())) {
                            try {
                                p = java.util.regex.Pattern.compile(ann.regexp());
                            } catch (Exception ignored) {
                            }
                        }
                        if (!p.matcher(strVal).matches()) {
                            result.addViolation(new ConstraintViolation(fieldName, ann.message(), value));
                        }
                    }
                }

                // @Pattern
                if (field.isAnnotationPresent(com.github.matheuscruzsouza.nanospring.validation.Pattern.class) && value != null) {
                    com.github.matheuscruzsouza.nanospring.validation.Pattern ann =
                            field.getAnnotation(com.github.matheuscruzsouza.nanospring.validation.Pattern.class);
                    String strVal = value.toString();
                    try {
                        if (!java.util.regex.Pattern.compile(ann.regexp()).matcher(strVal).matches()) {
                            result.addViolation(new ConstraintViolation(fieldName, ann.message(), value));
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Nested @Valid
                if (field.isAnnotationPresent(Valid.class) && value != null) {
                    if (value instanceof Collection<?>) {
                        int idx = 0;
                        for (Object elem : (Collection<?>) value) {
                            validateInternal(elem, fieldName + "[" + idx + "]", result);
                            idx++;
                        }
                    } else {
                        validateInternal(value, fieldName, result);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof CharSequence) return ((CharSequence) value).length() == 0;
        if (value instanceof Collection<?>) return ((Collection<?>) value).isEmpty();
        if (value instanceof Map<?, ?>) return ((Map<?, ?>) value).isEmpty();
        if (value.getClass().isArray()) return Array.getLength(value) == 0;
        return false;
    }

    private static int computeSize(Object value) {
        if (value == null) return 0;
        if (value instanceof CharSequence) return ((CharSequence) value).length();
        if (value instanceof Collection<?>) return ((Collection<?>) value).size();
        if (value instanceof Map<?, ?>) return ((Map<?, ?>) value).size();
        if (value.getClass().isArray()) return Array.getLength(value);
        return 0;
    }
}
