package com.stream.app.spring_stream_backend.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "db_course")
public class Course {
    @Id
    private String id;
    private String title;
}
