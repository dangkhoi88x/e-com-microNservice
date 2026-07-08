package com.example.searchservice.dto.request;

public record SearchSort(String field, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }
}
