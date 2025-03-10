package schoolface.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import schoolface.backend.dto.UsuarioAutenticarDTO;
import schoolface.backend.dto.UsuarioDTO;
import schoolface.backend.entities.Usuario;
import schoolface.backend.security.AuthenticationService;
import schoolface.backend.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final AuthenticationService authService;
    private final UsuarioService usuarioService;

    public AuthenticationController(AuthenticationManager authenticationManager, AuthenticationService authService, UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid UsuarioAutenticarDTO loginDTO) {
        System.out.println("Tentativa de login para usuário: " + loginDTO.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getSenha())
        );
    
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        if (userDetails.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_DELETED"))) {
            System.out.println("Tentativa de login de usuário inativo: " + loginDTO.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuário inativo ou deletado");
        }
    
        String token = authService.authenticate(authentication);
        System.out.println("Token gerado para usuário: " + loginDTO.getEmail());
        
        // Salva o novo token no usuário
        Usuario usuario = usuarioService.buscarPorEmail(loginDTO.getEmail());
        if (usuario != null) {
            usuario.setTokenAtual(token);
            usuarioService.salvar(usuario);
            System.out.println("Token salvo para usuário: " + loginDTO.getEmail());
        }
    
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<Usuario> register(@RequestBody @Valid UsuarioDTO usuarioDTO) {
        Usuario novoUsuario = usuarioService.criar(usuarioDTO);
        return ResponseEntity.status(201).body(novoUsuario);
    }
}