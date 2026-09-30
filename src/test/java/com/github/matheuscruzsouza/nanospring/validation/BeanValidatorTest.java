package com.github.matheuscruzsouza.nanospring.validation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeanValidatorTest {

    public static class TestUser {
        @NotNull
        @NotBlank
        private String name;

        @Email
        private String email;

        @Min(18)
        @Max(120)
        private int age;

        @Size(min = 2, max = 5)
        private List<String> tags;

        @Pattern(regexp = "^[0-9]{5}-[0-9]{3}$")
        private String cep;

        @Valid
        private Address address;

        public TestUser(String name, String email, int age, List<String> tags, String cep, Address address) {
            this.name = name;
            this.email = email;
            this.age = age;
            this.tags = tags;
            this.cep = cep;
            this.address = address;
        }
    }

    public static class Address {
        @NotBlank
        private String street;

        public Address(String street) {
            this.street = street;
        }
    }

    @Test
    public void testValidUserPasses() {
        TestUser user = new TestUser(
                "Matheus",
                "matheus@example.com",
                25,
                Arrays.asList("dev", "android"),
                "12345-678",
                new Address("Rua Principal")
        );

        ValidationResult result = BeanValidator.validate(user);
        assertTrue("Expected user to be valid", result.isValid());
        assertEquals(0, result.getViolations().size());
    }

    @Test
    public void testNullAndBlankViolations() {
        TestUser user = new TestUser(
                "   ",
                "matheus@example.com",
                25,
                Arrays.asList("a", "b"),
                "12345-678",
                null
        );

        ValidationResult result = BeanValidator.validate(user);
        assertFalse("Expected violations for blank name", result.isValid());
        boolean hasBlank = false;
        for (ConstraintViolation cv : result.getViolations()) {
            if ("name".equals(cv.getField())) {
                hasBlank = true;
            }
        }
        assertTrue("Should report violation on 'name'", hasBlank);
    }

    @Test
    public void testEmailAndMinMaxViolations() {
        TestUser user = new TestUser(
                "Valid Name",
                "invalid-email-format",
                15, // below min 18
                Arrays.asList("tag1", "tag2"),
                "12345-678",
                null
        );

        ValidationResult result = BeanValidator.validate(user);
        assertFalse(result.isValid());
        assertEquals(2, result.getViolations().size());

        Map<String, Object> errorMap = result.toErrorMap();
        assertEquals(400, errorMap.get("status"));
        assertEquals("Bad Request", errorMap.get("error"));
        assertTrue(errorMap.containsKey("errors"));
    }

    @Test
    public void testNestedAddressViolation() {
        TestUser user = new TestUser(
                "Valid Name",
                "valid@test.com",
                30,
                Arrays.asList("t1", "t2"),
                "12345-678",
                new Address("") // blank street
        );

        ValidationResult result = BeanValidator.validate(user);
        assertFalse(result.isValid());
        boolean hasNested = false;
        for (ConstraintViolation cv : result.getViolations()) {
            if ("address.street".equals(cv.getField())) {
                hasNested = true;
            }
        }
        assertTrue("Should report violation on 'address.street'", hasNested);
    }
}
