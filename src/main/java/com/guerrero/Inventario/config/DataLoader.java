package com.guerrero.Inventario.config;

import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Rol;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.CategoriaRepository;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    CommandLineRunner cargarDatosIniciales(CategoriaRepository categoriaRepository,
                                           UsuarioRepository usuarioRepository,
                                           PasswordEncoder passwordEncoder,
                                           @Value("${app.admin.default-password:}") String adminPasswordConfigurada) {
        return args -> {
            // Categorias base
            List<String[]> base = List.of(
                    new String[]{"PAPELERIA",   "Articulos de oficina y escolares"},
                    new String[]{"ELECTRONICA", "Aparatos y accesorios electronicos"},
                    new String[]{"RECARGAS",    "Recargas telefonicas y servicios prepago"},
                    new String[]{"JUGUETES",    "Juguetes y articulos infantiles"}
            );
            base.forEach(par -> {
                if (!categoriaRepository.existsByNombreIgnoreCase(par[0])) {
                    Categoria c = new Categoria();
                    c.setNombre(par[0]);
                    c.setDescripcion(par[1]);
                    categoriaRepository.save(c);
                    log.info("Categoria creada: {}", par[0]);
                }
            });

            // Usuario admin por defecto
            if (!usuarioRepository.existsByUsername("admin")) {
                boolean passwordGenerada = adminPasswordConfigurada == null || adminPasswordConfigurada.isBlank();
                String passwordFinal = passwordGenerada ? generarPasswordAleatoria() : adminPasswordConfigurada;

                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setUsername("admin");
                admin.setEmail("admin@papeleria.com");
                admin.setPassword(passwordEncoder.encode(passwordFinal));
                admin.setRol(Rol.ADMIN);
                admin.setActivo(Boolean.TRUE);
                usuarioRepository.save(admin);

                if (passwordGenerada) {
                    log.warn("Usuario admin creado con contraseña generada automaticamente: {} " +
                            "-- Guardela y cambiela de inmediato; no volvera a mostrarse.", passwordFinal);
                } else {
                    log.info("Usuario admin creado con la contraseña definida en app.admin.default-password");
                }
            }
        };
    }

    private static String generarPasswordAleatoria() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
