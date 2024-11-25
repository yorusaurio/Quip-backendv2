package com.example.quips.analytics.controller;

import com.example.quips.analytics.dto.UserActivityDTO;
import com.example.quips.analytics.dto.UserReferralDTO;
import com.example.quips.analytics.service.*;
import com.example.quips.authentication.domain.model.User;
import com.example.quips.authentication.dto.LoginRequest;
import com.example.quips.authentication.dto.UserDTO;
import com.example.quips.authentication.repository.UserRepository;
import com.example.quips.shared.util.JwtUtil;
import com.example.quips.transaction.dto.UserTransactionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "https://quips.netlify.app")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CycleAnalyticsService cycleAnalyticsService;
    @Autowired
    private TokenAnalyticsService tokenAnalyticsService;
    @Autowired
    private TransactionGraphService transactionGraphService;
    @Autowired
    private TopUsersTransactionService topUsersTransactionService;
    @Autowired
    private ReferralRewardsService referralRewardsService;
    @Autowired
    private PhaseTimelineService phaseTimelineService;
    @Autowired
    private TransactionActivityService transactionActivityService;

    @Autowired
    private UserActivityService userActivityService;

    @GetMapping("/cycle-status")
    public ResponseEntity<?> getCycleStatus() {
        return ResponseEntity.ok(cycleAnalyticsService.getCycleStatus());
    }

    @GetMapping("/token-status")
    public ResponseEntity<?> getTokenStatus() {
        return ResponseEntity.ok(tokenAnalyticsService.getTokenStatus());
    }

    @GetMapping("/transaction-graph")
    public ResponseEntity<?> getTransactionGraph() {
        return ResponseEntity.ok(transactionGraphService.getTransactionGraph());
    }

    @GetMapping("/user-activity/{userId}")
    public ResponseEntity<UserActivityDTO> getUserActivity(@PathVariable Long userId) {
        UserActivityDTO activity = userActivityService.calculateUserActivity(userId);
        return ResponseEntity.ok(activity);
    }

    @GetMapping("/user-activity")
    public ResponseEntity<List<UserActivityDTO>> getAllUsersActivity() {
        List<UserActivityDTO> activities = userActivityService.calculateAllUsersActivity();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/top-users-transactions")
    public ResponseEntity<?> getTopUsersByTransactions() {
        return ResponseEntity.ok(topUsersTransactionService.getTopUsersByTransactions());
    }

    // Nuevo endpoint para obtener los usuarios que más han enviado transacciones
    @GetMapping("/top-senders")
    public ResponseEntity<List<UserTransactionDTO>> getTopSenders() {
        return ResponseEntity.ok(topUsersTransactionService.getTopSenders());
    }

    @GetMapping("/top-receivers")
    public ResponseEntity<List<UserTransactionDTO>> getTopReceivers() {
        return ResponseEntity.ok(topUsersTransactionService.getTopReceivers());
    }
    @GetMapping("/top-users-referrals")
    public ResponseEntity<List<UserReferralDTO>> getTopUsersByReferrals() {
        List<UserReferralDTO> topUsers = referralRewardsService.getTopUsersByReferrals();
        return ResponseEntity.ok(topUsers);
    }

    @GetMapping("/phase-timeline")
    public ResponseEntity<?> getPhaseTimeline() {
        return ResponseEntity.ok(phaseTimelineService.getPhaseTimeline());
    }

    @GetMapping("/transaction-activity")
    public ResponseEntity<?> getTransactionActivityByHour() {
        return ResponseEntity.ok(transactionActivityService.getTransactionActivityByHour());
    }

    // Endpoint de login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername())); // Buscar por username o email

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!user.isActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cuenta no activada.");
            }

            // Verificar la contraseña o el PIN, con chequeo de null para sixDigitPin
            if (user.getPassword().equals(request.getPassword()) ||
                    (user.getSixDigitPin() != null && user.getSixDigitPin().equals(request.getPassword()))) {
                String token = jwtUtil.generateToken(user.getUsername());
                return ResponseEntity.ok(Map.of("token", token));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Contraseña o clave de 6 dígitos incorrecta.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
        }
    }



    @GetMapping("/me")
    public ResponseEntity<?> getMyUserInfo(@RequestHeader("Authorization") String token) {
        try {
            // Extraer el nombre de usuario del token
            String username = jwtUtil.getUsernameFromToken(token.replace("Bearer ", ""));

            // Buscar al usuario por nombre de usuario
            Optional<User> user = userRepository.findByUsername(username);

            // Retornar los datos del usuario si es encontrado
            if (user.isPresent()) {
                User foundUser = user.get();

                // Obtener los roles del usuario (si los tienes implementados)
                Set<String> roles = foundUser.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet());

                // Obtener los coins de la wallet
                double coins = (foundUser.getWallet() != null) ? foundUser.getWallet().getCoins() : 0.0;

                // Crear el DTO con la información del usuario, incluyendo las monedas
                UserDTO userDTO = new UserDTO(
                        foundUser.getId(),  // Agregar el ID aquí
                        foundUser.getUsername(),
                        foundUser.getSixDigitPin(),
                        foundUser.getFirstName(),
                        foundUser.getLastName(),
                        foundUser.getEmail(),
                        foundUser.getPhoneNumber(),
                        foundUser.getAccountNumber(),
                        foundUser.getReferralCode(),


                        roles,
                        coins,
                        foundUser.isActive()  // Pasar las monedas desde la wallet del usuario
                );

                return ResponseEntity.ok(userDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token no válido o expirado.");
        }
    }


}
