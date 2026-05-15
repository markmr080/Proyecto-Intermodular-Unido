package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCorreoBienvenida(String destinatario, String nombreUsuario) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("Bienvenido a Hundir la flota de Warhammer");
        mensaje.setText("Hola " + nombreUsuario + ", ¡tu registro ha sido un éxito!\n\n"
                + "Ya puedes acceder a tu cuenta en: " + frontendUrl);
        mailSender.send(mensaje);
    }

    @Async
    public void enviarCorreoRecuperacion(String destinatario, String token) {
        String urlRecuperacion = frontendUrl + "/reset-password?token=" + token;

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Warhammer Battleship");

            String htmlBody = "<p>Has solicitado restablecer tu contraseña.</p>"
                    + "<p><a href=\"" + urlRecuperacion + "\""
                    + " style=\"display:inline-block;padding:10px 20px;color:white;"
                    + "background-color:#a67b4b;text-decoration:none;border-radius:5px;\">"
                    + "Haz click aquí para restablecerla</a></p>"
                    + "<p>Si no has sido tú, ignora este mensaje.</p>";

            helper.setText(htmlBody, true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            // Fallback a texto plano
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(destinatario);
            fallback.setSubject("Recuperación de contraseña - Warhammer Battleship");
            fallback.setText("Has solicitado restablecer tu contraseña.\n\nEnlace: " + urlRecuperacion);
            mailSender.send(fallback);
        }
    }
}
