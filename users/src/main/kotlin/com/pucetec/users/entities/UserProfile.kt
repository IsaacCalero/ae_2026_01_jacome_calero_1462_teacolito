package com.pucetec.users.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_profiles",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_user_profiles_sub", columnNames = ["sub"]),
        UniqueConstraint(name = "uq_user_profiles_display_name", columnNames = ["display_name"])
    ]
)
class UserProfile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var sub: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime
)
