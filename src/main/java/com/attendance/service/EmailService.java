package com.attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendLowAttendanceAlert(String to, String subjectName, double percent) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("⚠️ Low Attendance Alert");

        msg.setText(
                "Your attendance in " + subjectName + " is low: " +
                        Math.round(percent) + "%.\nPlease improve it."
        );

        mailSender.send(msg);
    }
}