package com.cloudera.customerformhub.service;

final class QuestionDepartments {
    private QuestionDepartments() {}

    static boolean isAssigned(String department) {
        if (department == null || department.isBlank()) return false;
        String value = department.trim();
        return !value.equalsIgnoreCase("General") && !value.equalsIgnoreCase("TBD");
    }
}
