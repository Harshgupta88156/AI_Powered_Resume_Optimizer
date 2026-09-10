package com.ai_resume.user_service.entity;

/**
 * Application roles. Persisted as a STRING so the DB stays readable and adding
 * a new role never shifts existing ordinals.
 *
 * <p>Kept deliberately small: fine-grained permissions (if ever needed) should
 * be modelled as authorities derived from the role, not as extra roles.
 */
public enum Role {
    USER,
    ADMIN
}

