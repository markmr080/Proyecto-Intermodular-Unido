package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;

    // Inyectamos la URL desde el archivo de propiedades
    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void enviarCorreoBienvenida(String destinatario, String nombreUsuario) {
        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setTo(destinatario);
        mensaje.setSubject(
                "Bienvenido a Hundir la flota de warhammer, no sabemos por que las ratas estan aqui, pero lo estan.");
        mensaje.setText("Hola " + nombreUsuario + ", ¡tu registro ha sido un éxito!");

        mailSender.send(mensaje);
    }

    @Override
    @Async
    public void enviarCorreoRecuperacion(String destinatario, String token) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - Warhammer Battleship");

            // USAMOS LA VARIABLE EN LUGAR DE LOCALHOST
            String urlRecuperacion = frontendUrl + "/reset-password?token=" + token;

            String htmlBody = "<p>Has solicitado restablecer tu contraseña.</p>"
                    + "<p><a href=\"" + urlRecuperacion
                    + "\" style=\"display: inline-block; padding: 10px 20px; color: white; background-color: #a67b4b; text-decoration: none; border-radius: 5px;\">"
                    + "Haz click aquí para restablecerla"
                    + "</a></p>"
                    + "<p>Si no has sido tú, ignora este mensaje.</p>";

            helper.setText(htmlBody, true);
            mailSender.send(mensaje);

        } catch (MessagingException e) {
            // ... (el fallback también debería usar frontendUrl)
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setTo(destinatario);
            fallback.setSubject("Recuperación de contraseña - Warhammer Battleship");
            fallback.setText("Has solicitado restablecer tu contraseña.\n\n" +
                    "Enlace: " + frontendUrl + "/reset-password?token=" + token);
            mailSender.send(fallback);
        }
    }
}
