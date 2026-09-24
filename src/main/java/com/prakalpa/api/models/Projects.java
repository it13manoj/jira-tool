package com.prakalpa.api.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Table(name="projects")
public class Projects {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "project_key")
    private String projectKey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_type", nullable = false)
    private ProjectTypes projectTypes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_template", nullable = false)
    private Template template;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_leads", nullable = false)
    private Users leads;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "access_level")
    private boolean accessLevel;

    @Column(name = "status")
    private Long status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_categories", nullable = false)
    private Categories categories;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_stage", nullable = false)
    private Stages stages;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


}
