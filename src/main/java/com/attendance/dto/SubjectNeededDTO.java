package com.attendance.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubjectNeededDTO {
    private String subject;
    private double percent;
    private int needed;

    @Override
    public String toString() {
        return "SubjectNeededDTO{" +
                "subject='" + subject + '\'' +
                ", percent=" + percent +
                ", needed=" + needed +
                '}';
    }
}
