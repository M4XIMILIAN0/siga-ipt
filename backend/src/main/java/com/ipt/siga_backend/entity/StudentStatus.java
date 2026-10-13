package com.ipt.siga_backend.entity;

public enum StudentStatus {
    REGULAR,
    POSTPONED,
    TEMPORARY_WITHDRAWAL,
    GRADUATED,     // only set automatically at period closing (RN-1.7)
    ELIMINATED     // only set automatically at period closing (RN-1.7)
}