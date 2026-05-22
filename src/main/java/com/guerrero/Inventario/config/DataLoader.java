package com.guerrero.Inventario.config;

import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Rol;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.CategoriaRepository;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    CommandLineRunner cargarDatosIniciales(CategoriaRepository categoriaRepository,
                                           UsuarioRepository usuarioRepository,
                                           PasswordEncoder passwordEncoder) {
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
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setUsername("admin");
                admin.setEmail("admin@papeleria.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRol(Rol.ADMIN);
                admin.setActivo(Boolean.TRUE);
                usuarioRepository.save(admin);
                log.info("Usuario admin creado: admin / admin123 (cambialo en produccion)");
            }
        };
    }
}
