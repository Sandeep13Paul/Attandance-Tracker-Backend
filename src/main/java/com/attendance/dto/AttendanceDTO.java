package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDTO {
    private Long userId;
    private String userName;
    private LocalDate date;
    private boolean present;
    private String subjectName;  // ✅ ADD

    public AttendanceDTO(Long userId, String userName, LocalDate date, boolean present) {
        this.userId = userId;
        this.userName = userName;
        this.date = date;
        this.present = present;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    @Override
    public String toString() {
        return "AttendanceDTO{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", date=" + date +
                ", present=" + present +
                ", subjectName='" + subjectName + '\'' +
                '}';
    }
}
