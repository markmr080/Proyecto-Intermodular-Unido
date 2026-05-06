package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la tabla USUARIOS.
 * 
 * Campos renombrados respecto a la versión anterior:
 *   - nickname  → username  (columna: username)
 *   - password  → passwordHash  (columna: password_hash)
 * 
 * Se conservan 'role' y 'profilePicture' aunque no estén en el esquema SQL objetivo,
 * ya que son necesarios para la lógica de autenticación (middleware_admin) y el frontend.
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    private String role;

    @Column(name = "profile_picture", length = 512)
    private String profilePicture;

    public UsuarioEntity() {}

    public UsuarioEntity(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
