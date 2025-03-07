package org.example.carebridge.domain.user.enums;


import lombok.Getter;

@Getter
public enum UserStatus {

    ACTIVE("active"),
    PENDING("pending"),
    DELETE("delete"),
    REPORT("report");

    private final String name;

    UserStatus(String name) {
        this.name = name;
    }


}
