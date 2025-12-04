package ma.projet.grpc2.repositories;

import ma.projet.grpc2.entities.Compte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompteRepository extends JpaRepository<Compte, Long> {
}
