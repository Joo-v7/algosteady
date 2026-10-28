package com.algosteady.exercise.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/** 학습 목록 분류와 콘텐츠 분류 연결 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "exercise_category")
public class ExerciseCategory {

    @Id
    private String id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private int displayOrder;

    @ElementCollection
    @CollectionTable(name = "exercise_category_member", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "content_category", nullable = false)
    private Set<String> contentCategories;
}
